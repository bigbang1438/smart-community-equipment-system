package com.smart.community.controller;

import com.smart.community.common.Result;
import com.smart.community.service.StatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/device-type")
    public Result<List<Map<String, Object>>> deviceType() {
        return Result.ok(statsService.deviceType());
    }

    @GetMapping("/device-status")
    public Result<List<Map<String, Object>>> deviceStatus() {
        return Result.ok(statsService.deviceStatus());
    }

    @GetMapping("/lifecycle")
    public Result<Map<String, Object>> lifecycle() {
        return Result.ok(statsService.lifecycle());
    }

    @GetMapping("/repair-trend")
    public Result<Map<String, Object>> repairTrend(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsService.repairTrend(months));
    }

    @GetMapping("/task-completion")
    public Result<Map<String, Object>> taskCompletion(@RequestParam(defaultValue = "14") int days,
                                                      @RequestParam(defaultValue = "INSPECT") String type) {
        return Result.ok(statsService.taskCompletion(days, type));
    }

    @GetMapping("/health-rank")
    public Result<List<Map<String, Object>>> healthRank(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.healthRank(limit));
    }

    @GetMapping("/cost-trend")
    public Result<List<Map<String, Object>>> costTrend(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsService.costTrend(months));
    }
}
