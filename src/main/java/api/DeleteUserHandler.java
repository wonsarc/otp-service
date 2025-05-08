package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import service.UserService;
import util.ResponseUtil;
import util.TokenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteUserHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(DeleteUserHandler.class.getName());
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !TokenUtil.validateToken(authHeader) || !TokenUtil.isAdmin(authHeader)) {
            logger.log(Level.WARNING, "Unauthorized access attempt.");
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);

        try {
            JSONObject json = new JSONObject(body.toString());
            int userId = json.getInt("user_id");

            String result = userService.delete(userId);
            String responseMessage = ResponseUtil.createResponse(result, true);
            ResponseUtil.sendResponse(exchange, 200, responseMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting user: " + e.getMessage(), e);
            String responseMessage = ResponseUtil.createResponse(e.getMessage(), false);
            ResponseUtil.sendResponse(exchange, 500, responseMessage);
        } finally {
            exchange.close();
        }
    }
}
