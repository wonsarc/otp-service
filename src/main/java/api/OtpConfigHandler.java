package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.OtpConfigService;
import util.ResponseUtil;
import util.TokenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpConfigHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(OtpConfigHandler.class.getName());
    private final OtpConfigService configService = new OtpConfigService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warning("Method not allowed: " + exchange.getRequestMethod());
            ResponseUtil.sendResponse(exchange, 405, ResponseUtil.createResponse("Method Not Allowed", false));
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !TokenUtil.validateToken(authHeader) || !TokenUtil.isAdmin(authHeader)) {
            logger.warning("Unauthorized access attempt.");
            ResponseUtil.sendResponse(exchange, 403, ResponseUtil.createResponse("Forbidden", false));
            return;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        try {
            JSONObject json = new JSONObject(body.toString());
            int codeLength = json.optInt("code_length");
            int ttlSeconds = json.optInt("ttl_seconds");

            boolean success = configService.updateConfig(codeLength, ttlSeconds);
            logger.info("Configuration updated: codeLength=" + codeLength + ", ttl_seconds=" + ttlSeconds);
            ResponseUtil.sendResponse(exchange, 200, ResponseUtil.createResponse("Configuration updated successfully", success));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing request", e);
            ResponseUtil.sendResponse(exchange, 500, ResponseUtil.createResponse("Internal Server Error: " + e.getMessage(), false));
        } finally {
            exchange.close();
        }
    }
}
