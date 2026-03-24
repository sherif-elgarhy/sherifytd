import subprocess
import os
import re

# Detect platform for save path
import os as _os
_termux = "com.termux" in _os.environ.get("HOME", "") or _os.environ.get("TERMUX_VERSION")
BASE_DIR = _os.path.expanduser(
    "~/storage/shared/YTD" if _termux else "~/Downloads/YTD"
)

# Platforms that support playlists
PLAYLIST_DOMAINS = ["youtube.com", "youtu.be", "soundcloud.com", "vimeo.com"]

# Short-form content patterns
SHORTS_PATTERN = re.compile(r'shorts|/reel|share/r|tiktok\.com', re.IGNORECASE)

def detect_platform(url):
    if any(d in url for d in ["youtube.com", "youtu.be"]): return "YouTube"
    if "soundcloud.com" in url: return "SoundCloud"
    if "instagram.com" in url: return "Instagram"
    if "tiktok.com" in url: return "TikTok"
    if "facebook.com" in url: return "Facebook"
    if "x.com" in url: return "X"
    if "vimeo.com" in url: return "Vimeo"
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


def build_command(url, fmt, playlist, save_path):
    cmd = [
        "yt-dlp",
        "--newline",       # one line per progress update
        "--progress",      # force progress output even when not a TTY
        "--no-quiet",      # ensure output is not suppressed
        "--restrict-filenames",
        "--no-part",
        "--no-continue",
        "--no-cache-dir",
        "--trim-filenames", "100",
        "--print", "after_move:filepath",
        "-o", f"{save_path}/%(title)s.%(ext)s",
    ]

    if not playlist:
        cmd.append("--no-playlist")

    if fmt == "mp3":
        cmd += ["-x", "--audio-format", "mp3"]
    else:
        # Slash fallback: try requested format, fall back to best
        cmd += ["-f", f"{fmt}/best"]

    cmd.append(url)
    return cmd


def download_stream(url, fmt="best", playlist=False):
    save_path = get_smart_path(url, playlist)
    cmd = build_command(url, fmt, playlist, save_path)

    return subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
