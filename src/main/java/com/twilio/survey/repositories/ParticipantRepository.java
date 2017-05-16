package com.twilio.survey.repositories;

/**
 * Created by jbocharov on 5/16/17.
 */
import com.twilio.survey.models.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Participant getByUnmaskedPhoneNumber(String unmaskedPhoneNumber);
    List<Participant> getByPhoneNumber(String phoneNumber);
}
