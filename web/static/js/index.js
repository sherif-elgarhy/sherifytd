const urlInput     = document.getElementById('url-input');
const urlIcon      = document.getElementById('url-icon');
const formatSelect = document.getElementById('format-select');
const hint         = document.getElementById('quality-hint');

function updateUrlUI() {
    const val = urlInput.value.toLowerCase();
    const isYT = val.includes('youtube.com') || val.includes('youtu.be');

    urlIcon.className = isYT
        ? "fa-brands fa-youtube text-danger"
        : val.length > 0
            ? "fa-solid fa-link text-primary"
            : "fa-solid fa-globe text-muted";

    if (val.length > 0 && !isYT) {
        formatSelect.value = "best";
        formatSelect.disabled = true;
        hint.classList.remove('d-none');
    } else {
        formatSelect.disabled = false;
        hint.classList.add('d-none');
    }
}

urlInput.addEventListener('input', updateUrlUI);

// Trigger on load if auto_url was pre-filled
if (urlInput.value) updateUrlUI();

async function pasteUrl() {
    try {
        const text = await navigator.clipboard.readText();
        urlInput.value = text;
        urlInput.dispatchEvent(new Event('input'));
    } catch (err) { console.error(err); }
}

document.getElementById('dl-form').onsubmit = () => {
    document.getElementById('submit-btn').disabled = true;
    formatSelect.disabled = false; // ensure value is submitted
    document.getElementById('btn-text').innerHTML =
        '<i class="fa-solid fa-spinner fa-spin me-2"></i>PROCESSING...';
};

