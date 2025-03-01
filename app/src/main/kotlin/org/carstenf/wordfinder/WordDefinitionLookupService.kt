/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

interface WordDefinitionLookupService {
    fun lookupWordDefinition(gameState: GameState, task: WordLookupTask)

    val language: String
}
