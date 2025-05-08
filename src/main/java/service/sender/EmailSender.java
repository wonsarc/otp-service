package service.sender;

import config.Config;
import model.NotificationChannel;
import model.NotificationResult;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

public class EmailSender implements NotificationSender {
    private static final Logger logger = Logger.getLogger(EmailSender.class.getName());

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;

    public EmailSender() {
        this.smtpHost = Config.get("mail.smtp.host");
        this.smtpPort = Config.getInt("mail.smtp.port");
        this.smtpUsername = Config.get("mail.smtp.username");
        this.smtpPassword = Config.get("mail.smtp.password");
        this.fromAddress = Config.get("mail.from");
    }

    @Override
    public NotificationResult send(String code, NotificationChannel channel) {
        if (!isValidEmail(channel.address())) {
            return new NotificationResult(false, "Invalid email format", channel);
        }

        Properties props = new Properties();
        props.putAll(getSmtpProperties());

        try {
            Session session = createSession(props);
            Message message = createMessage(session, channel.address(), code);
            Transport.send(message);
            logger.info("Email sent to " + channel.address());
            return new NotificationResult(true, null, channel);
        } catch (MessagingException e) {
            String error = "Email sending failed: " + getErrorMessage(e);
            logger.warning(error);
            return new NotificationResult(false, error, channel);
        }
    }

    private Properties getSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", Config.get("mail.smtp.auth"));
        props.put("mail.smtp.starttls.enable", Config.get("mail.smtp.starttls.enable"));
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        return props;
    }

    private Session createSession(Properties props) {
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }

    private Message createMessage(Session session, String toEmail, String code)
            throws MessagingException {

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("Your OTP Code");
        message.setText("Your verification code is: " + code);
        return message;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-_.+]+@([\\w-]+\\.)+[\\w-]{2,}$");
    }

    private String getErrorMessage(MessagingException e) {
        if (e instanceof AuthenticationFailedException) {
            return "SMTP authentication failed";
        } else if (e instanceof SendFailedException) {
            return "Invalid recipient address";
        }
        return e.getMessage();
    }
}