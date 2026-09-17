# Pubblicazione su Google Play Store

Questa guida copre tutto quello che serve per pubblicare **Vibe Fitness** sul Play Store: build firmata (fatta da questo repo via CI) e i passaggi manuali sul Play Console (che solo il proprietario dell'account sviluppatore può fare).

## 1. Build firmata (Android App Bundle)

Google Play richiede un **Android App Bundle (.aab)**, non un APK. Il repo ha una GitHub Actions dedicata (`.github/workflows/build-release-aab.yml`) che lo compila e lo firma, ma serve prima impostare 4 repository secret (Settings → Secrets and variables → Actions → **New repository secret**):

| Secret | Valore |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | il keystore di firma, codificato in base64 |
| `RELEASE_STORE_PASSWORD` | password del keystore |
| `RELEASE_KEY_ALIAS` | alias della chiave |
| `RELEASE_KEY_PASSWORD` | password della chiave |

Ti ho generato e mandato un keystore reale pronto all'uso (valido 30 anni) insieme a un file con questi 4 valori già compilati — vedi i file allegati alla chat. **Salva il keystore in un posto sicuro fuori da qui**: se lo perdi non potrai più pubblicare aggiornamenti della stessa app su Play Store. Il keystore non va mai committato nel repository (infatti non lo troverai qui).

Servono anche i due secret già usati per l'APK debug, se vuoi che la build di produzione parli col backend reale:

| Secret | Valore |
|---|---|
| `SUPABASE_URL` | `https://qibthdzydlyvdknimfoj.supabase.co` |
| `SUPABASE_ANON_KEY` | l'anon key del progetto `app-palestra` |

Una volta impostati i secret, vai su **Actions → Build Release AAB → Run workflow**. Al termine, scarica l'artifact `vibe-fitness-release-aab`: è il file `.aab` da caricare su Play Console.

## 2. Google Play Console (passaggi manuali, fuori dalla mia portata)

Non ho accesso al tuo account Google Play Console, quindi questi passaggi li devi fare tu:

1. **Account sviluppatore**: registrati su [play.google.com/console](https://play.google.com/console) (una tantum, $25).
2. **Crea l'app**: nome "Vibe Fitness", categoria Salute e fitness, gratuita.
3. **Scheda del negozio (Store listing)**: usa i testi in `docs/play_store_listing.md` come punto di partenza. Servono anche:
   - Icona 512×512 px (puoi esportarla dall'icona adattiva dell'app, `app/src/main/res/drawable/ic_launcher_*.xml`, rasterizzata)
   - Almeno 2 screenshot per telefono (consigliati 4-8), formato verticale
   - Feature graphic 1024×500 px
4. **Privacy Policy**: Play Store la richiede obbligatoriamente. Ho preparato una bozza in `docs/privacy_policy.md` — devi pubblicarla su una pagina web pubblica (es. GitHub Pages, un sito, Notion pubblico) e incollarne l'URL nella scheda "App content" del Console.
5. **Data safety / Content rating**: compila i questionari nel Console. In base ai dati raccolti dall'app (email, dati di allenamento, dati corporei) dichiara: raccolta dati account e fitness, condivisi con nessuno, criptati in transito (Supabase usa HTTPS), cancellabili su richiesta.
6. **Carica l'AAB**: in "Produzione" (o prima in "Test interno" per provarla) crea una nuova release e carica il file `.aab` scaricato dalla Action.
7. **Invia in revisione**: Google impiega di solito da poche ore a qualche giorno per la prima revisione.

## 3. Aggiornamenti futuri

Ad ogni nuova versione: incrementa `versionCode` e `versionName` in `app/build.gradle.kts`, rilancia la Action "Build Release AAB", carica il nuovo `.aab` come nuova release sullo stesso Console.
