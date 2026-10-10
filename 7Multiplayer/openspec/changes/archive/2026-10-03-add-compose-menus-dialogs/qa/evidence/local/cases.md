| ID | Plan | Capability | Class | Case | Result | Seconds |
|---|---|---|---|---|---|---|
| UT-001 | QA01 | harness/build | com.edu.unal.tictactoe.ExampleUnitTest | addition_isCorrect | PASS | 0.001 |
| UT-002 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.DifficultyPolicyTest | hardDoesNotDeliberatelyBlockAndUsesTheRandomizerWhenItCannotWin | PASS | 0.007 |
| UT-003 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.DifficultyPolicyTest | easyAndHardUseTheRandomizerWhenNoTacticsExist | PASS | 0.001 |
| UT-004 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.DifficultyPolicyTest | everyDifficultyKeepsTheTerminalGuardsWithoutUsingTheRandomizer | PASS | 0.001 |
| UT-005 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.DifficultyPolicyTest | easyUsesTheRandomizerEvenWhenTacticsAreAvailable | PASS | 0.0 |
| UT-006 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.DifficultyPolicyTest | hardTakesItsImmediateWinBeforeConsultingTheRandomizer | PASS | 0.0 |
| UT-007 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossibleExhaustiveTest | impossibleNeverLosesWhenHumanXStarts | PASS | 0.019 |
| UT-008 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossibleExhaustiveTest | impossibleNeverLosesWhenComputerOStarts | PASS | 0.003 |
| UT-009 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleOnAnAlreadyLostBoardReturnsTheOptimalMoveWithoutPromisingARescue | PASS | 0.001 |
| UT-010 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleEmptyBoardOpensAtTheLowestIndexForADraw | PASS | 0.005 |
| UT-011 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleDefendsTheForkWithAnEdge | PASS | 0.0 |
| UT-012 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleAgainstCornerOpeningTakesTheCenter | PASS | 0.004 |
| UT-013 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleAgainstCenterOpeningTakesTheLowestCornerToHoldTheDraw | PASS | 0.002 |
| UT-014 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.ImpossiblePolicyTest | impossibleCompletesItsOwnWinningLine | PASS | 0.0 |
| UT-015 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | computerBlocksTheFirstHumanThreatWhenItCannotWin | PASS | 0.0 |
| UT-016 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | boardSnapshotsDoNotExposeTheGameContainer | PASS | 0.001 |
| UT-017 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | fullBoardWithoutAWinningLineIsATie | PASS | 0.0 |
| UT-018 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | occupiedCellsRejectBothPlayersWithoutChangingTheBoard | PASS | 0.0 |
| UT-019 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | invalidLocationsDoNotChangeTheBoard | PASS | 0.001 |
| UT-020 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | clearBoardLeavesTheBoardEmptyAndIsIdempotent | PASS | 0.0 |
| UT-021 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | appliedMovesPlaceTheCorrespondingMark | PASS | 0.0 |
| UT-022 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | randomSelectionUsesEachControlledFreeListIndexWithoutMutatingTheBoard | PASS | 0.0 |
| UT-023 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | omittedDifficultyMatchesExpertAndRepeatedQueriesDoNotMutateTheBoard | PASS | 0.0 |
| UT-024 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | clearBoardResetsOccupiedAndFinishedGamesAndAllowsMovesAgain | PASS | 0.0 |
| UT-025 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | selectedMovesAreValidFreeLocationsForEmptyOccupiedAndLastFreeBoards | PASS | 0.0 |
| UT-026 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | markSymbolsMatchTheDomainContract | PASS | 0.0 |
| UT-027 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | everyWinningLineWinsForBothPlayersOnlyOnTheThirdMark | PASS | 0.002 |
| UT-028 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | fullWinningBoardIsFinishedBeforeNoMovesAndDoesNotInvokeRandomizer | PASS | 0.0 |
| UT-029 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | computerChoosesTheFirstImmediateWinInBoardOrder | PASS | 0.0 |
| UT-030 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | terminalWinnerOrTieRejectsMovesWithoutChangingTheBoard | PASS | 0.0 |
| UT-031 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | fullTieReturnsNoMovesWithoutProbingOrInvokingRandomizer | PASS | 0.0 |
| UT-032 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | oldSnapshotsRemainStableAfterMovesAndClearBoard | PASS | 0.0 |
| UT-033 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | terminalWinnerWithFreeCellsDoesNotProbeOrInvokeRandomizer | PASS | 0.001 |
| UT-034 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | computerChoosesItsWinBeforeBlockingAWinWithALowerIndex | PASS | 0.0 |
| UT-035 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | initialBoardHasNineEmptyCells | PASS | 0.0 |
| UT-036 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | checkingTheWinnerDoesNotMutateTheBoard | PASS | 0.0 |
| UT-037 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | computerBlocksTheFirstOfMultipleHumanThreats | PASS | 0.0 |
| UT-038 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.TicTacToeGameTest | aWinningLastCellTakesPriorityOverTieForBothPlayers | PASS | 0.0 |
| UT-039 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.models.GameModelContractsTest | difficultyLevelsKeepTheirExactNamesAndStableOrder | PASS | 0.001 |
| UT-040 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.models.GameModelContractsTest | resultsKeepTheirAssociatedValuesAndRemainDistinct | PASS | 0.0 |
| UT-041 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.models.GameModelContractsTest | playerAndMarkSymbolsMatchTheGameContract | PASS | 0.0 |
| UT-042 | QA03 | specs/tic-tac-toe-game | com.edu.unal.tictactoe.game.models.GameModelContractsTest | scoreDefaultsToZeroAndCopyDoesNotMutateTheOriginal | PASS | 0.0 |
| UT-043 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.DifficultyLabelsTest | mapsEveryDifficultyToItsSpanishLabel | PASS | 0.0 |
| UT-044 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.DifficultyLabelsTest | keepsTheLabelsInTheDifficultyOrder | PASS | 0.0 |
| UT-045 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.DifficultyLabelsTest | usesADistinctLabelResourcePerDifficulty | PASS | 0.001 |
| UT-046 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | newGameConservesDifficultyAndScoreAndLocksDifficultyOnlyForTheOOpening | PASS | 0.005 |
| UT-047 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | easyDifficultyIsForwardedToTheEngineOnEveryComputerTurn | PASS | 0.0 |
| UT-048 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | aStaleRequestCapturedWhileEditableIsRejectedOnceTheGameIsActive | PASS | 0.0 |
| UT-049 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | setDifficultyAcceptsTheCurrentLevelWhenEditablePublishingTheSameValues | PASS | 0.0 |
| UT-050 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | aNewSessionStartsInExpertWithAnEditableEmptyBoard | PASS | 0.0 |
| UT-051 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | setDifficultyBeforeTheFirstMoveAcceptsPublishesAndDoesNotMoveOrScore | PASS | 0.0 |
| UT-052 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | difficultyIsPreservedAcrossTurnsAndTheTerminalPublication | PASS | 0.0 |
| UT-053 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | openingMoveUsesTheCanonicalDifficultyAndAvoidsTheRandomizerForImpossible | PASS | 0.002 |
| UT-054 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | setDifficultyAfterTerminalKeepsBoardResultAndScore | PASS | 0.0 |
| UT-055 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | selectingImpossibleDoesNotLeakIntoANewViewModelSession | PASS | 0.0 |
| UT-056 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | setDifficultyDuringAnActiveGameIsRejectedWithoutPublishingOrMoving | PASS | 0.0 |
| UT-057 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelDifficultyTest | difficultyGateFollowsTheDerivedStateThroughPlayAndTerminal | PASS | 0.0 |
| UT-058 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | newGamesPreserveScoresAcrossTerminalGamesAndKeepOldSnapshotsIntact | PASS | 0.001 |
| UT-059 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | incompleteGamesDoNotUpdateAnyScore | PASS | 0.0 |
| UT-060 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | humanFillingTheLastCellWithNoWinProducesATieWithoutComputerMove | PASS | 0.0 |
| UT-061 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | newGameAlternatesAfterAbandoningActiveXAndActiveOOpening | PASS | 0.0 |
| UT-062 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | newGamesAlternateOpenersAndRepeatedReadsDoNotStartAnotherGame | PASS | 0.0 |
| UT-063 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | repeatedStateReadsReturnTheSameSnapshotWithoutStartingOrScoring | PASS | 0.0 |
| UT-064 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | invalidAndOccupiedLocationsAreExactNoOpsWithoutInvokingComputer | PASS | 0.0 |
| UT-065 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | legalHumanMovePlacesXAndExactlyOneComputerMoveReturnsToHuman | PASS | 0.0 |
| UT-066 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | initialStateStartsWithAnEmptyBoardAndZeroScore | PASS | 0.0 |
| UT-067 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | stateFlowIsReadOnlyToConsumers | PASS | 0.0 |
| UT-068 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | humanVictoryIsDetectedBeforeComputerCanMove | PASS | 0.001 |
| UT-069 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | independentViewModelStartsWithAZeroScoreAndX | PASS | 0.0 |
| UT-070 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | humanInputDuringComputerTurnIsRejectedReentrantlyBeforeComputerMove | PASS | 0.0 |
| UT-071 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | initializationClearsAnInjectedGameWithoutCallingTheRandomizer | PASS | 0.0 |
| UT-072 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.GameViewModelTest | computerVictoryLeavesComputerTurnAndDoesNotEnableHumanMoves | PASS | 0.0 |
| UT-073 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | addingDifficultyKeepsTheBoardSnapshotDefensiveAndImmutable | PASS | 0.0 |
| UT-074 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | defaultsToExpertDifficulty | PASS | 0.0 |
| UT-075 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | difficultyIsEditableAfterATerminalResultEvenWithAFullBoard | PASS | 0.0 |
| UT-076 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | explicitDifficultyIsExposedWithoutChangingTheOtherFields | PASS | 0.001 |
| UT-077 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | difficultyIsLockedOnAnActiveBoardWithAtLeastOneMark | PASS | 0.0 |
| UT-078 | QA04/QA07 | specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateDifficultyTest | difficultyIsEditableOnAnInitialEmptyInProgressBoard | PASS | 0.0 |
| UT-079 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateTest | boardMustContainExactlyNineMarks | PASS | 0.0 |
| UT-080 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateTest | snapshotComposesTheDomainModels | PASS | 0.001 |
| UT-081 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateTest | originalBoardMutationDoesNotChangeTheSnapshot | PASS | 0.0 |
| UT-082 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateTest | defaultsRepresentTheFirstHumanStartedSession | PASS | 0.0 |
| UT-083 | QA05/QA09 | specs/game-session + specs/tic-tac-toe-game + specs/game-options | com.edu.unal.tictactoe.ui.game.models.GameUiStateTest | exposedBoardCannotBeMutated | PASS | 0.0 |
