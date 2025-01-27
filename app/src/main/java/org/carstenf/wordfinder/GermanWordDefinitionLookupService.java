package org.carstenf.wordfinder;

public class GermanWordDefinitionLookupService implements WordDefinitionLookupService {

    @Override
    public void lookupWordDefinition(GameState gameState, String word) {
        String lowercaseWord= word.toLowerCase();
        String capitalizedWord = Character.toUpperCase(lowercaseWord.charAt(0)) + lowercaseWord.substring(1);
        String searchTerm = capitalizedWord + "|" + lowercaseWord  ;

        WiktionaryLookup wiktionaryLookup = new WiktionaryLookup();
        wiktionaryLookup.getMeaningAsync( searchTerm, meaning -> {
            if (meaning != null  && !meaning.isBlank()) {
                gameState.processWordLookupResult(new WordInfo(word, getLanguage(), word+":\n" + meaning, null));
            } else {
                gameState.processWordLookupError(word, getLanguage(),
                        "Definition not found for: " + word);
            }
        });
    }

    @Override
    public String getLanguage() {
        return "D";
    }
}
