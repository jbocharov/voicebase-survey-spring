package com.twilio.survey.models;

/**
 * Created by jbocharov on 5/16/17.
 */

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/* DDL:
create table vocabularies (
    id SERIAL UNIQUE not null PRIMARY KEY,
    isVisible boolean,
    participant_id int REFERENCES participants (id) ON DELETE CASCADE,
    date timestamp not null
);
 */

@Entity
@Table(name = "vocabularies")
public class Vocabulary {
    @OneToMany(mappedBy = "vocabulary")
    @OrderBy("id ASC")
    List<Term> terms;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    public Vocabulary() {

    }

    public Vocabulary(List<Term> terms, Date date) {
        this.terms = terms;
        this.date = date;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public void setTerms(List<Term> terms) {
        this.terms = terms;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
