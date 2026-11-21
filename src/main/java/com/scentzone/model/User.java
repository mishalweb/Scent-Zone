package com.scentzone.model;

public record User(int id, String fullName, String email, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
