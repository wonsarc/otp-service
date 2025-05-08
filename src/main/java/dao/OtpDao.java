package dao;

import model.OtpCode;
import model.Status;
import util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpDao {
    private static final Logger logger = Logger.getLogger(OtpDao.class.getName());

    private static final String SQL_INSERT_OTP = "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_FIND_ACTIVE_BY_USER = "SELECT * FROM otp_codes WHERE user_id = ? AND code = ? AND status = 'ACTIVE'";
    private static final String SQL_UPDATE_STATUS = "UPDATE otp_codes SET status = ? WHERE id = ?";
    private static final String SQL_FIND_EXPIRED_OTPS = "SELECT * FROM otp_codes WHERE expires_at < NOW() AND status = 'ACTIVE'";

    public void save(OtpCode otp) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_OTP)) {
            ps.setInt(1, otp.userId());
            ps.setString(2, otp.operationId().toString());
            ps.setString(3, otp.code());
            ps.setString(4, otp.status().toString());
            ps.setTimestamp(5, Timestamp.valueOf(otp.createdAt()));
            ps.setTimestamp(6, Timestamp.valueOf(otp.expiresAt()));
            ps.executeUpdate();
            logger.info("OTP code saved for user ID: " + otp.userId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving OTP code for user ID: " + otp.userId(), e);
            throw e;
        }
    }

    public OtpCode findActiveByUser(int userId, String code) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_BY_USER)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                OtpCode otp = mapRowToOtpCode(rs);
                logger.info("Active OTP code found for user ID: " + userId);
                return otp;
            }
            logger.info("No active OTP code found for user ID: " + userId + " with code: " + code);
            return null;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding active OTP code for user ID: " + userId + " with code: " + code, e);
            throw e;
        }
    }

    public void updateStatus(int id, String newStatus) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            ps.executeUpdate();
            logger.info("Updated OTP code status to " + newStatus + " for OTP ID: " + id);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating OTP code status for ID: " + id, e);
            throw e;
        }
    }

    public List<OtpCode> findExpiredOtps() throws SQLException {
        List<OtpCode> expiredOtps = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_EXPIRED_OTPS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OtpCode otp = mapRowToOtpCode(rs);
                expiredOtps.add(otp);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding expired OTP codes", e);
            throw e;
        }

        return expiredOtps;
    }

    public static String generateCode(int digits) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            code.append(random.nextInt(10));
        }
        String generatedCode = code.toString();
        logger.info("Generated OTP code: " + generatedCode);
        return generatedCode;
    }


    private OtpCode mapRowToOtpCode(ResultSet rs) throws SQLException {
        return new OtpCode(
                rs.getInt("id"),
                rs.getInt("user_id"),
                UUID.fromString(rs.getString("operation_id")),
                rs.getString("code"),
                Status.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("expires_at").toLocalDateTime()
        );
    }
}

