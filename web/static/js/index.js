const urlInput     = document.getElementById('url-input');
const urlIcon      = document.getElementById('url-icon');
const formatSelect = document.getElementById('format-select');

function updateUrlUI() {
    const val = urlInput.value.toLowerCase();
    const isYT = val.includes('youtube.com') || val.includes('youtu.be');

    urlIcon.className = isYT
        ? "fa-brands fa-youtube text-danger"
        : val.length > 0
            ? "fa-solid fa-link text-primary"
            : "fa-solid fa-globe text-muted";
}

urlInput.addEventListener('input', updateUrlUI);

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
    document.getElementById('btn-text').innerHTML =
        '<i class="fa-solid fa-spinner fa-spin me-2"></i>PROCESSING...';
};

