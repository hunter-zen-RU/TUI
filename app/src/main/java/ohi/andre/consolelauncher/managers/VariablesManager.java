package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.os.BatteryManager;
import android.os.SystemClock;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ohi.andre.consolelauncher.tuils.Tuils;

public class VariablesManager {

    public static final String FILE_NAME = "variables.xml";
    private static final Map<String, String> cachedVars = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    public static String detectType(String val) {
        if (val == null) return "STRING";
        String trimmed = val.trim();
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return "BOOLEAN";
        }
        if (trimmed.equals("0") || trimmed.equals("1")) {
            return "BOOLEAN";
        }
        if (trimmed.equals("00") || trimmed.equals("01")) {
            return "INTEGER";
        }
        if (trimmed.matches("-?\\d+")) {
            return "INTEGER";
        }
        if (trimmed.matches("-?\\d+\\.\\d+")) {
            return "FLOAT";
        }
        if (trimmed.length() == 1) {
            return "CHAR";
        }
        return "STRING";
    }

    public static synchronized void loadFromFile() {
        cachedVars.clear();
        File xmlFile = new File(Tuils.getFolder(), FILE_NAME);
        if (!xmlFile.exists()) {
            loaded = true;
            return;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("entry");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String key = eElement.getAttribute("name");
                    String value = eElement.getAttribute("value");
                    if (key != null && !key.isEmpty()) {
                        cachedVars.put(key, value != null ? value : "");
                    }
                }
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
        loaded = true;
    }

    public static synchronized void saveToFile() {
        try {
            File xmlFile = new File(Tuils.getFolder(), FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("variables");
            doc.appendChild(rootElement);

            for (Map.Entry<String, String> entry : cachedVars.entrySet()) {
                Element entryElem = doc.createElement("entry");
                entryElem.setAttribute("name", entry.getKey());
                entryElem.setAttribute("value", entry.getValue());
                entryElem.setAttribute("type", detectType(entry.getValue()));
                rootElement.appendChild(entryElem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);

        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public static void setVariable(String name, String value) {
        if (!loaded) loadFromFile();
        if (name == null || name.trim().isEmpty()) return;
        cachedVars.put(name.trim(), value != null ? value : "");
        saveToFile();
    }

    public static String getVariable(String name) {
        if (!loaded) loadFromFile();
        if (name == null) return "";
        return cachedVars.getOrDefault(name.trim(), "");
    }

    public static Map<String, String> getAllVariables() {
        if (!loaded) loadFromFile();
        return new HashMap<>(cachedVars);
    }

    public static String resolveVariables(Context context, String input, Map<String, String> localVars) {
        if (input == null || input.isEmpty()) return input;
        String result = input;

        // Dynamic system variables
        if (result.contains("{{date}}")) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            result = result.replace("{{date}}", dateStr);
        }
        if (result.contains("{{time}}")) {
            String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            result = result.replace("{{time}}", timeStr);
        }
        if (result.contains("{{battery}}") && context != null) {
            try {
                android.content.Intent batIntent = context.getApplicationContext().registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                int level = batIntent != null ? batIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                result = result.replace("{{battery}}", String.valueOf(level));
            } catch (Exception e) {
                result = result.replace("{{battery}}", "0");
            }
        }
        if (result.contains("{{uptime}}")) {
            long sec = SystemClock.elapsedRealtime() / 1000;
            result = result.replace("{{uptime}}", String.valueOf(sec));
        }

        // Local script variables
        if (localVars != null) {
            for (Map.Entry<String, String> entry : localVars.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                result = result.replace("{{" + k + "}}", v);
                result = result.replace("$" + k, v);
            }
        }

        // Stored variables from variables.xml
        if (!loaded) loadFromFile();
        for (Map.Entry<String, String> entry : cachedVars.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            result = result.replace("{{" + k + "}}", v);
            result = result.replace("$" + k, v);
        }

        return result;
    }
}
