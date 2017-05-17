package com.twilio.survey.util;

/**
 * Created by jbocharov on 5/17/17.
 */

import javax.servlet.http.HttpServletRequest;
import com.twilio.survey.models.Participant;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ParticipantParserTest {
    @Test
    public void testUS10DigitPhoneNumberMasking() {
        final String unmaskedPhoneNumber = "+14155551212";
        final String expectedMaskedPhoneNumber = "+1415*****12";

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(FROM_QUERY_STRING_PARAMETER)).thenReturn(unmaskedPhoneNumber);

        Participant participant = ParticipantParser.parseParticipant(request);

        assertNotNull(participant);
        final String actualUnmaskedPhoneNumber = participant.getUnmaskedPhoneNumber();
        final String actualMaskedPhoneNumber = participant.getPhoneNumber();

        assertNotNull(participant.getDate());
        assertEquals("masked phone numbers must match", expectedMaskedPhoneNumber, actualMaskedPhoneNumber);
        assertEquals("unmasked phone numbers must match", unmaskedPhoneNumber, actualUnmaskedPhoneNumber);

    }

    final String FROM_QUERY_STRING_PARAMETER = "From";
}
