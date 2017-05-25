package com.twilio.survey.repositories;

/**
 * Created by jbocharov on 5/16/17.
 */
import com.twilio.survey.models.Media;
import com.twilio.survey.models.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByParticipant(Participant participant);

    List<Media> findByVoicebaseMediaId(String voicebaseMediaId);

    List<Media> findByNovocabMediaId(String novocabMediaId);
}
