package com.brentvatne.common.api

import androidx.media3.common.C
import androidx.media3.common.Format

/*
* internal representation of audio & text tracks
*/
class Track {
    var title: String? = null
    var mimeType: String? = null
    var language: String? = null
    var isSelected = false

    // in bps available only on audio tracks
    var bitrate = 0
    var index = 0

    // Aligned with the fields of Shaka's AudioTrack / TextTrack
    var isPrimary = false
    var roles: List<String> = emptyList()
    var accessibilityPurpose: String? = null

    /**
     * Fills [isPrimary], [roles] and [accessibilityPurpose] from the ExoPlayer format.
     * - primary: SELECTION_FLAG_DEFAULT, set by ExoPlayer from HLS `DEFAULT=YES` and DASH `Role main`
     * - roles: ExoPlayer role flags mapped to DASH role values (urn:mpeg:dash:role:2011),
     *   same vocabulary exposed on iOS
     */
    fun setRoleInfo(format: Format) {
        isPrimary = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0

        val trackRoles = ROLE_FLAGS.filter { (flag, _) -> (format.roleFlags and flag) != 0 }.map { it.second }.toMutableList()
        if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) {
            trackRoles.add("forced-subtitle")
        }
        roles = trackRoles
        accessibilityPurpose = getAccessibilityPurpose(trackRoles)
    }

    companion object {
        private val ROLE_FLAGS = listOf(
            C.ROLE_FLAG_MAIN to "main",
            C.ROLE_FLAG_ALTERNATE to "alternate",
            C.ROLE_FLAG_SUPPLEMENTARY to "supplementary",
            C.ROLE_FLAG_COMMENTARY to "commentary",
            C.ROLE_FLAG_DUB to "dub",
            C.ROLE_FLAG_EMERGENCY to "emergency",
            C.ROLE_FLAG_CAPTION to "caption",
            C.ROLE_FLAG_SUBTITLE to "subtitle",
            C.ROLE_FLAG_SIGN to "sign",
            C.ROLE_FLAG_DESCRIBES_VIDEO to "description",
            C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND to "describes-music-and-sound",
            C.ROLE_FLAG_ENHANCED_DIALOG_INTELLIGIBILITY to "enhanced-audio-intelligibility",
            C.ROLE_FLAG_TRANSCRIBES_DIALOG to "transcribes-dialog",
            C.ROLE_FLAG_EASY_TO_READ to "easyreader"
        )

        /** Same values as Shaka's `shaka.media.ManifestParser.AccessibilityPurpose`. */
        fun getAccessibilityPurpose(roles: List<String>): String? =
            when {
                roles.contains("description") -> "visually impaired"
                roles.contains("enhanced-audio-intelligibility") -> "hard of hearing"
                else -> null
            }
    }
}
