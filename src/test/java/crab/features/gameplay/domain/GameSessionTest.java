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
    private static final ActionCard HELP = new ActionCard("help-common", "Helpful Tide", CardType.HELP, CardRarity.COMMON);
    private static final ActionCard STEAL = new ActionCard("steal-common", "Pearl Snatch", CardType.STEAL, CardRarity.COMMON);
    private static final ActionCard SABOTAGE = new ActionCard("sabotage-common", "Murky Water", CardType.SABOTAGE, CardRarity.COMMON);
    private static final ActionCard RARE_HELP = new ActionCard("help-rare", "Treasure Aid", CardType.HELP, CardRarity.RARE);

    @Test
    void developmentGrantsIncomeDrawsChosenCardsAndUpgradesRequestedBuilds() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        alice.addClams(40);

        GameSession session = GameSession.newLocal(
                List.of(alice, bob),
                3,
                List.of(HELP, STEAL, SABOTAGE, RARE_HELP)
        );

        session.resolveDevelopment(Map.of("alice", 1, "bob", 0), Set.of("alice"));

        assertEquals(GamePhase.ACTION, session.phase());
        assertEquals(10, alice.clams());
        assertEquals(2, alice.buildLevel());
        assertEquals(List.of(STEAL), alice.hand());
        assertEquals(10, bob.clams());
        assertEquals(1, bob.buildLevel());
        assertEquals(List.of(SABOTAGE), bob.hand());
    }

    @Test
    void actionsAreHiddenUntilAllPlayersLockInThenResolveTogether() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        bob.addClams(40);
        bob.addWealth(20);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, HELP, STEAL));
        session.resolveDevelopment(Map.of("alice", 1, "bob", 0), Set.of());

        session.submitAction(new PlayerAction("alice", STEAL, "bob", null));

        assertEquals(GamePhase.ACTION, session.phase());
        assertEquals(50, bob.clams());
        assertEquals(20, bob.wealth());

        session.submitAction(new PlayerAction("bob", HELP, "alice", null));

        assertEquals(GamePhase.RESOLUTION, session.phase());

        session.resolveActions();

        assertEquals(GamePhase.EVENT, session.phase());
        assertEquals(82, alice.wealth());
        assertEquals(-10, alice.reputation());
        assertEquals(32, bob.clams());
        assertEquals(10, bob.wealth());
        assertEquals(46, bob.reputation());
    }

    @Test
    void sabotageReducesTargetsPositiveRewardsDuringTheSameResolution() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.SABOTEUR);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(SABOTAGE, HELP, HELP, STEAL));
        session.resolveDevelopment(Map.of("alice", 0, "bob", 0), Set.of());

        session.submitAction(new PlayerAction("alice", SABOTAGE, "bob", null));
        session.submitAction(new PlayerAction("bob", HELP, "alice", null));
        session.resolveActions();

        assertEquals(50, alice.infamy());
        assertEquals(30, alice.wealth());
        assertEquals(14, bob.reputation());
        assertEquals(15, bob.clams());
    }

    @Test
    void eventPhaseAppliesEventThenRoundCompletionCanTriggerCrabPeak() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);
        alice.addWealth(80);
        alice.addClams(30);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, SABOTAGE, RARE_HELP), 100);
        session.resolveDevelopment(Map.of("alice", 0, "bob", 0), Set.of());
        session.submitAction(new PlayerAction("alice", HELP, "bob", null));
        session.submitAction(new PlayerAction("bob", HELP, "alice", null));
        session.resolveActions();

        session.applyEvent(GameEvent.crabHunt("alice", 15));

        assertEquals(GamePhase.ROUND_COMPLETE, session.phase());
        assertEquals(40, alice.clams());

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
        session.submitAction(new PlayerAction("alice", HELP, "bob", null));
        session.submitAction(new PlayerAction("bob", HELP, "cara", null));
        session.submitAction(new PlayerAction("cara", HELP, "alice", null));
        session.resolveActions();
        session.applyEvent(GameEvent.none());
        session.completeRound();

        Optional<WinnerResult> winner = session.winner();

        assertEquals(GamePhase.GAME_OVER, session.phase());
        assertTrue(winner.isPresent());
        assertEquals("bob", winner.orElseThrow().playerId());
        assertEquals(PlayerClass.ALTRUIST, winner.orElseThrow().winningClass());
        assertEquals(106, winner.orElseThrow().winningValue());
    }

    @Test
    void gameDoesNotReportWinnerBeforeGameOver() {
        PlayerState alice = PlayerState.create("alice", "Alice", PlayerClass.OPPORTUNIST);
        PlayerState bob = PlayerState.create("bob", "Bob", PlayerClass.ALTRUIST);

        GameSession session = GameSession.newLocal(List.of(alice, bob), 3, List.of(HELP, STEAL, SABOTAGE, RARE_HELP));

        assertFalse(session.winner().isPresent());
    }
}
