package com.smart.community.controller;

import com.smart.community.common.BusinessException;
import com.smart.community.common.PageResult;
import com.smart.community.common.Result;
import com.smart.community.common.RoleRequired;
import com.smart.community.entity.DeviceTask;
import com.smart.community.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/page")
    public Result<PageResult<DeviceTask>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long deviceId,
                                               @RequestParam(required = false) String planDate) {
        LocalDate pd = null;
        if (planDate != null && !planDate.isEmpty()) {
            try { pd = LocalDate.parse(planDate); } catch (Exception ignored) { }
        }
        return Result.ok(taskService.page(page, size, type, status, keyword, deviceId, pd));
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> today() {
        return Result.ok(taskService.todayStats());
    }

    /**
     * 自动生成巡检/保养计划
     * body: {type: INSPECT|MAINTAIN, deviceIds: [], horizonDays: 30, executor: "xxx"}
     */
    @PostMapping("/generate")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String type = body.get("type") == null ? "INSPECT" : String.valueOf(body.get("type"));
        List<Long> ids = null;
        if (body.get("deviceIds") != null && body.get("deviceIds") instanceof java.util.List) {
            ids = ((java.util.List<?>) body.get("deviceIds")).stream()
                    .map(x -> Long.valueOf(String.valueOf(x)))
                    .collect(java.util.stream.Collectors.toList());
        }
        int horizon = body.get("horizonDays") == null ? 30 : Integer.parseInt(String.valueOf(body.get("horizonDays")));
        String executor = body.get("executor") == null ? null : String.valueOf(body.get("executor"));
        if (!"INSPECT".equals(type) && !"MAINTAIN".equals(type)) {
            throw new BusinessException("任务类型不正确");
        }
        int count = taskService.generate(type, ids, horizon, executor);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("created", count);
        return Result.ok(result);
    }

    @PostMapping("/refreshOverdue")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<Map<String, Object>> refreshOverdue() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("updated", taskService.refreshOverdue());
        return Result.ok(result);
    }

    /**
     * 移动端打卡（按任务ID）
     */
    @PostMapping("/check")
    @RoleRequired({"INSPECTOR", "MAINTAINER", "ADMIN"})
    public Result<DeviceTask> check(@RequestBody Map<String, Object> body) {
        if (body.get("taskId") == null) {
            throw new BusinessException("缺少任务ID");
        }
        Long taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        return Result.ok(taskService.check(taskId,
                body.get("result") == null ? null : String.valueOf(body.get("result")),
                body.get("remark") == null ? null : String.valueOf(body.get("remark")),
                body.get("location") == null ? null : String.valueOf(body.get("location")),
                body.get("checkItems") == null ? null : String.valueOf(body.get("checkItems")),
                body.get("executor") == null ? null : String.valueOf(body.get("executor"))));
    }

    /**
     * 移动端扫码打卡（按设备编号）
     */
    @PostMapping("/checkByDevice")
    @RoleRequired({"INSPECTOR", "MAINTAINER", "ADMIN"})
    public Result<DeviceTask> checkByDevice(@RequestBody Map<String, Object> body) {
        String deviceCode = body.get("deviceCode") == null ? null : String.valueOf(body.get("deviceCode"));
        if (deviceCode == null || deviceCode.isEmpty()) {
            throw new BusinessException("缺少设备编号");
        }
        return Result.ok(taskService.checkByDeviceCode(deviceCode,
                body.get("result") == null ? null : String.valueOf(body.get("result")),
                body.get("remark") == null ? null : String.valueOf(body.get("remark")),
                body.get("location") == null ? null : String.valueOf(body.get("location")),
                body.get("checkItems") == null ? null : String.valueOf(body.get("checkItems")),
                body.get("executor") == null ? null : String.valueOf(body.get("executor"))));
    }

    /**
     * 移动端：我的待办任务（执行人取当前登录用户，防越权查询）
     */
    @GetMapping("/my")
    public Result<List<DeviceTask>> my(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) String executor) {
        String name = executor;
        com.smart.community.common.UserContext.LoginUser user = com.smart.community.common.UserContext.get();
        if (user != null) {
            name = user.getRealName();
        }
        return Result.ok(taskService.myTasks(type, name));
    }

    @GetMapping("/device/{deviceId}")
    public Result<List<DeviceTask>> byDevice(@PathVariable Long deviceId) {
        return Result.ok(taskService.recentByDevice(deviceId, 20));
    }
}
