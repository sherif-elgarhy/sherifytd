# 📥 SherifYTD

Download videos and audio from YouTube, Instagram, TikTok, X, SoundCloud, Vimeo, and more — directly on your phone or desktop. No ads, no accounts, no uploads.

Works on **Termux (Android)**, Linux, and macOS.

---

## Two modes, one entry point

| Mode | How it works | Best for |
|---|---|---|
| **Bash** (`ytd.sh`) | Runs directly in terminal | Fast downloads, batch mode, scripting |
| **Web UI** (`web/`) | Local Flask app in your browser | Easy paste, live progress, non-technical users |

Both use [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) under the hood and organize downloads by platform automatically.

On Android, sharing any link to Termux triggers `termux-url-opener` which asks which mode you want.

---

## Install

```bash
git clone https://github.com/sherif-elgarhy/sherifytd ~/sherifytd
bash ~/sherifytd/install.sh
```

That's it. The installer handles storage access (Termux), sets up `~/bin/`, installs `termux-url-opener`, and runs first-time dependency setup automatically — on Termux, Linux, and macOS.

---

## Bash mode

### Usage

```bash
# Interactive — prompts for URL and format
./ytd.sh

# Direct URL
./ytd.sh https://youtube.com/watch?v=...

# With options
./ytd.sh -f 'best[height<=720]' https://...
./ytd.sh -d ~/Music https://soundcloud.com/...
./ytd.sh -i urls.txt -n        # batch from file, no prompts
./ytd.sh -p https://...        # full playlist (YouTube, SoundCloud, Vimeo)
```

### Format options

| # | Format |
|---|---|
| 1 | 🎧 Music (M4A) |
| 2 | 🎵 Music (MP3) |
| 3 | 💿 Video 360p |
| 4 | 📹 Video 480p |
| 5 | 🎬 Video 720p |
| 6 | 🎬 Video 1080p |
| 7 | 🎬 Video 2160p |
| 8 | ✨ Best available |

---

## Web UI mode

```bash
cd ~/sherifytd/web
pip install flask yt-dlp
python app.py
# Opens automatically in your browser
```

Paste a URL, pick a format, watch it download live. On Android, sharing a link to Termux can open the browser directly to the download page.

---

## File organization

Downloads are sorted automatically by platform. Short-form content (Shorts, Reels, TikTok) is grouped together under `shorts/`:

```
~/Downloads/YTD/          (Linux/macOS)
~/storage/shared/YTD/     (Termux)
├── shorts/
│   ├── YouTube/
│   ├── Instagram/
│   ├── TikTok/
│   └── Facebook/
├── YouTube/
│   └── Playlist Name/
├── SoundCloud/
│   └── Playlist Name/
├── Instagram/
├── Facebook/
├── X/
├── Vimeo/
└── Other/
```

Playlist detection is supported for **YouTube, SoundCloud, and Vimeo** only.

---

## Supported sites

YouTube · Instagram · TikTok · X (Twitter) · Facebook · SoundCloud · Vimeo
and [1000+ more via yt-dlp](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md)

---

## Roadmap

- [x] Bash CLI with auto-install
- [x] Web UI (Flask)
- [x] Termux share hook
- [x] Cross-platform (Linux, macOS, Termux)
- [x] Smart file organization by platform
- [x] Shorts grouped separately
- [ ] Telegram bot

---

## License

MIT
