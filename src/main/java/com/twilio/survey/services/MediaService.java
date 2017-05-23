package com.twilio.survey.services;

/**
 * Created by jbocharov on 5/16/17.
 */

import com.twilio.survey.models.Media;
import com.twilio.survey.models.Participant;
import com.twilio.survey.repositories.MediaRepository;
import com.twilio.survey.repositories.TranscriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaService {
    private MediaRepository mediaRepository;

    @Autowired
    public MediaService(MediaRepository mediaRepository) { this.mediaRepository = mediaRepository; }

    public Media save(Media media) {
        mediaRepository.save(media);
        return media;
    }

    public void delete(Long id) {
        mediaRepository.delete(id);
    }

    public void deleteAll() {
        mediaRepository.deleteAll();
    }

    public Long count() {
        return mediaRepository.count();
    }

    public List<Media> findAll() {
        return mediaRepository.findAll();
    }

    public Media find(Long id) {
        return mediaRepository.findOne(id);
    }

    public List<Media> findByParticipant(Participant participant) {
        return mediaRepository.findByParticipant(participant);
    }

    public Media findOneLatestByVoiceBaseMediaId(String voicebaseMediaId) {
        final List<Media> mediaList = mediaRepository.findByVoicebaseMediaId(voicebaseMediaId);

        return getLatest(mediaList);
    }

    public Media findOneLatestByNovocabMediaId(String novocabMediaId) {
        final List<Media> mediaList = mediaRepository.findByNovocabMediaId(novocabMediaId);

        return getLatest(mediaList);
    }

    protected static Media getLatest(List<Media> mediaList) {
        if (mediaList.isEmpty()) {
            return null;
        }

        Media latestMedia = mediaList.get(0);

        for (Media media : mediaList) {
            if (media != null && media.getDate().after(latestMedia.getDate())) {
                latestMedia = media;
            }
        }

        return latestMedia;
    }
}
