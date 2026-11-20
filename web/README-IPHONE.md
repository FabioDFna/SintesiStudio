# SintesiStudio — Chrome, iPhone, PC

## Metodo consigliato (senza npx, senza password Netlify)

### Su Windows

1. Vai in `...\SintesiStudio\web`
2. **Doppio clic** su **`AVVIA-CHROME.bat`**

### Su Mac

1. Copia la cartella `web` sul Mac
2. **Doppio clic** su **`AVVIA-CHROME.command`** (prima volta: tasto destro → Apri)
3. Se non parte, in Terminale: `chmod +x AVVIA-CHROME.command && ./AVVIA-CHROME.command`

Vedi **`LEGGIMI-MAC.txt`** per i dettagli.
3. Si apre **Google Chrome** su `http://localhost:3000`
4. Lascia aperta la finestra nera (è il server)

### Impostazioni

- **API key Gemini** da [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
- **Non** è la password del Wi‑Fi

### Su telefono (iPhone o Android) con Chrome

1. Stessa **Wi‑Fi** del PC
2. Nella finestra del server copia l’indirizzo tipo `http://192.168.1.10:3000`
3. Aprilo nell’app **Chrome** sul telefono

---

## Perché non aprire `index.html` con doppio clic?

Chrome blocca alcune funzioni se apri il file direttamente.  
Usa sempre **`AVVIA-CHROME.bat`** → così l’indirizzo è `http://localhost:3000`.

---

## Se il .bat dice «Node.js non trovato»

Installa [Node.js LTS](https://nodejs.org) e riprova, oppure usa il PC con **Cursor** installato (il bat trova Node automaticamente).

---

## Hosting online (opzionale)

Solo se vuoi un link fisso senza PC acceso: Netlify/Vercel richiedono **registrazione** (email + password del servizio).

---

Vedi anche: `LEGGIMI-CHROME.txt`
