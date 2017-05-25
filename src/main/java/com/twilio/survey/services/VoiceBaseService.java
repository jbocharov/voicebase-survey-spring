package com.twilio.survey.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.twilio.survey.controllers.MessageController;
import com.twilio.survey.models.Participant;
import com.twilio.survey.models.Term;
import com.twilio.survey.models.Vocabulary;
import com.twilio.survey.util.AppSetup;
import com.twilio.survey.util.VoiceBaseV2BetaUnirestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jbocharov on 5/18/17.
 */

@Service
public class VoiceBaseService {
    /**
     * Returns a mediaId (on a successful upload) for the posted recording
     * @param recordingUrl the URL where the recording can be obtained
     * @param returnPath the URL where the VoiceBase callback should be returned
     * @return a VoiceBase mediaId
     */
    public String upload(final String recordingUrl, final String returnPath, Participant participant) throws IOException {
        return upload(recordingUrl, returnPath, participant, null);
    }

    public String upload(final String recordingUrl, final String returnPath, Participant participant, Vocabulary vocabulary) throws IOException {
        Preconditions.checkNotNull(recordingUrl);
        Preconditions.checkNotNull(returnPath);

        final String participantId = participant.getId().toString();
        final String vocabularyId = (vocabulary != null) ? vocabulary.getId().toString() : null;

        final String callbackUrl = returnPath
                + "?pid=" + participantId
                + (
                    (vocabularyId != null) ? "&vid=" + vocabularyId : ""
                );


        logger.info("Uploading to VoiceBase with recordingUrl={}, pacticipantId={}, vocabularyId={}, callbackUrl={}",
                recordingUrl, participantId, vocabularyId, callbackUrl);

        final String token = new AppSetup().getVoiceBaseToken();

        final List<String> listOfStringTerms = getListOfStringTerms(vocabulary);
        final String mediaId = voiceBaseClient().upload(token, recordingUrl, callbackUrl, listOfStringTerms);

        logger.info("Uploaded to VoiceBase mediaId={}, recordingUrl={}, pacticipantId={}, vocabularyId={}, callbackUrl={}",
                mediaId, recordingUrl, participantId, vocabularyId, callbackUrl);

        return mediaId;
    }

    public CallbackResult getTranscriptFromCallback(final String callbackResponseJson) throws IOException {
        Preconditions.checkNotNull(callbackResponseJson);

        final VoiceBaseV2BetaUnirestClient.CallbackResponseEntity entity =
                voiceBaseClient().parseCallback(callbackResponseJson);

        if (! entity.callbackStatus.success) {
            throw new IllegalArgumentException("Callback is not a success callback");
        }

        VoiceBaseV2BetaUnirestClient.CallbackMediaEntity mediaEntity = entity.callbackMedia;

        if (mediaEntity == null) {
            throw new IllegalArgumentException("Callback has no media entity");
        }

        if (mediaEntity.mediaId == null) {
            throw new IllegalArgumentException("mediaId is null");
        }

        final String mediaId = mediaEntity.mediaId;


        VoiceBaseV2BetaUnirestClient.CallbackTranscriptsEntity transcriptsEntity = mediaEntity.callbackTranscripts;

        if (transcriptsEntity.text == null) {
            throw new IllegalArgumentException("text is null");
        }

        return new CallbackResult(mediaId, transcriptsEntity.text);
    }

    public static class CallbackResult {

        public CallbackResult(String mediaId, String text) {
            this.mediaId = mediaId;
            this.text = text;
        }

        final public String mediaId;
        final public String text;
    }

    protected static List<String> getListOfStringTerms(Vocabulary vocabulary) {
        if (vocabulary == null) { return null; }
        final List<Term> terms = vocabulary.getTerms();

        if (terms == null || terms.isEmpty()) { return null; }

        final List<String> listOfStringTerms = new ArrayList<String>(terms.size());

        for (Term term : terms) {
            final Float floatWeight = term.getWeight();
            final String stringWeight = (floatWeight != null)
                    ? Integer.toString((int) (float) floatWeight)
                    : null;

            final String stringTerm = (stringWeight == null)
                    ? term.getTerm()
                    : term.getTerm() + ";" + stringWeight;

            listOfStringTerms.add(stringTerm);
        }

        for (String stringTerm : STANDARD_DEMO_TERMS) {
            listOfStringTerms.add(stringTerm);
        }

        return listOfStringTerms;
    }

    protected VoiceBaseV2BetaUnirestClient voiceBaseClient() {
        return new VoiceBaseV2BetaUnirestClient();
    }

    protected final static List<String> STANDARD_DEMO_TERMS = Lists.newArrayList(
            "VoiceBase", "Twilio;tweeleeoh;2"

    );


    private final static Logger logger = LoggerFactory.getLogger(VoiceBaseService.class);
}
