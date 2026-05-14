package com.evcharging.security;

public class CurrentUser {
    private final String username;
    private final String role;

    public CurrentUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isDriver() {
        return "DRIVER".equalsIgnoreCase(role);
    }
}