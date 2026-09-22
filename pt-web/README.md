# Vibe Fitness — Gestionale PT

Web app riservata ai Personal Trainer di Vibe Fitness: clienti, schede, abbonamenti, note e chat,
collegata allo stesso backend Supabase dell'app Android (stesse tabelle, stesse policy RLS —
nessuna chiave elevata: qui gira solo la chiave `anon`, la sicurezza è tutta a livello di riga).

**Questo indirizzo è volutamente tenuto fuori dall'app e dall'APK.** Non compare in nessun link,
build config o codice sorgente dell'app Android: va comunicato ai PT direttamente (es. dal
titolare della palestra), non è pensato per essere scoperto navigando o decompilando l'app.

## Stack

- Next.js 14 (App Router), React 18 — SPA lato client, nessun server component con dati
- Tailwind CSS
- `@supabase/supabase-js` — stesso progetto/tabelle dell'app, autenticazione via email+password

## Sviluppo locale

```bash
cd pt-web
npm install
cp .env.local.example .env.local   # poi inserisci URL e anon key del progetto Supabase
npm run dev
```

## Deploy

Progetto Vercel separato con **Root Directory = `pt-web`**, variabili d'ambiente:

- `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_SUPABASE_ANON_KEY`

## Cosa fa

- **Login / Registrazione** — solo account con `role = 'PT'` possono accedere; un account Allievo
  che prova ad entrare viene disconnesso con un messaggio esplicativo.
- **Dashboard** — codice invito del PT (generato al primo accesso, come nell'app) e lista allievi.
- **Dettaglio allievo** — abbonamento (stato attivo/in scadenza/scaduto, storico rinnovi),
  infortuni/limitazioni, note private del PT, schede assegnate, chat.
- **Nuova scheda** — ricerca nel catalogo esercizi reale, serie/ripetizioni/peso/recupero per
  esercizio, riordino, stima minuti, categoria — assegnata subito all'allievo (arriva in tempo
  reale sull'app via Supabase Realtime, come già succedeva quando la creava il PT dall'app).

## Non ancora portato da app

Editor programmi multi-settimana, creazione scheda assistita da AI, modelli scheda riutilizzabili.
Il modello dati esiste già (`programs`, `plan_templates`, ...); sono stati lasciati fuori dalla
prima versione per tempo, non per limiti tecnici.
