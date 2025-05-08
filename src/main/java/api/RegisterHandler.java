package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Role;
import org.json.JSONObject;
import service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static util.ResponseUtil.createResponse;
import static util.ResponseUtil.sendResponse;

public class RegisterHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(RegisterHandler.class.getName());
    private final UserService userService = new UserService();

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
            String roleString = body.getString("role");
            Role role;

            try {
                role = Role.valueOf(roleString.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Invalid role: " + roleString);
                sendResponse(exchange, 400, createResponse("Invalid role", false));
                return;
            }

            if (userService.isLoginTaken(login)) {
                logger.log(Level.WARNING, "Login already taken: " + login);
                sendResponse(exchange, 400, createResponse("Login already taken", false));
                return;
            }

            userService.register(login, password, role.toString());
            logger.info("User registered successfully: " + login);
            sendResponse(exchange, 201, createResponse("User registered successfully", true));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Registration error: ", e);
            sendResponse(exchange, 400, createResponse("Registration failed", false));
        }
    }
}
