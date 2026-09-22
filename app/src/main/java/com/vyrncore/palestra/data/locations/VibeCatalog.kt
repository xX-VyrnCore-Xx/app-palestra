package com.vyrncore.palestra.data.locations

/** A club amenity, rendered as a small icon+label chip on the club card. Modeled as an enum
 * (not a free string) so the UI can map each one to a fixed icon without guessing. */
enum class ClubAmenity(val label: String) {
    OPEN_24_7("Aperta 24/7"),
    POOL("Piscina"),
    PARKING("Parcheggio"),
    AIR_CONDITIONING("Climatizzata"),
    JIU_JITSU("Area Jiu Jitsu"),
    HYROX("Zona Hyrox"),
    BEACH_VOLLEY("Beach volley"),
    OUTDOOR_TERRACE("Terrazza esterna"),
    UNLIMITED_CLASSES("Corsi illimitati"),
}

data class VibeClub(
    val name: String,
    val region: String,
    val highlight: String,
    val amenities: List<ClubAmenity>,
) {
    /** Query text for the "open in maps" intent - a name search resolves to the real place
     * without needing exact geocoded coordinates for all 17 clubs. */
    val mapsQuery: String get() = "ViBE Fitness $name"
}

enum class CourseIntensity(val label: String) {
    BASSA("Bassa"),
    MEDIA("Media"),
    ALTA("Alta"),
}

data class VibeCourse(val name: String, val intensity: CourseIntensity)

/** Real ViBE Fitness club roster and group-class catalog, sourced from vibefitness.it - static
 * content bundled with the app (like the exercise catalog) rather than a backend table: it almost
 * never changes, isn't user-generated, and a table+RLS+sync round trip for 17 rows would only add
 * backend surface area for no benefit. A new club opening is already an app-update event. */
object VibeCatalog {
    val clubs = listOf(
        VibeClub(
            "Alpignano", "Torino",
            "In via Cavour, al Centro Commerciale La Torre: comoda da Pianezza, Caselette, Collegno, Rivoli, San Gillio e Val della Torre.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.PARKING, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Varedo", "Monza e Brianza",
            "Sulla Milano-Meda: comoda da Limbiate, Nova Milanese, Bovisio Masciago, Cesano Maderno, Desio e Milano.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.JIU_JITSU, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Sesto San Giovanni", "Milano",
            "Comoda per tutta la zona nord di Milano: Cinisello Balsamo, Bresso, Cologno Monzese, Bicocca, Precotto.",
            listOf(ClubAmenity.POOL, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Seregno", "Monza e Brianza",
            "Zona residenziale vicino al centro: comoda da Desio, Giussano, Meda, Lissone, Carate Brianza, Seveso.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.PARKING, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Paderno Dugnano Calderara", "Milano",
            "Sul lato ovest della ferrovia: comoda da Bollate, Senago, Novate Milanese, Garbagnate, Limbiate.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.HYROX, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Milano Certosa", "Milano",
            "Zona Certosa, comoda per tutta Milano nord-ovest: Quarto Oggiaro, Villapizzone, Bovisa, Pero, Rho.",
            listOf(ClubAmenity.PARKING, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Lentate sul Seveso", "Monza e Brianza",
            "Sulla Nazionale dei Giovi: comoda da Seveso, Barlassina, Cesano Maderno, Meda, Lazzate, Cermenate.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Erba", "Como",
            "Riferimento per tutta la zona erbese: Albavilla, Canzo, Asso, Merone, Eupilio, lago di Pusiano.",
            listOf(ClubAmenity.OUTDOOR_TERRACE, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Desio", "Monza e Brianza",
            "Stile old school su due livelli, attrezzatura Panatta: comoda da Nova Milanese, Lissone, Seregno.",
            listOf(ClubAmenity.PARKING, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Como", "Como",
            "Zona Valleggio, comoda per chi vive la città di giorno: Camerlata, Lipomo, Tavernerio, Cernobbio.",
            listOf(ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Busnago", "Monza e Brianza",
            "Bacino ampio tra zone commerciali e direttrici: comoda da Trezzo sull'Adda, Vimercate, Cornate d'Adda.",
            listOf(ClubAmenity.AIR_CONDITIONING, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Besana in Brianza", "Monza e Brianza",
            "Spazi luminosi, punto di riferimento per tutto il territorio: Carate Brianza, Triuggio, Casatenovo.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Barlassina", "Monza e Brianza",
            "Spazi ampi, zona funzionale e campo beach volley gratuito: Seveso, Meda, Lentate, Cesano Maderno.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.BEACH_VOLLEY, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Arosio", "Como",
            "Zona di passaggio tra Brianza e Comasco: Giussano, Mariano Comense, Inverigo, Carugo, Novedrate.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Arcore", "Monza e Brianza",
            "Presidio dell'est Brianza: comoda da Villasanta, Lesmo, Usmate Velate, Vimercate, Concorezzo.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Albese con Cassano", "Como",
            "Vista montagne e sala corsi enorme: comoda da Albavilla, Como, Tavernerio, Alzate Brianza, Cantù.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.UNLIMITED_CLASSES),
        ),
        VibeClub(
            "Paderno Dugnano Comasina", "Milano",
            "Pensata per il quartiere: comoda dal centro di Dugnano, Palazzolo Milanese, Cusano Milanino, Bresso.",
            listOf(ClubAmenity.OPEN_24_7, ClubAmenity.PARKING, ClubAmenity.UNLIMITED_CLASSES),
        ),
    )

    val regions: List<String> = clubs.map { it.region }.distinct().sorted()

    val courses = listOf(
        VibeCourse("Stretching & Meditazione", CourseIntensity.BASSA),
        VibeCourse("Postural Yoga", CourseIntensity.BASSA),
        VibeCourse("Yoga Dolce", CourseIntensity.BASSA),
        VibeCourse("Yoga del Risveglio", CourseIntensity.BASSA),
        VibeCourse("Flexibility", CourseIntensity.BASSA),
        VibeCourse("Salsa", CourseIntensity.MEDIA),
        VibeCourse("Afrostep Coreografico", CourseIntensity.MEDIA),
        VibeCourse("Heels", CourseIntensity.MEDIA),
        VibeCourse("Zumba Fitness", CourseIntensity.MEDIA),
        VibeCourse("Yoga Dinamico", CourseIntensity.MEDIA),
        VibeCourse("Salsa Base", CourseIntensity.MEDIA),
        VibeCourse("Hybrid Workout", CourseIntensity.MEDIA),
        VibeCourse("Fitness Dance", CourseIntensity.MEDIA),
        VibeCourse("Fitball Training", CourseIntensity.MEDIA),
        VibeCourse("Body Pump", CourseIntensity.ALTA),
        VibeCourse("Abs", CourseIntensity.ALTA),
        VibeCourse("Pole Dance Base", CourseIntensity.ALTA),
        VibeCourse("HIIT Functional", CourseIntensity.ALTA),
        VibeCourse("Difesa Personale", CourseIntensity.ALTA),
        VibeCourse("Boxe", CourseIntensity.ALTA),
    )
}
