# Vibe Fitness

App Android nativa per il brand di palestre **Vibe Fitness**, pensata per due ruoli: **Allievo** (traccia allenamenti, progressi, dati corporei) e **Personal Trainer** (assegna schede e monitora i propri allievi).

## Stack tecnico

- **Kotlin + Jetpack Compose** (Material 3) per l'interfaccia
- **Room** come database locale, per un'esperienza **offline-first**: ogni allenamento si registra localmente anche senza connessione
- **Supabase** (Postgres + Auth + Realtime) come backend cloud
- **Hilt** per la dependency injection
- **WorkManager** per sincronizzare in background i dati locali pendenti verso Supabase non appena torna la connessione

## Funzionalità principali

- Autenticazione con due ruoli (PT / Allievo)
- Il PT crea schede di allenamento e le assegna ai propri allievi
- L'allievo segue la scheda, registra serie/ripetizioni/peso per ogni esercizio
- Home con streak di allenamenti consecutivi, allenamenti della settimana, traguardi/badge (3/7/14/30/60/100 giorni) e CTA per la prossima scheda
- Cronologia allenamenti (data, durata, serie, volume totale)
- Timer di recupero tra le serie
- Statistiche di progressione (record personali, grafico del carico nel tempo)
- Tracciamento dati corporei (peso, massa grassa, misure) con grafico dell'andamento peso
- Il PT vede, per ogni allievo, allenamenti svolti, ultimo allenamento e andamento peso
- Notifica di promemoria giornaliera se l'allievo non si allena da 2+ giorni (disattivabile dal Profilo)
- Chat in tempo reale tra Allievo e PT (Supabase Realtime), con invio di link (riconosciuti e cliccabili) e allegati (file/immagini via Supabase Storage), e notifica quando arriva un messaggio e app aperta/in background attivo
- Calendario allenamenti: vista mensile con indicatore del giorno corrente, riepilogo allenamenti del mese e legenda
- Note private del PT su ogni allievo (obiettivi, infortuni, osservazioni), visibili solo al PT
- Statistiche avanzate: volume di allenamento per gruppo muscolare e andamento del volume settimanale, oltre al grafico di progressione per esercizio
- Gamification: livelli/XP con titolo (Novizio → Leggenda), obiettivo settimanale, e tre serie di traguardi (streak, numero allenamenti, kg totali sollevati)
- Schede di allenamento con categoria (Full Body, Push, Pull, Gambe, Cardio, Mobilità), durata stimata e numero di esercizi mostrati in lista
- Profilo con tema chiaro/scuro/di sistema (persistito) e logout
- Catalogo di 24 esercizi comuni precaricato al primo avvio (offline e su Supabase)
- Sincronizzazione bidirezionale: push dei dati registrati offline + pull periodico di fallback; schede, esercizi assegnati e metriche corporee arrivano però **in tempo reale** via Supabase Realtime (come la chat), senza bisogno di riaprire l'app o aspettare il sync — nessuna nuova build richiesta per vedere dati aggiornati, solo per nuove funzionalità/modifiche al codice

## Architettura

```
data/
  local/       Room: entità, DAO, database (fonte di verità locale, sempre scrivibile offline)
  remote/      Client Supabase, DTO, mapper entità -> DTO
  repository/  Un repository per dominio (Auth, Workout, BodyMetrics): scrivono sempre prima in locale
  sync/        SyncManager + SyncWorker (WorkManager): spingono su Supabase le righe con syncStatus pendente
di/            Moduli Hilt (Database, Supabase)
ui/            Schermate Compose organizzate per feature (auth, workout, timer, stats, bodymetrics, pt, dashboard)
```

Ogni riga locale ha uno `syncStatus` (`SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`). Le scritture (log di una serie, nuova misurazione, ecc.) avvengono sempre e solo su Room; `SyncWorker` gira periodicamente, al login e dopo ogni assegnazione scheda, e quando c'è rete: prima spinge (`push`) le righe pendenti su Supabase, poi tira giù (`pull`) schede/sessioni/metriche rilevanti per l'utente loggato (e, se PT, per i suoi allievi) — è questo pull a far arrivare su un dispositivo una scheda assegnata da un altro.

Le entità Room hanno indici e foreign key sulle relazioni possedute nello stesso flusso locale (scheda→esercizi, sessione→serie), per integrità referenziale e query più veloci; le relazioni cross-utente (PT/allievo) restano senza FK stretta perché arrivano via sync e non sempre nello stesso ordine.

## Setup

Un progetto Supabase dedicato (`app-palestra`, org VyrnCore IT) è già stato creato e lo schema di `docs/supabase_schema.sql` (tabelle + Row Level Security) è già stato applicato. Per motivi di sicurezza URL e anon key **non sono committati** nel repository: vanno impostati localmente.

1. Copia `local.properties.example` in `local.properties` e imposta `sdk.dir`, `SUPABASE_URL` e `SUPABASE_ANON_KEY` (chiedi le credenziali del progetto `app-palestra` al proprietario, oppure creane uno tuo come descritto sotto).
2. Apri il progetto in Android Studio (Iguana o successivo) e lascia sincronizzare Gradle.
3. Esegui l'app su un emulatore/dispositivo con API 26+.

Per usare un **tuo** progetto Supabase invece: crea un progetto su [supabase.com](https://supabase.com), esegui `docs/supabase_schema.sql` nello SQL editor, poi imposta `SUPABASE_URL`/`SUPABASE_ANON_KEY` in `local.properties`.

## Modello dati (Supabase / Room)

| Tabella | Descrizione |
|---|---|
| `profiles` | Utente con ruolo `PT` o `ALLIEVO`; un allievo referenzia il proprio `pt_id` |
| `exercises` | Catalogo esercizi (di sistema o custom) |
| `workout_plans` | Scheda creata da un PT e assegnata a un allievo |
| `plan_exercises` | Esercizi di una scheda con serie/ripetizioni/recupero target |
| `workout_sessions` | Una sessione di allenamento svolta da un allievo |
| `set_entries` | Singola serie registrata (reps, peso, RPE) |
| `body_metrics` | Storico peso corporeo e misure |

## Build dell'APK

Una GitHub Actions (`.github/workflows/build-apk.yml`) compila un APK debug ad ogni push su `main` che tocca il codice Android, e lo carica come artifact scaricabile dalla pagina dell'esecuzione (tab **Actions** del repository). Può anche essere lanciata a mano con **Run workflow**.

Per far sì che l'APK compilato in CI si connetta davvero a Supabase, imposta questi due **repository secret** (Settings → Secrets and variables → Actions):
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

Senza questi secret l'APK viene comunque generato (utile per testare solo la UI), ma senza credenziali valide per il backend.

## Assistente AI (NVIDIA NIM)

Il PT e l'allievo hanno ciascuno un assistente AI privato (tab "Assistente"), con prompt di sistema
diversi scelti automaticamente in base al ruolo. La chiamata a NVIDIA NIM avviene solo lato server,
tramite l'Edge Function Supabase `ai-chat` (`supabase/functions/ai-chat`): la API key **non è mai**
presente nel codice dell'app. La funzione applica anche un rate limit globale (40 richieste/minuto,
limite dell'account NIM) condiviso tra tutti gli utenti.

Per attivarla, imposta un secret sul progetto Supabase:
```
supabase secrets set NVIDIA_NIM_API_KEY=<la-tua-chiave> --project-ref qibthdzydlyvdknimfoj
```
Senza questo secret l'assistente risponde con un errore "non configurato", il resto dell'app funziona normalmente.

## Pubblicazione su Google Play Store

Vedi `docs/play_store_release.md` per la guida completa: build firmata (Android App Bundle) via `.github/workflows/build-release-aab.yml`, testi della scheda (`docs/play_store_listing.md`) e bozza dell'informativa privacy (`docs/privacy_policy.md`).

## Prossimi passi suggeriti

- Notifiche push per nuove schede assegnate (Supabase Realtime + FCM)
- Editor esercizi custom con immagini/GIF dimostrative
- Export PDF della scheda o dei progressi
- Test strumentali per il flusso offline -> sync
