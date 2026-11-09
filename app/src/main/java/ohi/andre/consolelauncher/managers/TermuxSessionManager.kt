package ohi.andre.consolelauncher.managers

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Termux Terminal Session Architecture for Android 8.0+ (API 26 to 34+)
 * Manages Scoped Storage directory environments, PTY bridges, and fallback setups.
 */
class TermuxSessionManager(private val context: Context) {

    companion object {
        private const val TAG = "TermuxSessionManager"
    }

    data class SessionConfig(
        val homePath: String,
        val prefixPath: String,
        val tmpPath: String,
        val binPath: String,
        val envMap: Map<String, String>
    )

    /**
     * Prepares environment directories compliant with Android 8.0+ Scoped Storage requirements
     */
    fun createSessionConfig(): SessionConfig {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(null) ?: context.filesDir
        } else {
            context.filesDir
        }

        val homeDir = File(baseDir, "home").apply { if (!exists()) mkdirs() }
        val prefixDir = File(baseDir, "usr").apply { if (!exists()) mkdirs() }
        val binDir = File(prefixDir, "bin").apply { if (!exists()) mkdirs() }
        val tmpDir = File(context.cacheDir, "tmp").apply { if (!exists()) mkdirs() }

        val systemPath = System.getenv("PATH") ?: "/system/bin:/system/xbin"
        val fullPath = "${binDir.absolutePath}:$systemPath"

        val env = mutableMapOf(
            "HOME" to homeDir.absolutePath,
            "PREFIX" to prefixDir.absolutePath,
            "TMPDIR" to tmpDir.absolutePath,
            "PATH" to fullPath,
            "TERM" to "xterm-256color",
            "COLORTERM" to "truecolor",
            "ANDROID_DATA" to System.getenv("ANDROID_DATA").orEmpty(),
            "ANDROID_ROOT" to System.getenv("ANDROID_ROOT").orEmpty()
        )

        Log.d(TAG, "Termux session configured at: HOME=${homeDir.absolutePath}, PATH=$fullPath")

        return SessionConfig(
            homePath = homeDir.absolutePath,
            prefixPath = prefixDir.absolutePath,
            tmpPath = tmpDir.absolutePath,
            binPath = binDir.absolutePath,
            envMap = env
        )
    }
}
