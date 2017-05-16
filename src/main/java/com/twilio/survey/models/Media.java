package com.twilio.survey.models;

/**
 * Created by jbocharov on 5/16/17.
 */

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/* DDL:
create table media (
    id SERIAL UNIQUE not null PRIMARY KEY,
    voicebase_media_id varchar(255) not null,
    participant_id int REFERENCES participants (id) ON DELETE CASCADE,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    date timestamp not null
);
 */

@Entity
@Table(name = "media")
public class Media {
    @OneToMany(mappedBy = "media")
    @OrderBy("id ASC")
    List<Transcript> transcripts;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "voicebase_media_id")
    private String voicebaseMediaId;

    @Column(name = "date")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    public Media() {
    }

    public Media(String voicebaseMediaId, Date date) {
        this.voicebaseMediaId = voicebaseMediaId;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVoicebaseMediaId() {
        return voicebaseMediaId;
    }

    public void setVoicebaseMediaId(String voicebaseMediaId) {
        this.voicebaseMediaId = voicebaseMediaId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<Transcript> getTranscripts() {
        return transcripts;
    }

    public void setTranscripts(List<Transcript> transcripts) {
        this.transcripts = transcripts;
    }
}
