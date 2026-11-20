(function () {
  const { summaryPrompt, audioPrompt, qaPrompt } = window.SintesiPrompts;
  const { chunkText, excerptForQuestion } = window.SintesiChunker;

  const DEFAULT_MODEL = "gemini-1.5-flash";
  const FREE_TIER_MODELS = [
    "gemini-1.5-flash",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash-lite",
  ];

  const MIN_GAP_MS = 3500;
  let lastCallAt = 0;

  function buildModelChain(preferred) {
    const chain = [];
    if (preferred?.trim()) chain.push(preferred.trim());
    for (const m of FREE_TIER_MODELS) {
      if (!chain.includes(m)) chain.push(m);
    }
    return chain;
  }

  function sleep(ms) {
    return new Promise((r) => setTimeout(r, ms));
  }

  async function pace() {
    const wait = MIN_GAP_MS - (Date.now() - lastCallAt);
    if (wait > 0) await sleep(wait);
    lastCallAt = Date.now();
  }

  function parseRetrySeconds(msg) {
    const m = msg?.match(/retry in ([\d.]+)s/i);
    return m ? parseFloat(m[1]) : null;
  }

  function isRetryable(status, msg) {
    if (status === 429 || status === 503 || status === 404) return true;
    const l = (msg || "").toLowerCase();
    return (
      l.includes("quota") ||
      l.includes("429") ||
      l.includes("rate") ||
      l.includes("resource exhausted") ||
      l.includes("not found") ||
      l.includes("limit: 0")
    );
  }

  function parseResponseText(data) {
    const parts = data?.candidates?.[0]?.content?.parts;
    if (!parts?.length) throw new Error("Risposta vuota da Gemini.");
    const text = parts.map((p) => p.text || "").join("").trim();
    if (!text) throw new Error("Nessun testo nella risposta.");
    return text;
  }

  async function callGeminiRest(apiKey, prompt, model) {
    await pace();
    const url =
      "https://generativelanguage.googleapis.com/v1beta/models/" +
      model +
      ":generateContent?key=" +
      encodeURIComponent(apiKey);
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.4, maxOutputTokens: 8192 },
      }),
    });

    let data = {};
    try {
      data = await res.json();
    } catch (_) {}

    if (!res.ok) {
      const msg = data?.error?.message || res.statusText || "HTTP " + res.status;
      const err = new Error(msg);
      err.status = res.status;
      throw err;
    }
    return parseResponseText(data);
  }

  async function generate(apiKey, prompt, preferredModel = DEFAULT_MODEL) {
    const models = buildModelChain(preferredModel);
    const errors = [];

    for (const model of models) {
      try {
        return await callGeminiRest(apiKey, prompt, model);
      } catch (e) {
        const msg = e.message || String(e);
        errors.push(model + ": " + msg.slice(0, 120));
        if (!isRetryable(e.status, msg)) throw new Error(msg);
        const sec = parseRetrySeconds(msg);
        if (sec) await sleep(Math.min(sec * 1000, 90000));
      }
    }

    throw new Error(
      "Quota API Gemini esaurita. Attendi 2-5 minuti o cambia modello in Impostazioni.\n" +
        errors.slice(0, 3).join("\n")
    );
  }

  async function buildFullSummary(apiKey, fullText, preferredModel, onProgress) {
    const chunks = chunkText(fullText);
    if (chunks.length === 1) {
      return generate(
        apiKey,
        summaryPrompt(chunks[0], "documento completo", false),
        preferredModel
      );
    }
    const partials = [];
    for (let i = 0; i < chunks.length; i++) {
      onProgress?.("Sintesi parte " + (i + 1) + " di " + chunks.length + "…");
      partials.push(
        await generate(
          apiKey,
          summaryPrompt(chunks[i], "parte " + (i + 1) + " di " + chunks.length, false),
          preferredModel
        )
      );
    }
    onProgress?.("Unione finale…");
    const merged = partials.map((s, i) => "### Parte " + (i + 1) + "\n" + s).join("\n\n");
    return generate(apiKey, summaryPrompt(merged, "finale", true), preferredModel);
  }

  async function buildAudioScript(apiKey, title, summary, fullText, preferredModel) {
    const base = summary?.trim() ? summary : fullText.slice(0, 60_000);
    return generate(apiKey, audioPrompt(base, title), preferredModel);
  }

  async function answerQuestion(apiKey, question, fullText, preferredModel) {
    const excerpt = excerptForQuestion(fullText, question);
    return generate(apiKey, qaPrompt(question, excerpt), preferredModel);
  }

  window.SintesiGemini = {
    DEFAULT_MODEL,
    FREE_TIER_MODELS,
    generate,
    buildFullSummary,
    buildAudioScript,
    answerQuestion,
  };
})();
