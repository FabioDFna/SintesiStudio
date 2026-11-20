const fs = require("fs");
const path = require("path");

const dir = __dirname;
const css = fs.readFileSync(path.join(dir, "css/style.css"), "utf8");

function readJs(name) {
  let code = fs.readFileSync(path.join(dir, name), "utf8");
  if (name === "js/gemini.js") {
    code = code.replace(/^\(function \(\) \{\s*/, "");
    code = code.replace(/\}\)\(\);\s*$/, "");
    code = code.replace(/window\.SintesiPrompts/g, "({ summaryPrompt, audioPrompt, qaPrompt: qaPromptFn })");
    code = code.replace(/window\.SintesiChunker/g, "({ chunkText, excerptForQuestion })");
    code = code.replace(
      /const \{ summaryPrompt, audioPrompt, qaPrompt \} = \(\{ summaryPrompt, audioPrompt, qaPrompt: qaPromptFn \}\);/,
      "/* prompts inlined */"
    );
    code = code.replace(
      /const \{ chunkText, excerptForQuestion \} = \(\{ chunkText, excerptForQuestion \}\);/,
      "/* chunker inlined */"
    );
    code = code.replace(/window\.SintesiGemini\s*=\s*\{[\s\S]*?\};?\s*$/, "");
  }
  return code
    .replace(/window\.\w+\s*=\s*\{[^}]*\};?\s*/g, "")
    .replace(/window\.parseFile\s*=\s*parseFile;?\s*/g, "");
}

const jsBundle = [
  "js/prompts.js",
  "js/chunker.js",
  "js/db.js",
  "js/gemini.js",
  "js/parsers.js",
]
  .map(readJs)
  .join("\n\n");

let appJs = fs.readFileSync(path.join(dir, "js/app.js"), "utf8");
appJs = appJs
  .replace(/const parseFile = window\.parseFile;\s*/g, "")
  .replace(/const db = window\.SintesiDb;\s*/g, "")
  .replace(
    /const \{ DEFAULT_MODEL, buildFullSummary, buildAudioScript, answerQuestion \} =\s*window\.SintesiGemini;\s*/g,
    ""
  )
  .replace(/if \("serviceWorker" in navigator\) \{[\s\S]*?\}\s*/g, "");

const bodyMatch = fs.readFileSync(path.join(dir, "index.html"), "utf8");
const bodyStart = bodyMatch.indexOf("<body>");
const bodyEnd = bodyMatch.indexOf("</body>") + 7;
let body = bodyMatch.slice(bodyStart, bodyEnd);
body = body.replace(/<link rel="manifest"[^>]*>\s*/gi, "");
body = body.replace(/<script>[\s\S]*?<\/script>\s*(?=<!-- Home)/i, "");

const out = `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
  <meta name="theme-color" content="#1B5E20" />
  <title>SintesiStudio</title>
  <style>${css}</style>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"><\/script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"><\/script>
  <script src="https://cdn.jsdelivr.net/npm/epubjs@0.3.93/dist/epub.min.js"><\/script>
</head>
${body.replace("</body>", `<script>\n${jsBundle}\n\n${appJs}\ninit();\n</script>\n</body>`)}
`;

fs.writeFileSync(path.join(dir, "sintesi-standalone.html"), out);
console.log("OK: sintesi-standalone.html (" + Math.round(out.length / 1024) + " KB)");
