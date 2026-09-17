# Palestra

App Android nativa per la gestione di allenamenti in palestra, pensata per due ruoli: **Allievo** (traccia allenamenti, progressi, dati corporei) e **Personal Trainer** (assegna schede e monitora i propri allievi).

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
- Timer di recupero tra le serie
- Statistiche di progressione (record personali, grafico del carico nel tempo)
- Tracciamento dati corporei (peso, massa grassa, misure)
- Sincronizzazione offline-first: tutto ciò che viene registrato in palestra senza rete viene inviato al cloud appena disponibile

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

Ogni riga locale ha uno `syncStatus` (`SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`). Le scritture (log di una serie, nuova misurazione, ecc.) avvengono sempre e solo su Room; `SyncWorker` gira periodicamente e quando c'è rete per svuotare la coda verso Supabase.

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

## Prossimi passi suggeriti

- Notifiche push per nuove schede assegnate (Supabase Realtime + FCM)
- Editor esercizi custom con immagini/GIF dimostrative
- Export PDF della scheda o dei progressi
- Test strumentali per il flusso offline -> sync
