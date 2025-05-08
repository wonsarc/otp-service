import api.*;
import com.sun.net.httpserver.HttpServer;
import service.OtpManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/user/register", new RegisterHandler());
        server.createContext("/user/login", new LoginHandler());

        server.createContext("/otp/generate", new OtpGenerateHandler());
        server.createContext("/otp/validate", new OtpValidateHandler());

        server.createContext("/admin/config", new OtpConfigHandler());
        server.createContext("/admin/users", new UserManagementHandler());
        server.createContext("/admin/delete", new DeleteUserHandler());
        server.setExecutor(null);
        server.start();

        logger.info("Сервер запущен на http://localhost:8080");

        OtpManager otpManager = new OtpManager();
        Runtime.getRuntime().addShutdownHook(new Thread(otpManager::shutdown));
        logger.info("Шедулер запущен");
    }
}
