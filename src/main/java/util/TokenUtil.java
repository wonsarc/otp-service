package util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import model.Role;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenUtil {
    private static final String SECRET_KEY = "super-secret-key";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(ALGORITHM).build();
    private static final Logger logger = Logger.getLogger(TokenUtil.class.getName());

    public static String generateToken(int userId, String role, long ttlSeconds) {
        logger.info("Generating token for user ID: " + userId + " with role: " + role);

        String token = JWT.create()
                .withClaim("id", userId)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ttlSeconds * 1000))
                .sign(ALGORITHM);

        logger.info("Token generated successfully for user ID: " + userId);
        return token;
    }

    public static boolean validateToken(String token) {
        try {
            VERIFIER.verify(stripBearerPrefix(token));
            logger.info("Token validation successful.");
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public static int getUserId(String token) {
        DecodedJWT jwt = VERIFIER.verify(stripBearerPrefix(token));
        int userId = jwt.getClaim("id").asInt();
        logger.info("Extracted user ID from token: " + userId);
        return userId;
    }

    public static boolean isAdmin(String token) {
        try {
            DecodedJWT jwt = VERIFIER.verify(stripBearerPrefix(token));
            String role = jwt.getClaim("role").asString();
            boolean isAdmin = Role.ADMIN.toString().equalsIgnoreCase(role);
            logger.info("User role extracted from token: " + role + ". Is admin: " + isAdmin);
            return isAdmin;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check admin role: " + e.getMessage());
            return false;
        }
    }

    private static String stripBearerPrefix(String token) {
        return token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
