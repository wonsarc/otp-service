package service;

import config.Config;
import util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpConfigService {
    private static final Logger logger = Logger.getLogger(OtpConfigService.class.getName());
    private final int defaultCodeLength = Config.getInt("otp.default.length");
    private final int defaultTtlSeconds = Config.getInt("otp.default.ttl.seconds");

    public boolean updateConfig(int codeLength, int ttlSeconds) throws SQLException {
        String sql = "INSERT INTO otp_config (id, code_length, ttl_seconds) VALUES (1, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET code_length = EXCLUDED.code_length, ttl_seconds = EXCLUDED.ttl_seconds";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, ttlSeconds);
            boolean success = ps.executeUpdate() > 0;
            logger.info("Configuration updated: codeLength=" + codeLength + ", ttlSeconds=" + ttlSeconds + ", success=" + success);
            return success;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating configuration", e);
            throw e;
        }
    }

    public int getCodeLength() throws SQLException {
        return getConfigValue("code_length", defaultCodeLength);
    }

    public int getTtlSeconds() throws SQLException {
        return getConfigValue("ttl_seconds", defaultTtlSeconds);
    }

    private int getConfigValue(String columnName, int defaultValue) throws SQLException {
        String sql = "SELECT " + columnName + " FROM otp_config WHERE id = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int value = rs.getInt(columnName);
                logger.info("Retrieved " + columnName + ": " + value);
                return value;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving " + columnName, e);
            throw e;
        }
        logger.warning("Returning default value for " + columnName + ": " + defaultValue);
        return defaultValue;
    }
}
