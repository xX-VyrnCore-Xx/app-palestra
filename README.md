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
- Gamification: livelli/XP a tema palestra (Principiante → Elite → Leggenda), obiettivo settimanale, e tre serie di traguardi (streak, numero allenamenti, kg totali sollevati)
- Classifica settimanale motivazionale: allievo e PT vedono chi si è allenato di più negli ultimi 7 giorni tra gli allievi dello stesso PT (funzione Postgres server-side che restituisce solo nome e conteggio, nessun accesso incrociato ai dati altrui)
- Schede di allenamento con categoria (Full Body, Push, Pull, Gambe, Cardio, Mobilità), durata stimata e numero di esercizi mostrati in lista
- Profilo con tema chiaro/scuro/di sistema (persistito) e logout
- Catalogo di 60 esercizi precaricato al primo avvio (offline e su Supabase), con gruppo muscolare, attrezzatura, difficoltà e nota tecnica; picker per i PT con filtri per gruppo muscolare e ricerca tutorial video
- Login essenziale (email + password, nessun passaggio extra) e registrazione con ruolo, dati corporali opzionali e onboarding guidato post-registrazione per gli allievi (esperienza, giorni di allenamento, obiettivo, stile di vita, lesioni, alimentazione, note + riepilogo); il profilo resta personalizzabile in ogni momento (bio, obiettivo, altezza, peso)
- Sincronizzazione bidirezionale: push dei dati registrati offline + pull periodico di fallback; schede, esercizi assegnati e metriche corporee arrivano però **in tempo reale** via Supabase Realtime (come la chat), senza bisogno di riaprire l'app o aspettare il sync — nessuna nuova build richiesta per vedere dati aggiornati, solo per nuove funzionalità/modifiche al codice
- Collegamento PT↔Allievo tramite codice invito a 6 caratteri (il PT lo genera e condivide, invece di far incollare un ID grezzo), in registrazione o in un secondo momento dal Profilo
- Creazione scheda assistita da AI per il PT: descrive l'obiettivo in linguaggio naturale, l'AI propone 4-8 esercizi dal catalogo reale (mai inventati) tenendo conto degli infortuni noti del cliente, come bozza da rivedere prima di salvare
- Video tutorial esercizi riprodotti **in app** (WebView), senza aprire YouTube esterno
- Chat: indicatore "sta scrivendo", allegati immagine/file, messaggi vocali (registrazione + player play/pausa), doppia spunta di lettura
- Timer di recupero con notifica push (e vibrazione) se l'allievo lascia la schermata o l'app va in background prima che scada
- La lista schede dell'allievo evidenzia automaticamente la "prossima" in rotazione dopo un allenamento completato
- Livelli di progressione a tema palestra (Principiante → Intermedio → Avanzato → Elite → Leggenda), non più gradi militari
- App bloccata in orientamento verticale
- Modelli scheda riutilizzabili del PT ("Push day", ecc.) sincronizzati su Supabase, non più solo sul dispositivo

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

Un progetto Supabase dedicato (`app-palestra`) è già stato creato e lo schema di `docs/supabase_schema.sql` (tabelle + Row Level Security) è già stato applicato. Per motivi di sicurezza URL e anon key **non sono committati** nel repository: vanno impostati localmente.

1. Copia `local.properties.example` in `local.properties` e imposta `sdk.dir`, `SUPABASE_URL` e `SUPABASE_ANON_KEY` (chiedi le credenziali del progetto `app-palestra` al proprietario, oppure creane uno tuo come descritto sotto).
2. Apri il progetto in Android Studio (Iguana o successivo) e lascia sincronizzare Gradle.
3. Esegui l'app su un emulatore/dispositivo con API 26+.

> **Nota build da terminale:** il progetto richiede **JDK 17**. Se il sistema ha una Java più recente
> (es. 21+), il compilatore Kotlin fallisce con un errore opaco (`IllegalArgumentException: <version>`).
> Imposta `JAVA_HOME` su una JDK 17 prima di invocare Gradle, oppure lancia da Android Studio che
> usa il suo JBR configurato per Gradle JVM.

Per usare un **tuo** progetto Supabase invece: crea un progetto su [supabase.com](https://supabase.com), esegui `docs/supabase_schema.sql` nello SQL editor, poi imposta `SUPABASE_URL`/`SUPABASE_ANON_KEY` in `local.properties`.

## Modello dati (Supabase / Room)

| Tabella | Descrizione |
|---|---|
| `profiles` | Utente con ruolo `PT` o `ALLIEVO`; un allievo referenzia il proprio `pt_id`; il PT ha un `invite_code` univoco |
| `allievo_private_profiles` | Risposte del questionario di onboarding (Welcome Page), leggibile dal proprio PT ma mai scrivibile da lui |
| `exercises` | Catalogo esercizi (di sistema o custom) |
| `workout_plans` | Scheda creata da un PT e assegnata a un allievo (eventualmente parte di un `program`) |
| `plan_exercises` | Esercizi di una scheda con serie/ripetizioni/recupero target |
| `plan_templates` / `plan_template_exercises` | Libreria di modelli scheda riutilizzabili del PT |
| `programs` | Mesociclo multi-settimana (raggruppa più `workout_plans`) |
| `workout_sessions` | Una sessione di allenamento svolta da un allievo |
| `set_entries` | Singola serie registrata (reps, peso, RPE) |
| `body_metrics` | Storico peso corporeo e misure |
| `messages` | Chat PT↔Allievo, con allegati (immagine/file/vocale) opzionali |
| `pt_notes` | Note private del PT su un allievo, con promemoria opzionale |
| `plotone_feed_posts` | Bacheca auto-pubblicata a fine allenamento |
| `ai_messages` / `ai_rate_limit_events` | Storico conversazione con l'assistente AI e rate limiting condiviso |

## Build dell'APK

Una GitHub Actions (`.github/workflows/build-apk.yml`) compila un APK ad ogni push su `main` che tocca il codice Android, e lo pubblica automaticamente come **Release** del repository (tab **Releases**) oltre che come artifact dell'esecuzione (tab **Actions**). Può anche essere lanciata a mano con **Run workflow**. Il file si chiama sempre `VibeFitness-<versione>.apk`, senza suffissi "debug"/"release".

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

## Notifiche push (Firebase Cloud Messaging)

Oltre alla Realtime (attiva solo mentre l'app è aperta), un messaggio in chat innesca anche una
push FCM verso **tutti i dispositivi** su cui il destinatario ha installato l'app, tramite l'Edge
Function Supabase `send-push` (`supabase/functions/send-push`), così arriva anche ad app chiusa.
Lato client, `FcmService` riceve il payload e mostra la notifica; il token del dispositivo viene
salvato in automatico su `device_tokens` (una riga per utente+dispositivo, non un unico campo) al
login, con pulizia automatica dei token non più validi (app disinstallata) e rimozione del token
di questo dispositivo al logout.

Per attivarla, genera una chiave service account su Firebase (Project Settings → Service accounts
→ Generate new private key) e impostala come secret sul progetto Supabase:
```
supabase secrets set FIREBASE_SERVICE_ACCOUNT_JSON='<contenuto del file json>' --project-ref qibthdzydlyvdknimfoj
```
Senza questo secret la funzione risponde con un no-op silenzioso: l'app funziona comunque, solo senza push.

## Email di benvenuto (Resend)

Dopo la registrazione, l'app invia un'email di benvenuto tramite l'Edge Function Supabase
`send-welcome-email` (`supabase/functions/send-welcome-email`), best-effort (non blocca né fa
fallire la registrazione). Supabase Auth continua a gestire da solo le email funzionali
(verifica, reset password) tramite il proprio SMTP: questa è solo un'email di benvenuto brandizzata
aggiuntiva via [Resend](https://resend.com).

Per attivarla, imposta due secret sul progetto Supabase (serve un account Resend con un dominio
mittente verificato):
```
supabase secrets set RESEND_API_KEY=<la-tua-chiave> --project-ref qibthdzydlyvdknimfoj
supabase secrets set RESEND_FROM_ADDRESS='Vibe Fitness <no-reply@tuodominio.it>' --project-ref qibthdzydlyvdknimfoj
```
Senza questi secret la funzione risponde con un no-op silenzioso.

## Pubblicazione su Google Play Store

Vedi `docs/play_store_release.md` per la guida completa: build firmata (Android App Bundle) via `.github/workflows/build-release-aab.yml`, testi della scheda (`docs/play_store_listing.md`) e bozza dell'informativa privacy (`docs/privacy_policy.md`).

## Assistente AI per la creazione scheda (PT)

Oltre alla chat con l'assistente, il PT può generare una bozza di scheda dal Plan Editor
("Crea con AI"): descrive l'obiettivo in italiano, e l'Edge Function `ai-plan-builder`
(`supabase/functions/ai-plan-builder`) chiede al modello NIM di scegliere 4-8 esercizi **solo**
dal catalogo reale (mai inventati), tenendo conto degli infortuni noti del cliente. La risposta
popola il draft del Plan Editor esattamente come una scheda costruita a mano: il PT la rivede,
modifica sets/reps/recupero, e la salva solo quando è soddisfatto — non viene mai assegnata
automaticamente. Usa lo stesso secret `NVIDIA_NIM_API_KEY` dell'assistente AI.

## Nota su CI e branch

`.github/workflows/build-apk.yml` e `build-release-aab.yml` si attivano solo su push a `main`
(non su pull request o altri branch): un branch di feature non ha quindi build automatica finché
non viene mergiato. Verifica sempre lo stato della build su `main` dopo un merge.

## Prossimi passi suggeriti

- Test strumentali per il flusso offline -> sync
- Editor esercizi custom con immagini/GIF dimostrative (oggi solo URL manuale)
- Abilitare "leaked password protection" di Supabase Auth (richiede la dashboard, non è automatizzabile via SQL/API)
