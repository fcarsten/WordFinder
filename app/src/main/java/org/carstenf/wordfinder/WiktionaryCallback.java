package org.carstenf.wordfinder;

@FunctionalInterface
public interface WiktionaryCallback {
    void onResult(String meaning);
}
