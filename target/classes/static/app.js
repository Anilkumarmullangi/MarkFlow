(() => {
  "use strict";

  const API_CONVERT = "/api/convert";
  const API_HEALTH = "/api/health";
  const SAMPLE_URL = "sample.md";
  const DEBOUNCE_MS = 300;
  const MAX_MARKDOWN_LENGTH = 200000;
  const THEME_KEY = "mdpro-theme";

  const EXPORT_STYLES = [
    "body{font-family:system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif;line-height:1.65;color:#1b2033;max-width:820px;margin:40px auto;padding:0 20px;}",
    "h1,h2,h3,h4,h5,h6{line-height:1.25;margin:1.5em 0 .6em;}",
    "h1,h2{border-bottom:1px solid #d9deef;padding-bottom:.3em;}",
    "a{color:#3730a3;}",
    "code{background:#eef0fa;padding:.15em .4em;border-radius:5px;font-family:Consolas,'Liberation Mono',Menlo,monospace;font-size:.9em;}",
    "pre{background:#f3f5fc;border:1px solid #d9deef;border-radius:8px;padding:14px 16px;overflow:auto;}",
    "pre code{background:none;padding:0;}",
    "blockquote{margin:0 0 1em;padding:.2em 1em;border-left:4px solid #4f46e5;background:#f3f5fc;color:#4a5578;}",
    "table{border-collapse:collapse;margin:0 0 1em;}",
    "th,td{border:1px solid #ccd3ea;padding:8px 12px;}",
    "th{background:#f0f2fb;}",
    "img{max-width:100%;height:auto;}",
    "hr{border:0;border-top:1px solid #d9deef;margin:1.8em 0;}"
  ].join("\n");

  const el = {
    html: document.documentElement,
    editor: document.getElementById("editor"),
    preview: document.getElementById("preview"),
    source: document.getElementById("source"),
    emptyState: document.getElementById("empty-state"),
    panelPreview: document.getElementById("panel-preview"),
    panelSource: document.getElementById("panel-source"),
    tabPreview: document.getElementById("tab-preview"),
    tabSource: document.getElementById("tab-source"),
    btnExample: document.getElementById("btn-example"),
    btnEmptyExample: document.getElementById("btn-empty-example"),
    btnClear: document.getElementById("btn-clear"),
    btnViewSource: document.getElementById("btn-view-source"),
    btnCopy: document.getElementById("btn-copy"),
    btnDownload: document.getElementById("btn-download"),
    btnRetry: document.getElementById("btn-retry"),
    btnDismissError: document.getElementById("btn-dismiss-error"),
    themeToggle: document.getElementById("theme-toggle"),
    errorBanner: document.getElementById("error-banner"),
    errorMessage: document.getElementById("error-message"),
    convertState: document.getElementById("convert-state"),
    convertStateText: document.getElementById("convert-state-text"),
    backendStatus: document.getElementById("backend-status"),
    backendStatusText: document.getElementById("backend-status-text"),
    charCount: document.getElementById("char-count"),
    wordCount: document.getElementById("word-count"),
    htmlSize: document.getElementById("html-size"),
    toast: document.getElementById("toast"),
    dialog: document.getElementById("confirm-dialog"),
    dialogTitle: document.getElementById("confirm-title"),
    dialogMessage: document.getElementById("confirm-message"),
    dialogAccept: document.getElementById("confirm-accept")
  };

  const state = {
    html: "",
    activeTab: "preview",
    requestId: 0,
    abortController: null,
    debounceTimer: null,
    toastTimer: null
  };

  class ApiError extends Error {
    constructor(message, status) {
      super(message);
      this.name = "ApiError";
      this.status = status;
    }
  }

  function formatNumber(value) {
    return value.toLocaleString("en-US");
  }

  function pluralize(count, singular, plural) {
    return formatNumber(count) + " " + (count === 1 ? singular : plural);
  }

  function countWords(text) {
    const trimmed = text.trim();
    return trimmed ? trimmed.split(/\s+/).length : 0;
  }

  function escapeHtml(value) {
    return value
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function safeStorageGet(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }

  function safeStorageSet(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (error) {
      return;
    }
  }

  function updateStats() {
    const text = el.editor.value;
    el.charCount.textContent = pluralize(text.length, "character", "characters");
    el.wordCount.textContent = pluralize(countWords(text), "word", "words");
    el.htmlSize.textContent = "HTML: " + pluralize(state.html.length, "character", "characters");
  }

  function showToast(message) {
    el.toast.textContent = message;
    window.clearTimeout(state.toastTimer);
    state.toastTimer = window.setTimeout(() => {
      el.toast.textContent = "";
    }, 2800);
  }

  function showError(message) {
    el.errorMessage.textContent = message;
    el.errorBanner.hidden = false;
  }

  function hideError() {
    el.errorBanner.hidden = true;
    el.errorMessage.textContent = "";
  }

  function setConvertState(name, text) {
    el.convertState.dataset.state = name;
    el.convertStateText.textContent = text;
  }

  function setBackendStatus(online) {
    el.backendStatus.dataset.state = online ? "online" : "offline";
    el.backendStatusText.textContent = online ? "Server online" : "Server unreachable";
  }

  function updateButtons() {
    const hasHtml = state.html.length > 0;
    el.btnCopy.disabled = !hasHtml;
    el.btnDownload.disabled = !hasHtml;
    el.btnViewSource.disabled = !hasHtml;
  }

  function enhanceLinks(root) {
    root.querySelectorAll("a[href]").forEach((anchor) => {
      anchor.target = "_blank";
      anchor.rel = "noopener noreferrer nofollow";
    });
  }

  function render() {
    const hasHtml = state.html.length > 0;
    if (hasHtml) {
      el.preview.innerHTML = state.html;
      enhanceLinks(el.preview);
    } else {
      el.preview.innerHTML = "";
    }
    el.preview.hidden = !hasHtml;
    el.emptyState.hidden = hasHtml;
    el.source.textContent = state.html;
    updateButtons();
    updateStats();
    if (!hasHtml && state.activeTab === "source") {
      selectTab("preview");
    }
  }

  function selectTab(name, moveFocus) {
    state.activeTab = name;
    const showSource = name === "source";
    el.tabPreview.setAttribute("aria-selected", String(!showSource));
    el.tabSource.setAttribute("aria-selected", String(showSource));
    el.tabPreview.tabIndex = showSource ? -1 : 0;
    el.tabSource.tabIndex = showSource ? 0 : -1;
    el.panelPreview.hidden = showSource;
    el.panelSource.hidden = !showSource;
    el.btnViewSource.textContent = showSource ? "View preview" : "View HTML source";
    if (moveFocus) {
      (showSource ? el.tabSource : el.tabPreview).focus();
    }
  }

  async function readJson(response) {
    try {
      return await response.json();
    } catch (error) {
      return null;
    }
  }

  function cancelPending() {
    window.clearTimeout(state.debounceTimer);
    if (state.abortController) {
      state.abortController.abort();
      state.abortController = null;
    }
    state.requestId += 1;
  }

  async function convertNow() {
    const markdown = el.editor.value;

    if (!markdown.trim()) {
      cancelPending();
      state.html = "";
      hideError();
      render();
      setConvertState("idle", "Ready");
      return;
    }

    if (markdown.length > MAX_MARKDOWN_LENGTH) {
      cancelPending();
      showError("The Markdown is longer than the " + formatNumber(MAX_MARKDOWN_LENGTH) + " character limit. Please shorten it.");
      setConvertState("error", "Too long");
      return;
    }

    if (state.abortController) {
      state.abortController.abort();
    }
    const controller = new AbortController();
    state.abortController = controller;
    state.requestId += 1;
    const requestId = state.requestId;
    setConvertState("loading", "Converting…");

    try {
      const response = await fetch(API_CONVERT, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify({ markdown }),
        signal: controller.signal
      });
      const data = await readJson(response);
      if (requestId !== state.requestId) {
        return;
      }
      if (!response.ok) {
        const message = data && data.message ? data.message : "The server responded with status " + response.status + ".";
        throw new ApiError(message, response.status);
      }
      state.html = data && typeof data.html === "string" ? data.html : "";
      hideError();
      render();
      setBackendStatus(true);
      setConvertState("ready", "Up to date");
    } catch (error) {
      if (error.name === "AbortError" || requestId !== state.requestId) {
        return;
      }
      if (error instanceof ApiError) {
        showError("Conversion failed: " + error.message);
        setBackendStatus(true);
      } else {
        showError("Cannot reach the conversion server. Make sure the Spring Boot application is running, then press Retry.");
        setBackendStatus(false);
      }
      setConvertState("error", "Conversion failed");
    }
  }

  function scheduleConvert() {
    updateStats();
    window.clearTimeout(state.debounceTimer);
    if (!el.editor.value.trim()) {
      convertNow();
      return;
    }
    setConvertState("loading", "Typing…");
    state.debounceTimer = window.setTimeout(convertNow, DEBOUNCE_MS);
  }

  async function checkHealth() {
    try {
      const response = await fetch(API_HEALTH, { headers: { "Accept": "application/json" } });
      const data = await readJson(response);
      setBackendStatus(response.ok && data !== null && data.status === "UP");
    } catch (error) {
      setBackendStatus(false);
    }
  }

  function confirmAction(options) {
    if (typeof el.dialog.showModal !== "function") {
      return Promise.resolve(window.confirm(options.message));
    }
    el.dialogTitle.textContent = options.title;
    el.dialogMessage.textContent = options.message;
    el.dialogAccept.textContent = options.confirmLabel;
    el.dialog.returnValue = "";
    return new Promise((resolve) => {
      el.dialog.addEventListener("close", () => resolve(el.dialog.returnValue === "confirm"), { once: true });
      el.dialog.showModal();
    });
  }

  async function clearEditor() {
    if (el.editor.value.length > 0) {
      const confirmed = await confirmAction({
        title: "Clear the editor?",
        message: "This removes all of your Markdown and cannot be undone.",
        confirmLabel: "Clear"
      });
      if (!confirmed) {
        return;
      }
    }
    el.editor.value = "";
    await convertNow();
    el.editor.focus();
    showToast("Editor cleared");
  }

  async function loadExample() {
    let sample;
    try {
      const response = await fetch(SAMPLE_URL, { cache: "no-cache" });
      if (!response.ok) {
        throw new Error("HTTP " + response.status);
      }
      sample = await response.text();
    } catch (error) {
      showError("Could not load the example document. Make sure the application is running, then try again.");
      return;
    }

    const current = el.editor.value;
    if (current.trim().length > 0 && current !== sample) {
      const confirmed = await confirmAction({
        title: "Replace your Markdown?",
        message: "Loading the example will replace what is currently in the editor.",
        confirmLabel: "Replace"
      });
      if (!confirmed) {
        return;
      }
    }
    el.editor.value = sample;
    await convertNow();
    el.editor.focus();
    el.editor.setSelectionRange(0, 0);
    showToast("Example loaded");
  }

  async function writeToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const helper = document.createElement("textarea");
    helper.value = text;
    helper.setAttribute("readonly", "");
    helper.style.position = "fixed";
    helper.style.opacity = "0";
    document.body.appendChild(helper);
    helper.select();
    const copied = document.execCommand("copy");
    document.body.removeChild(helper);
    if (!copied) {
      throw new Error("Copy command was rejected");
    }
  }

  async function copyHtml() {
    if (!state.html) {
      return;
    }
    try {
      await writeToClipboard(state.html);
      showToast("HTML copied to clipboard");
    } catch (error) {
      showToast("Copy failed. Select the text in the HTML source view instead.");
    }
  }

  function buildDocumentTitle() {
    const heading = el.preview.querySelector("h1, h2, h3");
    const text = heading ? heading.textContent.trim() : "";
    return text || "Markdown export";
  }

  function buildHtmlDocument() {
    return [
      "<!DOCTYPE html>",
      "<html lang=\"en\">",
      "<head>",
      "<meta charset=\"utf-8\">",
      "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">",
      "<title>" + escapeHtml(buildDocumentTitle()) + "</title>",
      "<style>",
      EXPORT_STYLES,
      "</style>",
      "</head>",
      "<body>",
      state.html,
      "</body>",
      "</html>",
      ""
    ].join("\n");
  }

  function downloadHtml() {
    if (!state.html) {
      return;
    }
    const blob = new Blob([buildHtmlDocument()], { type: "text/html;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "markdown-export.html";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    showToast("Download started");
  }

  function applyTheme(theme) {
    el.html.dataset.theme = theme;
    const isLight = theme === "light";
    el.themeToggle.setAttribute("aria-pressed", String(isLight));
    el.themeToggle.setAttribute("aria-label", isLight ? "Switch to dark theme" : "Switch to light theme");
  }

  function toggleTheme() {
    const next = el.html.dataset.theme === "light" ? "dark" : "light";
    applyTheme(next);
    safeStorageSet(THEME_KEY, next);
  }

  function handleTabKeys(event) {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight" && event.key !== "Home" && event.key !== "End") {
      return;
    }
    if (el.btnViewSource.disabled) {
      return;
    }
    event.preventDefault();
    const goToSource = event.key === "ArrowRight" || event.key === "End";
    selectTab(goToSource ? "source" : "preview", true);
  }

  function toggleSourceView() {
    selectTab(state.activeTab === "source" ? "preview" : "source");
  }

  function retry() {
    hideError();
    checkHealth();
    convertNow();
  }

  function init() {
    const storedTheme = safeStorageGet(THEME_KEY);
    applyTheme(storedTheme === "light" ? "light" : "dark");

    el.editor.addEventListener("input", scheduleConvert);
    el.btnExample.addEventListener("click", loadExample);
    el.btnEmptyExample.addEventListener("click", loadExample);
    el.btnClear.addEventListener("click", clearEditor);
    el.btnCopy.addEventListener("click", copyHtml);
    el.btnDownload.addEventListener("click", downloadHtml);
    el.btnViewSource.addEventListener("click", toggleSourceView);
    el.btnRetry.addEventListener("click", retry);
    el.btnDismissError.addEventListener("click", hideError);
    el.themeToggle.addEventListener("click", toggleTheme);
    el.tabPreview.addEventListener("click", () => selectTab("preview"));
    el.tabSource.addEventListener("click", () => {
      if (!el.btnViewSource.disabled) {
        selectTab("source");
      }
    });
    el.tabPreview.addEventListener("keydown", handleTabKeys);
    el.tabSource.addEventListener("keydown", handleTabKeys);

    selectTab("preview");
    render();
    setConvertState("idle", "Ready");
    checkHealth();
    if (el.editor.value.trim()) {
      convertNow();
    }
  }

  init();
})();
