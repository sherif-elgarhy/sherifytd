import subprocess
import os
import re

# Detect platform for save path
import os as _os
_termux = "com.termux" in _os.environ.get("HOME", "") or _os.environ.get("TERMUX_VERSION")
BASE_DIR = _os.path.expanduser(
    "~/storage/shared/YTD" if _termux else "~/Downloads/YTD"
)

PLATFORM_DOMAINS = {
    "youtube.com":     "YouTube",
    "youtu.be":        "YouTube",
    "soundcloud.com":  "SoundCloud",
    "vimeo.com":       "Vimeo",
    "instagram.com":   "Instagram",
    "tiktok.com":      "TikTok",
    "facebook.com":    "Facebook",
    "x.com":           "X",
    "twitter.com":     "X",
    "twitch.tv":       "Twitch",
    "dailymotion.com": "Dailymotion",
    "reddit.com":      "Reddit",
    "rumble.com":      "Rumble",
    "bilibili.com":    "Bilibili",
    "ted.com":         "TED",
    "streamable.com":  "Streamable",
    "linkedin.com":    "LinkedIn",
    "pinterest.com":   "Pinterest",
}

PLAYLIST_DOMAINS = {"youtube.com", "youtu.be", "soundcloud.com", "vimeo.com"}

# Short-form content patterns
SHORTS_PATTERN = re.compile(r'shorts|/reel|share/r|tiktok\.com', re.IGNORECASE)

def detect_platform(url):
    for domain, name in PLATFORM_DOMAINS.items():
        if domain in url:
            return name
    return "Other"

def supports_playlist(url):
    return any(d in url for d in PLAYLIST_DOMAINS)

def get_smart_path(url, is_playlist):
    platform = detect_platform(url)

    # Shorts go under shorts/<Platform>/
    if SHORTS_PATTERN.search(url):
        target_dir = os.path.join(BASE_DIR, "shorts", platform)

    # Playlist subfolder — only for supported platforms
    elif is_playlist and supports_playlist(url):
        try:
            result = subprocess.run(
                [
                    "yt-dlp",
                    "--flat-playlist",
                    "--restrict-filenames",
                    "--print", "%(playlist_title)s",
                    "--no-warnings",
                    "--playlist-items", "1",
                    url,
                ],
                capture_output=True,
                text=True,
                timeout=15,
            )
            title = result.stdout.strip().splitlines()[0] if result.stdout.strip() else ""
            target_dir = os.path.join(BASE_DIR, platform, title or "Playlist")
        except Exception:
            target_dir = os.path.join(BASE_DIR, platform, "Playlist")

    else:
        target_dir = os.path.join(BASE_DIR, platform)

    os.makedirs(target_dir, exist_ok=True)
    return target_dir


def build_command(url, fmt, playlist, save_path, subs=False, sponsorblock=False, archive=False):
    cmd = [
        "yt-dlp",
        "--newline",
        "--progress",
        "--no-quiet",
        "--restrict-filenames",
        "--no-part",
        "--no-continue",
        "--no-cache-dir",
        "--trim-filenames", "100",
        "--embed-metadata",
        "--embed-thumbnail",
        "--print", "after_move:filepath",
        "-o", f"{save_path}/%(title)s.%(ext)s",
    ]

    if not playlist:
        cmd.append("--no-playlist")

    if fmt == "mp3":
        cmd += ["-x", "--audio-format", "mp3"]
    else:
        cmd += ["-f", fmt]

    if subs:
        cmd += ["--write-subs", "--write-auto-subs", "--sub-langs", "en,ar", "--embed-subs"]

    if sponsorblock:
        cmd += ["--sponsorblock-remove", "sponsor,selfpromo,interaction"]

    if archive:
        archive_file = os.path.expanduser("~/.sherifytd-archive.txt")
        cmd += ["--download-archive", archive_file]

    cmd.append(url)
    return cmd


def download_stream(url, fmt="bv*+ba/b", playlist=False, subs=False, sponsorblock=False, archive=False):
    save_path = get_smart_path(url, playlist)
    cmd = build_command(url, fmt, playlist, save_path, subs=subs, sponsorblock=sponsorblock, archive=archive)

    return subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
