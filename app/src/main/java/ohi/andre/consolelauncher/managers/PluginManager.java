package ohi.andre.consolelauncher.managers;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import dalvik.system.DexClassLoader;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.TuiCommandPlugin;

public class PluginManager {

    private static final String PLUGINS_DIR = "plugins";
    private final Context context;
    private final List<TuiCommandPlugin> loadedPlugins = new ArrayList<>();

    public PluginManager(Context context) {
        this.context = context;
    }

    public List<TuiCommandPlugin> loadPlugins() {
        loadedPlugins.clear();
        File pluginsDir = new File(Tuils.getFolder(), PLUGINS_DIR);
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
            return loadedPlugins;
        }

        File[] files = pluginsDir.listFiles((dir, name) -> name.endsWith(".dex") || name.endsWith(".jar"));
        if (files == null) return loadedPlugins;

        File dexOptDir = context.getCodeCacheDir();

        for (File file : files) {
            try {
                DexClassLoader loader = new DexClassLoader(
                        file.getAbsolutePath(),
                        dexOptDir.getAbsolutePath(),
                        null,
                        context.getClassLoader()
                );

                // For simplicity, we assume the entry point class is named 'TuiPlugin' in the root package
                // Real implementation would use a manifest or search for classes implementing TuiCommandPlugin
                Class<?> pluginClass = loader.loadClass("ohi.andre.tui.plugin.PluginEntry");
                if (TuiCommandPlugin.class.isAssignableFrom(pluginClass)) {
                    TuiCommandPlugin plugin = (TuiCommandPlugin) pluginClass.newInstance();
                    plugin.onLoaded();
                    loadedPlugins.add(plugin);
                    Tuils.sendOutput(context, "Loaded plugin: " + plugin.getPluginName());
                }

            } catch (Exception e) {
                Tuils.log("Failed to load plugin " + file.getName() + ": " + e.getMessage());
            }
        }

        return loadedPlugins;
    }

    public List<TuiCommandPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }
}
