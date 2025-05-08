package service;

import dao.OtpDao;
import model.OtpCode;
import model.Status;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

public class OtpService {
    private static final Logger logger = Logger.getLogger(OtpService.class.getName());
    private final OtpDao otpDao = new OtpDao();
    private final OtpConfigService configService = new OtpConfigService();

    public String generate(int userId, UUID operationId) throws SQLException {
        int digits = configService.getCodeLength();
        int lifetimeSeconds = configService.getTtlSeconds();

        logger.info("Generating OTP for userId=" + userId + ", operationId=" + operationId + ", digits=" + digits + ", lifetime=" + lifetimeSeconds);

        String code = OtpDao.generateCode(digits);
        LocalDateTime now = LocalDateTime.now();
        OtpCode otp = new OtpCode(0, userId, operationId, code, Status.ACTIVE, now, now.plusSeconds(lifetimeSeconds));

        otpDao.save(otp);
        logger.info("OTP generated and saved: " + otp);
        return code;
    }

    public boolean validate(int userId, String code, UUID operationId) throws SQLException {
        logger.info("Validating OTP for userId=" + userId + ", code=" + code + ", operationId=" + operationId);

        OtpCode otp = otpDao.findActiveByUser(userId, code);
        if (otp == null) {
            logger.warning("OTP not found or inactive for userId=" + userId + ", code=" + code);
            return false;
        }

        if (!otp.operationId().equals(operationId)) {
            logger.warning("OTP operation ID mismatch for userId=" + userId + ", code=" + code +
                    ". Expected: " + operationId + ", Found: " + otp.operationId());
            return false;
        }

        if (otp.expiresAt().isBefore(LocalDateTime.now())) {
            otpDao.updateStatus(otp.id(), Status.EXPIRED.toString().toUpperCase());
            logger.warning("OTP expired for userId=" + userId + ", code=" + code);
            return false;
        }

        otpDao.updateStatus(otp.id(), Status.USED.toString().toUpperCase());
        logger.info("OTP validated successfully for userId=" + userId + ", code=" + code);
        return true;
    }
}
