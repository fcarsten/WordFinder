package org.carstenf.wordfinder

interface WordDefinitionLookupService {
    fun lookupWordDefinition(gameState: GameState, task: WordLookupTask)

    val language: String
}
