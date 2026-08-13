package com.smart.community.controller;

import com.smart.community.common.PageResult;
import com.smart.community.common.Result;
import com.smart.community.common.RoleRequired;
import com.smart.community.entity.Contract;
import com.smart.community.service.ContractService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contract")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/page")
    public Result<PageResult<Contract>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(contractService.page(page, size, status, keyword));
    }

    /**
     * 到期提醒（已过期 + 30天内到期）
     */
    @GetMapping("/reminders")
    public Result<List<Contract>> reminders() {
        return Result.ok(contractService.reminders());
    }

    @PostMapping
    @RoleRequired({"ADMIN"})
    public Result<Contract> add(@RequestBody Contract contract) {
        return Result.ok(contractService.add(contract));
    }

    @PutMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Contract> update(@PathVariable Long id, @RequestBody Contract contract) {
        contract.setId(id);
        return Result.ok(contractService.updateContract(contract));
    }

    @DeleteMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("reminderCount", contractService.countExpiringOrExpired());
        return Result.ok(result);
    }
}
