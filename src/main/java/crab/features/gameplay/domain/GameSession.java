package crab.features.gameplay.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public final class GameSession {
    private static final int DEFAULT_WIN_THRESHOLD = 1000;
    private static final double CRAB_PEAK_TRIGGER_PERCENT = 0.80;

    private final Map<String, PlayerState> players = new LinkedHashMap<>();
    private final Queue<ActionCard> deck = new ArrayDeque<>();
    private final List<ActionCard> deckTemplate;
    private final Map<String, PlayerAction> pendingActions = new LinkedHashMap<>();
    private final int maxRounds;
    private final int winThreshold;
    private int currentRound = 1;
    private int crabPeakFinalRound = -1;
    private boolean crabPeakActive;
    private GamePhase phase = GamePhase.DEVELOPMENT;
    private WinnerResult winner;

    private GameSession(List<PlayerState> players, int maxRounds, List<ActionCard> deck, int winThreshold) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("A local session requires at least two players");
        }
        if (maxRounds < 1) {
            throw new IllegalArgumentException("Max rounds must be at least 1");
        }
        if (deck.isEmpty()) {
            throw new IllegalArgumentException("Deck cannot be empty");
        }
        if (winThreshold < 1) {
            throw new IllegalArgumentException("Win threshold must be positive");
        }

        for (PlayerState player : players) {
            PlayerState previous = this.players.put(player.id(), player);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate player id: " + player.id());
            }
        }

        this.maxRounds = maxRounds;
        this.winThreshold = winThreshold;
        this.deckTemplate = List.copyOf(deck);
        this.deck.addAll(deck);
    }

    public static GameSession newLocal(List<PlayerState> players, int maxRounds, List<ActionCard> deck) {
        return newLocal(players, maxRounds, deck, DEFAULT_WIN_THRESHOLD);
    }

    public static GameSession newLocal(List<PlayerState> players, int maxRounds, List<ActionCard> deck, int winThreshold) {
        return new GameSession(players, maxRounds, deck, winThreshold);
    }

    public GamePhase phase() {
        return phase;
    }

    public int currentRound() {
        return currentRound;
    }

    public boolean crabPeakActive() {
        return crabPeakActive;
    }

    public Optional<WinnerResult> winner() {
        return Optional.ofNullable(winner);
    }

    public List<PlayerState> players() {
        return List.copyOf(players.values());
    }

    public void resolveDevelopment(Map<String, Integer> selectedCardIndexes, Set<String> upgradeRequests) {
        requirePhase(GamePhase.DEVELOPMENT);
        Objects.requireNonNull(selectedCardIndexes, "selectedCardIndexes");
        Objects.requireNonNull(upgradeRequests, "upgradeRequests");

        for (PlayerState player : players.values()) {
            player.addGold(player.buildLevel().income());
        }

        for (PlayerState player : players.values()) {
            int selectedIndex = selectedCardIndexes.getOrDefault(player.id(), 0);
            List<ActionCard> choices = List.of(drawCard(), drawCard());
            if (selectedIndex < 0 || selectedIndex >= choices.size()) {
                throw new IllegalArgumentException("Selected card index must be 0 or 1 for player: " + player.id());
            }

            player.addCard(choices.get(selectedIndex));
        }

        for (String playerId : upgradeRequests) {
            requirePlayer(playerId).upgradeBuild();
        }

        phase = GamePhase.ACTION;
    }

    public void submitAction(PlayerAction action) {
        requirePhase(GamePhase.ACTION);
        Objects.requireNonNull(action, "action");
        PlayerState actor = requirePlayer(action.playerId());
        if (action.targetPlayerId() != null) {
            requirePlayer(action.targetPlayerId());
        }

        pendingActions.put(actor.id(), action);
        actor.removeCard(action.card());
        if (pendingActions.size() == players.size()) {
            phase = GamePhase.RESOLUTION;
        }
    }

    public void resolveActions() {
        requirePhase(GamePhase.RESOLUTION);
        Map<String, Double> rewardReductions = collectRewardReductions();

        for (PlayerAction action : pendingActions.values()) {
            PlayerState actor = requirePlayer(action.playerId());
            switch (action.card().type()) {
                case HELP -> resolveHelp(actor, action, rewardReductions);
                case STEAL -> resolveSteal(actor, action, rewardReductions);
                case SABOTAGE -> resolveSabotage(actor, action);
            }
        }

        pendingActions.clear();
        phase = GamePhase.EVENT;
    }

    public void applyEvent(GameEvent event) {
        requirePhase(GamePhase.EVENT);
        Objects.requireNonNull(event, "event");
        if (event.targetPlayerId() != null) {
            PlayerState target = requirePlayer(event.targetPlayerId());
            target.addGold(event.goldDelta());
            target.addWealth(event.wealthDelta());
            target.addReputation(event.reputationDelta());
            target.addInfamy(event.infamyDelta());
        }

        phase = GamePhase.ROUND_COMPLETE;
    }

    public void completeRound() {
        requirePhase(GamePhase.ROUND_COMPLETE);
        if (currentRound >= maxRounds || isCompletedCrabPeakFinalRound()) {
            phase = GamePhase.GAME_OVER;
            winner = determineWinner();
            return;
        }

        if (!crabPeakActive && highestScore() >= winThreshold * CRAB_PEAK_TRIGGER_PERCENT) {
            crabPeakActive = true;
            crabPeakFinalRound = currentRound + 1;
        }

        currentRound++;
        phase = GamePhase.DEVELOPMENT;
    }

    private void resolveHelp(PlayerState actor, PlayerAction action, Map<String, Double> rewardReductions) {
        PlayerState target = requireTarget(action);
        int effect = positiveEffect(action.card(), actor);
        int actorReputation = reducePositiveGain(effect, rewardReductions.getOrDefault(actor.id(), 0.0));
        int actorGold = reducePositiveGain(Math.round(effect / 2.0f), rewardReductions.getOrDefault(actor.id(), 0.0));
        int targetWealth = reducePositiveGain(effect, rewardReductions.getOrDefault(target.id(), 0.0));

        actor.addReputation(actorReputation);
        actor.addGold(actorGold);
        target.addWealth(targetWealth);
    }

    private void resolveSteal(PlayerState actor, PlayerAction action, Map<String, Double> rewardReductions) {
        PlayerState target = requireTarget(action);
        int positiveEffect = positiveEffect(action.card(), actor);
        int negativeEffect = negativeEffect(action.card());
        int actorWealth = reducePositiveGain(positiveEffect, rewardReductions.getOrDefault(actor.id(), 0.0));

        actor.addWealth(actorWealth);
        actor.addReputation(-negativeEffect);
        target.addGold(-negativeEffect);
        target.addWealth(-negativeEffect);
    }

    private void resolveSabotage(PlayerState actor, PlayerAction action) {
        requireTarget(action);
        actor.addInfamy(negativeEffect(action.card()));
    }

    private Map<String, Double> collectRewardReductions() {
        Map<String, Double> rewardReductions = new LinkedHashMap<>();
        for (PlayerAction action : pendingActions.values()) {
            if (action.card().type() == CardType.SABOTAGE && action.targetPlayerId() != null) {
                PlayerState actor = requirePlayer(action.playerId());
                double reduction = actor.playerClass() == PlayerClass.SABOTEUR ? 0.70 : 0.50;
                rewardReductions.merge(action.targetPlayerId(), reduction, Math::max);
            }
        }

        return rewardReductions;
    }

    private int positiveEffect(ActionCard card, PlayerState actor) {
        double effect = card.baseEffect() * card.rarity().multiplier() * (1.0 + actor.buildLevel().positiveGainBonus());
        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int negativeEffect(ActionCard card) {
        double effect = card.baseEffect() * card.rarity().multiplier();
        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int reducePositiveGain(int effect, double reduction) {
        return Math.round((float) (effect * (1.0 - reduction)));
    }

    private ActionCard drawCard() {
        if (deck.isEmpty()) {
            deck.addAll(deckTemplate);
        }

        return deck.remove();
    }

    private PlayerState requireTarget(PlayerAction action) {
        if (action.targetPlayerId() == null) {
            throw new IllegalArgumentException("Card requires a target: " + action.card().id());
        }

        return requirePlayer(action.targetPlayerId());
    }

    private PlayerState requirePlayer(String playerId) {
        PlayerState player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Unknown player: " + playerId);
        }

        return player;
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Expected phase " + expected + " but was " + phase);
        }
    }

    private boolean isCompletedCrabPeakFinalRound() {
        return crabPeakActive && currentRound >= crabPeakFinalRound;
    }

    private int highestScore() {
        int highest = 0;
        for (PlayerState player : players.values()) {
            highest = Math.max(highest, player.wealth());
            highest = Math.max(highest, player.reputation());
            highest = Math.max(highest, player.infamy());
        }

        return highest;
    }

    private WinnerResult determineWinner() {
        List<WinnerResult> candidates = new ArrayList<>();
        for (PlayerState player : players.values()) {
            candidates.add(new WinnerResult(player.id(), PlayerClass.OPPORTUNIST, player.wealth()));
            candidates.add(new WinnerResult(player.id(), PlayerClass.ALTRUIST, player.reputation()));
            candidates.add(new WinnerResult(player.id(), PlayerClass.SABOTEUR, player.infamy()));
        }

        WinnerResult best = candidates.get(0);
        for (WinnerResult candidate : candidates) {
            if (candidate.winningValue() > best.winningValue()) {
                best = candidate;
            }
        }

        return best;
    }
}
