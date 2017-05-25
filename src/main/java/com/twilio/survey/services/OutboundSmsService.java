package com.twilio.survey.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.survey.util.AppSetup;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.twilio.example.Example.ACCOUNT_SID;

/**
 * Created by jbocharov on 5/25/17.
 */
@Service
public class OutboundSmsService {
    public OutboundSmsService() {
        final AppSetup appSetup = new AppSetup();
        final String ACCOUNT_SID = appSetup.getTwilioAccountSid();
        final String AUTH_TOKEN = appSetup.getTwilioAuthToken();
        outboundTwilioNumber = appSetup.getPhoneNumberTwilio();

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public void sendSms(String message, String recipientPhoneNumber) {
        try {
            logger.info("Sending SMS recipientPhoneNumber={}, outboundTwilioNumber={}, message={}",
                    recipientPhoneNumber, outboundTwilioNumber, message
            );

            Message twilioMessage = Message.creator(
                    new PhoneNumber(recipientPhoneNumber),
                    new PhoneNumber(outboundTwilioNumber),
                    message).create();

            logger.info("Sent SMS recipientPhoneNumber={}, outboundTwilioNumber={}, message={}, messageSid",
                    recipientPhoneNumber, outboundTwilioNumber, message, twilioMessage.getSid()
            );


        } catch (Throwable t) {
            logger.error("Failed to send recipientPhoneNumber={},message={}, error={}", recipientPhoneNumber, message, t);
        }
    }

    private final String outboundTwilioNumber;

    private final static Logger logger = LoggerFactory.getLogger(OutboundSmsService.class);
}
