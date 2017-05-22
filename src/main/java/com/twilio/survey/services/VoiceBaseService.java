package com.twilio.survey.services;

import com.google.common.base.Preconditions;
import com.twilio.survey.controllers.MessageController;
import com.twilio.survey.models.Participant;
import com.twilio.survey.models.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    public String upload(final String recordingUrl, final String returnPath, Participant participant) {
        return upload(recordingUrl, returnPath, participant, null);
    }

    public String upload(final String recordingUrl, final String returnPath, Participant participant, Vocabulary vocabulary) {
        Preconditions.checkNotNull(recordingUrl);
        Preconditions.checkNotNull(returnPath);

        final String participantId = participant.getId().toString();
        final String vocabularyId = (vocabulary != null) ? vocabulary.getId().toString() : null;

        final String callbackUrl = returnPath + "/messages/callback?"
                + "pid=" + participantId
                + (
                    (vocabularyId != null) ? "&vid=" + vocabularyId : ""
                );


        logger.info("Uploading to VoiceBase with recordingUrl={}, pacticipantId={}, vocabularyId={}, callbackUrl={}",
                recordingUrl, participantId, vocabularyId, callbackUrl);

        return null;

    }

    private final static Logger logger = LoggerFactory.getLogger(VoiceBaseService.class);
}
