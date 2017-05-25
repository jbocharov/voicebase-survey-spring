package com.twilio.survey.models;

/**
 * Created by jbocharov on 5/16/17.
 */

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/* DDL:
create table transcripts (
    id SERIAL UNIQUE not null PRIMARY KEY,
    media_id int REFERENCES media (id) ON DELETE CASCADE,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    transcript_text text not null,
    date timestamp not null
);
 */

@Entity
@Table(name = "transcripts")
public class Transcript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Column(name = "transcript_text")
    private String transcriptText;

    @Column(name = "novocab_text")
    private String novocabText;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "terms_list")
    private String termsList;

    @Column(name = "rating")
    private Integer rating;


    public Transcript() {
    }

    public Transcript(Media media, Vocabulary vocabulary, Integer rating, String termsList, String phoneNumber, Date date) {
        this.media = media;
        this.vocabulary = vocabulary;
        this.date = date;
        this.transcriptText = "";
        this.novocabText = "";
        this.rating = rating;
        this.termsList = termsList;
        this.phoneNumber = phoneNumber;
    }

    public Transcript(String transcriptText, Date date) {
        this.date = date;
        this.transcriptText = transcriptText;
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

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public String getNovocabText() {
        return novocabText;
    }

    public void setNovocabText(String novocabText) {
        this.novocabText = novocabText;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTermsList() {
        return termsList;
    }

    public void setTermsList(String termsList) {
        this.termsList = termsList;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public boolean getHasRating() {
        return rating != null;
    }
}
