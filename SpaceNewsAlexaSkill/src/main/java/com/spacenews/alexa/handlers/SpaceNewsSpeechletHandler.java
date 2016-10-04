package com.spacenews.alexa.handlers;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public class SpaceNewsSpeechletHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds = new HashSet<>();
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds.add("amzn1.ask.skill.0d97b0a7-38cf-4228-add3-1c023fdfd6f9");
    }

    public SpaceNewsSpeechletHandler() {
        super(new SpaceNewsSpeechlet(), supportedApplicationIds);
    }

}
