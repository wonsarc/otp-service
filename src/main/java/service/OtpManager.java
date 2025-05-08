package service;

import dao.OtpDao;
import model.OtpCode;
import model.Status;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OtpManager {
    private static final Logger logger = Logger.getLogger(OtpManager.class.getName());
    private final OtpDao otpDao = new OtpDao();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public OtpManager() {
        startOtpExpirationCheck();
    }

    private void startOtpExpirationCheck() {
        scheduler.scheduleAtFixedRate(this::checkExpiredOtps, 0, 5, TimeUnit.SECONDS);
        logger.info("OTP expiration check scheduled to run every 5 seconds.");
    }

    private void checkExpiredOtps() {
        logger.info("Checking for expired OTP codes...");
        try {
            List<OtpCode> expiredOtps = otpDao.findExpiredOtps();

            for (OtpCode otp : expiredOtps) {
                otpDao.updateStatus(otp.id(), Status.EXPIRED.toString().toUpperCase());
                logger.info("OTP with ID " + otp.id() + " has expired and status updated.");
            }

            logger.info("OTP expiration check completed. Expired OTPs processed: " + expiredOtps.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking expired OTP codes", e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
