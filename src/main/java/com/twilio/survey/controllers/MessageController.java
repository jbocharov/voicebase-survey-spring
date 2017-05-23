package com.twilio.survey.controllers;

import com.twilio.survey.models.Media;
import com.twilio.survey.models.Participant;
import com.twilio.survey.models.Vocabulary;
import com.twilio.survey.services.*;
import com.twilio.survey.util.AppSetup;
import com.twilio.survey.util.ParticipantParser;
import com.twilio.survey.util.TwiMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

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

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private VoiceBaseService voiceBaseService;

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
        final String recordingUrl = getRecordingUrl(request);
        final Long recordingDuration = getRecordingDuration(request);
        final String returnPathUrl = new AppSetup().getReturnPathURL() + "/message/callback";
        final Participant participant = ensureParticipantFromRequest(request);
        final Vocabulary vocabulary = vocabularyService.findOneLatestByParticipant(participant);

        logger.info("Got a recording link url={}, duration={}, returnPathUrl={}, vocabulary={}",
                recordingUrl, recordingDuration, returnPathUrl, vocabulary);

        final String voicebaseMediaId = voiceBaseService.upload(recordingUrl, returnPathUrl, participant, vocabulary);
        logger.info("Started vocab transcription voicebaseMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                voicebaseMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary);
        final String novocabMediaId = voiceBaseService.upload(recordingUrl, returnPathUrl, participant);

        logger.info("Started no-vocab transcription novocabMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                novocabMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary);

        final Media media = mediaService.save(
                new Media(recordingUrl, voicebaseMediaId, novocabMediaId, participant, vocabulary, new Date())
        );

        logger.info(
                "Created media databaseMediaId={}, voicebaseMediaId={}, novocabMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                media.getId().toString(), voicebaseMediaId, novocabMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary
        );
        response.getWriter().print("");
        response.setContentType("application/xml");
    }

    @RequestMapping(value = "/message/callback", method = RequestMethod.POST, consumes = "application/json")
    public void callback(@RequestParam("pid") String participantId,
                         @RequestParam(value = "vid", required = false) String vocabularyId,
                         @RequestBody String jsonString) throws Exception {

        final Participant participant = participantService.find(Long.parseLong(participantId));
        final Vocabulary vocabulary = (vocabularyId != null)
                ? vocabularyService.find(Long.parseLong(vocabularyId))
                : null;
        logger.info("Got callback participantId={}, vocabularyId={}, body={}", participantId, vocabularyId, jsonString);

        VoiceBaseService.CallbackResult callbackResult = voiceBaseService.getTranscriptFromCallback(jsonString);

        final String mediaId = callbackResult.mediaId;

        logger.info("Got callback participantId={}, vocabularyId={}, mediaId={}, text={}",
                participantId, vocabularyId, mediaId, callbackResult.text);

    }


    protected String getLeaveAMessageResponse(HttpServletRequest request, Participant participant) throws Exception {
        final String message = "Please leave a message at the tone and tell us what your learned today. Be creative and use the unique terms you provided";
        final String recordingUrl = "/message/recording?pid=" + participant.getId().toString();
        return TwiMLUtil.voiceResponseWithRecordingCallback(message, recordingUrl);
    }

    protected String getRecordingUrl(final HttpServletRequest request) {
        final String rawRecordingUrl = request.getParameter(RECORDING_URL);
        return (rawRecordingUrl != null) ? rawRecordingUrl + DOT_WAV : null;
    }

    protected Long getRecordingDuration(final HttpServletRequest request) {
        final String rawRecordingDuration = request.getParameter(RECORDING_DURATION);
        return (rawRecordingDuration != null) ? Long.parseLong(rawRecordingDuration) : null;
    }

    //TODO (john@): DRY violation vs SurveyController class -  refactor into a util class
    protected Participant ensureParticipantFromRequest(HttpServletRequest request) {
        final Participant requestParticipant = ParticipantParser.parseParticipant(request);

        final Participant savedParticipant = participantService.getByUnmaskedPhoneNumber(
                requestParticipant.getUnmaskedPhoneNumber()
        );

        return (savedParticipant != null)
                ? savedParticipant
                : participantService.save(ParticipantParser.parseParticipant(request));
    }

    protected final static String RECORDING_URL = "RecordingUrl";
    protected final static String RECORDING_DURATION = "RecordingDuration";
    protected final static String DOT_WAV = ".wav";

    private final static Logger logger = LoggerFactory.getLogger(MessageController.class);
}
