package com.smart.community.controller;

import com.smart.community.common.PageResult;
import com.smart.community.common.Result;
import com.smart.community.common.RoleRequired;
import com.smart.community.entity.SysUser;
import com.smart.community.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    @RoleRequired({"ADMIN"})
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    /**
     * 维保人员候选列表（派单用，MAINTAINER/ADMIN 可用）
     */
    @GetMapping("/maintainers")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<java.util.List<SysUser>> maintainers() {
        return Result.ok(userService.maintainers());
    }

    @PostMapping
    @RoleRequired({"ADMIN"})
    public Result<SysUser> add(@RequestBody SysUser user) {
        userService.add(user);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
