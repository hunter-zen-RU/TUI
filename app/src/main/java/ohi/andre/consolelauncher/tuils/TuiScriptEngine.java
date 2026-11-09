package ohi.andre.consolelauncher.tuils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.MainManager;

public class TuiScriptEngine {

    public static void executeScriptFile(final Context context, final File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            Tuils.sendOutput(Color.RED, context, "Cannot read .tui script file: " + (file != null ? file.getAbsolutePath() : "null"));
            return;
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            Tuils.sendOutput(Color.RED, context, "Error reading .tui script: " + e.getMessage());
            return;
        }

        executeScriptLines(context, file.getName(), lines);
    }

    public static void executeScriptInputStream(final Context context, final String scriptName, final InputStream inputStream) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            Tuils.sendOutput(Color.RED, context, "Error reading .tui stream: " + e.getMessage());
            return;
        }

        executeScriptLines(context, scriptName, lines);
    }

    public static void executeScriptLines(final Context context, final String scriptName, final List<String> lines) {
        new Thread(() -> {
            Tuils.sendOutput(context, "[T-UI Engine] Running automation: " + scriptName);
            for (String rawLine : lines) {
                if (rawLine == null) continue;
                String line = rawLine.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                String lower = line.toLowerCase();
                
                // Automation delay / sleep
                if (lower.startsWith("sleep ") || lower.startsWith("delay ")) {
                    String delayStr = line.substring(line.indexOf(' ') + 1).trim();
                    try {
                        long ms = Long.parseLong(delayStr);
                        Thread.sleep(Math.max(10, ms));
                    } catch (Exception ignored) {}
                    continue;
                }

                // Automation echo / output
                if (lower.startsWith("echo ")) {
                    String msg = line.substring(5).trim();
                    Tuils.sendOutput(context, msg);
                    continue;
                }

                // Synchronous shell execution in .tui scripts
                if (lower.startsWith("shell ") || lower.startsWith("sh ")) {
                    String shCmd = line.substring(line.indexOf(' ') + 1).trim();
                    try {
                        ohi.andre.consolelauncher.managers.TermuxSessionManager mgr = new ohi.andre.consolelauncher.managers.TermuxSessionManager(context);
                        String res = mgr.executeDirectShellCommand(shCmd, 15);
                        if (res != null && !res.trim().isEmpty()) {
                            Tuils.sendOutput(context, res.trim());
                        }
                    } catch (Exception e) {
                        Tuils.sendOutput(Color.RED, context, "Shell execution error: " + e.getMessage());
                    }
                    continue;
                }

                // Automation intent action trigger
                if (lower.startsWith("action ")) {
                    String actionStr = line.substring(7).trim();
                    try {
                        Intent actionIntent = new Intent(actionStr);
                        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(actionIntent);
                    } catch (Exception e) {
                        try {
                            Intent bIntent = new Intent(actionStr);
                            context.sendBroadcast(bIntent);
                        } catch (Exception ex) {
                            Tuils.sendOutput(Color.RED, context, "Failed action: " + actionStr);
                        }
                    }
                    continue;
                }

                // Send command line to T-UI execution pipeline
                try {
                    Intent cmdIntent = new Intent(MainManager.ACTION_EXEC);
                    cmdIntent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
                    cmdIntent.putExtra(MainManager.CMD, line);
                    LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(cmdIntent);
                } catch (Exception e) {
                    Tuils.log(e);
                }

                // Pacing interval between command lines
                try {
                    Thread.sleep(200);
                } catch (Exception ignored) {}
            }
            Tuils.sendOutput(context, "[T-UI Engine] Script completed: " + scriptName);
        }).start();
    }
}
