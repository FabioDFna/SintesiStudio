#!/bin/bash
cd "$(dirname "$0")"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 non trovato. Installa Xcode Command Line Tools:"
  echo "  xcode-select --install"
  read -r -p "Premi Invio..."
  exit 1
fi

echo ""
echo "SintesiStudio (Python) — http://localhost:3000"
echo "Chiudi questa finestra per fermare il server."
echo ""

(sleep 1; open -a "Google Chrome" "http://localhost:3000" 2>/dev/null || open "http://localhost:3000") &

exec python3 -m http.server 3000 --bind 127.0.0.1
