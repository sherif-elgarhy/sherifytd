const log        = document.getElementById("log");
const actions    = document.getElementById("action-area");
const errorArea  = document.getElementById("error-area");
const badge      = document.getElementById("status-badge");
const statusText = document.getElementById("status-text");

// Read params safely from DOM data attributes
const dlParams   = document.getElementById("dl-params").dataset;
const dlUrl      = dlParams.url;
const dlFmt      = dlParams.fmt;
const dlPlaylist = dlParams.playlist;

const params = new URLSearchParams({ url: dlUrl, fmt: dlFmt, playlist: dlPlaylist });
const evt = new EventSource("/stream?" + params.toString());

evt.onmessage = (e) => {
    if (e.data === "__DONE__") {
        evt.close();
        finishUI();
        return;
    }
    if (e.data === "__ERROR__") {
        evt.close();
        errorUI();
        return;
    }
    log.textContent += e.data + "\n";
    log.scrollTop = log.scrollHeight;
};

evt.onerror = () => {
    evt.close();
    errorUI();
};

function finishUI() {
    badge.className = "badge bg-success";
    badge.innerText = "DONE";
    statusText.innerHTML = '<i class="fa-solid fa-check-circle text-success me-2"></i>Finished';
    actions.classList.remove("d-none");
}

function errorUI() {
    badge.className = "badge bg-danger";
    badge.innerText = "FAILED";
    statusText.innerHTML = '<i class="fa-solid fa-circle-xmark text-danger me-2"></i>Failed';
    errorArea.classList.remove("d-none");
}

function shutdown() {
    if (confirm("Stop SherifYTD?")) {
        fetch("/shutdown", { method: "POST" });
        window.close();
    }
}
