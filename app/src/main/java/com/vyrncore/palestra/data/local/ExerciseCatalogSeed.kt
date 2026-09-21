package com.vyrncore.palestra.data.local

import com.vyrncore.palestra.data.local.entity.ExerciseEntity

/** Coarse difficulty tier shown in the exercise picker and plan editors. */
enum class ExerciseDifficulty { PRINCIPIANTE, INTERMEDIO, AVANZATO }

/**
 * Built-in exercise catalog. IDs are fixed on purpose: the same rows are inserted server-side
 * by the `seed_exercise_catalog` Supabase migration, so a device that seeds its local Room
 * catalog and later syncs never creates duplicates (the push is a plain upsert by id).
 *
 * 60 exercises across 7 muscle groups, each with equipment, difficulty and a short technique
 * cue surfaced in the workout screen so the allievo never has to guess how to perform a lift.
 */
object ExerciseCatalogSeed {

    private data class Seed(
        val id: String,
        val name: String,
        val muscleGroup: String,
        val equipment: String?,
        val difficulty: ExerciseDifficulty,
        val notes: String,
    )

    private val seeds: List<Seed> = listOf(
        // ---- Petto ----
        Seed("a10c9b1e-1111-4a11-8000-000000000001", "Panca piana", "Petto", "Bilanciere + panca Technogym", ExerciseDifficulty.INTERMEDIO, "Scapole retratte e arcuate, piedi ben piantati: discesa controllata di 2 secondi, tocca il petto senza rimbalzi e spingi spingendo anche i piedi a terra (leg drive)."),
        Seed("a10c9b1e-1111-4a11-8000-000000000002", "Panca inclinata", "Petto", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Inclinazione 30-45°: enfasi sul petto superiore, gomiti non oltre 45° dal busto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000003", "Croci ai cavi", "Petto", "Cavi (Technogym Cable Station)", ExerciseDifficulty.PRINCIPIANTE, "Un passo avanti a ogni piede, busto leggermente piegato: movimento ad arco costante, spremi al centro per un secondo incrociando leggermente le mani prima di tornare."),
        Seed("a10c9b1e-1111-4a11-8000-000000000004", "Piegamenti", "Petto", null, ExerciseDifficulty.PRINCIPIANTE, "Corpo rigido come una tavola, gomiti a 45°: meglio 5 pulite che 15 sbagliate."),
        Seed("a10c9b1e-1111-4a11-8000-000000000005", "Panca declinata", "Petto", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Enfasi sul petto inferiore: il bilanciere scende sulla linea dei capezzoli."),
        Seed("a10c9b1e-1111-4a11-8000-000000000006", "Chest press alle macchine", "Petto", "Macchina (Technogym Chest Press)", ExerciseDifficulty.PRINCIPIANTE, "Regola il sedile così che le maniglie siano all'altezza del centro petto: scapole contro lo schienale, gomiti a 45° dal busto, spremi 1 secondo a braccia quasi tese."),
        Seed("a10c9b1e-1111-4a11-8000-000000000007", "Croci su panca", "Petto", "Manubri", ExerciseDifficulty.INTERMEDIO, "Gomiti leggermente piegati e fissi: apri fino a sentire l'allungamento, non oltre."),
        Seed("a10c9b1e-1111-4a11-8000-000000000008", "Dip alle parallele", "Petto", "Peso corporeo", ExerciseDifficulty.AVANZATO, "Busto inclinato in avanti per il petto; verticale se vuoi coinvolgere i tricipiti."),

        // ---- Dorso ----
        Seed("a10c9b1e-1111-4a11-8000-000000000009", "Trazioni alla lat machine", "Dorso", "Lat machine (Technogym)", ExerciseDifficulty.PRINCIPIANTE, "Presa larga, busto leggermente indietro: porta la barra all'altezza del clavicolo, non dietro la testa. Le scapole scendono prima che le braccia si pieghino."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000a", "Rematore con bilanciere", "Dorso", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Busto a 45°, schiena neutra: tira verso l'ombelico, non verso il petto."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000b", "Trazioni alla sbarra", "Dorso", "Sbarra", ExerciseDifficulty.AVANZATO, "Parti da braccia estese, mento oltre la sbarra: niente scatti o slanci."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000c", "Stacco da terra", "Dorso", "Bilanciere", ExerciseDifficulty.AVANZATO, "Barra vicina alle tibie, schiena neutra: spingi col pavimento, non tirare col busto."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000d", "Rematore con manubrio", "Dorso", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Un ginocchio sulla panca, schiena parallela al suolo: tira il manubrio verso l'anca."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000e", "Pulley basso", "Dorso", "Cavi (Technogym Low Row)", ExerciseDifficulty.PRINCIPIANTE, "Busto fermo, ginocchia leggermente flesse: le spalle si spostano solo all'indietro, non il tronco. Tira verso l'ombelico e stringi le scapole a fine corsa."),
        Seed("a10c9b1e-1111-4a11-8000-00000000000f", "Lat machine presa inversa", "Dorso", "Lat machine", ExerciseDifficulty.PRINCIPIANTE, "Presa supina alla larghezza delle spalle: più carico sul gran dorsale basso."),
        Seed("a10c9b1e-1111-4a11-8000-000000000010", "Pullover ai cavi", "Dorso", "Cavi", ExerciseDifficulty.INTERMEDIO, "Braccia quasi tese: senti l'allungamento del dorsale in alto prima di tirare."),
        Seed("a10c9b1e-1111-4a11-8000-000000000011", "Australian pull-up", "Dorso", "Sbarra", ExerciseDifficulty.PRINCIPIANTE, "Corpo in riga sotto la sbarra bassa: porta il petto, non il mento, alla sbarra."),
        Seed("a10c9b1e-1111-4a11-8000-000000000012", "Shrug con manubri", "Dorso", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Alza solo le spalle verso le orecchie, senza ruotarle: pausa di 1 secondo in alto."),

        // ---- Gambe ----
        Seed("a10c9b1e-1111-4a11-8000-000000000013", "Squat", "Gambe", "Bilanciere", ExerciseDifficulty.AVANZATO, "Scendi sotto il parallelo se la mobilità lo consente: ginocchia in linea coi piedi."),
        Seed("a10c9b1e-1111-4a11-8000-000000000014", "Leg press", "Gambe", "Macchina (Panatta Leg Press 45°)", ExerciseDifficulty.PRINCIPIANTE, "Piedi alla larghezza delle spalle sul Centro della piattaforma, lombardia incollata allo schienale: scendi fino a 90° di ginocchio senza staccare il bacino, spingi con tutto il piede senza bloccare le ginocchia."),
        Seed("a10c9b1e-1111-4a11-8000-000000000015", "Affondi", "Gambe", "Manubri", ExerciseDifficulty.INTERMEDIO, "Passo lungo, ginocchio posteriore vicino al suolo: il busto resta verticale."),
        Seed("a10c9b1e-1111-4a11-8000-000000000016", "Leg curl", "Gambe", "Macchina (Panatta Leg Curl)", ExerciseDifficulty.PRINCIPIANTE, "Anca ben premuta sul pancale (il rullo sotto il polpaccio, non dietro la caviglia): contra i femorali per un secondo a fine corsa, discesa lenta di 2-3 secondi."),
        Seed("a10c9b1e-1111-4a11-8000-000000000017", "Leg extension", "Gambe", "Macchina (Panatta Leg Extension)", ExerciseDifficulty.PRINCIPIANTE, "Schiena a contatto, rullo sopra la caviglia: estendi completamente con pausa di 1 secondo in alto, il movimento parte dal quadricipite non con slanci."),
        Seed("a10c9b1e-1111-4a11-8000-000000000018", "Polpacci in piedi", "Gambe", "Macchina (Panatta Calf)", ExerciseDifficulty.PRINCIPIANTE, "Massimo allungamento in basso (tallone sotto il gradino), punta massima in alto con pausa di 1 secondo: range completo sempre, movimento lento."),
        Seed("a10c9b1e-1111-4a11-8000-000000000019", "Hip thrust", "Gambe", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Mentoniero al petto, spremi i glutei in alto per un secondo prima di scendere."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001a", "Hack squat", "Gambe", "Macchina (Panatta Hack Squat)", ExerciseDifficulty.INTERMEDIO, "Schiena interamente contro lo schienale, piedi poco avanti sulla pedana: scendi in profondità tenendo i talloni premuti, le ginocchia seguono la linea dei piedi."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001b", "Squat bulgaro", "Gambe", "Manubri", ExerciseDifficulty.AVANZATO, "Piede posteriore sulla panca: tutto il peso resta sulla gamba anteriore."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001c", "Polpacci seduto", "Gambe", "Macchina (Panatta Calf Seduto)", ExerciseDifficulty.PRINCIPIANTE, "Colpisce il soleo: ripetizioni lente e controllate, pausa di 1 secondo in alto e allungamento completo in basso."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001d", "Stacco rumeno", "Gambe", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Ginocchia semiflesse fisse: scendi col bacino indietro fino a sentire i femorali."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001e", "Step-up su panca", "Gambe", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Spingi solo con la gamba sul gradino: niente rimbalzi con quella posteriore."),
        Seed("a10c9b1e-1111-4a11-8000-00000000001f", "Goblet squat", "Gambe", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Manubrio al petto come un calice: il migliore squat per imparare la tecnica."),

        // ---- Spalle ----
        Seed("a10c9b1e-1111-4a11-8000-000000000020", "Military press", "Spalle", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Core contratto, niente inarcamento lombare: la testa passa oltre la barra in alto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000021", "Alzate laterali", "Spalle", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Alza fino all'altezza delle spalle, mignolo leggermente in alto come versare acqua."),
        Seed("a10c9b1e-1111-4a11-8000-000000000022", "Alzate posteriori", "Spalle", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Busto piegato in avanti: apri le braccia in arcata, pollici verso il basso."),
        Seed("a10c9b1e-1111-4a11-8000-000000000023", "Push press", "Spalle", "Bilanciere", ExerciseDifficulty.AVANZATO, "Piccolo dip delle ginocchia per slanciare: la spinta parte dalle anche."),
        Seed("a10c9b1e-1111-4a11-8000-000000000024", "Arnold press", "Spalle", "Manubri", ExerciseDifficulty.INTERMEDIO, "Rotazione completa dal palmo verso di te a palmo in avanti: movimento lento."),
        Seed("a10c9b1e-1111-4a11-8000-000000000025", "Alzate frontali", "Spalle", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Alza fino all'altezza degli occhi senza dondolare il busto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000026", "Face pull", "Spalle", "Cavi", ExerciseDifficulty.PRINCIPIANTE, "Tira verso la fronte separando le mani: salute delle spalle, fai sempre."),
        Seed("a10c9b1e-1111-4a11-8000-000000000027", "Russian press", "Spalle", "Manubri", ExerciseDifficulty.INTERMEDIO, "Parti con i manubri alle spalle, ruota il palmo in avanti spingendo verso l'alto."),

        // ---- Braccia ----
        Seed("a10c9b1e-1111-4a11-8000-000000000028", "Curl bicipiti", "Braccia", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Gomiti incollati ai fianchi: se il busto oscilla, il peso è troppo alto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000029", "Curl a martello", "Braccia", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Presa neutra: colpisce brachiale e lungo del bicipite, spessore del braccio."),
        Seed("a10c9b1e-1111-4a11-8000-000000000030", "Push down ai cavi", "Braccia", "Cavi", ExerciseDifficulty.PRINCIPIANTE, "Gomiti fissi ai fianchi: estendi completamente solo con il tricipite."),
        Seed("a10c9b1e-1111-4a11-8000-000000000031", "French press", "Braccia", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Gomiti puntati al soffitto e fermi: solo l'avambraccio si muove."),
        Seed("a10c9b1e-1111-4a11-8000-000000000032", "Curl con bilanciere", "Braccia", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Presa alla larghezza delle spalle: corsa completa, dal pieno all'estensione."),
        Seed("a10c9b1e-1111-4a11-8000-000000000033", "Panca stretta", "Braccia", "Bilanciere", ExerciseDifficulty.INTERMEDIO, "Presa alla larghezza delle spalle: gomiti vicini al busto nella discesa."),
        Seed("a10c9b1e-1111-4a11-8000-000000000034", "Curl su panca Scott", "Braccia", "Manubri", ExerciseDifficulty.INTERMEDIO, "Il pancale inclinato blocca gli imbrogli: braccio esteso a fine corsa."),
        Seed("a10c9b1e-1111-4a11-8000-000000000035", "Kick back", "Braccia", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Braccio parallelo al suolo: estendi solo l'avambraccio, pausa in alto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000036", "Dip alle parallele ai cavi", "Braccia", "Macchina", ExerciseDifficulty.INTERMEDIO, "Variante assistita: perfetta per imparare i dip prima del peso corporeo."),
        Seed("a10c9b1e-1111-4a11-8000-000000000037", "Curl ai cavi", "Braccia", "Cavi", ExerciseDifficulty.PRINCIPIANTE, "Tensione costante per tutta la corsa, anche in basso: niente pause a braccia rilassate."),

        // ---- Core ----
        Seed("a10c9b1e-1111-4a11-8000-000000000038", "Plank", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Gomiti sotto le spalle, glutei contratti: meglio 30 secondi dritte che 2 minuti storte."),
        Seed("a10c9b1e-1111-4a11-8000-000000000039", "Crunch", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Stacca solo le scapole dal suolo: la parte bassa della schiena resta a contatto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000040", "Russian twist", "Core", "Disco", ExerciseDifficulty.PRINCIPIANTE, "Ruota il busto intero, non solo le braccia: schiena dritta."),
        Seed("a10c9b1e-1111-4a11-8000-000000000041", "Hanging leg raise", "Core", "Sbarra", ExerciseDifficulty.AVANZATO, "Senza slancio: solleva le gambe con l'addome, le anche fanno la flessione."),
        Seed("a10c9b1e-1111-4a11-8000-000000000042", "Mountain climber", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Anche basse e ritmo costante: le ginocchia vanno al petto, non al soffitto."),
        Seed("a10c9b1e-1111-4a11-8000-000000000043", "Ab wheel", "Core", "Rotella", ExerciseDifficulty.AVANZATO, "Estendi senza inarcare la lombardia: parti con un raggio corto e aumenta col tempo."),
        Seed("a10c9b1e-1111-4a11-8000-000000000044", "Side plank", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Gomito sotto la spalla, anche allineate: il corpo forma una linea unica."),
        Seed("a10c9b1e-1111-4a11-8000-000000000045", "Dead bug", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Lombardia incollata al suolo: estendi braccio e gamba opposti lentamente."),
        Seed("a10c9b1e-1111-4a11-8000-000000000046", "Bicycle crunch", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Gomito verso il ginocchio opposto, lento: la velocità ruba lavoro all'addome."),
        Seed("a10c9b1e-1111-4a11-8000-000000000047", "Superman", "Core", null, ExerciseDifficulty.PRINCIPIANTE, "Prono: solleva braccia e gambe contemporaneamente, pausa di 2 secondi."),

        // ---- Cardio & Full Body ----
        Seed("a10c9b1e-1111-4a11-8000-000000000048", "Burpees", "Cardio", null, ExerciseDifficulty.AVANZATO, "Ritmo costante: meglio burpee completi lenti che saltati male."),
        Seed("a10c9b1e-1111-4a11-8000-000000000049", "Jumping jack", "Cardio", null, ExerciseDifficulty.PRINCIPIANTE, "Perfetto per il riscaldamento: 2-3 minuti per alzare la frequenza cardiaca."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004a", "Corsa su tapis roulant", "Cardio", "Tapis roulant (Technogym Run)", ExerciseDifficulty.PRINCIPIANTE, "Non tenere le maniglie, corsa naturale al centro nastro: inclinazione 1-2% per simulare la corsa outdoor, aumenta il ritmo con la velocità non con gli scatti."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004b", "Cyclette", "Cardio", "Bike (Technogym)", ExerciseDifficulty.PRINCIPIANTE, "Regola la sella all'altezza dell'anca: il ginocchio resta leggermente flesso in basso, spinta fluida e resistenza a scatti solo per intervalli."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004c", "Vogatore", "Cardio", "Row (Technogym)", ExerciseDifficulty.INTERMEDIO, "Sequenza gambe-busto-braccia nella spinta, braccia-busto-gambe nel ritorno: schiena neutra, la potenza parte dalle gambe non dalle braccia."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004d", "Salto della corda", "Cardio", "Corda", ExerciseDifficulty.INTERMEDIO, "Salti bassi, polsi che girano: 30 secondi on, 15 off per gli intervalli."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004e", "Kettlebell swing", "Cardio", "Kettlebell", ExerciseDifficulty.AVANZATO, "La spinta parte dall'anca, non dalle braccia: il bilanciere vola fino al petto."),
        Seed("a10c9b1e-1111-4a11-8000-00000000004f", "Wall ball", "Cardio", "Med ball", ExerciseDifficulty.INTERMEDIO, "Squat profondo + lancio al bersaglio: ricevi in ammortizzazione e riparti."),
        Seed("a10c9b1e-1111-4a11-8000-000000000050", "Farmer's walk", "Full Body", "Manubri", ExerciseDifficulty.PRINCIPIANTE, "Spalle indietro e core attivo: cammina eretto con presa salda."),
        Seed("a10c9b1e-1111-4a11-8000-000000000051", "Thruster", "Full Body", "Bilanciere", ExerciseDifficulty.AVANZATO, "Front squat + push press in un solo movimento fluido: esplosivo ma controllato."),
        Seed("a10c9b1e-1111-4a11-8000-000000000052", "Turkish get-up", "Full Body", "Kettlebell", ExerciseDifficulty.AVANZATO, "Dalla posizione sdraiata in piedi con un braccio esteso: lentamente, senza fretta."),
    )

    /** First 20 IDs overlap with the server-side seed: same exercises, now with notes+difficulty. */
    private fun Seed.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        equipment = equipment,
        notes = notes,
        difficulty = difficulty.name,
        isCustom = false,
        syncStatus = SyncStatus.PENDING_CREATE,
    )

    val exercises: List<ExerciseEntity> = seeds.map { it.toEntity() }

    /** Stable ordering for the picker: group → name, so sections never shuffle between visits. */
    val groupOrder = listOf("Petto", "Dorso", "Gambe", "Spalle", "Braccia", "Core", "Cardio", "Full Body")
}
