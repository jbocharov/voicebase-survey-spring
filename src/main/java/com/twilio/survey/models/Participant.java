package com.twilio.survey.models;

/**
 * Created by jbocharov on 5/16/17.
 */

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/* DDL:
create table participants (
    id SERIAL UNIQUE not null PRIMARY KEY,
    phone_number varchar(255) not null,
    unmasked_phone_number varchar(255) not null,
    isVisible boolean,
    date timestamp not null
);
 */

@Entity
@Table(name = "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToMany(mappedBy = "participant")
    @OrderBy("id ASC")
    List<Vocabulary> vocabularies;

    @Column(name = "date")
    private Date date;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "unmasked_phone_number")
    private String unmaskedPhoneNumber;

    public Participant() {
    }

    public Participant(String phoneNumber, String unmaskedPhoneNumber, Date date) {
        this.date = date;
        this.phoneNumber = phoneNumber;
        this.unmaskedPhoneNumber = unmaskedPhoneNumber;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUnmaskedPhoneNumber() {
        return unmaskedPhoneNumber;
    }

    public void setUnmaskedPhoneNumber(String unmaskedPhoneNumber) {
        this.unmaskedPhoneNumber = unmaskedPhoneNumber;
    }

    public List<Vocabulary> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(List<Vocabulary> vocabularies) {
        this.vocabularies = vocabularies;
    }
}
