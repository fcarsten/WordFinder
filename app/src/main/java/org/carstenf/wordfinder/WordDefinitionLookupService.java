package org.carstenf.wordfinder;

public interface WordDefinitionLookupService {
    void lookupWordDefinition(GameState gameState, String string);

    String getLanguage();
}
