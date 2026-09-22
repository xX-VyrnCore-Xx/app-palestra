package com.vyrncore.palestra.util

import java.net.URLEncoder

/** The premium equipment brands a gym exercise is best performed on, with a link to the
 * manufacturer's official page so the allievo can see the exact machine before the workout. */
enum class MachineBrand(val displayName: String, val siteUrl: String) {
    TECHNOGYM("Technogym", "https://www.technogym.com/it-IT/"),
    PANATTA("Panatta", "https://www.panattasport.com/en/"),
}

/** Everything the exercise sheet needs to show a demo video and the recommended machine. */
data class ExerciseMedia(
    /** Demo video URL: a verified official video when one exists, otherwise a curated YouTube
     * search that includes the machine brand, so results show the exact equipment in use. */
    val videoUrl: String,
    /** True only for a hand-verified official tutorial ([verifiedVideos]); false means [videoUrl]
     * is a search results page, not a specific video - the UI must label these differently so
     * "VIDEO TUTORIAL" never overpromises a search link as a checked demo. */
    val isVerified: Boolean,
    /** Brand of the machine this exercise is best performed on, null for free-weight/bodyweight. */
    val machineBrand: MachineBrand?,
    /** What the machine is called in the brand's catalog, shown on the exercise sheet. */
    val machineName: String?,
)

private fun ytSearch(query: String): String =
    "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")

/** Verified official videos (channel checked at link time). Keyed by a lowercase fragment of
 * the exercise name; matched with [contains], first match wins. */
private val verifiedVideos: List<Pair<String, String>> = listOf(
    // Technogym official equipment tutorials (YouTube, verified Sep 2026)
    "chest press" to "https://www.youtube.com/watch?v=n1Dyy3De1Rc",
    "lat machine" to "https://www.youtube.com/watch?v=N_kTW-coueo", // lat pulldown
    "pulley basso" to "https://www.youtube.com/watch?v=vY1-i6sAOMc", // low row PureStrength
    "shoulder press" to "https://www.youtube.com/watch?v=c3Z8NXdcvEs", // Shoulder Press (neutral grip)
    "military press" to "https://www.youtube.com/watch?v=c3Z8NXdcvEs", // same machine pattern
    "vertical traction" to "https://www.youtube.com/watch?v=db_BT6FyTtE",
    "arm curl" to "https://www.youtube.com/watch?v=VV8Q9kb5dPw",
    "curl su panca scott" to "https://www.youtube.com/watch?v=VV8Q9kb5dPw", // arm curl machine pattern
    // Panatta official product videos (YouTube, verified Sep 2026)
    "leg press" to "https://www.youtube.com/watch?v=4xU_AKXz1q8", // Super Leg Press 45°
    "panca inclinata" to "https://www.youtube.com/watch?v=Vvz1iys3IF8", // Inclined Chest Press FitEvo
    "french press" to "https://www.youtube.com/watch?v=HP_WrZildms", // French Press FW ONE
    "leg curl" to "https://www.youtube.com/watch?v=8joxx9xnMzM", // Dual Leg Extension | Seated Leg Curling
    "leg extension" to "https://www.youtube.com/watch?v=8joxx9xnMzM", // same dual machine
    "hack squat" to "https://www.youtube.com/watch?v=lLpe1tTcW0A", // Super Squat machine walkthrough
    "croci" to "https://www.youtube.com/watch?v=JHvxKMAHUH4", // Standing Multi Flight FitEvo
)

/** YouTube thumbnail for a watch URL (maxres might not exist; hq does for every video). */
fun youtubeThumbnailUrl(videoUrl: String): String? {
    val id = Regex("v=([A-Za-z0-9_-]+)").find(videoUrl)?.groupValues?.get(1) ?: return null
    return "https://img.youtube.com/vi/$id/hqdefault.jpg"
}

/** Machine-brand recommendation per equipment label: the catalog seed now names the exact
 * brand in the label ("Macchina (Panatta Leg Press 45°)") and that wins outright; for rows
 * from an older seed, generic equipment names fall back to per-name heuristics. */
private fun brandForEquipment(equipment: String?, name: String): MachineBrand? {
    val n = name.lowercase()
    val e = equipment?.lowercase() ?: return null
    if (e.contains("technogym")) return MachineBrand.TECHNOGYM
    if (e.contains("panatta")) return MachineBrand.PANATTA
    return when {
        e.contains("macchina") || e.contains("lat machine") -> {
            if (n.contains("leg press") || n.contains("hack squat") || n.contains("leg curl") ||
                n.contains("leg extension") || n.contains("polpacci")
            ) MachineBrand.PANATTA else MachineBrand.TECHNOGYM
        }
        e.contains("cavi") -> MachineBrand.TECHNOGYM
        e.contains("tapis") || e.contains("cyclette") || e.contains("vogatore") -> MachineBrand.TECHNOGYM
        else -> null
    }
}

private fun machineNameFor(name: String): String? {
    val n = name.lowercase()
    return when {
        n.contains("chest press") || n.contains("panca piana macchinari") -> "Chest Press"
        n.contains("lat machine") -> "Lat Machine"
        n.contains("pulley") -> "Low Row"
        n.contains("leg press") -> "Leg Press 45°"
        n.contains("hack squat") -> "Hack Squat"
        n.contains("leg curl") -> "Leg Curl"
        n.contains("leg extension") -> "Leg Extension"
        n.contains("polpacci") -> "Calf Machine"
        n.contains("tapis") -> "Run:"
        n.contains("cyclette") -> "Bike"
        n.contains("vogatore") -> "Row"
        n.contains("panca inclinata") -> "Inclined Chest Press"
        n.contains("french press") -> "French Press Machine"
        n.contains("military press") || n.contains("shoulder press") || n.contains("push press") -> "Shoulder Press"
        n.contains("arm curl") || n.contains("curl su panca scott") -> "Arm Curl"
        n.contains("croci") -> "Multi Flight"
        else -> null
    }
}

/** Resolves the demo video and recommended machine for a catalog exercise. Matched by name
 * (not ID) so it also works for rows already in the database from an older seed. */
fun exerciseMedia(name: String, equipment: String?): ExerciseMedia {
    val lower = name.lowercase()
    val brand = brandForEquipment(equipment, name)

    // A verified official video wins when one exists for this exercise.
    val verified = verifiedVideos.firstOrNull { (fragment, _) -> lower.contains(fragment) }?.second

    val videoUrl = verified ?: when (brand) {
        MachineBrand.TECHNOGYM -> ytSearch("$name technogym tutorial esecuzione")
        MachineBrand.PANATTA -> ytSearch("$name panatta tutorial esecuzione")
        null -> ytSearch("$name tutorial tecnica esecuzione esercizio")
    }

    // "Macchina (Panatta Leg Press 45°)" -> "Panatta Leg Press 45°": the seed's parenthesised
    // label is the most precise machine name we have, the name-based table is the fallback.
    val machineLabel = Regex("\\((.+)\\)").find(equipment ?: "")?.groupValues?.get(1)

    return ExerciseMedia(
        videoUrl = videoUrl,
        isVerified = verified != null,
        machineBrand = brand,
        machineName = (machineLabel ?: machineNameFor(name))?.let { mn ->
            brand?.takeIf { !mn.contains(it.displayName, ignoreCase = true) }?.let { b -> "${b.displayName} $mn" } ?: mn
        },
    )
}
