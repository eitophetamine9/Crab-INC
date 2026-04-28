package crab.features.gameplay.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameSessionTest {
    private static final ActionCard HELP = new ActionCard("help-common", "Helpful Tide", CardType.HELP, CardRarity.COMMON, 10);
    private static final ActionCard STEAL = new ActionCard("steal-common", "Pearl Snatch", CardType.STEAL, CardRarity.COMMON, 10);
    private static final ActionCard SABOTAGE = new ActionCard("sabotage-common", "Murky Water", CardType.SABOTAGE, CardRarity.COMMON, 10);
    private static final ActionCard RARE_HELP = new ActionCard("help-rare", "Treasure Aid", CardType.HELP, CardRarity.RARE, 10);

    @Test
    void developmentGrantsIncomeDrawsChosenCardsAndUpgradesRequestedBuilds() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        alice.addGold(40);

        GameSession session = GameSession.newLocal(
                List.of(alice, bob),
                3,
                List.of(HELP, STEAL, SABOTAGE, RARE_HELP)
        );

        session.resolveDevelopment(Map.of("alice", 1, "bob", 0), Set.of("alice"));

        assertEquals(GamePhase.ACTION, session.phase());
        assertEquals(10, alice.gold());
        assertEquals(BuildLevel.TWO, alice.buildLevel());
        assertEquals(List.of(STEAL), alice.hand());
        assertEquals(10, bob.gold());
        assertEquals(BuildLevel.ONE, bob.buildLevel());
        assertEquals(List.of(SABOTAGE), bob.hand());
    }

    @Test
    void actionsAreHiddenUntilAllPlayersLockInThenResolveTogether() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        bob.addGold(40);
        bob.addWealth(20);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, HELP, STEAL));
        session.resolveDevelopment(Map.of("alice", 1, "bob", 0), Set.of());

        session.submitAction(new PlayerAction("alice", STEAL, "bob"));

        assertEquals(GamePhase.ACTION, session.phase());
        assertEquals(50, bob.gold());
        assertEquals(20, bob.wealth());

        session.submitAction(new PlayerAction("bob", HELP, "alice"));

        assertEquals(GamePhase.RESOLUTION, session.phase());

        session.resolveActions();

        assertEquals(GamePhase.EVENT, session.phase());
        assertEquals(20, alice.wealth());
        assertEquals(-10, alice.reputation());
        assertEquals(45, bob.gold());
        assertEquals(10, bob.wealth());
        assertEquals(10, bob.reputation());
    }

    @Test
    void sabotageReducesTargetsPositiveRewardsDuringTheSameResolution() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.SABOTEUR);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(SABOTAGE, HELP, HELP, STEAL));
        session.resolveDevelopment(Map.of("alice", 0, "bob", 0), Set.of());

        session.submitAction(new PlayerAction("alice", SABOTAGE, "bob"));
        session.submitAction(new PlayerAction("bob", HELP, "alice"));
        session.resolveActions();

        assertEquals(10, alice.infamy());
        assertEquals(10, alice.wealth());
        assertEquals(3, bob.reputation());
        assertEquals(12, bob.gold());
    }

    @Test
    void eventPhaseAppliesEventThenRoundCompletionCanTriggerCrabPeak() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        alice.addWealth(80);
        alice.addGold(30);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, SABOTAGE, RARE_HELP), 100);
        session.resolveDevelopment(Map.of("alice", 0, "bob", 0), Set.of());
        session.submitAction(new PlayerAction("alice", HELP, "bob"));
        session.submitAction(new PlayerAction("bob", HELP, "alice"));
        session.resolveActions();

        session.applyEvent(GameEvent.crabHunt("alice", 15));

        assertEquals(GamePhase.ROUND_COMPLETE, session.phase());
        assertEquals(30, alice.gold());

        session.completeRound();

        assertTrue(session.crabPeakActive());
        assertEquals(2, session.currentRound());
        assertEquals(GamePhase.DEVELOPMENT, session.phase());
    }

    @Test
    void gameEndsAfterFinalRoundAndReportsDominantStatWinner() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.SABOTEUR);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        PlayerState cara = PlayerState.create("cara", "Cara", PlayerClass.OPPORTUNIST);
        alice.addInfamy(30);
        bob.addReputation(60);
        cara.addWealth(40);

        GameSession session = GameSession.newLocal(List.of(alice, bob, cara), 1, List.of(HELP, STEAL, SABOTAGE, RARE_HELP));

        session.resolveDevelopment(Map.of("alice", 0, "bob", 0, "cara", 0), Set.of());
        session.submitAction(new PlayerAction("alice", HELP, "bob"));
        session.submitAction(new PlayerAction("bob", HELP, "cara"));
        session.submitAction(new PlayerAction("cara", HELP, "alice"));
        session.resolveActions();
        session.applyEvent(GameEvent.none());
        session.completeRound();

        Optional<WinnerResult> winner = session.winner();

        assertEquals(GamePhase.GAME_OVER, session.phase());
        assertTrue(winner.isPresent());
        assertEquals("bob", winner.orElseThrow().playerId());
        assertEquals(PlayerClass.ALTRUIST, winner.orElseThrow().winningClass());
        assertEquals(70, winner.orElseThrow().winningValue());
    }

    @Test
    void gameDoesNotReportWinnerBeforeGameOver() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, SABOTAGE, RARE_HELP));

        assertFalse(session.winner().isPresent());
    }
}
