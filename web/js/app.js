const parseFile = window.parseFile;
const db = window.SintesiDb;
const { DEFAULT_MODEL, buildFullSummary, buildAudioScript, answerQuestion } =
  window.SintesiGemini;

const $ = (sel) => document.querySelector(sel);

let currentDocId = null;
let speaking = false;

function settings() {
  return {
    apiKey: localStorage.getItem("gemini_api_key") || "",
    model: localStorage.getItem("gemini_model") || DEFAULT_MODEL,
  };
}

function saveSettings(apiKey, model) {
  localStorage.setItem("gemini_api_key", apiKey.trim());
  localStorage.setItem("gemini_model", model);
}

function toast(msg, ms = 4000) {
  const el = $("#toast");
  el.textContent = msg;
  el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), ms);
}

function requireApiKey() {
  const { apiKey } = settings();
  if (!apiKey) {
    throw new Error("Inserisci la API key Gemini in Impostazioni.");
  }
  return apiKey;
}

function showView(name) {
  $("#view-home").classList.toggle("hidden", name !== "home");
  $("#view-doc").classList.toggle("hidden", name !== "doc");
  $("#view-settings").classList.toggle("hidden", name !== "settings");
}

async function renderHome() {
  const list = await db.listDocuments();
  const container = $("#doc-list");
  if (!list.length) {
    container.innerHTML =
      '<p class="meta">Importa PDF, EPUB, iPUB o TXT per sintesi, podcast TTS e Q&A.</p>';
    return;
  }
  container.innerHTML = list
    .map(
      (d) => `
    <div class="card" data-id="${d.id}">
      <h3>${escapeHtml(d.title)}</h3>
      <p class="meta">${d.format?.toUpperCase()} · ${d.charCount} caratteri</p>
      ${d.summary ? '<p class="meta">Sintesi pronta</p>' : ""}
      <button class="secondary delete-btn" data-id="${d.id}" style="margin-top:8px">Elimina</button>
    </div>`
    )
    .join("");

  container.querySelectorAll(".card[data-id]").forEach((card) => {
    card.addEventListener("click", (e) => {
      if (e.target.classList.contains("delete-btn")) return;
      openDoc(card.dataset.id);
    });
  });
  container.querySelectorAll(".delete-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      e.stopPropagation();
      await db.deleteDocument(btn.dataset.id);
      await renderHome();
    });
  });
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

async function openDoc(id) {
  currentDocId = Number(id);
  const doc = await db.getDocument(currentDocId);
  if (!doc) return;
  $("#doc-title").textContent = doc.title;
  const warn = doc.parseWarnings?.length ? doc.parseWarnings.join("\n") : "";
  $("#doc-warnings").textContent = warn ? `Avvisi: ${warn}` : "";
  $("#doc-warnings").classList.toggle("hidden", !warn);

  $("#summary-out").textContent = doc.summary || "";
  $("#audio-out").textContent = doc.audioScript || "";
  await renderChat();
  showView("doc");
  setTab("summary");
}

async function renderChat() {
  const msgs = await db.listMessages(currentDocId);
  const box = $("#chat-box");
  if (!msgs.length) {
    box.innerHTML = '<p class="meta">Domande basate sul testo completo in memoria.</p>';
    return;
  }
  box.innerHTML = msgs
    .map(
      (m) =>
        `<div class="msg ${m.role}"><strong>${m.role === "user" ? "Tu" : "Assistente"}:</strong> ${escapeHtml(m.content)}</div>`
    )
    .join("");
  box.scrollTop = box.scrollHeight;
}

function setTab(tab) {
  document.querySelectorAll(".tab").forEach((t) => {
    t.classList.toggle("active", t.dataset.tab === tab);
  });
  document.querySelectorAll(".panel").forEach((p) => {
    p.classList.toggle("active", p.id === `panel-${tab}`);
  });
}

async function importFile(file) {
  toast("Importazione in corso…");
  const parsed = await parseFile(file);
  const id = await db.saveDocument({
    title: parsed.title,
    format: parsed.format,
    fullText: parsed.text,
    charCount: parsed.charCount,
    parseWarnings: parsed.warnings,
    summary: null,
    audioScript: null,
  });
  toast("Documento importato");
  await renderHome();
  openDoc(id);
}

async function runAi(fn) {
  $("#loading").classList.remove("hidden");
  try {
    const apiKey = requireApiKey();
    const { model } = settings();
    const doc = await db.getDocument(currentDocId);
    await fn(apiKey, doc, model);
    await openDoc(currentDocId);
  } catch (e) {
    toast(e.message || String(e), 10000);
  } finally {
    $("#loading").classList.add("hidden");
    $("#loading").textContent = "Elaborazione in corso…";
  }
}

function speak(text) {
  if (!("speechSynthesis" in window)) {
    toast("Sintesi vocale non supportata su questo browser.");
    return;
  }
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = "it-IT";
  u.rate = 1;
  speaking = true;
  u.onend = () => (speaking = false);
  window.speechSynthesis.speak(u);
}

function init() {
  $("#btn-settings").addEventListener("click", () => {
    const s = settings();
    $("#api-key").value = s.apiKey;
    $("#model-select").value = s.model;
    showView("settings");
  });

  $("#btn-back-home").addEventListener("click", () => {
    showView("home");
    renderHome();
  });
  $("#btn-back-settings").addEventListener("click", () => {
    showView("home");
    renderHome();
  });
  $("#btn-save-settings").addEventListener("click", () => {
    saveSettings($("#api-key").value, $("#model-select").value);
    toast("Impostazioni salvate");
    showView("home");
    renderHome();
  });

  $("#fab-import").addEventListener("click", () => $("#file-input").click());
  $("#file-input").addEventListener("change", async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (file) {
      try {
        await importFile(file);
      } catch (err) {
        toast(err.message || String(err), 6000);
      }
    }
  });

  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => setTab(tab.dataset.tab));
  });

  $("#btn-summary").addEventListener("click", () =>
    runAi(async (apiKey, doc, model) => {
      const summary = await buildFullSummary(apiKey, doc.fullText, model, (msg) => {
        $("#loading").textContent = msg;
      });
      doc.summary = summary;
      await db.saveDocument(doc);
      toast("Sintesi completata");
    })
  );

  $("#btn-audio").addEventListener("click", () =>
    runAi(async (apiKey, doc, model) => {
      const script = await buildAudioScript(
        apiKey,
        doc.title,
        doc.summary || "",
        doc.fullText,
        model
      );
      doc.audioScript = script;
      await db.saveDocument(doc);
      toast("Script podcast pronto");
    })
  );

  $("#btn-play").addEventListener("click", async () => {
    const doc = await db.getDocument(currentDocId);
    if (doc?.audioScript) speak(doc.audioScript);
    else toast("Genera prima lo script podcast");
  });

  $("#btn-stop").addEventListener("click", () => {
    window.speechSynthesis?.cancel();
    speaking = false;
  });

  $("#btn-ask").addEventListener("click", async () => {
    const q = $("#question").value.trim();
    if (!q) return;
    $("#question").value = "";
    $("#loading").classList.remove("hidden");
    try {
      const apiKey = requireApiKey();
      const { model } = settings();
      const doc = await db.getDocument(currentDocId);
      await db.addMessage(currentDocId, "user", q);
      const answer = await answerQuestion(apiKey, q, doc.fullText, model);
      await db.addMessage(currentDocId, "assistant", answer);
      await renderChat();
    } catch (e) {
      toast(e.message || String(e), 8000);
    } finally {
      $("#loading").classList.add("hidden");
    }
  });

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("./sw.js").catch(() => {});
  }

  showView("home");
  renderHome();
}

init();
