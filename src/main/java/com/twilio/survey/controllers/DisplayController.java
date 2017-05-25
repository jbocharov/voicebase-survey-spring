package com.twilio.survey.controllers;

import com.twilio.survey.models.Question;
import com.twilio.survey.models.Survey;
import com.twilio.survey.models.Transcript;
import com.twilio.survey.repositories.ResponseRepository;
import com.twilio.survey.repositories.SurveyRepository;
import com.twilio.survey.services.ResponseService;
import com.twilio.survey.services.SurveyService;
import com.twilio.survey.services.TranscriptService;
import com.twilio.survey.util.AppSetup;
import com.twilio.survey.util.CustomVocabularyHighlighter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class DisplayController {
    @Autowired
    private SurveyRepository surveyRepository;
    private SurveyService surveyService;
    @Autowired
    private ResponseRepository responseRepository;
    private ResponseService responseService;

    @Autowired
    private TranscriptService transcriptService;

    public DisplayController() {
    }

    /**
     * Renders the survey results
     *
     * @param model    Empty model where you fill in the data that the template will use
     * @param request  Standard HttpServletRequest request
     * @param response Standard HttpServletResponse response
     * @return returns the template's name
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Map<String, Object> model, HttpServletRequest request,
                        HttpServletResponse response) {
        this.surveyService = new SurveyService(surveyRepository);
        this.responseService = new ResponseService(responseRepository);

        Survey lastSurvey = surveyService.findLast();
        model.put("surveyTitle", lastSurvey.getTitle());

        List<Question> questions = lastSurvey.getQuestions();

        model.put("questions", questions);

        return "index";
    }

    /**
     * Renders the moderation view results
     *
     * @param model    Empty model where you fill in the data that the template will use
     * @param request  Standard HttpServletRequest request
     * @param response Standard HttpServletResponse response
     * @return returns the template's name
     */
    @RequestMapping(value = "/moderation", method = RequestMethod.GET)
    public String moderation(Map<String, Object> model, HttpServletRequest request,
                        HttpServletResponse response) {


        model.put("callInPhoneNumber", new AppSetup().getPhoneNumberHuman());

        final List<Transcript> transcripts = transcriptService.findAllReverseChronological();

        // Keeping dirty entities without saving makes Hiberate angry...
        // and we don't want to see Hiberate when it's angry
        final List<Map<String, Object>> normalizedTranscriptResults = normalizeTranscriptResults(transcripts);

        for (Map<String, Object> transcript: normalizedTranscriptResults) {
            final String termsList = (String) transcript.get("termsList");

            final String rawNovocabText = (String) transcript.get("novocabText");
            final String novocabText = (String) CustomVocabularyHighlighter.highlight(
                    rawNovocabText, termsList, "<b>", "</b>"
            );
            transcript.put("novocabText", novocabText);

            final String rawTranscriptText = (String) transcript.get("transcriptText");
            final String transcriptText = CustomVocabularyHighlighter.highlight(
                    rawTranscriptText, termsList, "<b>", "</b>"
            );
            transcript.put("transcriptText", transcriptText);

            final String highlightedTermsList = CustomVocabularyHighlighter.highlight(
                    termsList, termsList, "<b>", "</b>"
            );
            transcript.put("termsList", highlightedTermsList);

        }

        model.put("transcripts", normalizedTranscriptResults);

        return "moderation";
    }

    /**
     * Renders the demo view results
     *
     * @param model    Empty model where you fill in the data that the template will use
     * @param request  Standard HttpServletRequest request
     * @param response Standard HttpServletResponse response
     * @return returns the template's name
     */
    @RequestMapping(value = "/demo", method = RequestMethod.GET)
    public String demo(Map<String, Object> model, HttpServletRequest request,
                             HttpServletResponse response) {


        model.put("callInPhoneNumber", new AppSetup().getPhoneNumberHuman());

        final List<Transcript> transcripts = transcriptService.findAllRatedReverseChronological();

        // Keeping dirty entities without saving makes Hiberate angry...
        // and we don't want to see Hiberate when it's angry
        final List<Map<String, Object>> normalizedTranscriptResults = normalizeTranscriptResults(transcripts);

        for (Map<String, Object> transcript: normalizedTranscriptResults) {
            final String termsList = (String) transcript.get("termsList");
            final String rawTranscriptText = (String) transcript.get("transcriptText");
            final String transcriptText = CustomVocabularyHighlighter.highlight(rawTranscriptText, termsList, "<b>", "</b>");
            transcript.put("transcriptText", transcriptText);
        }

        model.put("transcripts", normalizedTranscriptResults);

        return "demo";
    }

    /**
     * Renders the demo view results
     *
     * @param response Standard HttpServletResponse response
     * @return returns the transcripts
     */
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @RequestMapping(value = "/results", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Object> results(
            HttpServletResponse response) {

        final List<Transcript> transcripts = transcriptService.findAllRatedReverseChronological();

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET");


        final Map<String, Object> responseEntity = new LinkedHashMap<>();

        // Wrk around Hiberate strangeness with responseEntity.put("transcripts", transcripts);
        final int transcriptCount = transcripts.size();
        List<Map<String, Object>> normalizedTranscriptResults = normalizeTranscriptResults(transcripts);

        responseEntity.put("transcriptCount", transcriptCount);
        responseEntity.put("transcripts", normalizedTranscriptResults);

        return responseEntity;
    }

    protected static List<Map<String, Object>> normalizeTranscriptResults(List<Transcript> transcripts) {

        final List<Map<String, Object>> normalizedTranscriptResults = new ArrayList<>();

        for (Transcript transcript: transcripts) {
            Map<String, Object> abstractedTranscript = new LinkedHashMap<>();
            abstractedTranscript.put("transcriptText", transcript.getTranscriptText());
            abstractedTranscript.put("novocabText", transcript.getNovocabText());
            abstractedTranscript.put("termsList", transcript.getTermsList());
            abstractedTranscript.put("phoneNumber", transcript.getPhoneNumber());
            abstractedTranscript.put("rating", transcript.getRating());
            abstractedTranscript.put("hasRating", transcript.getHasRating());
            abstractedTranscript.put("date", transcript.getDate());
            abstractedTranscript.put("id", transcript.getId());

            normalizedTranscriptResults.add(abstractedTranscript);
        }

        return normalizedTranscriptResults;
    }
}
