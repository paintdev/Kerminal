// Imagine a JVM language, with no unnecessary verbosity!

package org.paintdev

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.CursorShape
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.jediterm.terminal.TerminalColor
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.StandardCharsets
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        try {
            createTerminalWindow()
        } catch (error: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Could not start the terminal: ${error.message ?: error.javaClass.simpleName}",
                "Kerminal",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}

private fun createTerminalWindow() {
    val baseConnector = createShellConnector()
    val settings = object : DefaultSettingsProvider() {
        @Suppress("DEPRECATION")
        override fun getDefaultStyle(): TextStyle =
            TextStyle(TerminalColor.rgb(218, 218, 218), TerminalColor.rgb(17, 17, 17))

        override fun getDefaultForeground(): TerminalColor = TerminalColor.rgb(218, 218, 218)
        override fun getDefaultBackground(): TerminalColor = TerminalColor.rgb(17, 17, 17)
        override fun getTerminalFontSize(): Float = 14f
    }

    class InterceptingConnector(
        private val delegate: TtyConnector,
        private val onExit: () -> Unit
    ) : TtyConnector by delegate {
        private val buffer = StringBuilder()

        override fun write(data: String) {
            for (char in data) {
                if (char == '\n' || char == '\r') {
                    val currentInput = buffer.toString().trim()
                    if (currentInput == "exit") {
                        onExit()
                        buffer.setLength(0)
                    } else {
                        delegate.write(char.toString())
                        buffer.setLength(0)
                    }
                } else {
                    buffer.append(char)
                    delegate.write(char.toString())
                }
            }
        }
    }

    val interceptingConnector = InterceptingConnector(baseConnector) {
        Runtime.getRuntime().halt(0)
    }

    val terminal = JediTermWidget(80, 24, settings).apply {
        terminalPanel.setDefaultCursorShape(CursorShape.BLINK_VERTICAL_BAR)
        terminalPanel.setCursorShape(CursorShape.BLINK_VERTICAL_BAR)
        setTtyConnector(interceptingConnector)
        start()
    }

    JFrame("Kerminal").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        minimumSize = Dimension(640, 400)
        size = Dimension(900, 600)
        setLocationByPlatform(true)
        contentPane = terminal
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                terminal.close()
            }
        })
        isVisible = true
        terminal.requestFocusInWindow()
    }
}

private fun createShellConnector(): TtyConnector {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val environment = HashMap(System.getenv()).apply {
        put("TERM", "xterm-256color")
        put("COLORTERM", "truecolor")
        put("CLICOLOR_FORCE", "1")
        put("FORCE_COLOR", "1")
    }
    val shell = if (isWindows) {
        listOf("cmd.exe")
    } else {
        val loginShell = System.getenv("SHELL")
            ?.takeIf { it.startsWith("/") && it.substringAfterLast('/').isNotBlank() }
            ?: "/bin/bash"
        if (loginShell.substringAfterLast('/') == "bash") {
            val fakeCommands = "kerminal-whatsnew() { echo -e \"What's new in v1.3-2-test:\\n- More polished source code\\n- New version formatting\\n  Instead of v1.3.0, now it'll be v1.3!\\n- Added alert when running exit\\n\\nv1.3 is not complete yet, and this is an extremely experimental version. Please use Rewrite 1.2.1 if you want a somewhat stable Kerminal experience.\"; }; " +
                               "app-info() { echo \"Kerminal v1.3-2-test (Unstable Build)\"; }; " +
                               "kizzy() { echo \"kizzy-june\"; }; " +
                               "exit-app() { echo \"Exit is currently broken in Kerminal and you have to close it manually instead. Sorry for any inconvenience this may have caused.\"; }; " +
                               "alias exit='echo \"Exit is currently broken in Kerminal and you have to close it manually instead. Sorry for any inconvenience this may have caused.\"'; "

            environment["PROMPT_COMMAND"] =
                "printf 'Kerminal - A terminal made in Kotlin\\nVersion 1.3-2-test !This is a test build, and is therefore unstable!\\nRun kerminal-whatsnew to see what is new!\\n'; " +
                        "$fakeCommands " +
                        "PS1='\\[\\e[32m\\][\\u@\\h \\W]\\$\\[\\e[0m\\] '; unset PROMPT_COMMAND"
        }
        listOf(loginShell, "--login")
    }

    val process = PtyProcessBuilder()
        .setCommand(shell.toTypedArray())
        .setEnvironment(environment)
        .setDirectory(System.getProperty("user.home"))
        .start()

    return ResizablePtyConnector(process, shell)
}

private class ResizablePtyConnector(
    private val process: PtyProcess,
    command: List<String>
) : ProcessTtyConnector(process, StandardCharsets.UTF_8, command) {
    override fun resize(termSize: TermSize) {
        if (isConnected()) {
            process.setWinSize(WinSize(termSize.columns, termSize.rows))
        }
    }

    override fun getName(): String = "Local"

    override fun close() {
        val descendants = process.descendants().toList()
        super.close()
        descendants.forEach { child -> child.destroy() }
    }
}
