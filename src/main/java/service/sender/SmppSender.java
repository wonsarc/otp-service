package service.sender;

import config.Config;
import model.NotificationChannel;
import model.NotificationResult;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;

import java.util.logging.Logger;

public class SmppSender implements NotificationSender {
    private static final Logger logger = Logger.getLogger(SmppSender.class.getName());

    private final String smppHost;
    private final int smppPort;
    private final String smppSystemId;
    private final String smppPassword;
    private final String smppSystemType;
    private final String smppSourceAddr;

    public SmppSender() {
        this.smppHost = Config.get("smpp.host");
        this.smppPort = Config.getInt("smpp.port");
        this.smppSystemId = Config.get("smpp.system_id");
        this.smppPassword = Config.get("smpp.password");
        this.smppSystemType = Config.get("smpp.system_type");
        this.smppSourceAddr = Config.get("smpp.source_addr");
    }

    @Override
    public NotificationResult send(String code, NotificationChannel channel) {
        if (!isValidPhoneNumber(channel.address())) {
            return new NotificationResult(false, "Invalid phone number format", channel);
        }

        try {
            TCPIPConnection connection = new TCPIPConnection(smppHost, smppPort);
            Session session = new Session(connection);

            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(smppSystemId);
            bindRequest.setPassword(smppPassword);
            bindRequest.setSystemType(smppSystemType);
            bindRequest.setInterfaceVersion((byte) 0x34);
            bindRequest.setAddressRange(smppSourceAddr);

            BindResponse bindResponse = session.bind(bindRequest);
            if (bindResponse.getCommandStatus() != 0) {
                throw new Exception("Bind failed: " + bindResponse.getCommandStatus());
            }

            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(smppSourceAddr);
            submitSM.setDestAddr(channel.address());
            submitSM.setShortMessage("Your code: " + code);

            session.submit(submitSM);
            logger.info("SMS sent to " + channel.address());
            return new NotificationResult(true, null, channel);
        } catch (Exception e) {
            String error = "SMS sending failed: " + e.getMessage();
            logger.warning(error);
            return new NotificationResult(false, error, channel);
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+?[0-9]{10,15}$");
    }
}
