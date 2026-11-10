package ohi.andre.consolelauncher.commands.main.raw;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.PackageManager;
import ohi.andre.consolelauncher.managers.VariablesManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class tui extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        install {
            @Override
            public int[] args() { return new int[] {CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String pkgName = pack.getString().trim();
                if (pkgName.isEmpty()) return pack.context.getString(R.string.output_lessarg);

                Tuils.sendOutput(pack.context, "TUI Package Manager: Installing '" + pkgName + "'...");
                PackageManager.installPackage(pack.context, pkgName, new PackageManager.InstallCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Tuils.sendOutput(pack.context, message);
                    }
                    @Override
                    public void onError(String error) {
                        Tuils.sendOutput(android.graphics.Color.RED, pack.context, "TUI Install Error: " + error);
                    }
                });
                return null;
            }
        },
        set {
            @Override
            public int[] args() { return new int[] {CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String mgr = pack.getString().trim();
                VariablesManager.setVariable("active_pkg_mgr", mgr);
                return "Package manager set to: " + mgr;
            }
        },
        update {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                Tuils.sendOutput(pack.context, "Checking for updates...");
                PackageManager.fetchRepoIndex(pack.context, new PackageManager.RepoCallback() {
                    @Override
                    public void onSuccess(List<PackageManager.PackageInfo> packages) {
                        Map<String, String> installed = PackageManager.getInstalledPackagesMap(pack.context);
                        boolean found = false;
                        for (PackageManager.PackageInfo p : packages) {
                            if (installed.containsKey(p.name)) {
                                Tuils.sendOutput(pack.context, "Update available: " + p.name + " (" + p.version + ")");
                                found = true;
                            }
                        }
                        if (!found) Tuils.sendOutput(pack.context, "All packages up to date.");
                    }
                    @Override
                    public void onError(String error) { Tuils.sendOutput(android.graphics.Color.RED, pack.context, "Update check failed: " + error); }
                });
                return null;
            }
        },
        upgrade {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                Tuils.sendOutput(pack.context, "Upgrading packages...");
                PackageManager.fetchRepoIndex(pack.context, new PackageManager.RepoCallback() {
                    @Override
                    public void onSuccess(List<PackageManager.PackageInfo> packages) {
                        Map<String, String> installed = PackageManager.getInstalledPackagesMap(pack.context);
                        for (PackageManager.PackageInfo p : packages) {
                            if (installed.containsKey(p.name)) {
                                Tuils.sendOutput(pack.context, "Upgrading " + p.name + "...");
                                PackageManager.installPackage(pack.context, p.name, null);
                            }
                        }
                    }
                    @Override
                    public void onError(String error) { Tuils.sendOutput(android.graphics.Color.RED, pack.context, "Upgrade failed: " + error); }
                });
                return null;
            }
        },
        update_app {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                String primaryAbi = PackageManager.getPrimaryAbi();
                Tuils.sendOutput(pack.context, "Checking for TUI update (Device ABI: " + primaryAbi + ")...");
                PackageManager.fetchRepoIndex(pack.context, new PackageManager.RepoCallback() {
                    @Override
                    public void onSuccess(List<PackageManager.PackageInfo> packages) {
                        PackageManager.PackageInfo target = null;
                        // 1. Check for ABI specific entry, e.g. "tui-app-arm64-v8a" or abi property
                        for (PackageManager.PackageInfo p : packages) {
                            if (p.name.equalsIgnoreCase("tui-app-" + primaryAbi) ||
                                ("tui-app".equalsIgnoreCase(p.name) && primaryAbi.equalsIgnoreCase(p.abi))) {
                                target = p;
                                break;
                            }
                        }
                        // 2. Fallback to generic "tui-app"
                        if (target == null) {
                            for (PackageManager.PackageInfo p : packages) {
                                if ("tui-app".equalsIgnoreCase(p.name)) {
                                    target = p;
                                    break;
                                }
                            }
                        }

                        if (target != null) {
                            final PackageManager.PackageInfo chosen = target;
                            Tuils.sendOutput(pack.context, "Found update: " + chosen.name + " (" + chosen.version + ")" + (chosen.abi != null && !chosen.abi.isEmpty() ? " [" + chosen.abi + "]" : ""));
                            PackageManager.installPackage(pack.context, chosen.name, new PackageManager.InstallCallback() {
                                @Override
                                public void onSuccess(String message) { Tuils.sendOutput(pack.context, "Package installer initiated. Please complete installation."); }
                                @Override
                                public void onError(String error) { Tuils.sendOutput(android.graphics.Color.RED, pack.context, "App update failed: " + error); }
                            });
                            return;
                        }
                        Tuils.sendOutput(pack.context, "No TUI app update found.");
                    }
                    @Override
                    public void onError(String error) { Tuils.sendOutput(android.graphics.Color.RED, pack.context, "Update check failed: " + error); }
                });
                return null;
            }
        },
        check_update {
            @Override
            public int[] args() { return new int[0]; }
            @Override
            public String exec(ExecutePack pack) {
                String primaryAbi = PackageManager.getPrimaryAbi();
                Tuils.sendOutput(pack.context, "Scanning repository for TUI releases (ABI: " + primaryAbi + ")...");
                PackageManager.fetchRepoIndex(pack.context, new PackageManager.RepoCallback() {
                    @Override
                    public void onSuccess(List<PackageManager.PackageInfo> packages) {
                        boolean found = false;
                        for (PackageManager.PackageInfo p : packages) {
                            if (p.name.startsWith("tui-app")) {
                                Tuils.sendOutput(pack.context, "• " + p.name + " v" + p.version + " (" + (p.abi != null && !p.abi.isEmpty() ? p.abi : "universal") + ")");
                                if (p.sha256 != null && !p.sha256.isEmpty()) {
                                    Tuils.sendOutput(pack.context, "  SHA-256: " + p.sha256);
                                }
                                found = true;
                            }
                        }
                        if (!found) Tuils.sendOutput(pack.context, "No TUI releases indexed in repository.");
                    }
                    @Override
                    public void onError(String error) { Tuils.sendOutput(android.graphics.Color.RED, pack.context, "Check failed: " + error); }
                });
                return null;
            }
        },
        get {
            @Override
            public int[] args() { return new int[0]; }

            @Override
            public String exec(ExecutePack pack) {
                String mgr = VariablesManager.getVariable("active_pkg_mgr");
                if (mgr == null || mgr.isEmpty()) mgr = "hybrid";
                return "Current package manager: " + mgr;
            }
        },
        run {
            @Override
            public int[] args() { return new int[] {CommandAbstraction.PLAIN_TEXT}; }

            @Override
            public String exec(ExecutePack pack) {
                String cmd = pack.getString().trim();
                if (cmd.isEmpty()) return pack.context.getString(R.string.output_lessarg);

                new Thread(() -> {
                    String result = ohi.andre.consolelauncher.managers.PackageHelper.tuiRun(pack.context, cmd);
                    Tuils.sendOutput(pack.context, result);
                }).start();
                return "Launching Termux command: " + cmd;
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
        public String onNotArgEnough(ExecutePack pack, int n) { return pack.context.getString(R.string.help_tui_manager); }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) { return pack.context.getString(R.string.output_invalid_param) + Tuils.SPACE + pack.get(); }
    }

    @Override
    public String[] params() { return Param.labels(); }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) { return Param.get(param); }

    @Override
    protected String doThings(ExecutePack pack) { return null; }

    @Override
    public int priority() { return 3; }

    @Override
    public int helpRes() { return R.string.help_tui_manager; }
}
