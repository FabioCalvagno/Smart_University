import os

# In Codespaces, la directory di lavoro corrente (".") è la radice del repository
cartella_origine = "." 
file_output = "documento_repository.txt"

# Estensioni dei file da includere
estensioni_codice = ('.py', '.js', '.html', '.css', '.java', '.cpp', '.php', '.cs', '.ts', '.txt', '.sql', '.json')

# Cartelle di sistema o dipendenze da ignorare
cartelle_ignorate = ['.git', '.github', 'node_modules', '__pycache__', 'venv', '.vscode']

with open(file_output, 'w', encoding='utf-8') as out:
    out.write("=== STRUTTURA DELLA CARTELLA ===\n\n")
    
    # 1. Genera l'albero delle directory
    for root, dirs, files in os.walk(cartella_origine):
        # Rimuove le cartelle ignorate dalla navigazione
        dirs[:] = [d for d in dirs if d not in cartelle_ignorate]
        
        livello = root.replace(cartella_origine, '').count(os.sep)
        indent = ' ' * 4 * livello
        nome_cartella = os.path.basename(root)
        if not nome_cartella or nome_cartella == ".":
            nome_cartella = "Repository Root"
        
        out.write(f"{indent}📁 {nome_cartella}/\n")
        indent_file = ' ' * 4 * (livello + 1)
        for f in files:
            out.write(f"{indent_file}📄 {f}\n")
            
    out.write("\n\n=== CONTENUTO DEI FILE ===\n")
    
    # 2. Estrae il codice
    for root, dirs, files in os.walk(cartella_origine):
        dirs[:] = [d for d in dirs if d not in cartelle_ignorate]
        for f in files:
            # Evita di auto-includere il file di output o lo script stesso
            if f.endswith(estensioni_codice) and f not in [file_output, "combina.py"]:
                percorso = os.path.join(root, f)
                out.write(f"\n\n{'='*60}\n")
                out.write(f"FILE: {percorso.replace('./', '')}\n")
                out.write(f"{'='*60}\n\n")
                try:
                    with open(percorso, 'r', encoding='utf-8') as file_letto:
                        out.write(file_letto.read())
                except Exception as e:
                    out.write(f"[Errore di lettura: {e}]\n")

print(f"Finito! Creato il file: {file_output}")