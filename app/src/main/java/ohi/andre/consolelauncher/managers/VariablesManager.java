package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
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

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
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

        // Predefined System Variables
        if (result.contains("{{device}}")) {
            result = result.replace("{{device}}", Build.MODEL);
        }
        if (result.contains("{{user}}")) {
            String user = XMLPrefsManager.get(Ui.username);
            result = result.replace("{{user}}", user != null ? user : "user");
        }
        if (result.contains("{{os_version}}")) {
            result = result.replace("{{os_version}}", "Android " + Build.VERSION.RELEASE);
        }
        if (result.contains("{{cpu_usage}}")) {
            result = result.replace("{{cpu_usage}}", String.valueOf(Tuils.getCpuUsage()));
        }
        if (result.contains("{{ram_total}}")) {
            result = result.replace("{{ram_total}}", Tuils.round(Tuils.totalRam() / 1024.0 / 1024.0, 1) + "G");
        }
        if (result.contains("{{ram_avail}}") && context != null) {
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            result = result.replace("{{ram_avail}}", Tuils.round(mi.availMem / 1024.0 / 1024.0 / 1024.0, 1) + "G");
        }
        if (result.contains("{{ram_used}}") && context != null) {
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long total = Tuils.totalRam() * 1024;
            long used = total - mi.availMem;
            result = result.replace("{{ram_used}}", Tuils.round(used / 1024.0 / 1024.0 / 1024.0, 1) + "G");
        }
        if (result.contains("{{storage_total}}")) {
            result = result.replace("{{storage_total}}", (int) Tuils.getTotalInternalMemorySize(Tuils.GIGA) + "G");
        }
        if (result.contains("{{storage_avail}}")) {
            result = result.replace("{{storage_avail}}", (int) Tuils.getAvailableInternalMemorySize(Tuils.GIGA) + "G");
        }
        if (result.contains("{{network}}") && context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            String net = (activeNetwork != null && activeNetwork.isConnected()) ? activeNetwork.getTypeName() : "OFFLINE";
            result = result.replace("{{network}}", net);
        }
        if (result.contains("{{network_name}}") && context != null) {
            String name = "OFFLINE";
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo info = wifiManager.getConnectionInfo();
                    name = info.getSSID();
                    if (name != null) name = name.replace("\"", "");
                } else {
                    name = activeNetwork.getSubtypeName();
                }
            }
            result = result.replace("{{network_name}}", name != null ? name : "UNKNOWN");
        }
        if (result.contains("{{network_upload}}") || result.contains("{{network_download}}")) {
            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();
            // These are total since boot. For "speed" we'd need a delta, but for now we'll provide formatted total as requested or just raw values.
            // Usually users want current speed, but without a background task tracking deltas, we'll provide formatted total human readable.
            if (result.contains("{{network_download}}")) {
                result = result.replace("{{network_download}}", Tuils.formatSize(rxBytes, Tuils.MEGA) + "MB");
            }
            if (result.contains("{{network_upload}}")) {
                result = result.replace("{{network_upload}}", Tuils.formatSize(txBytes, Tuils.MEGA) + "MB");
            }
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
