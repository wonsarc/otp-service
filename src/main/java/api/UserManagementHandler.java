package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import service.UserService;
import util.ResponseUtil;
import util.TokenUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserManagementHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(UserManagementHandler.class.getName());
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warning("Invalid request method: " + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !TokenUtil.validateToken(authHeader) || !TokenUtil.isAdmin(authHeader)) {
            logger.warning("Unauthorized access attempt.");
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        logger.info("Processing request to retrieve users.");

        try {
            List<User> users = userService.users();
            JSONArray jsonUsers = new JSONArray();
            for (User user : users) {
                JSONObject jsonUser = new JSONObject();
                jsonUser.put("id", user.id());
                jsonUser.put("login", user.login());
                jsonUser.put("role", user.role());
                jsonUsers.put(jsonUser);
            }

            byte[] out = jsonUsers.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            logger.info("Successfully retrieved " + users.size() + " users.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving users", e);
            String responseMessage = ResponseUtil.createResponse(e.getMessage(), false);
            ResponseUtil.sendResponse(exchange, 500, responseMessage);
        } finally {
            exchange.close();
        }
    }
}
