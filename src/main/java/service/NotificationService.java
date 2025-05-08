package service;

import model.NotificationChannel;
import model.NotificationResult;
import service.sender.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    private final Map<String, NotificationSender> senders = new HashMap<>();
    private final FileSender fileSender = new FileSender("otp_notifications.txt");

    public NotificationService() {
        senders.put("email", new EmailSender());
        senders.put("sms", new SmppSender());
        senders.put("telegram", new TelegramSender());
        senders.put("file", fileSender);
    }

    public void sendNotifications(String code, List<NotificationChannel> channels) {
        List<NotificationResult> errors = new ArrayList<>();

        for (NotificationChannel channel : channels) {
            try {
                NotificationSender sender = senders.get(channel.type().toLowerCase());
                if (sender == null) {
                    handleUnsupportedChannel(code, channel, errors);
                    continue;
                }

                NotificationResult result = sender.send(code, channel);
                if (!result.success()) {
                    errors.add(result);
                    logError(result);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error during sending", e);
            }
        }

        if (!errors.isEmpty()) {
            writeErrorsToFile(errors, code);
        }
    }

    private void handleUnsupportedChannel(String code, NotificationChannel channel,
                                          List<NotificationResult> errors) {
        String errorMsg = "Unsupported channel type: " + channel.type();
        NotificationResult result = new NotificationResult(
                false,
                errorMsg,
                channel
        );
        errors.add(result);
        logger.warning(errorMsg);
        fileSender.send(code, channel);
    }

    private void logError(NotificationResult result) {
        String errorLog = String.format(
                "Failed to send via %s to %s: %s",
                result.channel().type(),
                result.channel().address(),
                result.errorMessage()
        );
        logger.warning(errorLog);
    }

    private void writeErrorsToFile(List<NotificationResult> errors, String code) {
        errors.forEach(error ->
                fileSender.send("Failed: " + code, error.channel())
        );
    }
}
