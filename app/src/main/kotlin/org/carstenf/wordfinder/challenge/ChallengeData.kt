/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.challenge

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.carstenf.wordfinder.GameState
import java.security.MessageDigest
import java.util.Locale

/**
 * Payload for sharing a game challenge. Contains board, settings, timer info,
 * and hashes of sender-found words (no plain text).
 */
data class ChallengeData(
    @JsonProperty("v") val version: Int = 1,
    @JsonProperty("b") val board: String,
    @JsonProperty("d") val dictionaryName: String,
    @JsonProperty("t3") val isAllow3LetterWords: Boolean,
    @JsonProperty("s") val scoring: String,
    @JsonProperty("r") val letterSelector: String,
    @JsonProperty("a") val autoAddPrefixalWords: Boolean,
    @JsonProperty("m") val timerMode: String,
    @JsonProperty("cd") val countDownStartTimeMs: Long,
    @JsonProperty("st") val senderTimeSeconds: Long,
    @JsonProperty("h") val senderFoundWordHashes: List<String>
) {
    companion object {
        private const val CHALLENGE_MIME_TYPE = "application/vnd.wordfinder.challenge+json"
        private const val FILE_EXTENSION = ".wfchallenge"
        private const val FILE_NAME_PREFIX = "WordFinder-Challenge"

        private val objectMapper = ObjectMapper()
        private val sha256 = MessageDigest.getInstance("SHA-256")

        /**
         * Computes SHA-256 hash of normalized word (uppercase) for challenge matching.
         * Used so sender's words are not shared in plain text.
         */
        fun hashWord(word: String): String {
            val normalized = word.uppercase(Locale.getDefault())
            val bytes = sha256.digest(normalized.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun toJson(challenge: ChallengeData): String = objectMapper.writeValueAsString(challenge)
        fun fromJson(json: String): ChallengeData = objectMapper.readValue(json, ChallengeData::class.java)

        fun getMimeType(): String = CHALLENGE_MIME_TYPE
        fun getFileExtension(): String = FILE_EXTENSION
        fun getFileNamePrefix(): String = FILE_NAME_PREFIX
    }
}
