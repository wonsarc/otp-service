package model;

public record AuthToken(String token, long expiresAt) {
}
