package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Dashboard;
import ohi.andre.consolelauncher.tuils.Tuils;

public class DashboardManager {

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View rootContainer;
    private View gridWrapper;
    private View nativeWrapper;

    private TextView titleTv;
    private TextView timeTv;
    private TextView dotTv;

    private GridLayout gridContainer;

    private String dashboardMode = "grid";
    private boolean enableBlur = true;

    private int rows = 2;
    private int cols = 2;
    private int refreshIntervalMs = 1000;

    private String fillChar = "█";
    private String emptyChar = "░";

    private int boxBgColor = Color.TRANSPARENT;
    private int boxBorderColor = Color.parseColor("#00FF66");

    private int cpuColor = Color.parseColor("#00FF66");
    private int ramColor = Color.parseColor("#FFD700");
    private int batteryColor = Color.parseColor("#FF5555");
    private int storageColor = Color.parseColor("#00FFFF");
    private int netColor = Color.parseColor("#FF00FF");
    private int uptimeColor = Color.parseColor("#00FF66");

    private ActivityManager activityManager;
    private ActivityManager.MemoryInfo memoryInfo;

    private boolean isRunning = false;
    private boolean dotState = true;

    private static class BoxHolder {
        int index;
        int widgetType; // 0=CPU, 1=RAM, 2=BATTERY, 3=STORAGE, 4=NET, 5=UPTIME, 6=DEVICE, 7=THERMAL, 8=DISPLAY, 9=SHELL
        TextView titleTv;
        TextView detailTv;
        TextView barTv;
        int accentColor;
    }

    private final List<BoxHolder> boxHolders = new ArrayList<>();

    public DashboardManager(Context context, View rootView) {
        this.context = context;
        if (rootView == null) return;

        rootContainer = rootView.findViewById(R.id.dashboard_root_container);
        gridWrapper = rootView.findViewById(R.id.dashboard_grid_wrapper);
        nativeWrapper = rootView.findViewById(R.id.native_dashboard_wrapper);

        titleTv = rootView.findViewById(R.id.dashboard_title_tv);
        timeTv = rootView.findViewById(R.id.dashboard_time_tv);
        dotTv = rootView.findViewById(R.id.dashboard_dot_tv);
        gridContainer = rootView.findViewById(R.id.dashboard_grid_container);

        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        memoryInfo = new ActivityManager.MemoryInfo();

        loadConfig();
        applyModeAndStyling();
        if ("grid".equalsIgnoreCase(dashboardMode)) {
            setupGrid();
        }
    }

    private void loadConfig() {
        try {
            dashboardMode = XMLPrefsManager.get(Dashboard.dashboard_mode);
            if (dashboardMode == null || dashboardMode.isEmpty()) dashboardMode = "grid";

            enableBlur = XMLPrefsManager.getBoolean(Dashboard.enable_blur_effect);

            String title = XMLPrefsManager.get(Dashboard.dashboard_title);
            if (titleTv != null && title != null && !title.isEmpty()) {
                titleTv.setText(" " + title);
            }

            String gridLayoutStr = XMLPrefsManager.get(Dashboard.grid_layout);
            int xmlCols = XMLPrefsManager.getInt(Dashboard.grid_columns);
            int xmlRows = XMLPrefsManager.getInt(Dashboard.grid_rows);

            if (gridLayoutStr != null && gridLayoutStr.toLowerCase().contains("x")) {
                String[] parts = gridLayoutStr.toLowerCase().split("x");
                if (parts.length >= 2) {
                    try {
                        xmlRows = Integer.parseInt(parts[0].trim());
                        xmlCols = Integer.parseInt(parts[1].trim());
                    } catch (Exception ignored) {}
                }
            }

            rows = Math.max(1, Math.min(5, xmlRows > 0 ? xmlRows : 2));
            cols = Math.max(1, Math.min(5, xmlCols > 0 ? xmlCols : 2));

            String fill = XMLPrefsManager.get(Dashboard.bar_fill_char);
            if (fill != null && !fill.isEmpty()) fillChar = fill;

            String empty = XMLPrefsManager.get(Dashboard.bar_empty_char);
            if (empty != null && !empty.isEmpty()) emptyChar = empty;

            refreshIntervalMs = XMLPrefsManager.getInt(Dashboard.refresh_interval_ms);
            if (refreshIntervalMs < 200) refreshIntervalMs = 1000;

            try {
                boxBgColor = Color.parseColor(XMLPrefsManager.get(Dashboard.box_bg_color));
            } catch (Exception e) {
                boxBgColor = Color.TRANSPARENT;
            }

            try { boxBorderColor = Color.parseColor(XMLPrefsManager.get(Dashboard.box_border_color)); } catch (Exception e) {}
            try { cpuColor = Color.parseColor(XMLPrefsManager.get(Dashboard.cpu_color)); } catch (Exception e) {}
            try { ramColor = Color.parseColor(XMLPrefsManager.get(Dashboard.ram_color)); } catch (Exception e) {}
            try { batteryColor = Color.parseColor(XMLPrefsManager.get(Dashboard.battery_color)); } catch (Exception e) {}
            try { storageColor = Color.parseColor(XMLPrefsManager.get(Dashboard.storage_color)); } catch (Exception e) {}
            try { netColor = Color.parseColor(XMLPrefsManager.get(Dashboard.net_color)); } catch (Exception e) {}
            try { uptimeColor = Color.parseColor(XMLPrefsManager.get(Dashboard.uptime_color)); } catch (Exception e) {}

        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private void applyModeAndStyling() {
        boolean isGrid = "grid".equalsIgnoreCase(dashboardMode);

        if (gridWrapper != null) {
            gridWrapper.setVisibility(isGrid ? View.VISIBLE : View.GONE);
        }
        if (nativeWrapper != null) {
            nativeWrapper.setVisibility(isGrid ? View.GONE : View.VISIBLE);
        }

        if (gridWrapper != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    gridWrapper.setRenderEffect(null);
                } catch (Exception ignored) {}
            }

            if (isGrid) {
                // Apply Glassmorphism translucent background
                GradientDrawable frostedGlassBackground = new GradientDrawable();
                frostedGlassBackground.setColor(Color.parseColor("#D90B0F19")); // Translucent dark panel fill
                frostedGlassBackground.setStroke(Tuils.dpToPx(context, 1), Color.parseColor("#6000FF66")); // Accent border
                frostedGlassBackground.setCornerRadius(Tuils.dpToPx(context, 8));
                gridWrapper.setBackground(frostedGlassBackground);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                    gridWrapper.setElevation(Tuils.dpToPx(context, 8));
                }
            }
        }

        // Clear any blur effect from the terminal output view
        if (rootContainer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                View terminalView = null;
                View parentView = (View) rootContainer.getParent();
                if (parentView != null) {
                    terminalView = parentView.findViewById(R.id.terminal_view);
                }
                if (terminalView == null && rootContainer.getRootView() != null) {
                    terminalView = rootContainer.getRootView().findViewById(R.id.terminal_view);
                }

                if (terminalView != null) {
                    terminalView.setRenderEffect(null);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupGrid() {
        if (gridContainer == null) return;

        gridContainer.removeAllViews();
        boxHolders.clear();

        gridContainer.setColumnCount(cols);
        gridContainer.setRowCount(rows);

        int totalBoxes = rows * cols;
        Typeface monoTypeface = Tuils.getTypeface(context);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels - Tuils.dpToPx(context, 24);
        int cellWidth = (screenWidth / cols) - Tuils.dpToPx(context, 8);

        for (int i = 0; i < totalBoxes; i++) {
            int widgetType = i % 10;
            int accent = cpuColor;
            String boxTitle = "[ CPU LOAD ]";

            switch (widgetType) {
                case 0: accent = cpuColor; boxTitle = "[ CPU CORE LOAD ]"; break;
                case 1: accent = ramColor; boxTitle = "[ RAM MEM ALLOC ]"; break;
                case 2: accent = batteryColor; boxTitle = "[ BATTERY CELL ]"; break;
                case 3: accent = storageColor; boxTitle = "[ DISK STORAGE ]"; break;
                case 4: accent = netColor; boxTitle = "[ NETWORK NET ]"; break;
                case 5: accent = uptimeColor; boxTitle = "[ SYS UPTIME ]"; break;
                case 6: accent = Color.parseColor("#00FFFF"); boxTitle = "[ DEVICE MODEL ]"; break;
                case 7: accent = Color.parseColor("#FF5555"); boxTitle = "[ THERMAL SENS ]"; break;
                case 8: accent = Color.parseColor("#FFD700"); boxTitle = "[ DISPLAY MATRIX ]"; break;
                case 9: accent = Color.parseColor("#00FF66"); boxTitle = "[ SHELL KERNEL ]"; break;
            }

            LinearLayout boxLayout = new LinearLayout(context);
            boxLayout.setOrientation(LinearLayout.VERTICAL);
            boxLayout.setPadding(
                    Tuils.dpToPx(context, 6),
                    Tuils.dpToPx(context, 5),
                    Tuils.dpToPx(context, 6),
                    Tuils.dpToPx(context, 5)
            );

            GradientDrawable borderDrawable = new GradientDrawable();
            borderDrawable.setColor(boxBgColor);
            borderDrawable.setStroke(Tuils.dpToPx(context, 1), accent);
            borderDrawable.setCornerRadius(Tuils.dpToPx(context, 4));
            boxLayout.setBackground(borderDrawable);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = Math.max(cellWidth, Tuils.dpToPx(context, 60));
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(
                    Tuils.dpToPx(context, 4),
                    Tuils.dpToPx(context, 4),
                    Tuils.dpToPx(context, 4),
                    Tuils.dpToPx(context, 4)
            );

            boxLayout.setLayoutParams(params);

            // Title + Value Row
            LinearLayout titleRow = new LinearLayout(context);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView boxTitleTv = new TextView(context);
            boxTitleTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            boxTitleTv.setText(boxTitle);
            boxTitleTv.setTextColor(accent);
            boxTitleTv.setTextSize(9.5f);
            boxTitleTv.setTypeface(monoTypeface, Typeface.BOLD);

            TextView boxDetailTv = new TextView(context);
            boxDetailTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            boxDetailTv.setText("--");
            boxDetailTv.setTextColor(accent);
            boxDetailTv.setTextSize(9.5f);
            boxDetailTv.setTypeface(monoTypeface);

            titleRow.addView(boxTitleTv);
            titleRow.addView(boxDetailTv);

            TextView barTv = new TextView(context);
            barTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            barTv.setText("[░░░░░░░░░░]");
            barTv.setTextColor(accent);
            barTv.setTextSize(10f);
            barTv.setTypeface(monoTypeface);

            boxLayout.addView(titleRow);
            boxLayout.addView(barTv);

            gridContainer.addView(boxLayout);

            BoxHolder holder = new BoxHolder();
            holder.index = i;
            holder.widgetType = widgetType;
            holder.titleTv = boxTitleTv;
            holder.detailTv = boxDetailTv;
            holder.barTv = barTv;
            holder.accentColor = accent;
            boxHolders.add(holder);
        }
    }

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            if ("grid".equalsIgnoreCase(dashboardMode)) {
                updateDashboardData();
            }

            if (handler != null) {
                handler.postDelayed(this, refreshIntervalMs);
            }
        }
    };

    public void start() {
        if (isRunning) return;
        isRunning = true;
        handler.post(tickRunnable);
    }

    public void pause() {
        isRunning = false;
        handler.removeCallbacks(tickRunnable);
    }

    public void resume() {
        start();
    }

    private void updateDashboardData() {
        try {
            // Live clock & Pulsating dot animation
            if (timeTv != null) {
                timeTv.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            }
            if (dotTv != null) {
                dotState = !dotState;
                dotTv.setText(dotState ? "● LIVE" : "○ LIVE");
            }

            int cpuUsage = Tuils.getCpuUsage();

            int ramUsagePercent = 0;
            long ramAvailMb = 0;
            long ramTotalMb = 0;
            if (activityManager != null && memoryInfo != null) {
                activityManager.getMemoryInfo(memoryInfo);
                long tot = memoryInfo.totalMem;
                long avail = memoryInfo.availMem;
                ramTotalMb = tot / (1024 * 1024);
                ramAvailMb = avail / (1024 * 1024);
                if (tot > 0) {
                    ramUsagePercent = (int) (((tot - avail) * 100) / tot);
                }
            }

            int batteryPercent = 0;
            String batteryStatusStr = "DISCHG";
            Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int batTemp = 0;
            int batVolt = 0;
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    batteryPercent = (int) ((level * 100) / scale);
                }
                int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    batteryStatusStr = "CHG ⚡";
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    batteryStatusStr = "FULL";
                }
                batTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
                batVolt = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            }

            long storageFreeGb = 0;
            long storageTotalGb = 0;
            int storagePercent = 0;
            try {
                StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
                long blockSize = stat.getBlockSizeLong();
                long totalBlocks = stat.getBlockCountLong();
                long availableBlocks = stat.getAvailableBlocksLong();
                storageTotalGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024);
                storageFreeGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024);
                if (storageTotalGb > 0) {
                    storagePercent = (int) (((storageTotalGb - storageFreeGb) * 100) / storageTotalGb);
                }
            } catch (Exception ignored) {}

            String netTypeStr = "OFFLINE";
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNet = cm != null ? cm.getActiveNetworkInfo() : null;
                if (activeNet != null && activeNet.isConnected()) {
                    if (activeNet.getType() == ConnectivityManager.TYPE_WIFI) netTypeStr = "WIFI ONLINE";
                    else if (activeNet.getType() == ConnectivityManager.TYPE_MOBILE) netTypeStr = "CELL 4G/5G";
                    else netTypeStr = "ONLINE";
                }
            } catch (Exception ignored) {}

            long uptimeSeconds = SystemClock.elapsedRealtime() / 1000;
            long hours = uptimeSeconds / 3600;
            long mins = (uptimeSeconds % 3600) / 60;
            long secs = uptimeSeconds % 60;
            String uptimeStr = String.format(Locale.getDefault(), "%02dh:%02dm:%02ds", hours, mins, secs);

            int barChars = Math.max(6, 16 - (cols * 2));

            for (BoxHolder holder : boxHolders) {
                switch (holder.widgetType) {
                    case 0: // CPU
                        holder.detailTv.setText(cpuUsage + "%");
                        holder.barTv.setText(Tuils.getTerminalBar(cpuUsage, barChars, fillChar, emptyChar));
                        break;
                    case 1: // RAM
                        holder.detailTv.setText(ramUsagePercent + "%");
                        holder.barTv.setText(Tuils.getTerminalBar(ramUsagePercent, barChars, fillChar, emptyChar));
                        break;
                    case 2: // BATTERY
                        holder.detailTv.setText(batteryPercent + "% [" + batteryStatusStr + "]");
                        holder.barTv.setText(Tuils.getTerminalBar(batteryPercent, barChars, fillChar, emptyChar));
                        break;
                    case 3: // STORAGE
                        holder.detailTv.setText(storagePercent + "% (" + storageFreeGb + "GB Free)");
                        holder.barTv.setText(Tuils.getTerminalBar(storagePercent, barChars, fillChar, emptyChar));
                        break;
                    case 4: // NETWORK
                        holder.detailTv.setText(netTypeStr);
                        holder.barTv.setText("[ " + netTypeStr + " ]");
                        break;
                    case 5: // UPTIME
                        holder.detailTv.setText(uptimeStr);
                        holder.barTv.setText("[ UP: " + uptimeStr + " ]");
                        break;
                    case 6: // DEVICE
                        holder.detailTv.setText(Build.MODEL);
                        holder.barTv.setText("[ OS: Android " + Build.VERSION.RELEASE + " ]");
                        break;
                    case 7: // THERMAL
                        holder.detailTv.setText(batTemp + "°C");
                        holder.barTv.setText("[ TEMP: " + batTemp + "°C | " + batVolt + "mV ]");
                        break;
                    case 8: // DISPLAY
                        DisplayMetrics dm = context.getResources().getDisplayMetrics();
                        holder.detailTv.setText(dm.widthPixels + "x" + dm.heightPixels);
                        holder.barTv.setText("[ DPI: " + dm.densityDpi + " | SCALE: " + dm.density + " ]");
                        break;
                    case 9: // SHELL
                        holder.detailTv.setText("ACTIVE");
                        holder.barTv.setText("[ KERNEL MATRIX OK ]");
                        break;
                }
            }

        } catch (Exception e) {
            Tuils.log(e);
        }
    }
}
