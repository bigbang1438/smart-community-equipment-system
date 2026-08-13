package com.smart.community.common;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class LoginUser {
        private Long id;
        private String username;
        private String realName;
        private String role;

        public LoginUser(Long id, String username, String realName, String role) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.role = role;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getRealName() { return realName; }
        public String getRole() { return role; }
    }
}
