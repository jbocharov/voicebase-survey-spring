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

        final String method = "POST";
        final List<String> includes = Lists.newArrayList("transcripts");
        final Configuration configuration = Configuration
                .defaultConfiguration()
                .withCallback(callbackUrl, method, includes);

        if ((customVocabularyTerms != null) && (! customVocabularyTerms.isEmpty())) {
            configuration.withCustomVocabulary(customVocabularyTerms);
        }

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

    public CallbackResponseEntity parseCallback(String callbackResponseJson) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final CallbackResponseEntity callbackResponseEntity = objectMapper.readValue(
                callbackResponseJson, CallbackResponseEntity.class
        );

        return callbackResponseEntity;
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
            return new Configuration(new ConfigurationEntity());
        }

        /**
         * Convenience: builds a configuration including specified custom terms:
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
        public Configuration withCustomVocabulary(List<String> terms) {

            if (configurationEntity.transcriptsSection == null) {
                configurationEntity.transcriptsSection = new TranscriptsSection();
            }

            final TranscriptsSection transcriptSection = configurationEntity.transcriptsSection;

            if (transcriptSection.vocabularyEntities == null) {
                transcriptSection.vocabularyEntities = new ArrayList<>();
            }

            final List<VocabularyEntity> vocabularyEntities = transcriptSection.vocabularyEntities;

            final VocabularyEntity vocabularyEntity = new VocabularyEntity(terms);

            vocabularyEntities.add(vocabularyEntity);

            return this;
        }

        /**
         * Convenience : builds a configuration including specified custom terms:
         * {
         *   "configuration": {
         *     "publish": {
         *       "callbacks": [
         *         {
         *           "url": url,
         *           "method": method,
         *           "include: [ includes ]
         *         }
         *       ]
         *     }
         *   }
         * }
         * @param terms list of custom terms as a string array
         * @return the configuration attachment for the VoiceBase v2-beta API
         */
        public Configuration withCallback(String url, String method, List<String> includes) {

            if (configurationEntity.publishSection == null) {
                configurationEntity.publishSection = new PublishSection();
            }

            final PublishSection publishSection = configurationEntity.publishSection;

            if (publishSection.callbackEntities == null) {
                publishSection.callbackEntities= new ArrayList<>();
            }

            final List<CallbackEntity> callbackEntities = publishSection.callbackEntities;

            final CallbackEntity callbackEntity = new CallbackEntity(url, method, includes);

            callbackEntities.add(callbackEntity);

            return this;
        }


        public Configuration(ConfigurationEntity configurationEntity) {
            this.configurationEntity = configurationEntity;
        }

        @JsonProperty("configuration")
        protected ConfigurationEntity configurationEntity;

        @JsonAutoDetect
        @JsonInclude(Include.NON_NULL)
        public static class ConfigurationEntity {

            public ConfigurationEntity() { }

            public ConfigurationEntity(TranscriptsSection transcriptsSection) {
                this.transcriptsSection = transcriptsSection;
            }

            public ConfigurationEntity(PublishSection publishSection) {
                this.publishSection = publishSection;
            }

            public ConfigurationEntity(TranscriptsSection transcriptsSection, PublishSection publishSection) {
                this.transcriptsSection = transcriptsSection;
                this.publishSection = publishSection;
            }

            @JsonProperty("transcripts")
            public TranscriptsSection transcriptsSection;

            @JsonProperty("publish")
            public PublishSection publishSection;
        }

        @JsonInclude(Include.NON_NULL)
        @JsonAutoDetect
        public static class TranscriptsSection {

            public TranscriptsSection() { }

            public TranscriptsSection(List<VocabularyEntity> vocabularyEntities) {
                this.vocabularyEntities = vocabularyEntities;
            }

            @JsonProperty("vocabularies")
            public List<VocabularyEntity> vocabularyEntities;
        }

        @JsonAutoDetect
        @JsonInclude(Include.NON_NULL)
        public static class VocabularyEntity {
            public VocabularyEntity(List<String> terms) {
                this.terms = terms;
            }

            @JsonProperty("terms")
            public List<String> terms;
        }

        @JsonInclude(Include.NON_NULL)
        @JsonAutoDetect
        public static class PublishSection {

            public PublishSection() { }

            public PublishSection(List<CallbackEntity> callbackEntities) {
                this.callbackEntities = callbackEntities;
            }

            @JsonProperty("callbacks")
            public List<CallbackEntity> callbackEntities;
        }

        @JsonInclude(Include.NON_NULL)
        @JsonAutoDetect
        public static class CallbackEntity {

            public CallbackEntity() { }

            public CallbackEntity(String url, String method, List<String> includes) {
                this.url = url;
                this.method = method;
                this.includes = includes;
            }

            @JsonProperty("url")
            public String url;

            @JsonProperty("method")
            public String method;

            @JsonProperty("include")
            public List<String> includes;
        }

    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadResponse {
        public String status;
        public String mediaId;
    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackResponseEntity {
        @JsonProperty("callback")
        public CallbackStatusEntity callbackStatus;

        @JsonProperty("media")
        public CallbackMediaEntity callbackMedia;
    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackStatusEntity {
        @JsonProperty("success")
        public Boolean success;
    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackMediaEntity {
        @JsonProperty("transcripts")
        public CallbackTranscriptsEntity callbackTranscripts;

        @JsonProperty("mediaId")
        public String mediaId;

        @JsonProperty("status")
        public String status;
    }

    @JsonAutoDetect
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackTranscriptsEntity {
        @JsonProperty("text")
        public String text;
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
