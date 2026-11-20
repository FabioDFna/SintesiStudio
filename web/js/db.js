const DB_NAME = "sintesi_studio";
const STORE = "documents";
const CHAT = "chat";

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: "id", autoIncrement: true });
      }
      if (!db.objectStoreNames.contains(CHAT)) {
        const chat = db.createObjectStore(CHAT, { keyPath: "id", autoIncrement: true });
        chat.createIndex("documentId", "documentId", { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

function tx(store, mode = "readonly") {
  return openDb().then((db) => db.transaction(store, mode).objectStore(store));
}

async function listDocuments() {
  const store = await tx(STORE);
  return new Promise((resolve, reject) => {
    const req = store.getAll();
    req.onsuccess = () =>
      resolve(
        (req.result || []).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
      );
    req.onerror = () => reject(req.error);
  });
}

async function getDocument(id) {
  const store = await tx(STORE);
  return new Promise((resolve, reject) => {
    const req = store.get(Number(id));
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function saveDocument(doc) {
  const store = await tx(STORE, "readwrite");
  return new Promise((resolve, reject) => {
    const payload = { ...doc, createdAt: doc.createdAt || Date.now() };
    const req = doc.id ? store.put(payload) : store.add(payload);
    req.onsuccess = () => {
      const newId = req.result;
      resolve(typeof newId === "number" ? newId : doc.id);
    };
    req.onerror = () => reject(req.error);
  });
}

async function deleteDocument(id) {
  const store = await tx(STORE, "readwrite");
  await new Promise((res, rej) => {
    const r = store.delete(Number(id));
    r.onsuccess = res;
    r.onerror = () => rej(r.error);
  });
  const chatStore = await tx(CHAT, "readwrite");
  const all = await new Promise((res, rej) => {
    const r = chatStore.getAll();
    r.onsuccess = () => res(r.result || []);
    r.onerror = () => rej(r.error);
  });
  for (const m of all.filter((x) => x.documentId === Number(id))) {
    await new Promise((res, rej) => {
      const r = chatStore.delete(m.id);
      r.onsuccess = res;
      r.onerror = () => rej(r.error);
    });
  }
}

async function listMessages(documentId) {
  const store = await tx(CHAT);
  return new Promise((resolve, reject) => {
    const req = store.index("documentId").getAll(Number(documentId));
    req.onsuccess = () =>
      resolve(
        (req.result || []).sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0))
      );
    req.onerror = () => reject(req.error);
  });
}

async function addMessage(documentId, role, content) {
  const store = await tx(CHAT, "readwrite");
  return new Promise((resolve, reject) => {
    const req = store.add({
      documentId: Number(documentId),
      role,
      content,
      createdAt: Date.now(),
    });
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

window.SintesiDb = {
  listDocuments,
  getDocument,
  saveDocument,
  deleteDocument,
  listMessages,
  addMessage,
};
