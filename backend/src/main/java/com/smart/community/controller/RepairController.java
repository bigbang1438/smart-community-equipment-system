package com.smart.community.controller;

import com.smart.community.common.PageResult;
import com.smart.community.common.Result;
import com.smart.community.common.RoleRequired;
import com.smart.community.entity.RepairOrder;
import com.smart.community.service.RepairService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/repair")
public class RepairController {

    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping("/page")
    public Result<PageResult<RepairOrder>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String level,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long deviceId) {
        return Result.ok(repairService.page(page, size, status, level, keyword, deviceId));
    }

    /**
     * 各状态工单统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(repairService.stats());
    }

    @GetMapping("/{id}")
    public Result<RepairOrder> detail(@PathVariable Long id) {
        return Result.ok(repairService.getById(id));
    }

    /**
     * 报修登记
     */
    @PostMapping
    public Result<RepairOrder> create(@RequestBody RepairOrder order) {
        return Result.ok(repairService.create(order));
    }

    /**
     * 派单
     */
    @PostMapping("/{id}/assign")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<RepairOrder> assign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(repairService.assign(id, body.get("assignee")));
    }

    /**
     * 维修完成
     */
    @PostMapping("/{id}/finish")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<RepairOrder> finish(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal cost = body.get("cost") == null ? null : new BigDecimal(String.valueOf(body.get("cost")));
        BigDecimal hours = body.get("fixHours") == null ? null : new BigDecimal(String.valueOf(body.get("fixHours")));
        return Result.ok(repairService.finish(id, body.get("fixResult") == null ? null : String.valueOf(body.get("fixResult")), cost, hours));
    }

    /**
     * 验收
     */
    @PostMapping("/{id}/verify")
    @RoleRequired({"ADMIN"})
    public Result<RepairOrder> verify(@PathVariable Long id) {
        return Result.ok(repairService.verify(id));
    }

    @DeleteMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        repairService.removeById(id);
        return Result.ok();
    }
}
