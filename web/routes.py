import os
import threading
from flask import Blueprint, render_template, request, Response, jsonify, redirect, url_for
from downloader import download_stream

bp = Blueprint("main", __name__)

# Thread-local last file tracking — keyed by download session
_last_file_lock = threading.Lock()
_last_file = None


def set_last_file(path):
    global _last_file
    with _last_file_lock:
        _last_file = path


def get_last_file():
    with _last_file_lock:
        return _last_file


@bp.route("/", methods=["GET"])
def index():
    auto_url = request.args.get("url", "")
    return render_template("index.html", auto_url=auto_url)


@bp.route("/start", methods=["POST"])
def start():
    url = request.form["url"]
    fmt = request.form.get("format", "best")
    playlist = 1 if request.form.get("playlist") else 0
    return redirect(url_for("main.progress", url=url, fmt=fmt, playlist=playlist))


@bp.route("/progress")
def progress():
    url = request.args["url"]
    fmt = request.args["fmt"]
    playlist = bool(int(request.args.get("playlist", 0)))
    return render_template("progress.html", url=url, fmt=fmt, playlist=playlist)


@bp.route("/stream")
def stream():
    url = request.args["url"]
    fmt = request.args["fmt"]
    playlist = bool(int(request.args.get("playlist", 0)))

    proc = download_stream(url, fmt, playlist)

    def generate():
        for line in proc.stdout:
            line = line.strip()
            if not line:
                continue
            if os.path.exists(line):
                set_last_file(line)
                yield "data:__DONE__\n\n"
                proc.wait()
                return
            yield f"data:{line}\n\n"

        proc.wait()
        # If process exited without a filepath line
        if proc.returncode != 0:
            yield "data:__ERROR__\n\n"
        else:
            yield "data:__DONE__\n\n"

    return Response(generate(), mimetype="text/event-stream")


@bp.route("/open-file")
def open_file():
    """Serve the downloaded file directly so the browser can open/play it."""
    from flask import send_file
    last = get_last_file()
    if last and os.path.exists(last):
        return send_file(last, as_attachment=False)
    return jsonify(success=False, error="File not found"), 404


@bp.route("/shutdown", methods=["POST"])
def shutdown():
    # Graceful: let the request finish before exiting
    def _stop():
        import time, os
        time.sleep(0.3)
        os._exit(0)
    threading.Thread(target=_stop, daemon=True).start()
    return jsonify(success=True)
