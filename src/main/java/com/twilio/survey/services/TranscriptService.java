package com.twilio.survey.services;

/**
 * Created by jbocharov on 5/16/17.
 */

import com.twilio.survey.models.Transcript;
import com.twilio.survey.models.Media;
import com.twilio.survey.repositories.MediaRepository;
import com.twilio.survey.repositories.TranscriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TranscriptService {
    private TranscriptRepository transcriptRepository;

    @Autowired
    public TranscriptService(TranscriptRepository transcriptRepository) { this.transcriptRepository = transcriptRepository; }

    public Transcript save(Transcript transcript) {
        transcriptRepository.save(transcript);
        return transcript;
    }

    public void delete(Long id) {
        transcriptRepository.delete(id);
    }

    public void deleteAll() {
        transcriptRepository.deleteAll();
    }

    public Long count() {
        return transcriptRepository.count();
    }

    public List<Transcript> findAll() {
        return transcriptRepository.findAll();
    }

    public Transcript find(Long id) {
        return transcriptRepository.findOne(id);
    }

    public List<Transcript> findByMedia(Media media) {
        return transcriptRepository.findByMedia(media);
    }

    public Transcript findOneLatestByMedia(Media media) {
        final List<Transcript> transcripts = findByMedia(media);

        return getLatest(transcripts);
    }

    protected static Transcript getLatest(List<Transcript> transcripts) {
        if (transcripts.isEmpty()) {
            return null;
        }

        Transcript latestTranscript = transcripts.get(0);

        for (Transcript transcript : transcripts) {
            if (transcript != null && transcript.getDate().after(latestTranscript.getDate())) {
                latestTranscript = transcript;
            }
        }

        return latestTranscript;
    }
}
