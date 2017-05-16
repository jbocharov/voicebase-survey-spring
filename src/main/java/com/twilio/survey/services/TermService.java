package com.twilio.survey.services;

/**
 * Created by jbocharov on 5/16/17.
 */
import com.twilio.survey.models.Term;
import com.twilio.survey.models.Vocabulary;
import com.twilio.survey.repositories.TermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TermService {
    private TermRepository termRepository;

    @Autowired
    public TermService(TermRepository termRepository) { this.termRepository = termRepository; }

    public Term save(Term term) {
        termRepository.save(term);
        return term;
    }

    public void delete(Long id) {
        termRepository.delete(id);
    }

    public void deleteAll() {
        termRepository.deleteAll();
    }

    public Long count() {
        return termRepository.count();
    }

    public List<Term> findAll() {
        return termRepository.findAll();
    }

    public Term find(Long id) {
        return termRepository.findOne(id);
    }

    public List<Term> findByVocabulary(Vocabulary vocabulary) {
        return termRepository.findByVocabulary(vocabulary);
    }
}
