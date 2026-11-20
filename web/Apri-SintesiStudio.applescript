-- Tasto destro → Apri (prima volta). Avvia server HTTP — obbligatorio per Chrome.
set scriptPath to POSIX path of (path to me)
set webDir to do shell script "dirname " & quoted form of scriptPath

tell application "Terminal"
	activate
	do script "cd " & quoted form of webDir & " && clear && echo '====================================' && echo ' SintesiStudio' && echo ' NON chiudere questa finestra' && echo '====================================' && echo '' && python3 -m http.server 3000 --bind 127.0.0.1"
end tell

delay 4

try
	tell application "Google Chrome"
		activate
		open location "http://127.0.0.1:3000/"
	end tell
on error
	open location "http://127.0.0.1:3000/"
end try

display alert "SintesiStudio avviato" message "Chrome deve aprire: http://127.0.0.1:3000" & return & return & "Se vedi errori CORS, NON aprire index.html con doppio clic." & return & "Usa solo questo indirizzo http."
