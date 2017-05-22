package com.twilio.survey.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Created by jbocharov on 5/18/17.
 */
public class VoiceBaseV2BetaClient {
    /**
     * Returns a mediaId (on a successful upload
     * @param token the VoiceBase Bearer token for the request
     * @param recordingUrl the URL to the recording
     * @param callbackUrl the URL for the callback response
     * @param customVocabularyTerms (nullable) list of customVocabulary Terms
     * @return
     */
    public String upload(String token, String recordingUrl, String callbackUrl, List<String> customVocabularyTerms) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(recordingUrl);
        Preconditions.checkNotNull(callbackUrl);

        //Unirest.post

        return null;
    }

    protected static InputStream stringToInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }
}
