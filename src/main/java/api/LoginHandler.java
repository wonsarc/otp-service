package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.AuthToken;
import org.json.JSONObject;
import service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static util.ResponseUtil.createResponse;
import static util.ResponseUtil.sendResponse;

public class LoginHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(LoginHandler.class.getName());
    private final UserService userService = new UserService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            logger.warning("Unsupported request method: " + exchange.getRequestMethod());
            sendResponse(exchange, 405, createResponse("Method Not Allowed", false));
            return;
        }

        StringBuilder buf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
        }

        try {
            JSONObject body = new JSONObject(buf.toString());
            String login = body.getString("login");
            String password = body.getString("password");

            logger.info("Attempting to log in user: " + login);
            AuthToken token = userService.login(login, password);
            JSONObject response = new JSONObject();
            response.put("token", token.token());
            response.put("expiresAt", formatter.format(Instant.ofEpochMilli(token.expiresAt())));

            byte[] out = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            logger.info("User logged in successfully: " + login);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Login error for user: " + buf, e);
            sendResponse(exchange, 401, createResponse("Login failed", false));
        }
    }
}
