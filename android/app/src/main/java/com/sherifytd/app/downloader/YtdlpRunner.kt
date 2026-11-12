package com.sherifytd.app.downloader

import android.content.Context
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class YtdlpRunner(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val binDir   = File(context.filesDir, "bin")
    private val ytdlpBin = File(binDir, "yt-dlp")

    private var currentProcess: Process? = null

    private fun ytdlpUrl(): String {
        val binary = when {
            Build.SUPPORTED_ABIS[0].contains("arm64") -> "yt-dlp_linux_aarch64"
            Build.SUPPORTED_ABIS[0].contains("arm")   -> "yt-dlp_linux_armv7l"
            else                                        -> "yt-dlp_linux_aarch64"
        }
        return "https://github.com/yt-dlp/yt-dlp/releases/latest/download/$binary"
    }

    private fun ensureBinary(): Flow<String> = flow {
        if (ytdlpBin.exists()) return@flow
        binDir.mkdirs()
        emit("📥 Downloading yt-dlp (first-time setup)...")
        val request = Request.Builder().url(ytdlpUrl()).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            emit("❌ Setup failed: HTTP ${response.code}")
            return@flow
        }
        response.body!!.byteStream().use { input ->
            FileOutputStream(ytdlpBin).use { output -> input.copyTo(output) }
        }
        ytdlpBin.setExecutable(true)
        emit("✅ yt-dlp ready")
    }.flowOn(Dispatchers.IO)

    fun download(
        url: String,
        fmt: String,
        playlist: Boolean,
        subs: Boolean,
        sponsorblock: Boolean,
        archive: Boolean,
        subLangs: String,
    ): Flow<String> = flow {
        ensureBinary().collect { emit(it) }
        if (!ytdlpBin.exists()) {
            emit("__ERROR__"); return@flow
        }

        val saveDir = resolveDir(url)
        saveDir.mkdirs()
        emit("📁 ${saveDir.path}")

        val cmd = mutableListOf(
            ytdlpBin.absolutePath,
            "--newline", "--progress",
            "--restrict-filenames",
            "--no-part", "--no-continue", "--no-cache-dir",
            "--trim-filenames", "100",
            "--embed-metadata",
            "-o", "${saveDir.absolutePath}/%(title)s.%(ext)s",
        )

        if (!playlist) cmd += "--no-playlist"

        if (fmt == "mp3") cmd += listOf("-x", "--audio-format", "mp3")
        else              cmd += listOf("-f", fmt)

        if (subs) cmd += listOf("--write-subs", "--write-auto-subs",
                                 "--sub-langs", subLangs, "--embed-subs")
        if (sponsorblock) cmd += listOf("--sponsorblock-remove",
                                         "sponsor,selfpromo,interaction")
        if (archive) {
            val f = File(context.filesDir, ".ytd-archive.txt")
            cmd += listOf("--download-archive", f.absolutePath)
        }

        cmd += url

        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        currentProcess = proc

        proc.inputStream.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.isNotBlank()) emit(line)
            }
        }

        val exit = proc.waitFor()
        currentProcess = null
        emit(if (exit == 0) "__DONE__" else "__ERROR__")
    }.flowOn(Dispatchers.IO)

    fun cancel() {
        currentProcess?.destroy()
        currentProcess = null
    }

    private fun resolveDir(url: String): File {
        val platform = PLATFORM_MAP.entries.firstOrNull { url.contains(it.key) }?.value ?: "Other"
        val isShort  = Regex("shorts|/reel|share/r|tiktok\\.com", RegexOption.IGNORE_CASE)
                           .containsMatchIn(url)
        val base = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).let { File(it, "YTD") }
        return if (isShort) File(base, "shorts/$platform") else File(base, platform)
    }

    companion object {
        private val PLATFORM_MAP = mapOf(
            "youtube.com"    to "YouTube",  "youtu.be"       to "YouTube",
            "instagram.com"  to "Instagram","tiktok.com"     to "TikTok",
            "facebook.com"   to "Facebook", "x.com"          to "X",
            "twitter.com"    to "X",        "soundcloud.com" to "SoundCloud",
            "vimeo.com"      to "Vimeo",    "twitch.tv"      to "Twitch",
            "dailymotion.com" to "Dailymotion", "reddit.com" to "Reddit",
            "rumble.com"     to "Rumble",   "bilibili.com"   to "Bilibili",
            "ted.com"        to "TED",      "linkedin.com"   to "LinkedIn",
            "pinterest.com"  to "Pinterest",
        )
    }
}
