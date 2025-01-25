package org.carstenf.wordfinder;

import android.app.Activity;
import android.content.Context;

public interface WordDefinitionLookupService {
    void lookupWordDefinition(WordFinder wordFinderApp, String string);
}
