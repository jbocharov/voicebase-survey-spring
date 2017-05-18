package com.twilio.survey.controllers;

import com.twilio.survey.models.*;
import com.twilio.survey.repositories.QuestionRepository;
import com.twilio.survey.repositories.ResponseRepository;
import com.twilio.survey.services.*;
import com.twilio.survey.util.ResponseParser;
import com.twilio.survey.util.TwiMLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Date;

@Controller
public class ResponseController {
    @Autowired
    private QuestionRepository questionRepository;
    private QuestionService questionService;
    @Autowired
    private ResponseRepository responseRepository;
    private ResponseService responseService;

    @Autowired
    private ParticipantService participantService;
    @Autowired
    private VocabularyService vocabularyService;
    @Autowired
    private TermService termService;

    public ResponseController() {
    }

    /**
     * End point that saves a question response and redirects the call to the next question,
     * if one is available.
     */
    @RequestMapping(value = "/save_response", method = RequestMethod.POST, produces="application/xml")
    public void save(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter responseWriter = response.getWriter();
        this.questionService = new QuestionService(questionRepository);
        this.responseService = new ResponseService(responseRepository);

        Question currentQuestion = getQuestionFromRequest(request);
        Participant currentParticipant = getParticipantFromRequest(request);
        Vocabulary currentVocabulary = getVocabularyFromRequest(request);
        Survey survey = currentQuestion.getSurvey();

        final Response responseEntity = persistResponse(new ResponseParser(currentQuestion, currentParticipant, request).parse());
        final String termString = responseEntity.getResponse();
        final Term term = termService.save(new Term(termString, currentVocabulary, new Date()));

        if (survey.isLastQuestion(currentQuestion)) {
            String message = "Tank you for taking the " + survey.getTitle() + " survey. Good Bye";
            if (request.getParameter("MessageSid") != null) {
                responseWriter.print(TwiMLUtil.messagingResponse(message));
            } else {
                responseWriter.print(TwiMLUtil.voiceResponse(message));
            }
        } else {
            responseWriter.print(TwiMLUtil.redirect(survey.getNextQuestionNumber(currentQuestion), survey, currentVocabulary));
        }
        response.setContentType("application/xml");
    }

    private Response persistResponse(Response questionResponse) {
        Question currentQuestion = questionResponse.getQuestion();
        Response previousResponse = responseService.getBySessionSidAndQuestion(questionResponse.getSessionSid(), currentQuestion);
        if (previousResponse != null) {
            // it's already answered. That's an update from Twilio API (Transcriptions, for instance)
            questionResponse.setId(previousResponse.getId());
        }

        /** creates the question response on the db */
        return responseService.save(questionResponse);
    }

    private Question getQuestionFromRequest(HttpServletRequest request) {
        return questionService.find(Long.parseLong(request.getParameter("qid")));
    }

    private Participant getParticipantFromRequest(HttpServletRequest request) {
        return participantService.find(Long.parseLong(request.getParameter("pid")));
    }

    private Vocabulary getVocabularyFromRequest(HttpServletRequest request) {
        return vocabularyService.find(Long.parseLong(request.getParameter("vid")));
    }
}
