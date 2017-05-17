package com.twilio.survey.util;

import com.twilio.survey.models.Participant;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jbocharov on 5/16/17.
 */
public class ParticipantParser {
    public static Participant parseParticipant(final HttpServletRequest request) {
        final String unmaskedPhoneNumber = request.getParameter(FROM);
        final String phoneNumber = mask(unmaskedPhoneNumber);

        return new Participant(phoneNumber, unmaskedPhoneNumber, new Date());
    }

    /** Transforms +14155551212 into +1415*****12 */
    public static String mask(final String phoneNumber) {
        final int phoneNumberLength = phoneNumber.length();

        final StringBuilder stringBuilder = new StringBuilder(phoneNumberLength);

        for (int index = 0; index < phoneNumberLength; index ++) {
            boolean maskChar = (index >= KEEP_FIRST) && (index < phoneNumberLength - KEEP_LAST);
            stringBuilder.append(maskChar ? MASK : phoneNumber.charAt(index));
        }

        final String maskedPhoneNumber = stringBuilder.toString();
        return maskedPhoneNumber;
    }

    protected final static String FROM = "From";
    protected final static int KEEP_FIRST = 5;
    protected final static int KEEP_LAST = 2;

    protected final static char MASK = '*';
}
