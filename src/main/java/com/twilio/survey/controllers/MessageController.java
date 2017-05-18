package com.twilio.survey.controllers;

import com.twilio.survey.models.Participant;
import com.twilio.survey.services.MediaService;
import com.twilio.survey.services.ParticipantService;
import com.twilio.survey.services.TranscriptService;
import com.twilio.survey.services.VocabularyService;
import com.twilio.survey.util.ParticipantParser;
import com.twilio.survey.util.TwiMLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by jbocharov on 5/18/17.
 */

@Controller
public class MessageController {
    @Autowired
    private VocabularyService vobabularyService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private TranscriptService transcriptService;

    public MessageController() {
    }

    /**
     * Message endpoint; Welcomes a user and encourages them to leave a message with their custom vocabulary.
     */
    @RequestMapping(value = "/message/call", method = RequestMethod.GET, produces = "application/xml")
    public void message(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Participant participant = ensureParticipantFromRequest(request);
        response.getWriter().print(getLeaveAMessageResponse(request, participant));
        response.setContentType("application/xml");
    }

    @RequestMapping(value = "/message/recording", method = RequestMethod.POST, produces = "application/xml")
    public void recording(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.getWriter().print("");
        response.setContentType("application/xml");
    }

    private String getLeaveAMessageResponse(HttpServletRequest request, Participant participant) throws Exception {
        final String message = "Please leave a message at the tone";
        final String recordingUrl = "/message/recording?pid=" + participant.getId().toString();
        return TwiMLUtil.voiceResponseWithRecordingCallback(message, recordingUrl);
    }

    //TODO (john@): DRY violation vs SurveyController class -  refactor into a util class
    private Participant ensureParticipantFromRequest(HttpServletRequest request) {
        final Participant requestParticipant = ParticipantParser.parseParticipant(request);

        final Participant savedParticipant = participantService.getByUnmaskedPhoneNumber(
                requestParticipant.getUnmaskedPhoneNumber()
        );

        return (savedParticipant != null)
                ? savedParticipant
                : participantService.save(ParticipantParser.parseParticipant(request));
    }
}
