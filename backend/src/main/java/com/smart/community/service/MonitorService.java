package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.community.entity.Device;
import com.smart.community.entity.DeviceLog;
import com.smart.community.mapper.DeviceLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 设备运行状态监测（演示环境：基于设备类型基线 + 随机波动模拟实时数据）
 */
@Service
public class MonitorService {

    private final DeviceService deviceService;
    private final DeviceLogMapper deviceLogMapper;
    private final Random random = new Random();

    public MonitorService(DeviceService deviceService, DeviceLogMapper deviceLogMapper) {
        this.deviceService = deviceService;
        this.deviceLogMapper = deviceLogMapper;
    }

    /** 各类型设备指标基线 {温度, 振动, 电压} */
    private static final Map<String, double[]> BASELINE = new HashMap<>();

    static {
        BASELINE.put("ELEVATOR", new double[]{38, 1.2, 380});
        BASELINE.put("FIRE", new double[]{32, 0.5, 220});
        BASELINE.put("PUMP", new double[]{60, 2.5, 380});
        BASELINE.put("ACCESS", new double[]{30, 0.2, 12});
        BASELINE.put("OTHER", new double[]{48, 1.0, 380});
    }

    /**
     * 全部运行设备的实时状态快照
     */
    public List<Map<String, Object>> deviceStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Device d : deviceService.listRunning()) {
            Map<String, Object> m = snapshot(d);
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> snapshot(Device d) {
        double[] base = BASELINE.getOrDefault(d.getType(), new double[]{40, 1.0, 220});
        double temp = round(base[0] + rand(-4, 6));
        double vib = round(Math.max(0.1, base[1] + rand(-0.4, 0.6)));
        double volt = round(base[2] + rand(-6, 6));
        String status = d.getStatus();
        if ("FAULT".equals(status) || "REPAIRING".equals(status)) {
            temp = round(temp + 6);
            vib = round(vib * 1.8);
        }
        double health = healthScore(d, temp, vib);
        Map<String, Object> m = new HashMap<>();
        m.put("deviceId", d.getId());
        m.put("deviceCode", d.getDeviceCode());
        m.put("name", d.getName());
        m.put("type", d.getType());
        m.put("status", status);
        m.put("temperature", temp);
        m.put("vibration", vib);
        m.put("voltage", volt);
        m.put("health", health);
        m.put("runtimeDays", runtimeDays(d));
        m.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return m;
    }

    /**
     * 单设备实时指标序列（近 N 个采样点，用于折线图）
     */
    public Map<String, Object> realtime(Long deviceId, int points) {
        Device d = deviceService.getById(deviceId);
        if (d == null) {
            return new HashMap<>();
        }
        double[] base = BASELINE.getOrDefault(d.getType(), new double[]{40, 1.0, 220});
        List<Double> temps = new ArrayList<>();
        List<Double> vibs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        double t = base[0] + rand(-2, 3);
        double v = Math.max(0.1, base[1] + rand(-0.3, 0.4));
        for (int i = points - 1; i >= 0; i--) {
            LocalDateTime ts = now.minusMinutes(i * 5L);
            t = clamp(t + rand(-1.2, 1.2), base[0] - 8, base[0] + 14);
            v = clamp(v + rand(-0.25, 0.25), 0.05, base[1] + 2.5);
            if ("FAULT".equals(d.getStatus()) || "REPAIRING".equals(d.getStatus())) {
                t += 2.5;
            }
            temps.add(round(t));
            vibs.add(round(v));
            times.add(ts.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        Map<String, Object> m = new HashMap<>();
        m.put("device", d);
        m.put("times", times);
        m.put("temperature", temps);
        m.put("vibration", vibs);
        m.put("current", snapshot(d));
        return m;
    }

    /**
     * 历史指标（数据库样本）
     */
    public List<DeviceLog> history(Long deviceId, String metric) {
        return deviceLogMapper.selectList(new LambdaQueryWrapper<DeviceLog>()
                .eq(DeviceLog::getDeviceId, deviceId)
                .eq(DeviceLog::getMetric, metric == null ? "TEMPERATURE" : metric)
                .orderByDesc(DeviceLog::getRecordTime)
                .last("LIMIT 50"));
    }

    /**
     * 告警列表：故障/维修设备 + 健康度低设备 + 超龄设备
     */
    public List<Map<String, Object>> alerts() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Device d : deviceService.list()) {
            Map<String, Object> m = snapshot(d);
            String level = null;
            String message = null;
            if ("FAULT".equals(d.getStatus())) {
                level = "严重";
                message = "设备故障，请尽快安排维修";
            } else if ("REPAIRING".equals(d.getStatus())) {
                level = "警告";
                message = "设备维修中，运行数据异常";
            } else if ((Double) m.get("health") < 65) {
                level = "警告";
                message = "设备健康度偏低，建议提前保养";
            }
            if (level != null) {
                m.put("level", level);
                m.put("message", message);
                result.add(m);
            }
        }
        return result;
    }

    private double healthScore(Device d, double temp, double vib) {
        double[] base = BASELINE.getOrDefault(d.getType(), new double[]{40, 1.0, 220});
        double score = 100;
        if (temp > base[0] + 10) {
            score -= 25;
        } else if (temp > base[0] + 5) {
            score -= 10;
        }
        if (vib > base[1] + 1.2) {
            score -= 25;
        } else if (vib > base[1] + 0.5) {
            score -= 10;
        }
        if ("FAULT".equals(d.getStatus()) || "REPAIRING".equals(d.getStatus())) {
            score -= 30;
        }
        return (int) Math.max(10, Math.min(100, score));
    }

    private long runtimeDays(Device d) {
        if (d.getInstallDate() == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(d.getInstallDate(), LocalDateTime.now().toLocalDate());
    }

    private double rand(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
