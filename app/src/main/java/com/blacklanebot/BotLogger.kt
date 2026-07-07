package com.blacklanebot

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BotLogger {
    private val lines = ArrayDeque<String>(100)
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(ctx: Context, msg: String) {
        val line = "[${fmt.format(Date())}] $msg"
        append(line)
        val intent = Intent("com.blacklanebot.BOT_LOG")
        intent.putExtra("message", line)
        ctx.sendBroadcast(intent)
    }

    fun append(line: String) {
        if (lines.size >= 200) lines.removeFirst()
        lines.addLast(line)
    }

    fun getLog(): String = lines.joinToString("\n")

    fun clear() { lines.clear() }
}
