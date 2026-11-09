package ohi.andre.consolelauncher.commands.main.raw;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.VariablesManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class variables extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        set {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                String str = pack.getString().trim();
                int eqIdx = str.indexOf('=');
                String name, val;
                if (eqIdx > 0) {
                    name = str.substring(0, eqIdx).trim();
                    val = str.substring(eqIdx + 1).trim();
                } else {
                    String[] parts = str.split("\\s+", 2);
                    if (parts.length < 2) {
                        return pack.context.getString(R.string.output_lessarg);
                    }
                    name = parts[0].trim();
                    val = parts[1].trim();
                }

                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }

                VariablesManager.setVariable(name, val);
                String detectedType = VariablesManager.detectType(val);
                return "Variable saved: " + name + " = " + val + " [" + detectedType + "]";
            }
        },
        get {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString().trim();
                String val = VariablesManager.getVariable(name);
                if (val == null || val.isEmpty()) {
                    return "Variable '" + name + "' not found.";
                }
                String detectedType = VariablesManager.detectType(val);
                return name + " = " + val + " [" + detectedType + "]";
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                Map<String, String> all = VariablesManager.getAllVariables();
                if (all.isEmpty()) {
                    return "No variables defined in variables.xml.";
                }
                StringBuilder sb = new StringBuilder("[variables.xml]:\n");
                for (Map.Entry<String, String> entry : all.entrySet()) {
                    String type = VariablesManager.detectType(entry.getValue());
                    sb.append("  {{").append(entry.getKey()).append("}} = ").append(entry.getValue()).append(" [").append(type).append("]\n");
                }
                return sb.toString().trim();
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                File xmlFile = new File(Tuils.getFolder(), VariablesManager.FILE_NAME);
                if (!xmlFile.exists()) {
                    VariablesManager.setVariable("init", "1");
                }
                pack.context.startActivity(Tuils.openFile(pack.context, xmlFile));
                return null;
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int nArgs) {
            return pack.context.getString(R.string.help_variables);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.context.getString(R.string.output_invalid_param) + Tuils.SPACE + pack.get();
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_variables;
    }
}
