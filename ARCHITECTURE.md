# WordFinder – Architecture Diagram

High-level view of the codebase: packages, main components, and data flow (MVVM + solver + dictionary/definition lookup).

## Package overview

```
org.carstenf.wordfinder
├── GameState (ViewModel), WordFinder (Activity), WordFinderPreferences
├── dictionary/     → Dictionary, WordDefinitionLookup*, WordInfoCache, WordLookupTask
├── gui/            → Fragments, Adapters, LetterButton, Overlays
├── util/           → SolveTask, Result, CountUpTimer, AssetDbOpenHelper
├── letters/        → LetterPicker, LetterDistribution*
└── fireworks/      → FireworksPlayer, FireworkDialog, FireworkView, VictoryFanfarePlayer
```

## Architecture diagram (Mermaid)

```mermaid
flowchart TB
    subgraph UI["UI layer"]
        WF[WordFinder Activity]
        WFS[WordFinderSettingsFragment]
        CRA[ComputerResultListAdapter]
        PLA[Player List Adapter]
        Info[InfoDialogFragment]
        Table[TableDialogFragment]
        Hyper[HyperlinkDialogFragment]
        FireD[FireworkDialog]
    end

    subgraph VM["ViewModel"]
        GS[GameState]
    end

    subgraph Solver["Solver"]
        ST[SolveTask]
    end

    subgraph Dict["Dictionary (word list)"]
        DictDB[(Dictionary + AssetDbOpenHelper)]
    end

    subgraph DefLookup["Definition lookup"]
        WDLM[WordDefinitionLookupManager]
        WIC[WordInfoCache]
        WDLS[WordDefinitionLookupService]
        EngFree[EnglishWordDefinitionLookupServiceFreeApi]
        Eng[EnglishWordDefinitionLookupService]
        Ger[GermanWordDefinitionLookupService]
    end

    subgraph Utils["Utilities"]
        Letters[letters: pickRandomLetter, LetterDistribution*]
        FW[FireworksPlayer / FireworkView]
    end

    %% User → Activity → ViewModel
    WF -->|uses| GS
    WF -->|uses| WDLM
    WF -->|observes LiveData| GS
    WF -->|observes| WDLM

    %% ViewModel dependencies
    GS -->|uses| DictDB
    GS -->|creates & runs| ST
    GS -->|uses| Letters

    %% Solver ↔ GameState, Dictionary
    ST -->|getBoard, findWord, addComputerResults, onSolveFinished| GS
    ST -->|getAllWords(prefix)| DictDB

    %% Definition lookup chain
    WDLM -->|dictionaryName| GS
    WDLM -->|uses| WIC
    WDLM -->|gets service by name| WDLS
    EngFree & Eng & Ger -.->|implement| WDLS

    %% UI bound to ViewModel data
    CRA & PLA -->|bound to LiveData from| GS
    FireD -->|shown by| FW

    %% Styling
    classDef activity fill:#e1f5fe
    classDef viewmodel fill:#fff3e0
    classDef task fill:#e8f5e9
    classDef service fill:#f3e5f5
    class WF activity
    class GS viewmodel
    class ST task
    class WDLM,WDLS,WIC service
```

## Data flow (simplified)

1. **User input**  
   Tap letter / submit guess / shuffle / start → **WordFinder** → **GameState** (`play`, `validatePlayerGuess`, `insertPlayerResult`, `shuffle`, `startSolving`).

2. **Solving**  
   **GameState.startSolving()** creates **SolveTask**, which uses **Dictionary.getAllWords(prefix)** and **GameState.findWord()**, then calls **GameState.addComputerResults()** and **onSolveFinished()**.

3. **UI updates**  
   **GameState** updates `computerResultList`, `playerResultList`, `timerCurrentValue`, `gameLifecycleState` (LiveData). **WordFinder** observes these and updates **ComputerResultListAdapter**, player list, timer, and game-state UI.

4. **Definition lookup**  
   User taps result → **WordFinder** → **WordDefinitionLookupManager.wordDefinitionLookup()** → **WordInfoCache** or **WordDefinitionLookupService** → LiveData `wordLookupResult` / `wordLookupError` → **WordFinder** shows **InfoDialogFragment** / **TableDialogFragment** / **HyperlinkDialogFragment**.

## Key files (from CLAUDE.md)

| Area        | File |
|------------|------|
| Core model | `GameState.kt` |
| Solver     | `util/SolveTask.kt` |
| Dictionary | `dictionary/Dictionary.kt`, `AssetDbOpenHelper` |
| Definitions| `EnglishWordDefinitionLookupServiceFreeApi.kt`, `WordDefinitionLookupManager` |
| UI         | `ComputerResultListAdapter.kt`, fragments in `gui/` |
