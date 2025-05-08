package model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OtpCode(int id, int userId, UUID operationId, String code, Status status, LocalDateTime createdAt,
                      LocalDateTime expiresAt) {
}
