package com.campus.trade.security;

public class UserPrincipal {

    private final String username;
    private final long userId;
    private final String role;

    public UserPrincipal(String username, long userId, String role) {
        this.username = username;
        this.userId = userId;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
