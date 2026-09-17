package com.vyrncore.palestra.data.local

/** Tracks whether a locally-held row still needs to be pushed to Supabase. */
enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}
