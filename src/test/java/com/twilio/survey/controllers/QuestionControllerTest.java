package com.twilio.survey.controllers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.twilio.survey.SurveyJavaApplication;
import com.twilio.survey.models.Question;
import com.twilio.survey.models.Survey;
import com.twilio.survey.repositories.QuestionRepository;
import com.twilio.survey.repositories.SurveyRepository;
import com.twilio.survey.services.QuestionService;
import com.twilio.survey.services.SurveyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SurveyJavaApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class QuestionControllerTest extends BaseControllerTest{
  @Autowired
  private QuestionRepository questionRepository;
  @Autowired
  private SurveyRepository surveyRepository;
  private QuestionService questionService;
  private SurveyService surveyService;

  @Before
  public void before() {
    questionService = new QuestionService(questionRepository);
    surveyService = new SurveyService(surveyRepository);
    questionService.deleteAll();
    surveyService.deleteAll();
  }

  @Test
  public void getNoQuestionCallTest() throws Exception {
    Survey survey1 = surveyService.create(new Survey("New Title", new Date()));

    String response = getAsCall("/question?survey=" + survey1.getId() + "&question=1");

    assertTrue(response.contains(
            "<Say>We are sorry, there are no more questions available for this survey. Good bye.</Say>"));
  }

  @Test
  public void getNoQuestionSMSTest() throws Exception {
    Survey survey1 = surveyService.create(new Survey("New Title", new Date()));

    String response = getAsSMS("/question?survey=" + survey1.getId() + "&question=1");

    assertTrue(response.contains(
            "<Message>We are sorry, there are no more questions available for this survey. Good bye.</Message>"));
  }

  @Test
  public void getSMSQuestionTest() throws Exception {
    Survey survey1 = surveyService.create(new Survey("New Title", new Date()));
    questionService.create(new Question("Question Body", "Q_TYPE", survey1, new Date()));

    String response = getAsSMS("/question?survey=" + survey1.getId() + "&question=1");

    assertTrue(response.contains(
            "<Message>Question Body</Message>"));
  }

  @Test
  public void getYesNoQuestionSMSTest() throws Exception {
    Survey survey1 = surveyService.create(new Survey("New Title", new Date()));
    Question question = questionService.create(new Question("Question Body", "yes-no", survey1, new Date()));

    String response = getAsSMS("/question?survey=" + survey1.getId() + "&question=1");

    assertTrue(response.contains(
            "<Message>For the next question, type 1 for yes, and 0 for no. " + question.getBody()+ "</Message>"));
  }

  @Test
  public void getYesNoQuestionCallTest() throws Exception {
    Survey survey1 = surveyService.create(new Survey("New Title", new Date()));
    Question question = questionService.create(new Question("Question Body", "yes-no", survey1, new Date()));

    String response = getAsCall("/question?survey=" + survey1.getId() + "&question=1");

    assertTrue(response.contains(
            "<Say>For the next question, press 1 for yes, and 0 for no. Then press the pound key.</Say>"));
    assertTrue(response.contains(
            "<Say>"+question.getBody()+"</Say>"));
  }
}