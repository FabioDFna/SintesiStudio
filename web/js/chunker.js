/** Chunk più piccoli = meno chiamate API (importante per quota free tier). */
const CHUNK_SIZE = 55_000;
const OVERLAP = 2_000;

function chunkText(text) {
  if (text.length <= CHUNK_SIZE) return [text];
  const chunks = [];
  let start = 0;
  while (start < text.length) {
    let end = Math.min(start + CHUNK_SIZE, text.length);
    if (end < text.length) {
      const minBreak = start + CHUNK_SIZE / 2;
      const para = text.lastIndexOf("\n\n", end);
      const dot = text.lastIndexOf(".", end);
      const breakAt =
        (para > minBreak ? para : -1) >= 0 ? para :
        (dot > minBreak ? dot : -1) >= 0 ? dot : end;
      end = breakAt;
    }
    chunks.push(text.slice(start, end));
    if (end >= text.length) break;
    start = Math.max(end - OVERLAP, start + 1);
  }
  return chunks;
}

function excerptForQuestion(fullText, question, maxLen = 80_000) {
  const keywords = question
    .toLowerCase()
    .split(/\W+/)
    .filter((w) => w.length > 4)
    .slice(0, 12);
  if (keywords.length === 0) return fullText.slice(0, maxLen);
  const paragraphs = fullText.split(/\n\n+/);
  const top = paragraphs
    .map((p) => ({
      p,
      score: keywords.filter((k) => p.toLowerCase().includes(k)).length,
    }))
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 40)
    .map((x) => x.p);
  const selected = top.length ? top.join("\n\n") : fullText.slice(0, maxLen);
  return selected.length > maxLen ? selected.slice(0, maxLen) : selected;
}

window.SintesiChunker = { chunkText, excerptForQuestion };
