#!/bin/bash
cd "$(dirname "$0")" || exit 1

echo "======================================"
echo "  SintesiStudio"
echo "  Cartella: $(pwd)"
echo "======================================"
echo ""

if [ ! -f "index.html" ]; then
  echo "ERRORE: index.html non trovato in questa cartella."
  echo "Assicurati di essere nella cartella 'web'."
  read -r -p "Premi Invio..."
  exit 1
fi

PORT=3000

if command -v node >/dev/null 2>&1; then
  echo "Avvio con Node.js..."
  (sleep 2; open "http://127.0.0.1:${PORT}/") &
  exec node server.js
fi

if command -v python3 >/dev/null 2>&1; then
  echo "Avvio con Python su http://127.0.0.1:${PORT}/"
  echo ""
  echo "  NON chiudere questa finestra."
  echo "  Per fermare: Ctrl+C"
  echo ""
  (sleep 2; open "http://127.0.0.1:${PORT}/") &
  exec python3 -m http.server "$PORT" --bind 127.0.0.1
fi

echo "Installa Node.js (nodejs.org) o Python3 (xcode-select --install)"
read -r -p "Premi Invio..."
exit 1
