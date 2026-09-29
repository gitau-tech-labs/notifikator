package net.kzxiv.notify.client.service

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogStore {
    private const val MAX_LINES = 500
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun file(ctx: Context): File = File(ctx.filesDir, "forwarder_log.txt")

    @Synchronized
    fun append(ctx: Context, line: String) {
        try {
            val f = file(ctx)
            f.appendText("${fmt.format(Date())}  $line\n")
            val lines = f.readLines()
            if (lines.size > MAX_LINES) {
                f.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun read(ctx: Context): List<String> {
        val f = file(ctx)
        return if (f.exists()) f.readLines().reversed() else emptyList()
    }

    @Synchronized
    fun clear(ctx: Context) {
        file(ctx).writeText("")
    }
}
