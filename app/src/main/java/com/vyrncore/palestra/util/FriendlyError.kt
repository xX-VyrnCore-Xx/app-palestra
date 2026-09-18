package com.vyrncore.palestra.util

import com.vyrncore.palestra.BuildConfig

/**
 * Turns a raw exception into something an end user can act on, in Italian - never the raw
 * message. A Ktor exception's message dumps the full request (URL, headers, including the
 * Authorization bearer token and apikey), so surfacing it as-is would both look unprofessional
 * and leak credentials on screen.
 */
fun friendlyError(e: Throwable): String {
    if (BuildConfig.SUPABASE_URL.isBlank()) {
        return "Questa build non è collegata al backend (credenziali Supabase mancanti). Contatta chi ha generato questo APK."
    }
    val message = e.message.orEmpty()
    // IllegalStateException from our own repositories (kotlin's error(...)) already carries a
    // curated, Italian, safe-to-show message - e.g. the ai-chat Edge Function's own error field.
    // Everything else may be a raw Ktor/network exception, whose message dumps the full request
    // (URL, headers, Authorization bearer token included), so it's never shown as-is.
    if (e is IllegalStateException && message.isNotBlank()) return message
    return when {
        message.contains("Failed to connect", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ->
            "Impossibile connettersi al server. Controlla la connessione internet e riprova."
        message.contains("Invalid login credentials", ignoreCase = true) -> "Email o password non corretti."
        message.contains("already registered", ignoreCase = true) || message.contains("already exists", ignoreCase = true) ->
            "Esiste già un account con questa email."
        else -> "Si è verificato un problema, riprova tra poco."
    }
}
