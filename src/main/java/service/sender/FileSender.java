package service.sender;

import model.NotificationChannel;
import model.NotificationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSender implements NotificationSender {
    private static final Logger logger = Logger.getLogger(FileSender.class.getName());
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path filePath;

    public FileSender(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    @Override
    public NotificationResult send(String code, NotificationChannel channel) {
        String fileEntry = String.format("[%s] [%s] %s - Code: %s\n",
                LocalDateTime.now().format(formatter),
                channel.type().toUpperCase(),
                channel.address(),
                code
        );

        try {
            Files.write(filePath, fileEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("Notification sent to file: " + channel.address() + " - Code: " + code);
            return new NotificationResult(true, null, channel);
        } catch (IOException e) {
            String error = "File write error: " + e.getMessage();
            logger.log(Level.SEVERE, error, e);
            return new NotificationResult(false, error, channel);
        }
    }
}
