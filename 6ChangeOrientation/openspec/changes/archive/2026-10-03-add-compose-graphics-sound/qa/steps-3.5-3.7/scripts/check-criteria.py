#!/usr/bin/env python3
"""Verify each scoped criterion against fresh saved XML (groups may overlap)."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

qa = Path(__file__).resolve().parents[1]
groups = {
    "QA01": ["Lifecycle.pausedInitiallyForBothResponseAndOpening"],
    "QA02": ["CoroutineHarness.acceptedXPublishesComputerTurnAndWaitsWithoutAcceptingMoreHumanInput", "CoroutineHarness.OOpeningUsesSameOneSecondWaitAndIsCancelledByNewXGame"],
    "QA03": ["CoroutineHarness.earlySubMillisecondWakeIsRoundedUpAndNeverMovesBeforeMonotonicDeadline", "CoroutineHarness.coroutineDispatchDelayConsumesTimeFromAcceptanceBeforeSchedulingDelay"],
    "QA04": ["Lifecycle.pauseRetainsTimeAndRepeatedResumeDoesNotResetIt"],
    "QA05": ["Lifecycle.repeatedPauseDoesNotOverConsumeRemainingTime"],
    "QA06": ["Lifecycle.zeroRemainingFiresOnceAndRemainsStableAcrossPauseAndResume"],
    "QA07": ["Lifecycle.newGameWhilePausedInvalidatesPendingAndRotationUsesSameViewModel"],
    "QA08": ["Lifecycle.clearingViewModelStoreCancelsQueuedDeadlineAndLeavesStateStable"],
    "QA09": ["CoroutineHarness.startingANewGameCancelsThePendingResponseJob", "CoroutineHarness.newOpeningDoesNotRunAtDeadlineOfCancelledResponse", "CoroutineHarness.newGameAtOldDeadlineCancelsCallbackNotYetDispatched"],
    "QA10": ["Difficulty.staleEasySelectionIsDiscardedAfterReentrantDifficultyChange", "Difficulty.reentrantNewGamesDiscardTheOldOpeningSelectionAndKeepTheNewOpeningDeadline"],
    "QA11": ["Lifecycle.pauseDuringSynchronousSelectionDiscardsItAndResumeCompletesExactlyOnce"],
    "QA12": ["Difficulty.difficultyChangedAt400MillisecondsDoesNotRestartOpeningDeadline", "Difficulty.openingMoveUsesTheCanonicalDifficultyAndAvoidsTheRandomizerForImpossible"],
    "QA13": ["Difficulty.setDifficultyDuringAnActiveGameIsRejectedWithoutPublishingOrMoving", "Difficulty.aStaleRequestCapturedWhileEditableIsRejectedOnceTheGameIsActive"],
    "QA14": [".humanVictoryIsDetectedBeforeComputerCanMove", ".humanFillingTheLastCellWithNoWinProducesATieWithoutComputerMove", ".computerVictoryLeavesComputerTurnAndDoesNotEnableHumanMoves", "CoroutineHarness.terminalHumanWinAndTieNeverSelectOrScheduleAComputerMove", ".newGamesPreserveScoresAcrossTerminalGamesAndKeepOldSnapshotsIntact"],
    "QA15": ["Sound.mutedPreferenceSurvivesAcceptedComputerAndTerminalHumanMoves", "Sound.soundTogglesDuringOpeningWaitPreserveStateAndOriginalDeadline", "Sound.mutePersistsAcrossNewGamesMovesAndDifficultyChangesWithoutChangingOtherState"],
}
cases = {}
for path in (qa / "evidence/jvm-xml").glob("TEST-*.xml"):
    suite = ET.parse(path).getroot()
    for case in suite.findall("testcase"):
        key = suite.get("name").rsplit(".", 1)[-1] + "." + case.get("name")
        status = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"
        cases[key] = {"test": key, "status": status, "xml": "evidence/jvm-xml/" + path.name}
results = {}
for criterion, selectors in groups.items():
    selected = []
    for selector in selectors:
        short_class, test = selector.split(".", 1)
        key = "GameViewModel" + short_class + "Test." + test
        selected.append(cases.get(key, {"test": key, "status": "MISSING"}))
    results[criterion] = {"status": "PASS" if all(case["status"] == "PASS" for case in selected) else "FAIL", "tests": len(selected), "cases": selected}
results.update({criterion: {"status": "BLOCKED", "tests": 0, "reason": "No Android device; JVM cannot validate Activity/UI integration"} for criterion in ("QA16", "QA17", "QA18")})
(qa / "evidence/criterion-results.json").write_text(json.dumps(results, indent=2) + "\n")
for criterion, result in results.items():
    print(criterion, result["status"], result["tests"])
raise SystemExit(0 if all(results[criterion]["status"] == "PASS" for criterion in groups) else 1)
