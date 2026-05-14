package com.evcharging.model;

import java.time.LocalDateTime;

public class AuthToken {
    private String token;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public AuthToken() {
    }

    public AuthToken(String token, String username, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.token = token;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}