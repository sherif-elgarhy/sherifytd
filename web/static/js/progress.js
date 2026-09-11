const log        = document.getElementById("log");
const actions    = document.getElementById("action-area");
const cancelArea = document.getElementById("cancel-area");
const errorArea  = document.getElementById("error-area");
const badge      = document.getElementById("status-badge");
const statusText = document.getElementById("status-text");

const dlParams = document.getElementById("dl-params").dataset;

const params = new URLSearchParams({
    url:       dlParams.url,
    fmt:       dlParams.fmt,
    playlist:  dlParams.playlist,
    subs:      dlParams.subs,
    sb:        dlParams.sb,
    archive:   dlParams.archive,
    sub_langs: dlParams.subLangs,
});
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
    cancelArea.classList.add("d-none");
    badge.className = "badge bg-success";
    badge.innerText = "DONE";
    statusText.innerHTML = '<i class="fa-solid fa-check-circle text-success me-2"></i>Finished';
    actions.classList.remove("d-none");
}

function errorUI() {
    cancelArea.classList.add("d-none");
    badge.className = "badge bg-danger";
    badge.innerText = "FAILED";
    statusText.innerHTML = '<i class="fa-solid fa-circle-xmark text-danger me-2"></i>Failed';
    errorArea.classList.remove("d-none");
}

function cancelDownload() {
    evt.close();
    fetch("/cancel", { method: "POST" });
    cancelArea.classList.add("d-none");
    badge.className = "badge bg-warning text-dark";
    badge.innerText = "CANCELLED";
    statusText.innerHTML = '<i class="fa-solid fa-ban text-warning me-2"></i>Cancelled';
    errorArea.classList.remove("d-none");
}

function shutdown() {
    if (confirm("Stop SherifYTD?")) {
        fetch("/shutdown", { method: "POST" });
        window.close();
    }
}
