package service.sender;

import model.NotificationChannel;
import model.NotificationResult;

public interface NotificationSender {
    NotificationResult send(String code, NotificationChannel channel);
}
