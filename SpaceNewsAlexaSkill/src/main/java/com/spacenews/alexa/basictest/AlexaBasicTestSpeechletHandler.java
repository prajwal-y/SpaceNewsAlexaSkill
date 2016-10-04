package com.prajwal.alexa.basictest;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public class AlexaBasicTestSpeechletHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds = new HashSet<String>();
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds.add("amzn1.echo-sdk-ams.app.53de3746-d7c7-440d-952c-76c0f9770434");
    }

    public AlexaBasicTestSpeechletHandler() {
        super(new AlexaBasicTestSpeechlet(), supportedApplicationIds);
    }

}
