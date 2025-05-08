package util;


import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {
    public static String createResponse(String message, boolean status) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", status ? "success" : "failed");
        jsonResponse.put("message", message);
        return jsonResponse.toString();
    }

    public static void sendResponse(HttpExchange exchange, int code, String msg) throws IOException {
        byte[] response = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
