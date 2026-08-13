package com.smart.community.controller;

import com.smart.community.common.Result;
import com.smart.community.common.UserContext;
import com.smart.community.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.ok(authService.login(body.get("username"), body.get("password")));
    }

    @GetMapping("/me")
    public Result<UserContext.LoginUser> me() {
        return Result.ok(UserContext.get());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContext.get().getId(),
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }
}
