package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.NotificationChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import service.NotificationService;
import service.OtpService;
import util.TokenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpGenerateHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(OtpGenerateHandler.class.getName());
    private final OtpService otpService = new OtpService();
    private final NotificationService notificationService = new NotificationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !TokenUtil.validateToken(authHeader)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        int userId = TokenUtil.getUserId(authHeader);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            JSONObject req = new JSONObject(sb.toString());

            String operationIdStr = req.optString("operation_id", null);
            if (operationIdStr == null) {
                sendErrorResponse(exchange, 400, "Missing required field: operation_id");
                return;
            }

            UUID operationId;
            try {
                operationId = UUID.fromString(operationIdStr);
            } catch (IllegalArgumentException e) {
                sendErrorResponse(exchange, 400, "Invalid UUID format for operation_id");
                return;
            }

            List<NotificationChannel> channels = parseChannels(req);
            if (channels == null) {
                sendErrorResponse(exchange, 400, "Invalid channels format");
                return;
            }

            logger.info("Generating OTP for userId=" + userId + ", operationId=" + operationId);
            String code = otpService.generate(userId, operationId);

            if (!channels.isEmpty()) {
                try {
                    notificationService.sendNotifications(code, channels);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during notifications sending", e);
                }
            }

            JSONObject resp = new JSONObject();
            resp.put("code", code);
            sendSuccessResponse(exchange, resp);

            logger.info("OTP generated and sent successfully for userId=" + userId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing request", e);
            sendErrorResponse(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    private List<NotificationChannel> parseChannels(JSONObject request) {
        List<NotificationChannel> channels = new ArrayList<>();
        if (!request.has("channels")) return channels;

        try {
            JSONArray channelsArray = request.getJSONArray("channels");
            for (int i = 0; i < channelsArray.length(); i++) {
                JSONObject channelObj = channelsArray.getJSONObject(i);
                String type = channelObj.getString("type");
                String address = channelObj.getString("address");
                channels.add(new NotificationChannel(type, address));
            }
            return channels;
        } catch (Exception e) {
            logger.warning("Invalid channels format: " + e.getMessage());
            return null;
        }
    }

    private void sendSuccessResponse(HttpExchange exchange, JSONObject response) throws IOException {
        byte[] out = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, out.length);
        exchange.getResponseBody().write(out);
    }

    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        logger.warning("Error: " + code + " - " + message);
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", message);
        byte[] err = errorResponse.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, err.length);
        exchange.getResponseBody().write(err);
    }
}