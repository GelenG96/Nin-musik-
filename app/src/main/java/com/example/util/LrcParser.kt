package com.example.util

import com.example.data.model.LyricLine
import java.io.File
import java.util.regex.Pattern

object LrcParser {
    // Regex for standard LRC time tags [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
    private val TIME_TAG_REGEX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyricLines = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check for metadata tags like [ti:], [ar:], [al:], etc.
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:")
            ) {
                continue
            }

            val matcher = TIME_TAG_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msString = matcher.group(3) ?: "0"
                val ms = when (msString.length) {
                    1 -> msString.toLong() * 100
                    2 -> msString.toLong() * 10
                    else -> msString.take(3).toLong()
                }

                val totalMs = (min * 60 + sec) * 1000 + ms
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastMatchEnd).trim()
                for (timestamp in timestamps) {
                    lyricLines.add(LyricLine(timestamp, text))
                }
            }
        }

        // Sort by timestamp
        lyricLines.sortBy { it.timestampMs }
        return lyricLines
    }

    fun findLrcFileForTrack(dataPath: String): File? {
        if (dataPath.isEmpty()) return null
        val audioFile = File(dataPath)
        val parent = audioFile.parentFile ?: return null

        val baseName = audioFile.nameWithoutExtension

        // 1. Check exact same name with .lrc extension
        val exactLrc = File(parent, "$baseName.lrc")
        if (exactLrc.exists() && exactLrc.isFile) return exactLrc

        // 2. Check case-insensitive
        val files = parent.listFiles { _, name -> name.endsWith(".lrc", ignoreCase = true) }
        val matching = files?.firstOrNull { it.nameWithoutExtension.equals(baseName, ignoreCase = true) }
        if (matching != null && matching.exists()) return matching

        return null
    }

    fun findActiveLyricIndex(lyrics: List<LyricLine>, currentPositionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var low = 0
        var high = lyrics.size - 1
        var activeIndex = -1

        while (low <= high) {
            val mid = (low + high) / 2
            if (lyrics[mid].timestampMs <= currentPositionMs) {
                activeIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return activeIndex
    }
}
