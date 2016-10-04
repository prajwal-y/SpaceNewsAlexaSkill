package com.prajwal.alexa.basictest;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.prajwal.watsonclient.alchemydata.AlchemyDataNewsClient;
import com.prajwal.watsonclient.alchemydata.AlchemyDataNewsController;

public class AlexaBasicTestSpeechlet implements Speechlet {

    private static final Logger log = LoggerFactory.getLogger(AlexaBasicTestSpeechlet.class);

    private static final String TOPIC_SLOT = "Topic";

    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any initialization logic goes here
    }

    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("HelloWatsonIntent".equals(intentName)) {
            return getHelloResponse(intent, session);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to the IBM Watson helper on Echo";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HelloWatson");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelloResponse(final Intent intent, final Session session) {

        Map<String, Slot> slots = intent.getSlots();
        Slot topicSlot = slots.get(TOPIC_SLOT);

        String speechText = "Something seriously went wrong while accessing Watson";

        if(topicSlot != null) {
            String topic = topicSlot.getValue();

            String[] fields =
                    new String[] {AlchemyDataNewsController.FIELD_TITLE, AlchemyDataNewsController.FIELD_URL};
            AlchemyDataNewsController.AlchemyDataNewsBuilder queryBuilder = new AlchemyDataNewsController.AlchemyDataNewsBuilder();
            queryBuilder = queryBuilder.setStartTime("1459468800")
                    .setEndTime("1459728000")
                    .setReturn(StringUtils.join(fields, ","))
                    .setTitle(topic)
                    .setCount(3);

            AlchemyDataNewsClient alchemyDataNewsClient = new AlchemyDataNewsClient(queryBuilder.createAlchemyDataNewsController());
            List<Map<String, Object>> parsedResults = alchemyDataNewsClient.getNews();

            if(parsedResults != null) {
                speechText = "The most popular news article about " + topic + " is " + parsedResults.get(0).get("title");
            } else {
                speechText = "Error while getting news from Watson. Sorry!";
            }
        }

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HelloWatson");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "I can connect you to IBM Watson now! Try it out!";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HelloWorld");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}