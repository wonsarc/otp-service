package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.OtpService;
import util.ResponseUtil;
import util.TokenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpValidateHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(OtpValidateHandler.class.getName());
    private final OtpService otpService = new OtpService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warning("Invalid request method: " + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !TokenUtil.validateToken(authHeader)) {
            logger.warning("Unauthorized access attempt.");
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        int userId = TokenUtil.getUserId(authHeader);
        logger.info("Processing OTP validation for user ID: " + userId);

        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);

        try {
            JSONObject json = new JSONObject(body.toString());
            String code = json.getString("code");
            UUID operationId = UUID.fromString(json.getString("operation_id"));

            boolean valid = otpService.validate(userId, code, operationId);
            JSONObject resp = new JSONObject().put("valid", valid);

            logger.info("OTP validation result for user ID " + userId + ": " + valid);
            ResponseUtil.sendResponse(exchange, 200, resp.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error validating OTP for user ID: " + userId, e);
            String responseMessage = ResponseUtil.createResponse(e.getMessage(), false);
            ResponseUtil.sendResponse(exchange, 500, responseMessage);
        } finally {
            exchange.close();
        }
    }
}
