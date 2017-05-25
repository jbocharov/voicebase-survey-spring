package com.twilio.survey.controllers;

import com.google.common.base.Preconditions;
import com.twilio.survey.models.*;
import com.twilio.survey.services.*;
import com.twilio.survey.util.AppSetup;
import com.twilio.survey.util.ParticipantParser;
import com.twilio.survey.util.TwiMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

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

        final String returnPathUrl = new AppSetup().getReturnPathURL();
        final String vocabReturnPathUrl = returnPathUrl + "/message/callback-vocab";
        final String novocabReturnPathUrl = returnPathUrl + "/message/callback-novocab";

        final Participant participant = ensureParticipantFromRequest(request);
        final Vocabulary vocabulary = vocabularyService.findOneLatestByParticipant(participant);

        logger.info("Got a recording link url={}, duration={}, returnPathUrl={}, vocabulary={}",
                recordingUrl, recordingDuration, returnPathUrl, vocabulary);

        final String voicebaseMediaId = voiceBaseService.upload(recordingUrl, vocabReturnPathUrl, participant, vocabulary);
        logger.info("Started vocab transcription voicebaseMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                voicebaseMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary);
        final String novocabMediaId = voiceBaseService.upload(recordingUrl, novocabReturnPathUrl, participant);

        logger.info("Started no-vocab transcription novocabMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                novocabMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary);

        final Media media = mediaService.save(
                new Media(recordingUrl, voicebaseMediaId, novocabMediaId, participant, vocabulary, new Date())
        );

        final String termsList = generateStringTermsList(vocabulary);
        final String phoneNumber = participant.getPhoneNumber();

        final Transcript transcript = transcriptService.save(
                new Transcript(media, vocabulary, null, termsList, phoneNumber, new Date())
        );

        logger.info(
                "Created media databaseMediaId={}, transcriptId={}, voicebaseMediaId={}, novocabMediaId={}, url={}, duration={}, returnPathUrl={}, vocabulary={}",
                media.getId().toString(), transcript.getId().toString(), voicebaseMediaId, novocabMediaId, recordingUrl, recordingDuration, returnPathUrl, vocabulary
        );
        response.getWriter().print("");
        response.setContentType("application/xml");
    }

    @RequestMapping(value = "/message/moderate", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> moderate(@RequestParam("tid") Long transcriptId,
                         @RequestParam("rating") Integer rating) throws Exception {
        logger.info("Moderation: rating transcriptId={}, rating={}");

        final Transcript transcript = transcriptService.find(transcriptId);
        if (transcript == null) { throw new IllegalArgumentException("tid"); }
        transcript.setRating(rating);
        transcriptService.save(transcript);

        final Map<String, Object> response  = new HashMap<String, Object>();

        response.put("tid", transcriptId);
        response.put("rating", rating);
        response.put("success", true);

        logger.info("Moderation: rated transcriptId={}, rating={}");
        return response;
    }

    @RequestMapping(value = "/message/callback-vocab", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void callbackVocab(@RequestParam("pid") String participantId,
                         @RequestParam("vid") String vocabularyId,
                         @RequestBody String jsonString) throws Exception {

        final Participant participant = participantService.find(Long.parseLong(participantId));
        final Vocabulary vocabulary = vocabularyService.find(Long.parseLong(vocabularyId));

        logger.info("Got callback (vocab) participantId={}, vocabularyId={}, body={}", participantId, vocabularyId, jsonString);

        VoiceBaseService.CallbackResult callbackResult = voiceBaseService.getTranscriptFromCallback(jsonString);

        final String mediaId = callbackResult.mediaId;

        logger.info("Got callback (vocab) participantId={}, vocabularyId={}, mediaId={}, text={}",
                participantId, vocabularyId, mediaId, callbackResult.text);

        final Media media = mediaService.findOneLatestByVoiceBaseMediaId(mediaId);

        logger.info("Got media record (vocab) databaseMediaId={}, participantId={}, vocabularyId={}, mediaId={}, text={}",
            media.getId(), participantId, vocabularyId, mediaId, callbackResult.text);

        final Transcript transcript = transcriptService.findOneLatestByMedia(media);

        logger.info("Got transcript record (vocab) transcriptId={}, databaseMediaId={}, participantId={}, vocabularyId={}, mediaId={}, text={}",
                transcript.getId(), media.getId(), participantId, vocabularyId, mediaId, callbackResult.text);

        transcript.setTranscriptText(callbackResult.text);
        transcriptService.save(transcript);

        logger.info("Updated transcript record (vocab) transcriptId={}, databaseMediaId={}, participantId={}, vocabularyId={}, mediaId={}, text={}",
                transcript.getId(), media.getId(), participantId, vocabularyId, mediaId, callbackResult.text);
    }

    @RequestMapping(value = "/message/callback-novocab", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void callbackNovocab(@RequestParam("pid") String participantId,
                         @RequestBody String jsonString) throws Exception {

        final Participant participant = participantService.find(Long.parseLong(participantId));


        logger.info("Got callback (novocab) participantId={}, vocabularyId={}, body={}", participantId, jsonString);

        VoiceBaseService.CallbackResult callbackResult = voiceBaseService.getTranscriptFromCallback(jsonString);

        final String mediaId = callbackResult.mediaId;

        logger.info("Got callback (novocab) participantId={}, mediaId={}, text={}",
                participantId,  mediaId, callbackResult.text);

        final Media media = mediaService.findOneLatestByNovocabMediaId(mediaId);

        logger.info("Got media record (novocab) databaseMediaId={}, participantId={},  mediaId={}, text={}",
                media.getId(), participantId, mediaId, callbackResult.text);

        final Transcript transcript = transcriptService.findOneLatestByMedia(media);

        logger.info("Got transcript record (novocab) transcriptId={}, databaseMediaId={}, participantId={}, mediaId={}, text={}",
                transcript.getId(), media.getId(), participantId, mediaId, callbackResult.text);

        transcript.setNovocabText(callbackResult.text);
        transcriptService.save(transcript);

        logger.info("Updated transcript record (novocab) transcriptId={}, databaseMediaId={}, participantId={}, mediaId={}, text={}",
                transcript.getId(), media.getId(), participantId, mediaId, callbackResult.text);
    }


    protected String getLeaveAMessageResponse(HttpServletRequest request, Participant participant) throws Exception {
        final String message = "At the tone, please introduce yourself and leave a short message. Be sure to use the unique terms you provided!";
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

    protected String generateStringTermsList(Vocabulary vocabulary) {
        Preconditions.checkNotNull(vocabulary);
        final List<Term> terms = vocabulary.getTerms();

        final List<String> termStrings = new ArrayList<>(terms.size());

        for (Term term: terms) {
            termStrings.add(term.getTerm().replaceAll(",", ""));
        }

        return String.join(", ", termStrings);
    }

    protected final static String RECORDING_URL = "RecordingUrl";
    protected final static String RECORDING_DURATION = "RecordingDuration";
    protected final static String DOT_WAV = ".wav";

    private final static Logger logger = LoggerFactory.getLogger(MessageController.class);
}
