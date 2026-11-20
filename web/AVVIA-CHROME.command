#!/bin/bash
cd "$(dirname "$0")"

find_node() {
  command -v node 2>/dev/null && return 0
  for p in /opt/homebrew/bin/node /usr/local/bin/node \
    "/Applications/Cursor.app/Contents/Resources/app/resources/helpers/node"; do
    [ -x "$p" ] && echo "$p" && return 0
  done
  return 1
}

NODE=$(find_node)

if [ -z "$NODE" ]; then
  echo ""
  echo "Node.js non trovato. Usa AVVIA-PYTHON.command oppure:"
  echo "  brew install node   oppure   https://nodejs.org"
  echo ""
  read -r -p "Premi Invio per chiudere..."
  exit 1
fi

echo ""
echo "SintesiStudio — http://localhost:3000"
echo "Chiudi questa finestra per fermare il server."
echo ""

(sleep 1; open -a "Google Chrome" "http://localhost:3000" 2>/dev/null || open "http://localhost:3000") &

exec "$NODE" server.js
