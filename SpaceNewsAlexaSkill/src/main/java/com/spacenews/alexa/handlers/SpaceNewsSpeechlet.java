package com.spacenews.alexa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
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
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class SpaceNewsSpeechlet implements Speechlet {

    private static final Logger log = LoggerFactory.getLogger(SpaceNewsSpeechlet.class);
    private static final String TEMP_DIRECTORY = "/tmp";
    private static final String TEMP_FILE_NAME = "tempNewsFile";
    private static final String NEWS_DATA_DELIM = ":delim:";
    private static final String NEWS_S3_BUCKET = "space-news-data";
    private static final String NEWS_S3_KEY = "space_news_data";
    private static final String NEWS_INDEX = "NewsIndex";
    private static final String FIRST_NEWS_INTENT = "GetFirstNewsIntent";
    private static final String NEXT_NEWS_INTENT = "GetNextNewsIntent";
    private static final String PREVIOUS_NEWS_INTENT = "GetPreviousNewsIntent";
    private static final String NEWS_DETAILS_INTENT = "GetNewsDetailsIntent";
    private static final String HELP_INTENT = "AMAZON.HelpIntent";
    private static final String STOP_INTENT = "AMAZON.StopIntent";

    private static final int SPEECH_TEXT_LEN_LIMIT = 7999;

    private final AmazonS3Client s3Client;
    private final List<Pair<String, String>> spaceNewsList;

    public SpaceNewsSpeechlet() {
        s3Client = new AmazonS3Client();
        spaceNewsList = new ArrayList<>();
    }

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        session.setAttribute(NEWS_INDEX, 0);
        fetchAndPopulateNews();
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getStartResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        if (FIRST_NEWS_INTENT.equals(intentName) || NEXT_NEWS_INTENT.equals(intentName)
                || PREVIOUS_NEWS_INTENT.equals(intentName)) {
            return getSpaceNewsResponse(intent, session);
        } else if (NEWS_DETAILS_INTENT.equals(intentName)) {
            return getSpaceNewsContentResponse(session);
        } else if (HELP_INTENT.equals(intentName)) {
            return getHelpResponse();
        } else if (STOP_INTENT.equals(intentName)) {
            return createSpeechletResponse("Good bye. Have a nice day!", false);
        } else {
            return getErrorResponse();
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
    }

    private void fetchAndPopulateNews() throws SpeechletException {
        File tempNewsFile = null;
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(NEWS_S3_BUCKET, NEWS_S3_KEY);
            tempNewsFile = File.createTempFile(TEMP_FILE_NAME, ".tmp", new File(TEMP_DIRECTORY));
            s3Client.getObject(getObjectRequest, tempNewsFile);
            log.info("S3 fetch was successful");
            readAndPopulateNews(tempNewsFile);
            log.info("News population was successful");
        } catch (AmazonS3Exception | IOException e) {
            log.error("Exception occurred when fetching news with key: ", e);
            throw new SpeechletException(e);
        } finally {
            FileUtils.deleteQuietly(tempNewsFile);
        }
    }

    private void readAndPopulateNews(File newsFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(newsFile));
        String line;
        while ((line = br.readLine()) != null) {
            String[] data = line.split(NEWS_DATA_DELIM);
            // Each line consists of <newslink>:delim:<heading>:delim:<news_text>
            if (data.length != 3) {
                continue;
            }
            Pair<String, String> newsEntry = Pair.of(data[1], data[2]);
            spaceNewsList.add(newsEntry);
        }
    }

    private SpeechletResponse getStartResponse() {
        String speechText = "Welcome to the Space News skill. " +
                "You can use this to get the latest news from space dot com. " +
                "Just say give me space news";
        return createSpeechletResponse(speechText, true);
    }

    private SpeechletResponse getSpaceNewsResponse(final Intent intent, final Session session) {
        int newsIndex = (int) session.getAttribute(NEWS_INDEX);
        if (NEXT_NEWS_INTENT.equals(intent.getName())) {
            newsIndex += 1;
        } else if (PREVIOUS_NEWS_INTENT.equals(intent.getName()) && newsIndex > 0) {
            newsIndex -= 1;
        }
        session.setAttribute(NEWS_INDEX, newsIndex);
        String speechText;
        if (newsIndex >= spaceNewsList.size()) {
            log.info("newsIndex is invalid: " + newsIndex);
            speechText = "That's all the news I have for now. Please check after some time, " +
                    "or say give me space news to start from the beginning again";
        } else {
            speechText = spaceNewsList.get(newsIndex).getLeft();
            speechText += ". You can now either say next news, previous news, or just say \"more details\" " +
                    "to get more information for the current article.";
        }
        return createSpeechletResponse(speechText, true);
    }

    private SpeechletResponse getSpaceNewsContentResponse(final Session session) {
        int newsIndex = (int) session.getAttribute(NEWS_INDEX);
        String speechText;
        if (newsIndex >= spaceNewsList.size()) {
            log.info("newsIndex is invalid: " + newsIndex);
            speechText = "That's all the news I have for now. Please check after some time, " +
                    "or say give me space news to start from the beginning again";
        } else {
            speechText = spaceNewsList.get(newsIndex).getRight();
            if (speechText.length() > SPEECH_TEXT_LEN_LIMIT) {
                speechText = speechText.substring(0, SPEECH_TEXT_LEN_LIMIT);
            }
        }
        return createSpeechletResponse(speechText, true);
    }

    private SpeechletResponse getErrorResponse() {
        String speechText = "I did not understand what you meant. You can either say next news, or " +
                "start over by saying, give me space news";
        return createSpeechletResponse(speechText, true);
    }

    private SpeechletResponse getHelpResponse() {
        String speechText = "I can inform you about the latest news related to space and astronomy. " +
                "Just say, ask alexa for space news";
        return createSpeechletResponse(speechText, true);
    }

    private SpeechletResponse createSpeechletResponse(String speechText, boolean prompt) {
        SimpleCard card = new SimpleCard();
        card.setTitle("SpaceNewsCard");
        card.setContent(speechText);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        if (prompt) {
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(speech);
            return SpeechletResponse.newAskResponse(speech, reprompt, card);
        } else {
            return SpeechletResponse.newTellResponse(speech, card);
        }
    }
}
