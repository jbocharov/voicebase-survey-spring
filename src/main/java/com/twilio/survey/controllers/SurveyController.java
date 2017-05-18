package com.twilio.survey.controllers;

import com.twilio.survey.models.Participant;
import com.twilio.survey.models.Survey;
import com.twilio.survey.models.Term;
import com.twilio.survey.models.Vocabulary;
import com.twilio.survey.repositories.SurveyRepository;
import com.twilio.survey.services.ParticipantService;
import com.twilio.survey.services.SurveyService;
import com.twilio.survey.services.VocabularyService;
import com.twilio.survey.util.ParticipantParser;
import com.twilio.survey.util.TwiMLUtil;
import com.twilio.twiml.TwiML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class SurveyController {
    @Autowired
    private SurveyRepository surveyRepository;
    private SurveyService surveyService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private VocabularyService vocabularyService;

    public SurveyController() {
    }

    /**
     * Message endpoint; Welcomes a user and encourages them to leave a message with their custom vocabulary.
     */
    @RequestMapping(value = "/message/call", method = RequestMethod.GET)
    public void message(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Participant participant = ensureParticipantFromRequest(request);
        response.getWriter().print(getLeaveAMessageResponse(request, participant));
        response.setContentType("application/xml");
    }

    @RequestMapping(value = "/message/recording", method = RequestMethod.POST)
    public void recording(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.getWriter().print("");
        response.setContentType("application/xml");
    }

    /**
     * Calls endpoint; Welcomes a user and redirects to the question controller if there is a survey to be answered.
     * Otherwise it plays a message and hang up the call if there is no survey available.
     */
    @RequestMapping(value = "/survey/call", method = RequestMethod.GET)
    public void call(HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.surveyService = new SurveyService(surveyRepository);

        Survey lastSurvey = surveyService.findLast();

        if (lastSurvey != null) {
            Participant participant = ensureParticipantFromRequest(request);
            final Vocabulary vocabulary = vocabularyService.save(new Vocabulary(participant, new Date()));
            response.getWriter().print(getFirstQuestionRedirect(lastSurvey, request, participant, vocabulary));
        } else {
            response.getWriter().print(getHangupResponse(request));
        }
        response.setContentType("application/xml");
    }

    /**
     * SMS endpoint; Welcomes a user and redirects to the question controller if there is a survey to be answered.
     * As SMS is just a message instead of a long running call, we store state by mapping a Twilio's Cookie to a Session
     */
    @RequestMapping(value = "/survey/sms", method = RequestMethod.GET)
    public void sms(HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.surveyService = new SurveyService(surveyRepository);

        Survey lastSurvey = surveyService.findLast();
        HttpSession session = request.getSession(false);

        if (lastSurvey != null) {
            final Participant participant = ensureParticipantFromRequest(request);
            if (session == null || session.isNew()) {
                // New session,
                final Vocabulary vocabulary = vocabularyService.save(new Vocabulary(participant, new Date()));
                response.getWriter().print(getFirstQuestionRedirect(lastSurvey, request, participant, vocabulary));
            } else {
                // Ongoing session, redirect to ResponseController to save it's answer.
                response.getWriter().print(getSaveResponseRedirect(session, participant));
            }
        } else {
            // No survey
            response.getWriter().print(getHangupResponse(request));
        }
        response.setContentType("application/xml");
    }

    private String getSaveResponseRedirect(HttpSession session, Participant participant) throws Exception {
        String saveURL = "/save_response?qid="
                + getQuestionIdFromSession(session)
                + "&pid="
                + participant.getId().toString()
                + "&vid="
                + getVocabularyIdFromSession(session);
        return TwiMLUtil.redirectPost(saveURL);
    }

    /**
     * Creates the TwiMLResponse for the first question of the survey
     *
     * @param survey  Survey entity
     * @param request HttpServletRequest request
     * @return TwiMLResponse
     */
    private String getFirstQuestionRedirect(Survey survey,
                                            HttpServletRequest request,
                                            Participant participant,
                                            Vocabulary vocabulary) throws Exception {
        String welcomeMessage = "Welcome to the " + survey.getTitle() + " survey";
        String questionURL = "/question?survey="
                + survey.getId()
                + "&question=1&pid="
                + participant.getId().toString()
                + "&vid="
                + vocabulary.getId().toString();
        if (request.getParameter("MessageSid") != null) {
            return TwiMLUtil.messagingResponseWithRedirect(welcomeMessage, questionURL);
        } else {
            return TwiMLUtil.voiceResponseWithRedirect(welcomeMessage, questionURL);
        }
    }

    /**
     * Creates a TwiMLResponse if no surveys are found on the database
     * For SMS, it's just a message
     * For Voice it should also send a Hangup to the ongoing call
     *
     * @return TwiMLResponse
     */
    private String getHangupResponse(HttpServletRequest request) throws Exception {
        String errorMessage = "We are sorry, there are no surveys available. Good bye.";
        cleanSession(request);
        if (request.getParameter("MessageSid") != null) {
            return TwiMLUtil.messagingResponse(errorMessage);
        } else {
            return TwiMLUtil.voiceResponse(errorMessage);
        }
    }

    private String getLeaveAMessageResponse(HttpServletRequest request, Participant participant) throws Exception {
        final String message = "Please leave a message at the tone";
        final String recordingUrl = "/message/recording?pid=" + participant.getId().toString();
        return TwiMLUtil.voiceResponseWithRecordingCallback(message, recordingUrl);
    }

    private void cleanSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private Long getQuestionIdFromSession(HttpSession session) {
        return (Long) session.getAttribute("questionId");
    }

    private Long getVocabularyIdFromSession(HttpSession session) {
        return (Long) session.getAttribute("vocabularyId");
    }

    private Participant ensureParticipantFromRequest(HttpServletRequest request) {
        final Participant requestParticipant = ParticipantParser.parseParticipant(request);

        final Participant savedParticipant = participantService.getByUnmaskedPhoneNumber(
                requestParticipant.getUnmaskedPhoneNumber()
        );

        return (savedParticipant != null)
                ? savedParticipant
                : participantService.save(ParticipantParser.parseParticipant(request));
    }

    private final static Logger logger = LoggerFactory.getLogger(SurveyController.class);
}
