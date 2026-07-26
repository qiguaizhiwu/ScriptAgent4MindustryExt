@file:Import("org.jline:jline-terminal-jansi:3.21.0", mavenDepends = true)
@file:Import("org.jline:jline-terminal:3.21.0", mavenDepends = true)
@file:Import("org.fusesource.jansi:jansi:2.4.0", mavenDepends = true)
@file:Import("org.jline:jline-reader:3.21.0", mavenDepends = true)

package coreMindustry

import arc.util.Log
import arc.util.Strings
import org.jline.reader.*
import org.jline.utils.AttributedString
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.io.PrintStream
import java.util.logging.Level
import kotlin.system.exitProcess

// ===== 颜色转换 =====
private val arcColorToAnsiMap = mapOf(
    "&fr" to "[0m", "&fb" to "[1m", "&fd" to "[2m",
    "&fu" to "[4m", "&fi" to "[3m",
    "&k" to "[30m", "&K" to "[90m", "&w" to "[37m", "&W" to "[97m",
    "&r" to "[31m", "&R" to "[91m", "&g" to "[32m", "&G" to "[92m",
    "&y" to "[33m", "&Y" to "[93m", "&b" to "[34m", "&B" to "[94m",
    "&m" to "[35m", "&M" to "[95m", "&c" to "[36m", "&C" to "[96m",
    "&lc" to "[96m", "&lb" to "[94m", "&ly" to "[93m", "&lr" to "[91m",
    "&lg" to "[92m", "&lm" to "[95m", "&lk" to "[90m", "&lw" to "[97m",
    "&p" to "[35m", "&P" to "[95m",
    "&br" to "[41m", "&bg" to "[42m", "&by" to "[43m", "&bb" to "[44m",
    "&bd" to "[49m"
)

private val mindustryColorToArcMap = mapOf(
    "red" to "&r", "green" to "&g", "yellow" to "&y", "blue" to "&b",
    "purple" to "&m", "cyan" to "&c", "white" to "&w",
    "gray" to "&k", "lightgray" to "&K", "darkgray" to "&k",
    "scarlet" to "&r", "pink" to "&M", "orange" to "&y", "gold" to "&Y",
    "accent" to "&c", "sky" to "&c", "acid" to "&G", "tan" to "&y",
    "salmon" to "&R", "coral" to "&R", "violet" to "&m", "magenta" to "&M",
    "olive" to "&y", "goldenrod" to "&Y",
    "clear" to "&fr", "unlaunched" to "&fr", "light_gray" to "&K", "dark_gray" to "&k",
    "light_yellow" to "&Y", "light_red" to "&R", "light_green" to "&G",
    "light_blue" to "&B", "light_purple" to "&M", "light_cyan" to "&C"
)

fun mindustryColorToArc(text: String): String {
    var result = text.replace("[]", "&fr")
    result = Regex("""\[#([0-9a-fA-F]{6})]""").replace(result) { m ->
        val h = m.groupValues[1]
        closestArcColor(
            h.substring(0, 2).toInt(16),
            h.substring(2, 4).toInt(16),
            h.substring(4, 6).toInt(16)
        )
    }
    result = Regex("""\[([a-zA-Z_]+)]""").replace(result) { m ->
        mindustryColorToArcMap[m.groupValues[1]] ?: m.value
    }
    return result
}

fun closestArcColor(r: Int, g: Int, b: Int): String = when {
    r > 200 && g < 100 && b < 100 -> "&r"
    r < 100 && g > 200 && b < 100 -> "&g"
    r > 200 && g > 200 && b < 100 -> "&y"
    r < 100 && g < 100 && b > 200 -> "&b"
    r > 200 && g < 100 && b > 200 -> "&m"
    r < 100 && g > 200 && b > 200 -> "&c"
    r > 200 && g > 200 && b > 200 -> "&w"
    r < 100 && g < 100 && b < 100 -> "&k"
    r > 150 && g < 100 && b < 100 -> "&R"
    r < 100 && g > 150 && b < 100 -> "&G"
    r > 150 && g > 150 && b < 100 -> "&Y"
    r < 100 && g < 100 && b > 150 -> "&B"
    r > 150 && g < 100 && b > 150 -> "&M"
    r < 100 && g > 150 && b > 150 -> "&C"
    else -> "&w"
}

fun arcColorToAnsi(text: String): String {
    var result = text
    for ((arc, ansi) in arcColorToAnsiMap) {
        result = result.replace(arc, ansi)
    }
    return result
}

fun stripAllColors(text: String): String {
    var result = arcColorToAnsi(text)
    result = Regex("""\[[0-9;]*m""").replace(result, "")
    result = Regex("""\[[a-zA-Z_]*]""").replace(result, "")
    result = Regex("""\[#[0-9a-fA-F]{6}]""").replace(result, "")
    return result
}
// ===== 颜色转换结束 =====

class MyPrintStream(private val block: (String) -> Unit) : PrintStream(ByteArrayOutputStream()) {
    private val bufOut = out as ByteArrayOutputStream

    var last = -1
    override fun write(b: Int) {
        if (last == 13 && b == 10) {// \r\n
            last = -1
            return
        }
        last = b
        if (b == 13 || b == 10) flush()
        else super.write(b)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        if (len < 0) throw ArrayIndexOutOfBoundsException(len)
        for (i in 0 until len)
            write(buf[off + i].toInt())
    }

    @Synchronized
    override fun flush() {
        val str = try {
            bufOut.toString()
        } finally {
            bufOut.reset()
        }
        block(str)
    }
}

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val cmd = line.line().substring(0, line.cursor()).split(' ')
        val res = runBlocking(Dispatchers.game) {
            Commands.Root.tabComplete {
                arg = cmd
            }
        }
        candidates += res.map {
            Candidate(it)
        }
    }
}

@OptIn(LoaderApi::class)
suspend fun handleInput(reader: LineReader) {
    var last = 0
    while (isActive) {
        val line = try {
            runInterruptible {
                reader.readLine("[0m> ").let(RootCommands::trimInput)
            }
        } catch (e: InterruptedIOException) {
            return
        } catch (e: UserInterruptException) {
            if (!enabled) break//script disable
            if (last != 1) {
                reader.printAbove("Interrupt again to force exit application")
                last = 1
                continue
            }
            reader.printAbove("force exit")
            exitProcess(255)
        } catch (e: EndOfFileException) {
            if (last != 2) {
                reader.printAbove("Catch EndOfFile, again to exit application")
                last = 2
                continue
            }
            reader.printAbove("exit")
            ScriptManager.disableAll()
            exitProcess(1)
        }
        last = 0
        if (line.isEmpty()) continue
        launch(Job()) {//ignore cancel
            try {
                RootCommands.handleInput(line, null)
            } catch (e: Throwable) {
                logger.log(Level.SEVERE, "error when handle input", e)
            }
        }.join()
        try {
            reader.terminal.writer().write("[0m")
            reader.terminal.writer().flush()
        } catch (_: Exception) {}
    }
}

var started = false
lateinit var reader: LineReader
fun start() {
    if (started) return
    started = true
    launch(Dispatchers.IO + CoroutineName("Console Reader")) {
        java.util.logging.Logger.getLogger("org.jline").level = Level.OFF
        val scReader = try {
            Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }?.let { sc ->
                sc.javaClass.getDeclaredField("lineReader").apply { isAccessible = true }.get(sc) as? LineReader
            }
        } catch (_: Exception) { null }
        if (scReader != null) {
            // 复用 ServerControl 的 reader，设置我们的 completer
            try {
                val completerField = scReader.javaClass.getDeclaredField("completer")
                completerField.isAccessible = true
                completerField.set(scReader, MyCompleter)
            } catch (_: Exception) {}
        }
        val isOwnTerminal = scReader == null
        reader = scReader ?: LineReaderBuilder.builder()
            .completer(MyCompleter)
            .variable(LineReader.HISTORY_FILE, Config.cacheDir.resolve("console.history"))
            .build()
        val bakOut = System.out
        System.setOut(MyPrintStream {
            reader.printAbove(AttributedString.fromAnsi(it + "[0m"))
        })
        try {
            handleInput(reader)
        } finally {
            System.setOut(bakOut)
            if (isOwnTerminal) reader.terminal.close()
        }
    }
}

onEnable {
    Log.useColors = true
    Log.formatter = Log.LogFormatter { text, useColors, arg ->
        var formatted = text.replace("@", "&fb&lb@&fr")
        if (arg != null && arg.isNotEmpty()) {
            formatted = Strings.format(formatted, *arg)
        }
        val unified = mindustryColorToArc(formatted)
        if (useColors) {
            arcColorToAnsi(unified)
        } else {
            stripAllColors(unified)
        }
    }
    Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }?.apply {
        javaClass.getDeclaredField("serverInput")
            .set(this, Runnable {
                logger.info("Overwrite ServerControl.serverInput")
                start()
            })
    }
    start()
}
