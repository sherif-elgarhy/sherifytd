#!/usr/bin/env bash
# SherifYTD — Video downloader powered by yt-dlp
# Supports interactive (Termux share) and batch CLI usage
# Works on Termux (Android), Linux, and macOS

# Default settings
non_interactive=false
is_playlist=false
playlist_mode=false

format_flag="-f b"
format_prompt=true
playlist_flag="--no-playlist"
save_dir="$HOME/Downloads/YTD"

# 🧩 Platforms where format prompt is skipped (simpler media or fixed format)
skip_format_domains=("facebook.com" "instagram.com" "soundcloud.com" "tiktok.com")
# 🧩 Supported domains whitelist (extend as needed)
supported_domains=(
  "youtube.com"
  "youtu.be"
  "vimeo.com"
  "tiktok.com"
  "instagram.com"
  "facebook.com"
  "x.com"
  "soundcloud.com"
)

# 🧩 Build a single regex pattern for matching
domain_pattern=$(IFS="|"; echo "${supported_domains[*]}")

# ──────[ PLATFORM DETECTION ]──────

detect_platform() {
  if [[ -n "$TERMUX_VERSION" || "$HOME" == *com.termux* ]]; then
    echo "termux"
  elif [[ "$(uname -s)" == "Darwin" ]]; then
    echo "darwin"
  elif [[ -f /etc/debian_version ]]; then
    echo "debian"
  elif [[ -f /etc/arch-release ]]; then
    echo "arch"
  elif [[ "$(uname -s)" == "Linux" ]]; then
    echo "linux"
  else
    echo "unknown"
  fi
}

PLATFORM="$(detect_platform)"

# Set default save dir based on platform
if [[ "$PLATFORM" == "termux" ]]; then
  save_dir="$HOME/storage/shared/YTD"
else
  save_dir="$HOME/Downloads/YTD"
fi

# ──────[ DEPENDENCY INSTALLER ]──────

offer_install() {
  local tool="$1"
  local required="$2"   # "required" or "optional"

  echo ""
  if [[ "$required" == "required" ]]; then
    echo -e "❌ '$tool' is required but not installed."
  else
    echo -e "⚠️  '$tool' is not installed."
  fi

  local install_cmd=""
  case "$PLATFORM" in
    termux)  install_cmd="pkg install -y $tool" ;;
    darwin)
      if command -v brew &>/dev/null; then
        install_cmd="brew install $tool"
      else
        echo "⚠️  Homebrew not found. Install from https://brew.sh then run: brew install $tool"
      fi ;;
    debian)
      local pkg_name="$tool"
      [[ "$tool" == "python" ]] && pkg_name="python3"
      [[ "$tool" == "pip" ]]    && pkg_name="python3-pip"
      install_cmd="sudo apt install -y $pkg_name" ;;
    arch)    install_cmd="sudo pacman -S --noconfirm $tool" ;;
    *)       echo "💡 Please install '$tool' manually." ;;
  esac

  if [[ -n "$install_cmd" ]]; then
    echo "💡 Install command: $install_cmd"
    read -rp "👉 Install '$tool' now? [Y/n]: " ans
    if [[ "$ans" =~ ^[Yy]$ || -z "$ans" ]]; then
      if eval "$install_cmd"; then
        echo "✅ '$tool' installed successfully."
        return 0
      else
        echo "❌ Installation failed. Please install '$tool' manually."
      fi
    fi
  fi

  if [[ "$required" == "required" ]]; then
    echo "❌ '$tool' is required. Exiting."
    exit 1
  fi
  return 1
}

check_dependencies() {
  # python / ffmpeg
  for tool in python ffmpeg; do
    if ! command -v "$tool" &>/dev/null; then
      offer_install "$tool" "required"
      command -v "$tool" &>/dev/null || exit 1
    fi
  done

  # pip (may be separate on some systems)
  if ! command -v pip &>/dev/null && ! command -v pip3 &>/dev/null; then
    offer_install "pip" "required"
  fi

  # yt-dlp via pip
  if ! command -v yt-dlp &>/dev/null; then
    echo "📥 Installing yt-dlp..."
    pip install -U yt-dlp || pip3 install -U yt-dlp || {
      echo "❌ Could not install yt-dlp. Try: pip install yt-dlp"
      exit 1
    }
  fi
}

check_dependencies

# Parse command-line arguments for standalone mode
while [[ $# -gt 0 ]]; do
  case "$1" in
    -f|--format)
      user_fmt="$2" # Could update to handle mp3
      shift 2
      ;;
    -d|--dir)
      user_dir="$2"
      shift 2
      ;;
    -i|--input)
      input_file="$2"
      shift 2
      ;;
    -n|--non-interactive)
      non_interactive=true
      shift
      ;;
    -p|--playlist)
      playlist_mode=true
      shift
      ;;
    -h|--help)
      echo "📘 Usage:"
      echo "  termux-url-opener [URL] [options]"
      echo "  Options:"
      echo "   -f, --format <fmt>       Specify yt-dlp format (e.g. 'best', 'bestaudio[ext=m4a]')"
      echo "   -d, --dir <path>         Set custom save directory"
      echo "   -i, --input <file>       Read URLs from a file (one per line)"
      echo "   -n, --non-interactive    Skip prompts (batch mode)"
      echo "   -p, --playlist           Allow full playlist download"
      echo "   -h, --help               Show this help"
      exit 0
      ;;
    *)
      # Treat as URL if not a flag
      urls+=("$1")
      shift
      ;;
  esac
done

# ⚡ Ensure storage is accessible
if [[ "$PLATFORM" == "termux" ]]; then
  if [[ ! -d "$HOME/storage/shared" ]]; then
    echo "📂 Requesting access to shared storage..."
    echo "✅ Please allow storage permission in the Termux popup!"
    sleep 2
    termux-setup-storage
  fi
  default_dir="$HOME/storage/shared/YTD"
else
  default_dir="$HOME/Downloads/YTD"
fi

save_dir="$default_dir"

# 🧭 Handle custom directory if provided
if [ -n "$user_dir" ]; then
  echo "📁 Using custom directory: $user_dir"
  mkdir -p "$user_dir" 2>/dev/null

  if [ -d "$user_dir" ] && [ -w "$user_dir" ]; then
    save_dir="$user_dir"
  else
    echo "⚠️  Cannot access or write to $user_dir — falling back to default: $default_dir"
  fi
else
  echo "📁 Using default directory: $save_dir"
fi

# 🧩 Final safety net — make sure save_dir exists
mkdir -p "$save_dir" 2>/dev/null

# 🚀 Confirm
if [ ! -d "$save_dir" ] || [ ! -w "$save_dir" ]; then
  echo "❌ Cannot write to $save_dir"
  exit 1
fi

# If input file provided, read URLs from it
if [[ -n "$input_file" && -f "$input_file" ]]; then
  mapfile -t file_urls < "$input_file"
  urls+=("${file_urls[@]}")
fi

# Count the URLs
url_count=${#urls[@]}
if [[ $url_count -eq 0 ]]; then
  echo "🔄 Checking for yt-dlp updates..."
  update_output=$(pip install --upgrade yt-dlp 2>&1)

  if echo "$update_output" | grep -q "Successfully installed"; then
    echo "✅ yt-dlp upgraded!"
  elif echo "$update_output" | grep -q "Requirement already satisfied"; then
    echo "✅ yt-dlp already up to date."
  else
    echo "⚠️  Could not verify yt-dlp status. Check manually."
    echo "$update_output" | tail -n 3
  fi

  echo "🎉 Setup complete! Share any supported video link to Termux to download."
  exit 0
fi

echo "📥 Found $url_count URL(s) to process:"
# List them
for i in "${!urls[@]}"; do
  echo "  $((i+1))) ${urls[i]}"
done

for url in "${urls[@]}"; do
  echo "🔗 Processing: $url"
  # 🔄 Reset per-loop defaults
  playlist_flag="--no-playlist"
  format_flag="-f b"
  format_prompt=true
  output_dir="$save_dir"

  # Check URL validity
  if [[ ! "$url" =~ ^https?:// ]]; then
    echo "❌ Invalid URL: $url"
    continue
  fi

  # Optional: restrict to known yt-dlp supported domains
  # Extract domain using parameter expansion (optional but cleaner)
  domain=$(echo "$url" | awk -F/ '{print $3}' | sed 's/^www\.//')

  if [[ ! "$domain" =~ $domain_pattern ]]; then
    echo "⚠️  Probably unsupported URL: $url"
    # optional skip url or continue
  fi

  # ⚡ Initialize variables for platform-specific paths
  platform_dir="Other"
  # Use a basic regex to set a generic platform name
  if [[ "$url" =~ (youtu\.be|youtube\.com) ]]; then
    platform_dir="YouTube"
  elif [[ "$url" =~ soundcloud\.com ]]; then
    platform_dir="SoundCloud"
  elif [[ "$url" =~ vimeo\.com ]]; then
    platform_dir="Vimeo"
  elif [[ "$url" =~ tiktok\.com ]]; then
    platform_dir="TikTok"
  elif [[ "$url" =~ facebook\.com ]]; then
    platform_dir="Facebook"
  elif [[ "$url" =~ instagram\.com ]]; then
    platform_dir="Instagram"
  elif [[ "$url" =~ x\.com ]]; then
    platform_dir="X"
  fi

  # ⚡ Construct the save directory
  output_dir="$save_dir/$platform_dir"

  # 🧩 Detect short-form content → shorts/<Platform>/
  if echo "$url" | grep -Eiq 'shorts|/reel|share/r|tiktok\.com'; then
    output_dir="$save_dir/shorts/$platform_dir"

  else
    # ⭐️ REVISED: Playlist Detection and Flag Setting ⭐️
    # 1. Quick Check: Check if the URL contains common playlist identifiers
    # 2. Robust Check: Use yt-dlp only if the quick check failed,
    #    Or if we want to be absolutely sure for complex URLs.
    #    Exit status 0 means it IS a playlist. We redirect output to keep the terminal clean.

    # Only check for playlists on platforms that support them
    playlist_domains=("youtube.com" "youtu.be" "soundcloud.com" "vimeo.com")
    domain_supports_playlist=false
    for pd in "${playlist_domains[@]}"; do
      if [[ "$url" == *"$pd"* ]]; then
        domain_supports_playlist=true
        break
      fi
    done

    if [[ "$domain_supports_playlist" == "true" ]]; then
      if [[ "$url" =~ (list=|playlist) ]] || yt-dlp --flat-playlist --playlist-items 1 "$url" >/dev/null 2>&1; then
        is_playlist="true"
      fi
    fi

    # If it is a playlist, prompt the user for action
    # 🧩 Case 1: playlist_mode given as arg (interactive or non) >> Adjust flag

    # 🧩 Case 2: Interactive mode and playlist_mode is false >> Ask

    if [[ "$is_playlist" = true && "$playlist_mode" = true ]]; then
      playlist_flag=""
    elif [[ "$non_interactive" = false ]]; then
      echo -e "\n📂 Playlist Detected!"
      echo "1) 🎬 Download FULL PLAYLIST"
      echo "2) 🎵 Download FIRST FILE"
      read -rp "👉 Enter choice [1/2]: " playlist_choice

      # Handle the user's playlist choice (1=FULL, 2 or any =SINGLE)
      if [[ -z "$playlist_choice" ]]; then
        echo "❌ Cancelled by user"
        continue
      elif [[ "$playlist_choice" == "1" ]]; then
        playlist_flag=""  # Full playlist
        echo "✅ Full playlist download selected."
      else
        echo "✅ Only first file will be downloaded."
      fi
    fi

    if [ -z "$playlist_flag" ]; then
      # ⚡ Extract Playlist Title
      # We need to run yt-dlp in "simulate" mode to get the playlist title early.
      # Use yt-dlp with --flat-playlist to get metadata without downloading
      # --print "%(playlist_title)s", using --restrict-filenames to sanitize

      title=$(yt-dlp --flat-playlist --restrict-filenames \
        --print "%(playlist_title)s" --no-warnings \
        "$url" 2>/dev/null | head -n 1)

      # If the directory already exists, append a timestamp suffix
      if [ -d "$output_dir/$title" ]; then
        timestamp=$(date +"%Y%m%d_%H%M%S")
        title="${title}_${timestamp}"
      fi
      output_dir="$output_dir/$title"
    fi

    fmt="" # Initialize format variable
    # 🧩 Skip format prompt if non-interactive or format already passed manually
    if [[ "$non_interactive" = true || -n "$user_fmt" ]]; then
      format_prompt=false
    fi

    if [[ -n "$user_fmt" ]]; then
     fmt="$user_fmt"
    fi

    # 🧩 Skip also if certain domains
    for domain in "${skip_format_domains[@]}"; do
      if [[ "$url" == *"$domain"* ]]; then
        format_prompt=false
        break
      fi
    done

    if [[ "$format_prompt" = true ]]; then
      echo -e "\n🎥 Select Download Format:"
      echo "1) 🎧  Music (M4A)"
      echo "2) 🎵  Music (MP3)"
      echo "3) 💿  Video 360p (Low)"
      echo "4) 📹  Video 480p (SD)"
      echo "5) 🎬  Video 720p (HD)"
      echo "6) 🎬  Video 1080p (FHD)"
      echo "7) 🎬  Video 2160p (UHD)"
      echo "8) 🎬  Video Best Available"

      read -rp "👉 Enter choice [1-8]: " format_choice

      if [[ -z $format_choice ]]; then
        echo "❌ Cancelled by user"
        continue ;
      fi
      case $format_choice in
        1) fmt='bestaudio[ext=m4a]' ;;
        2) fmt="mp3" ;; # Special case for MP3
        3) fmt='best[height<=360]' ;;
        4) fmt='best[height<=480]' ;;
        5) fmt='best[height<=720]' ;;
        6) fmt='best[height<=1080]' ;;
        7) fmt='best[height<=2160]' ;;
        *) fmt='b' ;;
      esac

      format_flag="-f $fmt"
      # Special case for MP3
      if [ "$fmt" == "mp3" ]; then
        format_flag="-x --audio-format mp3"
      fi
    fi
  fi

  mkdir -p "$output_dir"
  outfile="$output_dir/%(title)s.%(ext)s"
  final_path=$(yt-dlp $playlist_flag $format_flag \
    --restrict-filenames --no-warnings -q --get-filename -o "$outfile" "$url")
  yt-dlp $playlist_flag $format_flag \
    --restrict-filenames --no-part --no-continue --no-cache-dir --trim-filenames 100 -o "$outfile" "$url"

  # Capture the exit status (0 means success, non-zero means failure)
  download_status=$?

  # 🩹 Fallback: Retry with 'best' if format not found
  if [ "$download_status" -ne 0 ]; then
    echo "⚠️  Requested format not found, retrying with best available..."
    yt-dlp $playlist_flag -f b \
      --restrict-filenames --no-part --no-continue --no-cache-dir --trim-filenames 100 -o "$outfile" "$url"
    download_status=$?
  fi

  # ⭐️ FINAL CHECK BLOCK (Uses download_status) ⭐️
  if [ "$download_status" -ne 0 ]; then
    echo "❌ Download failed for: $url"
    continue
  fi

  echo "✅ Download complete"
  echo "📂 Saved to: $output_dir"

  # Open the file after download (single files only, not playlists)
  if [[ -f "$final_path" ]]; then
    case "$PLATFORM" in
      termux)
        command -v termux-open &>/dev/null && termux-open "$final_path" ;;
      darwin)
        open "$final_path" ;;
      *)
        # Linux — try xdg-open, fall back to notify
        if command -v xdg-open &>/dev/null; then
          xdg-open "$final_path" &
        else
          echo "💡 File saved at: $final_path"
        fi ;;
    esac
  fi
done
