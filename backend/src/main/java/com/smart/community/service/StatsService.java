package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.community.entity.Device;
import com.smart.community.entity.DeviceTask;
import com.smart.community.mapper.DeviceTaskMapper;
import com.smart.community.mapper.RepairOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计分析
 */
@Service
public class StatsService {

    private final DeviceService deviceService;
    private final TaskService taskService;
    private final ContractService contractService;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceTaskMapper deviceTaskMapper;

    public StatsService(DeviceService deviceService,
                        TaskService taskService,
                        ContractService contractService,
                        RepairOrderMapper repairOrderMapper,
                        DeviceTaskMapper deviceTaskMapper) {
        this.deviceService = deviceService;
        this.taskService = taskService;
        this.contractService = contractService;
        this.repairOrderMapper = repairOrderMapper;
        this.deviceTaskMapper = deviceTaskMapper;
    }

    /**
     * 总览卡片数据
     */
    public Map<String, Object> overview() {
        long total = deviceService.count();
        long running = deviceService.count(new LambdaQueryWrapper<Device>().eq(Device::getStatus, "RUNNING"));
        long fault = deviceService.count(new LambdaQueryWrapper<Device>()
                .in(Device::getStatus, "FAULT", "REPAIRING"));
        long stopped = deviceService.count(new LambdaQueryWrapper<Device>()
                .in(Device::getStatus, "STOPPED", "SCRAPPED"));
        Map<String, Object> today = taskService.todayStats();
        long pendingOrders = repairOrderMapper.selectCount(new LambdaQueryWrapper<com.smart.community.entity.RepairOrder>()
                .in(com.smart.community.entity.RepairOrder::getStatus, "PENDING", "PROCESSING"));
        long expiring = contractService.countExpiringOrExpired();
        long monthCost = repairOrderMapper.selectCostTrend(1).stream()
                .mapToLong(m -> ((Number) m.get("amount")).longValue()).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceTotal", total);
        result.put("deviceRunning", running);
        result.put("deviceFault", fault);
        result.put("deviceStopped", stopped);
        result.put("runningRate", total == 0 ? 0 : Math.round(running * 1000.0 / total) / 10.0);
        result.putAll(today);
        result.put("pendingOrders", pendingOrders);
        result.put("expiringContracts", expiring);
        result.put("monthCost", monthCost);
        return result;
    }

    /**
     * 设备类型分布
     */
    public List<Map<String, Object>> deviceType() {
        return groupCount("type");
    }

    /**
     * 设备状态分布
     */
    public List<Map<String, Object>> deviceStatus() {
        return groupCount("status");
    }

    private List<Map<String, Object>> groupCount(String column) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Long> counter = new HashMap<>();
        for (Device d : deviceService.list()) {
            String key = "type".equals(column) ? d.getType() : d.getStatus();
            counter.merge(key, 1L, Long::sum);
        }
        counter.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", k);
            m.put("value", v);
            list.add(m);
        });
        return list;
    }

    /**
     * 使用年限分析：年限段分布 + 超龄设备列表
     */
    public Map<String, Object> lifecycle() {
        List<Map<String, Object>> buckets = new ArrayList<>();
        List<Map<String, Object>> overdueDevices = new ArrayList<>();
        int[] range = {3, 5, 8, 10};
        int[] counts = new int[range.length + 1];
        for (Device d : deviceService.list()) {
            Map<String, Object> info = deviceService.lifeInfo(d);
            int age = (Integer) info.get("ageYears");
            int idx = 0;
            while (idx < range.length && age >= range[idx]) {
                idx++;
            }
            counts[idx]++;
            // 已报废设备不再列入超期清单（处置闭环后应从风险列表移除）
            if ("SCRAPPED".equals(d.getStatus())) {
                continue;
            }
            if ((Boolean) info.get("overdue")) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("deviceCode", d.getDeviceCode());
                m.put("name", d.getName());
                m.put("type", d.getType());
                m.put("location", d.getLocation());
                m.put("installDate", d.getInstallDate());
                m.put("ageYears", age);
                m.put("lifeYears", d.getServiceLifeYears());
                m.put("overYears", age - d.getServiceLifeYears());
                overdueDevices.add(m);
            }
        }
        String[] labels = {"3年以下", "3-5年", "5-8年", "8-10年", "10年以上"};
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", labels[i]);
            m.put("value", counts[i]);
            buckets.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("buckets", buckets);
        result.put("overdueCount", overdueDevices.size());
        result.put("overdueDevices", overdueDevices);
        return result;
    }

    /**
     * 近6个月工单趋势（按状态堆叠）
     */
    public Map<String, Object> repairTrend(int months) {
        List<Map<String, Object>> rows = repairOrderMapper.selectTrend(months);
        Map<String, Long> verified = new LinkedHashMap<>();
        Map<String, Long> completed = new LinkedHashMap<>();
        Map<String, Long> processing = new LinkedHashMap<>();
        Map<String, Long> pending = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            Map<String, Long> target;
            switch (status) {
                case "VERIFIED":
                    target = verified;
                    break;
                case "COMPLETED":
                    target = completed;
                    break;
                case "PROCESSING":
                    target = processing;
                    break;
                default:
                    target = pending;
            }
            target.put(month, cnt);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("verified", verified);
        result.put("completed", completed);
        result.put("processing", processing);
        result.put("pending", pending);
        result.put("costTrend", repairOrderMapper.selectCostTrend(months));
        return result;
    }

    /**
     * 巡检完成率（近 days 天，按日）
     */
    public Map<String, Object> taskCompletion(int days, String type) {
        List<Map<String, Object>> rows = deviceTaskMapper.selectCompletion(type, LocalDate.now().minusDays(days - 1));
        List<String> dates = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        List<Long> dones = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add(String.valueOf(row.get("date")));
            totals.add(((Number) row.get("total")).longValue());
            dones.add(((Number) row.get("done")).longValue());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("totals", totals);
        result.put("dones", dones);
        result.put("rate", totals.stream().mapToLong(Long::longValue).sum() == 0 ? 0
                : Math.round(dones.stream().mapToLong(Long::longValue).sum() * 1000.0
                / totals.stream().mapToLong(Long::longValue).sum()) / 10.0);
        return result;
    }

    /**
     * 设备健康度排行：综合 状态/年限/近期工单 评分
     */
    public List<Map<String, Object>> healthRank(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();
        for (Device d : deviceService.list()) {
            int score = 100;
            String status = d.getStatus();
            if ("FAULT".equals(status)) {
                score -= 28;
            } else if ("REPAIRING".equals(status)) {
                score -= 16;
            } else if ("STOPPED".equals(status)) {
                score -= 34;
            } else if ("SCRAPPED".equals(status)) {
                score -= 45;
            }
            // 年限扣分
            if (d.getInstallDate() != null) {
                int age = (int) java.time.temporal.ChronoUnit.YEARS.between(d.getInstallDate(), now);
                int life = d.getServiceLifeYears() == null ? 10 : d.getServiceLifeYears();
                if (age > life) {
                    score -= Math.min(22, (age - life) * 5);
                } else if (age > life - 2) {
                    score -= 4; // 临近报废年限
                }
            }
            // 近90天工单扣分
            long recent = repairOrderMapper.selectCount(new LambdaQueryWrapper<com.smart.community.entity.RepairOrder>()
                    .eq(com.smart.community.entity.RepairOrder::getDeviceId, d.getId())
                    .ge(com.smart.community.entity.RepairOrder::getCreateTime, nowTime.minusDays(90)));
            score -= Math.min(15, recent * 3);
            score = Math.max(8, Math.min(99, score));

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("deviceCode", d.getDeviceCode());
            m.put("name", d.getName());
            m.put("type", d.getType());
            m.put("status", status);
            m.put("health", score);
            list.add(m);
        }
        list.sort((a, b) -> Integer.compare((Integer) b.get("health"), (Integer) a.get("health")));
        return list.subList(0, Math.min(limit, list.size()));
    }

    /**
     * 近 N 个月维保/维修费用趋势
     */
    public List<Map<String, Object>> costTrend(int months) {
        return repairOrderMapper.selectCostTrend(months);
    }
}
