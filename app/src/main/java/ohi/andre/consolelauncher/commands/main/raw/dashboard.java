package ohi.andre.consolelauncher.commands.main.raw;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Dashboard;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class dashboard extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        mode {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                String targetMode = pack.getString().trim().toLowerCase();
                if (!targetMode.equals("grid") && !targetMode.equals("native")) {
                    return "Invalid mode. Use 'grid' or 'native'.";
                }

                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.dashboard_mode, targetMode);
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.use_grid_dashboard, String.valueOf(targetMode.equals("grid")));

                if (pack.context instanceof Reloadable) {
                    ((Reloadable) pack.context).reload();
                }

                return "Dashboard mode set to: " + targetMode.toUpperCase();
            }
        },
        toggle {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                String current = XMLPrefsManager.get(Dashboard.dashboard_mode);
                String targetMode = "grid".equalsIgnoreCase(current) ? "native" : "grid";

                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.dashboard_mode, targetMode);
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.use_grid_dashboard, String.valueOf(targetMode.equals("grid")));

                if (pack.context instanceof Reloadable) {
                    ((Reloadable) pack.context).reload();
                }

                return "Dashboard mode toggled to: " + targetMode.toUpperCase();
            }
        },
        layout {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                String layoutStr = pack.getString().trim().toLowerCase();
                if (!layoutStr.contains("x")) {
                    return "Invalid layout format. Use e.g. 2x2, 1x2, 4x4, up to 5x5.";
                }

                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.grid_layout, layoutStr);

                if (pack.context instanceof Reloadable) {
                    ((Reloadable) pack.context).reload();
                }

                return "Dashboard grid layout set to: " + layoutStr;
            }
        },
        blur {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                String val = pack.getString().trim().toLowerCase();
                boolean enable = "true".equals(val) || "on".equals(val) || "1".equals(val);
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.enable_blur_effect, String.valueOf(enable));
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.blur_effect, String.valueOf(enable));

                if (pack.context instanceof Reloadable) {
                    ((Reloadable) pack.context).reload();
                }

                return "Dashboard blur effect set to: " + enable;
            }
        },
        on {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.dashboard_enabled, "true");
                if (pack.context instanceof Reloadable) ((Reloadable) pack.context).reload();
                return "Dashboard enabled.";
            }
        },
        off {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsManager.XMLPrefsRoot.DASHBOARD.write(Dashboard.dashboard_enabled, "false");
                if (pack.context instanceof Reloadable) ((Reloadable) pack.context).reload();
                return "Dashboard disabled.";
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                File dashboardFile = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.DASHBOARD.path);
                pack.context.startActivity(Tuils.openFile(pack.context, dashboardFile));
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
            return pack.context.getString(R.string.help_dashboard);
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
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_dashboard;
    }
}
