package com.twilio.survey.repositories;

/**
 * Created by jbocharov on 5/16/17.
 */
import com.twilio.survey.models.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {
}
