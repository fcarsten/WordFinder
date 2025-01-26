package org.carstenf.wordfinder;

import android.widget.Toast;

public class GermanWordDefinitionLookupService implements WordDefinitionLookupService {

    @Override
    public void lookupWordDefinition(WordFinder wordFinderApp, String word) {
        String lowercaseWord= word.toLowerCase();
        String capitalizedWord = Character.toUpperCase(lowercaseWord.charAt(0)) + lowercaseWord.substring(1);
        String searchTerm = capitalizedWord + "|" + lowercaseWord  ;

        WiktionaryLookup wiktionaryLookup = new WiktionaryLookup();
        wiktionaryLookup.getMeaningAsync( searchTerm, meaning -> {
            if (meaning != null  && !meaning.isBlank()) {
                wordFinderApp.displayWordDefinition(word+":\n" + meaning);
            } else {
                wordFinderApp.displayToast("Definition not found for: " + word, Toast.LENGTH_SHORT);
            }
        });
    }
}
