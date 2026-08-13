package com.smart.community.controller;

import com.smart.community.common.Result;
import com.smart.community.entity.DeviceLog;
import com.smart.community.service.MonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * 全部设备实时状态快照（前端轮询）
     */
    @GetMapping("/device-status")
    public Result<List<Map<String, Object>>> deviceStatus() {
        return Result.ok(monitorService.deviceStatus());
    }

    /**
     * 单设备实时指标序列
     */
    @GetMapping("/realtime/{deviceId}")
    public Result<Map<String, Object>> realtime(@PathVariable Long deviceId,
                                                @RequestParam(defaultValue = "30") int points) {
        return Result.ok(monitorService.realtime(deviceId, points));
    }

    /**
     * 历史指标
     */
    @GetMapping("/history/{deviceId}")
    public Result<List<DeviceLog>> history(@PathVariable Long deviceId,
                                           @RequestParam(required = false) String metric) {
        return Result.ok(monitorService.history(deviceId, metric));
    }

    /**
     * 告警列表
     */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> alerts() {
        return Result.ok(monitorService.alerts());
    }
}
