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
    val connector = createShellConnector()
    val settings = object : DefaultSettingsProvider() {
        @Deprecated("Deprecated in Java")
        override fun getDefaultStyle(): TextStyle =
            TextStyle(TerminalColor.rgb(218, 218, 218), TerminalColor.rgb(17, 17, 17))

        override fun getDefaultForeground(): TerminalColor = TerminalColor.rgb(218, 218, 218)
        override fun getDefaultBackground(): TerminalColor = TerminalColor.rgb(17, 17, 17)
        override fun getTerminalFontSize(): Float = 14f
    }
    val terminal = JediTermWidget(80, 24, settings).apply {
        terminalPanel.setDefaultCursorShape(CursorShape.BLINK_VERTICAL_BAR)
        terminalPanel.setCursorShape(CursorShape.BLINK_VERTICAL_BAR)
        setTtyConnector(connector)
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
            environment["PROMPT_COMMAND"] =
                "printf '\\nKerminal - A terminal made in Kotlin\\nVersion Rewrite 1.2.1\\n'; " +
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
