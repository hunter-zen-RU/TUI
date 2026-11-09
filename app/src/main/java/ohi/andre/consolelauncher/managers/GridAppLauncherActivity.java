package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.R;

public class GridAppLauncherActivity extends Activity {

    private RecyclerView recyclerView;
    private AppAdapter adapter;

    private static class AppInfo {
        String label;
        String packageName;
        android.graphics.drawable.Drawable icon;

        AppInfo(String label, String packageName, android.graphics.drawable.Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_app_launcher_layout);

        recyclerView = findViewById(R.id.app_grid_rv);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        loadApps();
    }

    private void loadApps() {
        new Thread(() -> {
            List<AppInfo> apps = new ArrayList<>();
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo info : list) {
                apps.add(new AppInfo(
                        info.loadLabel(pm).toString(),
                        info.activityInfo.packageName,
                        info.loadIcon(pm)
                ));
            }

            Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));

            runOnUiThread(() -> {
                adapter = new AppAdapter(apps);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        private final List<AppInfo> data;

        AppAdapter(List<AppInfo> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_grid_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo app = data.get(position);
            holder.labelTv.setText(app.label);
            holder.iconIv.setImageDrawable(app.icon);
            holder.itemView.setOnClickListener(v -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    finish();
                }
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconIv;
            TextView labelTv;

            ViewHolder(View v) {
                super(v);
                iconIv = v.findViewById(R.id.app_icon_iv);
                labelTv = v.findViewById(R.id.app_label_tv);
            }
        }
    }
}
