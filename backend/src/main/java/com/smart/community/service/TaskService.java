package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.community.common.BusinessException;
import com.smart.community.common.PageResult;
import com.smart.community.entity.Device;
import com.smart.community.entity.DeviceTask;
import com.smart.community.mapper.DeviceTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskService extends ServiceImpl<DeviceTaskMapper, DeviceTask> {

    private final DeviceService deviceService;

    public TaskService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 分页查询（status 动态判定逾期）
     */
    public PageResult<DeviceTask> page(int page, int size, String type, String status, String keyword, Long deviceId, LocalDate planDate) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        com.baomidou.mybatisplus.core.metadata.IPage<DeviceTask> p = baseMapper.selectTaskPage(
                new Page<>(page, size), type, status, keyword, deviceId, planDate, LocalDate.now());
        return PageResult.of(p);
    }

    /**
     * 刷新逾期状态：将已过期未执行的任务落库为 OVERDUE
     */
    public int refreshOverdue() {
        return baseMapper.markOverdue(LocalDate.now());
    }

    /**
     * 自动生成计划：按设备周期补齐未来 horizonDays 天的任务
     * 生成起点 = 该设备同类型最近任务 plan_date + cycle，若没有历史任务则从今天开始
     *
     * @return 新生成的任务数
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(String type, List<Long> deviceIds, int horizonDays, String executor) {
        List<Device> devices;
        if (deviceIds == null || deviceIds.isEmpty()) {
            devices = deviceService.listRunning();
        } else {
            devices = deviceService.listByIds(deviceIds);
        }
        if (devices.isEmpty()) {
            throw new BusinessException("没有可生成任务的设备");
        }
        AtomicInteger created = new AtomicInteger(0);
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(horizonDays);
        int seq = 0;
        for (Device d : devices) {
            int cycle = "MAINTAIN".equals(type) ? d.getMaintainCycle() : d.getInspectCycle();
            DeviceTask latest = baseMapper.selectLatest(d.getId(), type);
            LocalDate cursor;
            if (latest != null) {
                cursor = latest.getPlanDate().plusDays(cycle);
                if (cursor.isBefore(today)) {
                    cursor = today;
                }
            } else {
                cursor = today;
            }
            while (!cursor.isAfter(horizon)) {
                if (baseMapper.countByDate(d.getId(), type, cursor) == 0) {
                    DeviceTask task = new DeviceTask();
                    task.setTaskType(type);
                    task.setDeviceId(d.getId());
                    task.setPlanDate(cursor);
                    task.setStatus("PENDING");
                    task.setExecutor(executor);
                    task.setTaskCode(buildTaskCode(type, cursor, seq++));
                    save(task);
                    created.incrementAndGet();
                }
                cursor = cursor.plusDays(cycle);
            }
        }
        return created.get();
    }

    private String buildTaskCode(String type, LocalDate date, int seq) {
        String prefix = "MAINTAIN".equals(type) ? "BY" : "XJ";
        return prefix + "-" + date.format(CODE_DAY) + "-" + String.format("%03d", seq % 1000);
    }

    /**
     * 移动端打卡：按任务ID完成
     * 执行人取当前登录用户（防止伪造他人打卡）
     */
    public DeviceTask check(Long taskId, String result, String remark, String location, String checkItems, String executor) {
        DeviceTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("该任务已完成，请勿重复打卡");
        }
        // 归属校验：任务已指定执行人时，仅本人（或管理员）可完成，防止替他人打卡
        String currentName = currentUserName();
        String currentRole = currentUserRole();
        if (StringUtils.hasText(task.getExecutor())
                && !task.getExecutor().equals(currentName)
                && !"ADMIN".equals(currentRole)) {
            throw new BusinessException("该任务已分配给 " + task.getExecutor() + "，无权代打卡");
        }
        // 执行人优先取登录用户，其次才用请求参数（兼容历史调用）
        task.setStatus("COMPLETED");
        task.setResult(StringUtils.hasText(result) ? result : "NORMAL");
        task.setRemark(remark);
        task.setLocation(location);
        task.setCheckItems(checkItems);
        task.setExecutor(StringUtils.hasText(currentName) ? currentName : executor);
        task.setCheckTime(java.time.LocalDateTime.now());
        updateById(task);
        return task;
    }

    /**
     * 当前登录用户姓名（无登录态时返回 null）
     */
    private String currentUserName() {
        com.smart.community.common.UserContext.LoginUser user = com.smart.community.common.UserContext.get();
        return user == null ? null : user.getRealName();
    }

    /**
     * 当前登录用户角色
     */
    private String currentUserRole() {
        com.smart.community.common.UserContext.LoginUser user = com.smart.community.common.UserContext.get();
        return user == null ? null : user.getRole();
    }

    /**
     * 移动端扫码打卡：按设备编号找到该设备待执行任务并完成；
     * 若当天没有任务，则自动生成一条当日任务并立即完成（快速巡检）
     */
    @Transactional(rollbackFor = Exception.class)
    public DeviceTask checkByDeviceCode(String deviceCode, String result, String remark, String location, String checkItems, String executor) {
        Device device = deviceService.getByCode(deviceCode);
        if (device == null) {
            throw new BusinessException("设备不存在，请核对二维码");
        }
        LocalDate today = LocalDate.now();
        DeviceTask pending = getOne(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getDeviceId, device.getId())
                .eq(DeviceTask::getTaskType, "INSPECT")
                .eq(DeviceTask::getStatus, "PENDING")
                .le(DeviceTask::getPlanDate, today)
                .orderByAsc(DeviceTask::getPlanDate)
                .last("LIMIT 1"));
        if (pending == null) {
            // 自动生成当日任务并完成
            DeviceTask task = new DeviceTask();
            task.setTaskType("INSPECT");
            task.setDeviceId(device.getId());
            task.setPlanDate(today);
            task.setStatus("PENDING");
            task.setExecutor(executor);
            task.setTaskCode(buildTaskCode("INSPECT", today, (int) (System.currentTimeMillis() % 1000)));
            save(task);
            pending = task;
        }
        return check(pending.getId(), result, remark, location, checkItems, executor);
    }

    /**
     * 今日任务统计
     */
    public java.util.Map<String, Object> todayStats() {
        LocalDate today = LocalDate.now();
        long total = count(new LambdaQueryWrapper<DeviceTask>().eq(DeviceTask::getPlanDate, today));
        long done = count(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getPlanDate, today).eq(DeviceTask::getStatus, "COMPLETED"));
        long overdue = baseMapper.markOverdue(today);
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("todayTotal", total);
        m.put("todayDone", done);
        m.put("todayPending", Math.max(0, total - done));
        m.put("overdueCount", count(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getStatus, "OVERDUE")));
        m.put("pendingCount", count(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getStatus, "PENDING").ge(DeviceTask::getPlanDate, today)));
        return m;
    }

    /**
     * 设备待办任务（移动端我的任务）
     */
    public List<DeviceTask> myTasks(String type, String executor) {
        List<DeviceTask> tasks = list(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getTaskType, type == null ? "INSPECT" : type)
                // 显示“分配给本人”或“未分配”的待办（未分配任务谁都可领）
                .and(w -> w.eq(DeviceTask::getExecutor, executor)
                        .or().isNull(DeviceTask::getExecutor))
                .in(DeviceTask::getStatus, "PENDING", "OVERDUE")
                .orderByAsc(DeviceTask::getPlanDate));
        fillDeviceInfo(tasks);
        return tasks;
    }

    /**
     * 某设备最近的任务记录
     */
    public List<DeviceTask> listByDevice(Long deviceId, int limit) {
        List<DeviceTask> tasks = list(new LambdaQueryWrapper<DeviceTask>()
                .eq(DeviceTask::getDeviceId, deviceId)
                .orderByDesc(DeviceTask::getPlanDate)
                .last("LIMIT " + limit));
        fillDeviceInfo(tasks);
        return tasks;
    }

    private void fillDeviceInfo(List<DeviceTask> tasks) {
        for (DeviceTask t : tasks) {
            Device d = deviceService.getById(t.getDeviceId());
            if (d != null) {
                t.setDeviceName(d.getName());
                t.setDeviceCode(d.getDeviceCode());
                t.setDeviceLocation(d.getLocation());
            }
        }
    }

    public List<DeviceTask> recentByDevice(Long deviceId, int limit) {
        return listByDevice(deviceId, limit);
    }
}
