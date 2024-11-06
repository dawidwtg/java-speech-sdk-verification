package org.wtg;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public class TranslationTests {
    private static Semaphore stopTranslationWithFileSemaphore;

    private static String filename = "yt_pl.wav";
    private static String speechKey = System.getenv("SPEECH_KEY");
    private static String speechKeyPaid = System.getenv("SPEECH_KEY_PAID");
    private static String speechRegion = System.getenv("SPEECH_REGION");

    public static void translationWithFileAsync() throws InterruptedException, ExecutionException
    {
        stopTranslationWithFileSemaphore = new Semaphore(0);

        String v2EndpointUrl = "wss://" + speechRegion + ".stt.speech.microsoft.com/speech/universal/v2";
        SpeechTranslationConfig config = SpeechTranslationConfig.fromEndpoint(URI.create(v2EndpointUrl), speechKey);
        config.setProperty(PropertyId.SpeechServiceConnection_LanguageIdMode, "Continuous");

        AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig = AutoDetectSourceLanguageConfig.fromOpenRange();

        config.addTargetLanguage("en");

        AudioConfig audioInput = AudioConfig.fromWavFileInput(filename);

        TranslationRecognizer recognizer = new TranslationRecognizer(config, autoDetectSourceLanguageConfig, audioInput);
        {

            recognizer.recognizing.addEventListener((s, e) -> {

                // You can detect the language only in "recognized". Here from fromLanguage will be empty
                AutoDetectSourceLanguageResult autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult.fromResult(e.getResult());
                String fromLanguage = autoDetectSourceLanguageResult.getLanguage();
                System.out.println("RECOGNIZING in '" + fromLanguage + "': Text=" + e.getResult().getText());

                Map<String, String> map = e.getResult().getTranslations();
                for(String element : map.keySet()) {
                    System.out.println("    TRANSLATING into '" + element + "'': " + map.get(element));
                }
            });

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {

                    AutoDetectSourceLanguageResult autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult.fromResult(e.getResult());
                    String fromLanguage = autoDetectSourceLanguageResult.getLanguage();

                    // Here get text "e.getResult().getText()" will return "."
                    System.out.println("RECOGNIZED in '" + fromLanguage + "': Text=" + e.getResult().getText());

                    // But the translations are correct. So the audio quality is good. If it wasn't good, we would not get translation.
                    Map<String, String> map = e.getResult().getTranslations();
                    for(String element : map.keySet()) {
                        System.out.println("    TRANSLATED into '" + element + "'': " + map.get(element));
                    }
                }
                // I don't know why this is here. We are using TranslationRecognizer so this "if" will never be hit.
                // This was in the example.
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
                    System.out.println("    Speech not translated.");
                }
                else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NOMATCH: Speech could not be recognized.");
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
                }

                stopTranslationWithFileSemaphore.release();;
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                System.out.println("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                System.out.println("\nSession stopped event.");

                System.out.println("\nStop translation.");
                stopTranslationWithFileSemaphore.release();;
            });

            System.out.println("Start translation...");
            recognizer.startContinuousRecognitionAsync().get();

            stopTranslationWithFileSemaphore.acquire();;

            recognizer.stopContinuousRecognitionAsync().get();
        }
    }
}
