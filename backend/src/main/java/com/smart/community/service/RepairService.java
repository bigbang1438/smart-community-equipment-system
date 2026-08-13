package com.smart.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.community.common.BusinessException;
import com.smart.community.common.PageResult;
import com.smart.community.entity.Device;
import com.smart.community.entity.RepairOrder;
import com.smart.community.mapper.RepairOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RepairService extends ServiceImpl<RepairOrderMapper, RepairOrder> {

    private final DeviceService deviceService;

    public RepairService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public PageResult<RepairOrder> page(int page, int size, String status, String level, String keyword, Long deviceId) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        return PageResult.of(baseMapper.selectOrderPage(new Page<>(page, size), status, level, deviceId, keyword));
    }

    /**
     * 业主/员工报修
     */
    public RepairOrder create(RepairOrder order) {
        Device device = deviceService.getById(order.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!StringUtils.hasText(order.getFaultDesc())) {
            throw new BusinessException("故障描述不能为空");
        }
        order.setStatus("PENDING");
        order.setOrderCode(buildOrderCode(new AtomicInteger((int) (System.currentTimeMillis() % 9000))));
        save(order);
        return order;
    }

    /**
     * 派单
     */
    @Transactional(rollbackFor = Exception.class)
    public RepairOrder assign(Long id, String assignee) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可派单");
        }
        if (!StringUtils.hasText(assignee)) {
            throw new BusinessException("请选择维修人员");
        }
        order.setAssignee(assignee);
        order.setStatus("PROCESSING");
        order.setAssignTime(LocalDateTime.now());
        updateById(order);

        // 派单联动设备状态：报修受理 -> 设备进入维修中（完成后自动恢复运行）
        Device device = deviceService.getById(order.getDeviceId());
        if (device != null && !"STOPPED".equals(device.getStatus()) && !"SCRAPPED".equals(device.getStatus())) {
            device.setStatus("REPAIRING");
            deviceService.updateById(device);
        }
        return order;
    }

    /**
     * 维修完成（联动设备状态，事务保证一致）
     */
    @Transactional(rollbackFor = Exception.class)
    public RepairOrder finish(Long id, String fixResult, BigDecimal cost, BigDecimal fixHours) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        if (!"PROCESSING".equals(order.getStatus())) {
            throw new BusinessException("仅维修中的工单可提交完成");
        }
        order.setFixResult(fixResult);
        order.setCost(cost == null ? BigDecimal.ZERO : cost);
        order.setFixHours(fixHours);
        order.setStatus("COMPLETED");
        order.setFinishTime(LocalDateTime.now());
        updateById(order);

        // 维修完成联动设备状态：故障设备 -> 运行中
        Device device = deviceService.getById(order.getDeviceId());
        if (device != null && ("FAULT".equals(device.getStatus()) || "REPAIRING".equals(device.getStatus()))) {
            device.setStatus("RUNNING");
            deviceService.updateById(device);
        }
        return order;
    }

    /**
     * 验收
     */
    @Transactional(rollbackFor = Exception.class)
    public RepairOrder verify(Long id) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("仅待验收的工单可验收");
        }
        order.setStatus("VERIFIED");
        order.setVerifyTime(LocalDateTime.now());
        updateById(order);
        return order;
    }

    /**
     * 各状态工单统计（供列表页统计条使用）
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("PENDING", 0L);
        result.put("PROCESSING", 0L);
        result.put("COMPLETED", 0L);
        result.put("VERIFIED", 0L);
        for (Map<String, Object> row : baseMapper.selectStatusCount()) {
            result.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    private String buildOrderCode(AtomicInteger seq) {
        return "GZ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", seq.incrementAndGet());
    }
}
