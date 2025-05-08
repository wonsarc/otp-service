package model;

public record NotificationResult(
        boolean success,
        String errorMessage,
        NotificationChannel channel
) {}
