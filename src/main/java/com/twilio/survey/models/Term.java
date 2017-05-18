package com.twilio.survey.models;

/**
 * Created by jbocharov on 5/16/17.
 */

import javax.persistence.*;
import java.util.Date;

/* DDL:
create table terms (
    id SERIAL UNIQUE not null PRIMARY KEY,
    term varchar(255) not null,
    sounds_like text,
    weight float,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    date timestamp not null
);
*/

@Entity
@Table(name = "terms")
public class Term {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "term")
    private String term;

    @Column(name = "sounds_like")
    private String soundsLike;

    @Column(name = "weight")
    private Float weight;

    @Column(name = "date")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    public Term() {
    }

    public Term(String term, String soundsLike, Float weight, Date date) {
        this.term = term;
        this.soundsLike = soundsLike;
        this.weight = weight;
        this.date = date;
    }

    public Term(String term, Vocabulary vocabulary, Date date) {
        this.term = term;
        this.vocabulary = vocabulary;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getSoundsLike() {
        return soundsLike;
    }

    public void setSoundsLike(String soundsLike) {
        this.soundsLike = soundsLike;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }
}

