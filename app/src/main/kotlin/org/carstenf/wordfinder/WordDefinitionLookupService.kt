package org.carstenf.wordfinder

interface WordDefinitionLookupService {
    fun lookupWordDefinition(gameState: GameState, word: String)

    val language: String
}
