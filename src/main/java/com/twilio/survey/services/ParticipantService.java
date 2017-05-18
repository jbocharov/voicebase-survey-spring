package com.twilio.survey.services;

/**
 * Created by jbocharov on 5/16/17.
 */

import com.twilio.survey.models.Participant;
import com.twilio.survey.repositories.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantService {
    private ParticipantRepository participantRepository;

    @Autowired
    public ParticipantService(ParticipantRepository participantRepository) { this.participantRepository = participantRepository; }

    public Participant save(Participant participant) {
        participantRepository.save(participant);
        return participant;
    }

    public void delete(Long id) {
        participantRepository.delete(id);
    }

    public void deleteAll() {
        participantRepository.deleteAll();
    }

    public Long count() {
        return participantRepository.count();
    }

    public List<Participant> findAll() {
        return participantRepository.findAll();
    }

    public Participant find(Long id) {
        return participantRepository.findOne(id);
    }

    public Participant getByUnmaskedPhoneNumber(String unmaskedPhoneNumber) throws IndexOutOfBoundsException {
        final List<Participant> participants = participantRepository.getByUnmaskedPhoneNumber(unmaskedPhoneNumber);

        return participants.isEmpty() ? null : participants.get(0);
    }

    public List<Participant> getByPhoneNumber(String phoneNumber) { return participantRepository.getByPhoneNumber(phoneNumber); }
}
