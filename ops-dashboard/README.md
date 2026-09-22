# ViBE Ops Dashboard

Piccola dashboard **read-only** per monitorare la salute del progetto Supabase (spazio database,
storage, pulizia automatica, conteggi di attività). Non gestisce né mostra dati personali degli
utenti reali: solo numeri aggregati (vedi la funzione `get_admin_stats()` nel database).

Nessun framework/build step: pagine HTML statiche + 4 funzioni serverless Node su Vercel
(zero-config, nessuna dipendenza npm oltre alla libreria `crypto` built-in di Node).

## Perché non è già online

Il tentativo di deploy automatico da questa sessione ha ricevuto un errore Vercel:

```
403 Forbidden: "You don't have permission to create a Production Deployment for this project."
```

Il progetto Vercel `vibe-fitness-ops` è stato creato (con le env var già impostate), ma il token
usato in questa sessione non ha il permesso di completarne il primo deploy - probabilmente un
limite di ruolo/piano sul team `VyrnCore IT`. Va completato manualmente (2 minuti, vedi sotto).

## Deploy manuale

```bash
cd ops-dashboard
npx vercel link   # collega al progetto "vibe-fitness-ops" già creato, team VyrnCore IT
npx vercel --prod
```

Se il progetto non risultasse già collegato, in alternativa: vai su vercel.com → New Project →
Import → seleziona questa cartella (`ops-dashboard`) come root directory.

### Variabili d'ambiente richieste

Già impostate sul progetto `vibe-fitness-ops` (se ricreato da zero, vanno reinserite in
Project Settings → Environment Variables):

| Nome | Valore |
|---|---|
| `ADMIN_DASHBOARD_PASSWORD` | password demo per accedere alla dashboard |
| `SESSION_SECRET` | stringa casuale lunga (usata per firmare il cookie di sessione) |
| `SUPABASE_URL` | `https://qibthdzydlyvdknimfoj.supabase.co` |
| `SUPABASE_ANON_KEY` | chiave anon pubblica del progetto Supabase (stessa già nell'app Android) |

## Come funziona

- `index.html` — form di login, POST a `/api/login`.
- `/api/login.js` — verifica la password contro `ADMIN_DASHBOARD_PASSWORD`, imposta un cookie
  httpOnly firmato con HMAC-SHA256 (`SESSION_SECRET`), valido 12 ore.
- `/api/stats.js` — verifica il cookie, poi chiama `get_admin_stats()` su Supabase (via chiave
  anon, sola lettura di dati aggregati) e calcola gli avvisi (soglia 70%/90% dei limiti free tier).
- `dashboard.html` — mostra i numeri, si aggiorna da sola ogni 60 secondi.

## Limiti noti

- Password singola condivisa (va bene per una demo/team ristretto, non per accesso multi-utente
  con permessi differenziati).
- Le soglie di allarme in `api/stats.js` assumono il piano Supabase Free (500 MB DB, 1 GB storage)
  - da aggiornare se il progetto passa a Pro.
