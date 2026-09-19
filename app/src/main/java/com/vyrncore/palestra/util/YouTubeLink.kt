package com.vyrncore.palestra.util

import java.net.URLEncoder

/** A YouTube search results URL for an exercise's name - never a specific hardcoded video, so the
 * user picks whichever tutorial they trust instead of us endorsing one channel. */
fun youtubeTutorialSearchUrl(exerciseName: String): String {
    val query = URLEncoder.encode("$exerciseName tutorial esecuzione corretta", "UTF-8")
    return "https://www.youtube.com/results?search_query=$query"
}
