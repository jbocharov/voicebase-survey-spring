package com.twilio.survey.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.IOUtils;


/**
 * Created by jbocharov on 5/18/17.
 */
public class VoiceBaseV2BetaUnirestClient {

    /**
     * Returns a mediaId (on a successful upload
     * @param token the VoiceBase Bearer token for the request
     * @param recordingUrl the URL to the recording
     * @param callbackUrl the URL for the callback response
     * @param customVocabularyTerms (nullable) list of customVocabulary Terms
     * @return mediaId of the upload
     */
    public String upload(String token, String recordingUrl, String callbackUrl, List<String> customVocabularyTerms) throws IOException {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(recordingUrl);
        Preconditions.checkNotNull(callbackUrl);

        final Configuration configuration = ((customVocabularyTerms != null) && (! customVocabularyTerms.isEmpty()))
                ? Configuration.defaultConfiguration()
                : Configuration.withCustomVocabulary(customVocabularyTerms);

        final ObjectWriter ow = new ObjectMapper().writer();

        final String configurationString = ow.writeValueAsString(configuration);

        logger.info(">> Uploading to VoiceBase configuration={}, recordingUrl={}, callbackUrl = {}",
                configurationString, recordingUrl, callbackUrl);

        try {
            try ( AutoDeletingTempFile mediaUrlTempFile = new AutoDeletingTempFile();
                    AutoDeletingTempFile configurationTempFile = new AutoDeletingTempFile()) {

                // Workaround to force Unirest to correct do MIME multipart
                writeToAutoDeletingTempFile(mediaUrlTempFile, recordingUrl);
                writeToAutoDeletingTempFile(configurationTempFile, configurationString);

                HttpResponse<UploadResponse> uploadHttpResponse = post(MEDIA_POST_URL)
                        .header("Authorization", "Bearer " + token)
                        .field("mediaUrl", mediaUrlTempFile.getFile().toFile())
                        .field("configuration", configurationTempFile.getFile().toFile())
                        .asObject(UploadResponse.class);

                logger.info(">> Uploading to VoiceBase uploadResponse={}, configuration={}, recordingUrl={}, callbackUrl = {}",
                        uploadHttpResponse.toString(), configurationString, recordingUrl, callbackUrl);

                final String mediaId = getMediaId(uploadHttpResponse);

                logger.info(">> Uploaded to VoiceBase mediaId={}, configuration={}, recordingUrl={}, callbackUrl = {}",
                        mediaId, configurationString, recordingUrl, callbackUrl);

                return mediaId;
            }
        } catch (UnirestException | IOException e) {
            logger.info(">> Failed on upload to VoiceBase e={}, configuration={}, recordingUrl={}, callbackUrl = {}",
                    e.toString(), configurationString, recordingUrl, callbackUrl);
            throw new IllegalStateException(e);
        }
    }

    protected static void writeToAutoDeletingTempFile(AutoDeletingTempFile tempFile, String content) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(tempFile.getFile().toFile())) {
            writer.print(content);
        }
    }

    protected static byte[] stringToByteArray(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    protected static InputStream stringToInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    protected static String getMediaId(final HttpResponse<UploadResponse> uploadHttpResponse) throws IOException {
        final int statusCode = uploadHttpResponse.getStatus();

        if (statusCode != 200)  {
            throw new IOException("Received error status code from VoiceBase: " + Integer.toString(statusCode));
        }

        final Object bodyObject = uploadHttpResponse.getBody();



        final UploadResponse uploadResponse = guardedGetUploadResponse(bodyObject);

        if (uploadResponse == null) {
            throw new IOException("Received null response entity from VoiceBase");
        }

        final String voiceBaseStatus = uploadResponse.status;

        if (! "accepted".equals(voiceBaseStatus)) {
            throw new IOException("Got status instead of accepted: " + voiceBaseStatus);
        }

        final String mediaId = uploadResponse.mediaId;

        if (mediaId == null) {
            throw new IOException("Got null mediaId");
        }

        return mediaId;
    }

    protected static UploadResponse guardedGetUploadResponse(Object bodyObject) {
        if (bodyObject == null || bodyObject instanceof UploadResponse) {
            return (UploadResponse) bodyObject;
        } else {
            ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final UploadResponse uploadResponse = jacksonObjectMapper.convertValue(bodyObject, UploadResponse.class);
            return uploadResponse;
        }
    }

    /** Overridable with a mock for unit testing. */
    protected static HttpRequestWithBody post(final String url) {
        return Unirest.post(url);
    }

    @JsonAutoDetect
    public static class Configuration {
        /**
         * Convenience constructor: builds a default minimal configuration:
         * {
         *   "configuration": { }
         * }
         * @return
         */
        public static Configuration defaultConfiguration() {
            return new Configuration(new ConfigurationEntity(null));
        }

        /**
         * Convenience constructor: builds a configuration including specified custom terms:
         * {
         *   "configuration": {
         *     "transcripts": {
         *       "vocabularies": [
         *         {
         *           "terms": terms
         *         }
         *       ]
         *     }
         *   }
         * }
         * @param terms list of custom terms as a string array
         * @return the configuration attachment for the VoiceBase v2-beta API
         */
        public static Configuration withCustomVocabulary(List<String> terms) {
            final VocabularyEntity vocabularyEntity = new VocabularyEntity(terms);
            final TranscriptsSection transcriptsSection = new TranscriptsSection(Lists.newArrayList(vocabularyEntity));
            final ConfigurationEntity configurationEntity = new ConfigurationEntity(transcriptsSection);
            final Configuration configuration = new Configuration(configurationEntity);

            return configuration;
        }



        public Configuration(ConfigurationEntity configurationEntity) {
            this.configurationEntity = configurationEntity;
        }

        @JsonProperty("configuration")
        protected ConfigurationEntity configurationEntity;

        @JsonAutoDetect
        @JsonInclude(Include.NON_NULL)
        public static class ConfigurationEntity {
            public ConfigurationEntity(TranscriptsSection transcriptsSection) {
                this.transcriptsSection = transcriptsSection;
            }

            @JsonProperty("transcripts")
            protected TranscriptsSection transcriptsSection;
        }

        @JsonInclude(Include.NON_NULL)
        @JsonAutoDetect
        public static class TranscriptsSection {
            public TranscriptsSection(List<VocabularyEntity> vocabularyEntities) {
                this.vocabularyEntities = vocabularyEntities;
            }

            @JsonProperty("vocabularies")
            protected List<VocabularyEntity> vocabularyEntities;
        }

        @JsonAutoDetect
        @JsonInclude(Include.NON_NULL)
        public static class VocabularyEntity {
            public VocabularyEntity(List<String> terms) {
                this.terms = terms;
            }

            @JsonProperty("terms")
            protected List<String> terms;
        }
    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadResponse {
        public String status;
        public String mediaId;
    }

    // From: http://stackoverflow.com/questions/34049328/handle-temporary-file-in-try-with-resources
    public static class AutoDeletingTempFile implements AutoCloseable {

        private final Path file;

        public AutoDeletingTempFile() throws IOException {
            file = Files.createTempFile(null, null);
        }

        public Path getFile() {
            return file;
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(file);
        }
    }

    /** Necessary since Unirest does not natively use Jackson's ObjectMapper */

    static {
        Unirest.setObjectMapper(new com.mashape.unirest.http.ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public Object readValue(String value) {
                try {
                    return jacksonObjectMapper.readValue(value, Object.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    protected final static String VOICEBASE_V2_BETA_URL = "https://apis.voicebase.com/v2-beta";
    protected final static String MEDIA_POST_URL = VOICEBASE_V2_BETA_URL + "/media";
    //protected final static String MEDIA_POST_URL  = "https://requestb.in/1jqyjby1";
    //protected final static String MEDIA_POST_URL  = "http://localhost:8081/media";

    private final static Logger logger = LoggerFactory.getLogger(VoiceBaseV2BetaUnirestClient.class);
}
