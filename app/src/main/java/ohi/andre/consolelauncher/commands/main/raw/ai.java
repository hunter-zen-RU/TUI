package ohi.andre.consolelauncher.commands.main.raw;

import android.graphics.Color;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AiManager;
import ohi.andre.consolelauncher.managers.VariablesManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ai extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {
        key {
            @Override
            public int[] args() { return new int[]{CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String key = pack.getString().trim();
                if (key.isEmpty()) return pack.context.getString(R.string.output_lessarg);
                AiManager.setApiKey(key);
                return "AI API Key updated.";
            }
        },
        test {
            @Override
            public int[] args() { return new int[0]; }

            @Override
            public String exec(ExecutePack pack) {
                Tuils.sendOutput(pack.context, "Testing AI connection...");
                AiManager aiManager = new AiManager(pack.context);
                aiManager.testApi(new AiManager.AiCallback() {
                    @Override
                    public void onResponse(CharSequence response) {
                        Tuils.sendOutput(Color.GREEN, pack.context, response);
                    }

                    @Override
                    public void onError(String error) {
                        Tuils.sendOutput(Color.RED, pack.context, error);
                    }
                });
                return null;
            }
        },
        set {
            @Override
            public int[] args() { return new int[]{CommandAbstraction.PLAIN_TEXT, CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String prompt = pack.getString().trim();
                String varName = pack.getString().trim();
                
                if (prompt.isEmpty() || varName.isEmpty()) return pack.context.getString(R.string.output_lessarg);

                Tuils.sendOutput(pack.context, "AI processing: " + prompt);
                AiManager aiManager = new AiManager(pack.context);
                aiManager.generateResponse(prompt, new AiManager.AiCallback() {
                    @Override
                    public void onResponse(CharSequence response) {
                        VariablesManager.setVariable(varName, response.toString().trim());
                        Tuils.sendOutput(pack.context, "AI result saved to variable: " + varName);
                    }

                    @Override
                    public void onError(String error) {
                        Tuils.sendOutput(Color.RED, pack.context, "AI Error: " + error);
                    }
                });
                return null;
            }
        },
        sys {
            @Override
            public int[] args() { return new int[]{CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String prompt = pack.getString().trim();
                if (prompt.isEmpty()) return pack.context.getString(R.string.output_lessarg);
                AiManager.setSystemPrompt(prompt);
                return "AI System Prompt updated.";
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            for (Param p1 : values()) if (p.endsWith(p1.label())) return p1;
            return null;
        }

        static String[] labels() {
            String[] ss = new String[values().length];
            for (int count = 0; count < values().length; count++) ss[count] = values()[count].label();
            return ss;
        }

        @Override
        public String label() { return Tuils.MINUS + name(); }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) { return pack.context.getString(R.string.help_ai); }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) { return pack.context.getString(R.string.output_invalid_param) + Tuils.SPACE + pack.get(); }
    }

    @Override
    public String[] params() { return Param.labels(); }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) { return Param.get(param); }

    @Override
    protected String doThings(ExecutePack pack) {
        String prompt = pack.getString().trim();
        if (prompt.isEmpty()) return pack.context.getString(R.string.help_ai);

        Tuils.sendOutput(pack.context, "AI thinking...");
        AiManager aiManager = new AiManager(pack.context);
        aiManager.generateResponse(prompt, new AiManager.AiCallback() {
            @Override
            public void onResponse(CharSequence response) {
                Tuils.sendOutput(pack.context, response);
            }

            @Override
            public void onError(String error) {
                Tuils.sendOutput(Color.RED, pack.context, "AI Error: " + error);
            }
        });
        return null;
    }

    @Override
    public int priority() { return 5; }

    @Override
    public int helpRes() { return R.string.help_ai; }
}
