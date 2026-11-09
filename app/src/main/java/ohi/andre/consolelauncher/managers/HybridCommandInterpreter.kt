package ohi.andre.consolelauncher.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ohi.andre.consolelauncher.commands.main.MainPack
import java.io.File
import java.util.regex.Pattern

/**
 * Hybrid Command Interpreter for T-UI Launcher (API 26+)
 * Handlers:
 * 1. MacroDroid Triggering with variable extraction
 * 2. Native T-UI Launcher commands
 * 3. Fallback Termux / Shell PTY execution
 */
class HybridCommandInterpreter(
    private val context: Context,
    private val mainPack: MainPack? = null
) {

    companion object {
        private const val TAG = "HybridInterpreter"

        // MacroDroid Intent Constants
        const val MACRODROID_PACKAGE = "com.arlosoft.macrodroid"
        const val ACTION_RUN_MACRO = "com.arlosoft.macrodroid.macro.RUN_MACRO"
        const val EXTRA_MACRO_NAME = "macro_name"
        const val EXTRA_MACRO_NAME_ALT = "com.arlosoft.macrodroid.MACRO_NAME"

        // Regex for macro command parsing: macro "Macro Name" var1="val" var2=123
        private val MACRO_PATTERN = Pattern.compile("^macro\\s+(\"(?:[^\"]|\\\\\")*\"|\\S+)(?:\\s+(.*))?$", Pattern.CASE_INSENSITIVE)
        // Regex for key-value pair parsing: key="value", key='value', key=123
        private val KV_PATTERN = Pattern.compile("(\\w+)=(?:\"([^\"]*)\"|'([^']*)'|(\\S+))")
    }

    sealed class CommandResult {
        data class MacroTriggered(
            val macroName: String,
            val variables: Map<String, Any>,
            val intentSent: Boolean
        ) : CommandResult()

        data class NativeCommand(
            val commandName: String,
            val rawInput: String
        ) : CommandResult()

        data class ShellExecution(
            val command: String,
            val environment: Map<String, String>
        ) : CommandResult()
    }

    /**
     * Sequential input handler:
     * 1. MacroDroid Trigger check
     * 2. Native T-UI command check
     * 3. Termux PTY Shell fallback
     */
    fun handleInput(input: String): CommandResult {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            return CommandResult.ShellExecution("", getTermuxEnvironment())
        }

        // 1. MacroDroid Execution check
        val macroMatcher = MACRO_PATTERN.matcher(trimmedInput)
        if (macroMatcher.matches()) {
            val rawMacroName = macroMatcher.group(1) ?: ""
            val rawVars = macroMatcher.group(2) ?: ""

            // Extract Macro Name (strip surrounding quotes if present)
            val macroName = if (rawMacroName.startsWith("\"") && rawMacroName.endsWith("\"") && rawMacroName.length >= 2) {
                rawMacroName.substring(1, rawMacroName.length - 1).replace("\\\"", "\"")
            } else {
                rawMacroName
            }

            // Extract Key-Value variables
            val variables = parseVariables(rawVars)

            // Construct and fire Intent to MacroDroid
            val success = fireMacroDroidIntent(macroName, variables)
            return CommandResult.MacroTriggered(macroName, variables, success)
        }

        // 2. Native T-UI Commands check
        val firstToken = trimmedInput.split("\\s+".toRegex()).firstOrNull() ?: ""
        if (isNativeTUICommand(firstToken)) {
            return CommandResult.NativeCommand(firstToken, trimmedInput)
        }

        // 3. Fallback Termux Shell Execution
        val env = getTermuxEnvironment()
        return CommandResult.ShellExecution(trimmedInput, env)
    }

    /**
     * Parse trailing variable key-value pairs (e.g. var1="value" var2=123 var3=true)
     */
    fun parseVariables(argsString: String): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()
        if (argsString.isBlank()) return variables

        val matcher = KV_PATTERN.matcher(argsString)
        while (matcher.find()) {
            val key = matcher.group(1) ?: continue
            val quotedDouble = matcher.group(2)
            val quotedSingle = matcher.group(3)
            val unquoted = matcher.group(4)

            val stringVal = quotedDouble ?: quotedSingle ?: unquoted ?: ""

            // Type coercion: Boolean, Int, Double, or String
            val typedValue: Any = when {
                stringVal.equals("true", ignoreCase = true) -> true
                stringVal.equals("false", ignoreCase = true) -> false
                stringVal.toIntOrNull() != null -> stringVal.toInt()
                stringVal.toDoubleOrNull() != null -> stringVal.toDouble()
                else -> stringVal
            }

            variables[key] = typedValue
        }

        return variables
    }

    /**
     * Constructs and fires system-wide broadcast intent to MacroDroid with dynamic extras
     */
    fun fireMacroDroidIntent(macroName: String, variables: Map<String, Any>): Boolean {
        return try {
            val intent = Intent(ACTION_RUN_MACRO).apply {
                setPackage(MACRODROID_PACKAGE)
                putExtra(EXTRA_MACRO_NAME, macroName)
                putExtra(EXTRA_MACRO_NAME_ALT, macroName)

                // Attach dynamic key-value pair variables
                variables.forEach { (key, value) ->
                    when (value) {
                        is String -> putExtra(key, value)
                        is Int -> putExtra(key, value)
                        is Double -> putExtra(key, value)
                        is Boolean -> putExtra(key, value)
                        else -> putExtra(key, value.toString())
                    }
                }
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }

            context.sendBroadcast(intent)
            Log.d(TAG, "Sent MacroDroid intent for macro '$macroName' with ${variables.size} variables")
            MacroHistoryDbHelper(context).logExecution(macroName, variables, "SENT")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send MacroDroid intent", e)
            MacroHistoryDbHelper(context).logExecution(macroName, variables, "FAILED")
            false
        }
    }

    /**
     * Checks if a command matches native T-UI command system
     */
    private fun isNativeTUICommand(commandName: String): Boolean {
        if (mainPack == null) return false
        val commandGroup = mainPack.commandGroup ?: return false
        return commandGroup.getCommandByName(commandName) != null ||
               mainPack.aliasManager?.getAlias(commandName, false)?.get(0) != null
    }

    /**
     * Configures Termux environment variables for Scoped Storage (API 26+)
     */
    fun getTermuxEnvironment(): Map<String, String> {
        val externalFiles = context.getExternalFilesDir(null) ?: context.filesDir
        val homeDir = File(externalFiles, "home").apply { if (!exists()) mkdirs() }
        val prefixDir = File(externalFiles, "usr").apply { if (!exists()) mkdirs() }
        val tmpDir = File(context.cacheDir, "tmp").apply { if (!exists()) mkdirs() }
        val binDir = File(prefixDir, "bin").apply { if (!exists()) mkdirs() }

        val sysPath = System.getenv("PATH") ?: "/system/bin:/system/xbin"

        return mapOf(
            "HOME" to homeDir.absolutePath,
            "PREFIX" to prefixDir.absolutePath,
            "TMPDIR" to tmpDir.absolutePath,
            "PATH" to "${binDir.absolutePath}:$sysPath",
            "TERM" to "xterm-256color",
            "COLORTERM" to "truecolor"
        )
    }
}
