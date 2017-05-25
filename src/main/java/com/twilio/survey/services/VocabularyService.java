package com.twilio.survey.services;

/**
 * Created by jbocharov on 5/16/17.
 */
import com.twilio.survey.models.Participant;
import com.twilio.survey.models.Vocabulary;
import com.twilio.survey.repositories.VocabularyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VocabularyService {
    private VocabularyRepository vocabularyRepository;

    @Autowired
    public VocabularyService(VocabularyRepository vocabularyRepository) { this.vocabularyRepository = vocabularyRepository; }

    public Vocabulary save(Vocabulary vocabulary) {
        vocabularyRepository.save(vocabulary);
        return vocabulary;
    }

    public void delete(Long id) {
        vocabularyRepository.delete(id);
    }

    public void deleteAll() {
        vocabularyRepository.deleteAll();
    }

    public Long count() {
        return vocabularyRepository.count();
    }

    public List<Vocabulary> findAll() {
        return vocabularyRepository.findAll();
    }

    public Vocabulary find(Long id) {
        return vocabularyRepository.findOne(id);
    }

    public List<Vocabulary> findByParticipant(Participant participant) {
        return vocabularyRepository.getByParticipant(participant);
    }

    public Vocabulary findOneLatestByParticipant(Participant participant) {
        final List<Vocabulary> vocabularies = findByParticipant(participant);

        if (vocabularies.isEmpty()) {
            return null;
        }

        Vocabulary latestVocabulary = vocabularies.get(0);

        for (Vocabulary vocabulary : vocabularies) {
            if (vocabulary != null && vocabulary.getDate().after(latestVocabulary.getDate())) {
                latestVocabulary = vocabulary;
            }
        }

        return latestVocabulary;
    }
}
