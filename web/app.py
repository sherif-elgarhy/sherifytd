import sys
import os
import subprocess
import webbrowser

from threading import Timer
from flask import Flask
from routes import bp

app = Flask(__name__)
app.register_blueprint(bp)


def setup_storage():
    storage_path = os.path.expanduser("~/storage/shared")
    if not os.path.exists(storage_path):
        print("⚠️  Storage not linked. Run 'termux-setup-storage' first.")
    ytd_path = os.path.expanduser("~/storage/shared/YTD")
    os.makedirs(ytd_path, exist_ok=True)


def check_for_updates():
    print("🔄 Checking for yt-dlp updates...")
    try:
        result = subprocess.run(
            [sys.executable, "-m", "pip", "install", "--upgrade", "yt-dlp"],
            capture_output=True,
            text=True,
        )
        if "Successfully installed" in result.stdout:
            print("✅ yt-dlp upgraded!")
        else:
            print("✅ yt-dlp already up to date.")
    except Exception as e:
        print(f"⚠️  Update check failed: {e}")


def open_browser(url):
    if "ANDROID_ROOT" in os.environ:
        os.system(f'am start -a android.intent.action.VIEW -d "{url}"')
    else:
        try:
            os.system(f'xdg-open "{url}"')
        except Exception:
            webbrowser.open(url)


if __name__ == "__main__":
    setup_storage()
    check_for_updates()

    port = 5000
    host = "127.0.0.1"

    # URL passed from termux-url-opener (Android share intent)
    auto_url = sys.argv[1] if len(sys.argv) > 1 else ""
    if auto_url:
        print(f"🚀 Launching with shared URL: {auto_url}")
        # Pass URL as query param — survives Flask restarts, no env var needed
        browser_url = f"http://{host}:{port}/?url={auto_url}"
    else:
        browser_url = f"http://{host}:{port}/"

    Timer(1.0, lambda: open_browser(browser_url)).start()

    app.run(
        host=host,
        port=port,
        debug=False,   # Never expose debugger
        use_reloader=False,  # Prevent double-start on Termux
    )

