package com.smart.community.common;

import com.smart.community.entity.SysUser;
import com.smart.community.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 登录拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${smart.auth.whitelist:}")
    private List<String> whitelist;

    public AuthInterceptor(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        // 白名单（配置生效）：精确匹配
        if (whitelist != null && whitelist.contains(uri)) {
            return true;
        }
        // 兼容历史前缀放行（仅登录）
        if (uri.equals("/api/auth/login")) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }
        try {
            Claims claims = jwtUtil.parse(token.substring(7));
            Long userId = Long.valueOf(String.valueOf(claims.get("userId")));
            SysUser user = userService.getById(userId);
            if (user == null || user.getStatus() != 1) {
                UserContext.clear();
                writeUnauthorized(response, "账号不存在或已禁用");
                return false;
            }
            UserContext.set(new UserContext.LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole()));

            // 角色校验
            if (handler instanceof org.springframework.web.method.HandlerMethod) {
                RoleRequired roleRequired = ((org.springframework.web.method.HandlerMethod) handler)
                        .getMethodAnnotation(RoleRequired.class);
                if (roleRequired != null) {
                    boolean pass = false;
                    for (String role : roleRequired.value()) {
                        if (role.equals(user.getRole())) {
                            pass = true;
                            break;
                        }
                    }
                    if (!pass) {
                        UserContext.clear();
                        writeUnauthorized(response, "无权限执行该操作");
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            UserContext.clear();
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + msg + "\",\"data\":null}");
    }
}
