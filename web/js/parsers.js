function normalizeText(raw, warnings) {
  let text = raw.replace(/\u00a0/g, " ");
  const bad = (text.match(/\ufffd/g) || []).length;
  if (bad > 0) warnings.push(`Rilevati ${bad} caratteri non decodificabili.`);
  text = text.replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\ufffd]/g, " ");
  text = text.replace(/[ \t]+/g, " ").replace(/\n{3,}/g, "\n\n").trim();
  return text;
}

/** Estrazione testo da XHTML/HTML — funziona anche con namespace EPUB. */
function htmlToPlain(html) {
  if (!html || html.length < 10) return "";

  let t = html
    .replace(/<\?xml[\s\S]*?\?>/gi, "")
    .replace(/<!DOCTYPE[\s\S]*?>/gi, "")
    .replace(/<script[\s\S]*?<\/script>/gi, "")
    .replace(/<style[\s\S]*?<\/style>/gi, "");

  const bodyMatch = t.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
  if (bodyMatch) t = bodyMatch[1];

  t = t
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/p>/gi, "\n")
    .replace(/<\/div>/gi, "\n")
    .replace(/<\/h[1-6]>/gi, "\n\n")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")
    .replace(/&quot;/gi, '"')
    .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(Number(n)))
    .replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCharCode(parseInt(h, 16)))
    .replace(/&[a-z]+;/gi, " ");

  let plain = t.replace(/\s+/g, " ").trim();

  if (plain.length < 20) {
    try {
      const doc = new DOMParser().parseFromString(html, "text/html");
      plain = (doc.body?.textContent || doc.documentElement?.textContent || "")
        .replace(/\s+/g, " ")
        .trim();
    } catch (_) {}
  }
  return plain;
}

function decodeBytes(bytes) {
  const encodings = ["utf-8", "iso-8859-1", "windows-1252"];
  let best = "";
  let bestScore = -1;
  for (const enc of encodings) {
    try {
      const decoded = new TextDecoder(enc, { fatal: false }).decode(bytes);
      const score = [...decoded].filter((c) => /\p{L}|\p{N}|\s/u.test(c)).length;
      if (score > bestScore) {
        bestScore = score;
        best = decoded;
      }
    } catch (_) {}
  }
  return best;
}

function normalizeZipPath(p) {
  return decodeURIComponentSafe(p.replace(/\\/g, "/").replace(/^\/+/, ""));
}

function decodeURIComponentSafe(s) {
  try {
    return decodeURIComponent(s);
  } catch (_) {
    return s;
  }
}

function resolveHref(baseDir, href) {
  href = decodeURIComponentSafe(href);
  const stack = baseDir ? baseDir.split("/").filter(Boolean) : [];
  for (const part of href.split("/")) {
    if (!part || part === ".") continue;
    if (part === "..") stack.pop();
    else stack.push(part);
  }
  return stack.join("/");
}

function buildZipIndex(zip) {
  const index = new Map();
  for (const path of Object.keys(zip.files)) {
    if (zip.files[path].dir) continue;
    const norm = normalizeZipPath(path);
    index.set(norm, path);
    index.set(norm.toLowerCase(), path);
    const base = norm.split("/").pop();
    if (base) index.set(base.toLowerCase(), path);
  }
  return index;
}

function getZipFile(zip, index, logicalPath) {
  const norm = normalizeZipPath(logicalPath);
  let key = index.get(norm) || index.get(norm.toLowerCase());
  if (key) return zip.file(key);

  const base = norm.split("/").pop()?.toLowerCase();
  if (!base) return null;
  for (const [k, v] of index.entries()) {
    if (k === base || k.endsWith("/" + base)) return zip.file(v);
  }
  return null;
}

const TEXT_MEDIA =
  /application\/(xhtml\+xml|html\+xml|xml)|text\/(html|xml)/i;

function isTextResource(mediaType, href) {
  if (mediaType && TEXT_MEDIA.test(mediaType)) return true;
  return /\.(xhtml|html|htm|xht)$/i.test(href || "");
}

function stripXmlNs(xml) {
  return xml.replace(/xmlns(:\w+)?="[^"]*"/gi, "").replace(/<(\/?)(\w+):/g, "<$1");
}

function parseManifestItems(opf, opfDir) {
  const manifest = new Map();
  const clean = stripXmlNs(opf);
  const re = /<item\b([^>]*?)\/?>/gi;
  for (const m of clean.matchAll(re)) {
    const attrs = m[1];
    const id = attrs.match(/\bid\s*=\s*["']([^"']+)["']/i)?.[1];
    const href = attrs.match(/\bhref\s*=\s*["']([^"']+)["']/i)?.[1];
    const mediaType = attrs.match(/\bmedia-type\s*=\s*["']([^"']+)["']/i)?.[1] || "";
    if (id && href) {
      manifest.set(id, { href: resolveHref(opfDir, href), mediaType });
    }
  }
  return manifest;
}

function parseSpineIds(opf) {
  const clean = stripXmlNs(opf);
  return [...clean.matchAll(/<itemref\b[^>]*\bidref\s*=\s*["']([^"']+)["']/gi)].map(
    (m) => m[1]
  );
}

function hasEncryption(zip) {
  return Object.keys(zip.files).some((p) =>
    /encryption\.xml$/i.test(normalizeZipPath(p))
  );
}

async function readContainerRoot(zip) {
  const path = Object.keys(zip.files).find((p) =>
    /meta-inf\/container\.xml$/i.test(normalizeZipPath(p))
  );
  const f = path ? zip.file(path) : zip.file("META-INF/container.xml");
  return f?.async("string");
}

async function extractChapter(zip, index, logicalPath) {
  const zf = getZipFile(zip, index, logicalPath);
  if (!zf) return "";
  const bytes = await zf.async("uint8array");
  return htmlToPlain(decodeBytes(bytes));
}

async function parseEpubZip(file, warnings) {
  const zip = await JSZip.loadAsync(await file.arrayBuffer());
  const zipIndex = buildZipIndex(zip);

  if (hasEncryption(zip)) {
    warnings.push(
      "EPUB protetto da DRM/encryption: non è possibile estrarre il testo. Usa una copia senza protezione (Calibre, editoriale legale)."
    );
    return { title: file.name, text: "" };
  }

  const container = await readContainerRoot(zip);
  if (!container) throw new Error("EPUB non valido: container.xml mancante");

  const rootfile = container.match(/full-path\s*=\s*["']([^"']+)["']/i)?.[1];
  if (!rootfile) throw new Error("EPUB non valido: percorso OPF mancante");

  const opfPath = normalizeZipPath(rootfile);
  const opf = await getZipFile(zip, zipIndex, opfPath)?.async("string");
  if (!opf) throw new Error(`OPF non trovato: ${opfPath}`);

  const title =
    opf.match(/<dc:title[^>]*>([^<]+)<\/dc:title>/i)?.[1]?.trim() ||
    opf.match(/<title[^>]*>([^<]+)<\/title>/i)?.[1]?.trim() ||
    file.name.replace(/\.[^.]+$/, "");

  const opfDir = opfPath.includes("/") ? opfPath.slice(0, opfPath.lastIndexOf("/")) : "";
  const manifest = parseManifestItems(opf, opfDir);
  const spineIds = parseSpineIds(opf);
  const parts = [];
  const seen = new Set();

  async function pushText(logicalPath, label) {
    const key = logicalPath.toLowerCase();
    if (seen.has(key)) return;
    seen.add(key);
    const plain = await extractChapter(zip, zipIndex, logicalPath);
    if (plain.length > 15) parts.push(`## ${label}\n\n${plain}`);
  }

  for (const id of spineIds) {
    const item = manifest.get(id);
    if (item && isTextResource(item.mediaType, item.href)) {
      await pushText(item.href, item.href);
    }
  }

  if (!parts.length) {
    for (const [, item] of manifest) {
      if (isTextResource(item.mediaType, item.href)) {
        await pushText(item.href, item.href);
      }
    }
  }

  if (!parts.length) {
    const htmlFiles = Object.keys(zip.files)
      .filter((p) => !zip.files[p].dir && /\.(xhtml|html|htm|xht)$/i.test(p))
      .sort();
    warnings.push(`Scansione ZIP: ${htmlFiles.length} file HTML trovati.`);
    for (const p of htmlFiles) {
      await pushText(normalizeZipPath(p), p);
    }
  }

  return { title, text: parts.join("\n\n"), manifestSize: manifest.size };
}

/** Fallback con libreria epub.js (molti EPUB “difficili”). */
async function parseEpubJs(arrayBuffer, fileName, warnings) {
  const epubFn = globalThis.ePub;
  if (!epubFn) return null;

  try {
    const book = epubFn(arrayBuffer, { openAs: "binary" });
    await book.ready;

    const meta = book.package?.metadata || (await book.loaded?.metadata) || {};
    const title =
      meta.title ||
      (typeof meta.creator === "string" ? meta.creator : null) ||
      fileName.replace(/\.[^.]+$/, "");

    await book.loaded.spine;
    const spineItems = book.spine?.spineItems || book.spine?.items || [];
    const texts = [];

    for (const item of spineItems) {
      const href = item.href || item.url;
      if (!href) continue;
      try {
        const doc = await book.load(href);
        const plain = htmlToPlain(
          doc?.documentElement?.outerHTML || doc?.body?.innerHTML || ""
        );
        if (!plain && doc?.body) {
          texts.push(doc.body.textContent.replace(/\s+/g, " ").trim());
        } else if (plain) {
          texts.push(plain);
        }
      } catch (e) {
        warnings.push(`epub.js ${href}: ${e.message}`);
      }
    }

    if (!texts.length) return null;
    warnings.push("Testo estratto con motore epub.js (fallback).");
    return { title, text: texts.join("\n\n") };
  } catch (e) {
    warnings.push(`epub.js fallito: ${e.message}`);
    return null;
  }
}

async function parseEpub(file, warnings) {
  if (!globalThis.JSZip) throw new Error("JSZip non caricato");

  const buffer = await file.arrayBuffer();
  let result = await parseEpubZip(file, warnings);
  let text = normalizeText(result.text, warnings);

  if (text.length < 200) {
    const fallback = await parseEpubJs(buffer, file.name, warnings);
    if (fallback?.text) {
      result.title = fallback.title || result.title;
      text = normalizeText(fallback.text, warnings);
    }
  }

  if (text.length < 50) {
    warnings.push(
      "EPUB: nessun testo estratto. Possibili cause: DRM (Amazon/Kobo), solo immagini, o file corrotto. Prova a esportare senza DRM con Calibre."
    );
  }

  return { title: result.title, text };
}

async function parsePdf(file, warnings) {
  if (!globalThis.pdfjsLib) throw new Error("PDF.js non caricato");
  pdfjsLib.GlobalWorkerOptions.workerSrc =
    "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js";
  const data = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data }).promise;
  const parts = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    try {
      const page = await pdf.getPage(i);
      const content = await page.getTextContent();
      const t = content.items.map((it) => it.str).join(" ").trim();
      if (t) parts.push(`--- Pagina ${i} ---\n${t}`);
    } catch (e) {
      warnings.push(`Pagina ${i}: ${e.message}`);
    }
  }
  if (!parts.length) warnings.push("PDF senza testo selezionabile (forse scansione).");
  return normalizeText(parts.join("\n\n"), warnings);
}

async function parseTxt(file, warnings) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  return normalizeText(decodeBytes(bytes), warnings);
}

async function parseFile(file) {
  const warnings = [];
  const name = file.name.toLowerCase();
  let title = file.name.replace(/\.[^.]+$/, "");
  let text = "";
  let format = "txt";

  if (name.endsWith(".pdf")) {
    format = "pdf";
    text = await parsePdf(file, warnings);
  } else if (name.endsWith(".epub") || name.endsWith(".ipub")) {
    format = "epub";
    const r = await parseEpub(file, warnings);
    title = r.title || title;
    text = r.text;
  } else if (name.endsWith(".txt")) {
    format = "txt";
    text = await parseTxt(file, warnings);
  } else {
    throw new Error("Formato non supportato. Usa PDF, EPUB, iPUB o TXT.");
  }

  if (text.length < 200) {
    warnings.push("Testo estratto molto breve: verifica DRM, scansione o prova un altro formato.");
  }

  return { title, format, text, warnings, charCount: text.length };
}

window.parseFile = parseFile;
