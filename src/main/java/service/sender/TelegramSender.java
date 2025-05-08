package service.sender;

import config.Config;
import model.NotificationChannel;
import model.NotificationResult;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class TelegramSender implements NotificationSender {
    private static final Logger logger = Logger.getLogger(TelegramSender.class.getName());
    private final String telegramApiUrl;

    public TelegramSender() {
        String botToken = Config.get("tg.token");
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
    }

    @Override
    public NotificationResult send(String code, NotificationChannel channel) {
        String chatId = channel.address();
        String message = String.format("%s, your confirmation code is: %s", chatId, code);
        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                chatId,
                urlEncode(message));

        sendTelegramRequest(url);
        return new NotificationResult(true, null, channel);
    }

    private void sendTelegramRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.severe("Telegram API error. Status code: " + statusCode);
                } else {
                    logger.info("Telegram message sent successfully");
                }
            }
        } catch (IOException e) {
            logger.severe("Error sending Telegram message: " + e.getMessage());
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
