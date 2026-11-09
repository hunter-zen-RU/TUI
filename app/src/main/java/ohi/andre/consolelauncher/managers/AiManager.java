package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.noties.markwon.Markwon;
import ohi.andre.consolelauncher.tuils.Tuils;

public class AiManager {

    private static final String FILE_NAME = "ai.xml";
    private static String apiKey = "";
    private static String systemPrompt = "You are a helpful T-UI assistant. Keep responses concise and terminal-friendly.";
    private static String modelName = "gemini-1.5-flash";

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Markwon markwon;

    public interface AiCallback {
        void onResponse(CharSequence response);
        void onError(String error);
    }

    public AiManager(Context context) {
        this.context = context;
        this.markwon = Markwon.create(context);
        loadConfig();
    }

    public static void setApiKey(String key) {
        apiKey = key;
        saveConfig();
    }

    public static void setSystemPrompt(String prompt) {
        systemPrompt = prompt;
        saveConfig();
    }

    public static String getApiKey() {
        return apiKey;
    }

    private void loadConfig() {
        File file = new File(Tuils.getFolder(), FILE_NAME);
        if (!file.exists()) {
            saveConfig();
            return;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            apiKey = getTagValue("key", doc.getDocumentElement());
            systemPrompt = getTagValue("system_prompt", doc.getDocumentElement());
            String m = getTagValue("model", doc.getDocumentElement());
            if (m != null && !m.isEmpty()) modelName = m;

        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    private static void saveConfig() {
        try {
            File file = new File(Tuils.getFolder(), FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("ai");
            doc.appendChild(rootElement);

            Element keyElem = doc.createElement("key");
            keyElem.appendChild(doc.createTextNode(apiKey != null ? apiKey : ""));
            rootElement.appendChild(keyElem);

            Element promptElem = doc.createElement("system_prompt");
            promptElem.appendChild(doc.createTextNode(systemPrompt));
            rootElement.appendChild(promptElem);

            Element modelElem = doc.createElement("model");
            modelElem.appendChild(doc.createTextNode(modelName));
            rootElement.appendChild(modelElem);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public void generateResponse(String prompt, final AiCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API Key is missing. Set it with 'ai -key <key>'");
            return;
        }

        try {
            GenerativeModel gm = new GenerativeModel(modelName, apiKey);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText(systemPrompt + "\n\nUser: " + prompt)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String text = result.getText();
                    if (text != null) {
                        final CharSequence rendered = markwon.toMarkdown(text);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onResponse(rendered));
                    } else {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError("Empty response from AI."));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(t.getMessage()));
                }
            }, executor);

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void testApi(final AiCallback callback) {
        generateResponse("Hello, are you active?", new AiCallback() {
            @Override
            public void onResponse(CharSequence response) {
                callback.onResponse("AI is active and responding: " + response);
            }

            @Override
            public void onError(String error) {
                callback.onError("AI test failed: " + error);
            }
        });
    }
}
