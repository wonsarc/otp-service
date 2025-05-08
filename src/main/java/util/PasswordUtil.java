package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordUtil {
    private static final Logger logger = Logger.getLogger(PasswordUtil.class.getName());
    private static final String SALT = "secret_salt_31";

    public static String hash(String password, String login) {
        logger.info("Hashing password for login: " + login);
        try {
            String saltedPassword = login + password + SALT;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(saltedPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            logger.info("Password hashed successfully for login: " + login);
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error hashing password for login: " + login, e);
            throw new RuntimeException("Ошибка хэширования пароля", e);
        }
    }
}
