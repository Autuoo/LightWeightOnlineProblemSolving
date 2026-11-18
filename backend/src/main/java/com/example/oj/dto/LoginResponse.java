package com.example.oj.dto;

public record LoginResponse(String token, long expiresIn, UserView user) {

    public record UserView(Long id, String username, String nickname, String role) {
    }
}