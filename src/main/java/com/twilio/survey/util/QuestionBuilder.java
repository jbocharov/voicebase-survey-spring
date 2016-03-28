package com.twilio.survey.util;

import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

/**
 * Created by vshyba on 23/03/16.
 */
public interface QuestionBuilder {
    public String build() throws TwiMLException;
    public String buildNoMoreQuestions() throws TwiMLException;
}