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

public final class GameSession implements java.io.Serializable {
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
    private boolean charityWaveActive = false;
    private GamePhase phase = GamePhase.DEVELOPMENT;
    private WinnerResult winner;

    private final Map<String, List<ActionCard>> currentDrafts = new LinkedHashMap<>();

    private GameSession(List<PlayerState> players, int maxRounds, List<ActionCard> deck, int winThreshold) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("A local session requires at least two players");
        }
        if (maxRounds < 1) {
            throw new IllegalArgumentException("Max rounds must be at least 1");
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
        prepareDrafts();
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

    public Map<String, PlayerAction> pendingActions() {
        return Map.copyOf(pendingActions);
    }

    public Map<String, List<ActionCard>> currentDrafts() {
        return Map.copyOf(currentDrafts);
    }

    private void prepareDrafts() {
        currentDrafts.clear();
        for (PlayerState player : players.values()) {
            if (player.hand().size() < PlayerState.MAX_HAND_SIZE) {
                List<ActionCard> draft = new ArrayList<>();
                draft.add(drawCard(player));
                draft.add(drawCard(player));
                currentDrafts.put(player.id(), draft);
            }
        }
    }

    public void resolveDevelopment(Map<String, Integer> selectedCardIndexes, Set<String> upgradeRequests) {
        requirePhase(GamePhase.DEVELOPMENT);
        Objects.requireNonNull(selectedCardIndexes, "selectedCardIndexes");
        Objects.requireNonNull(upgradeRequests, "upgradeRequests");

        for (PlayerState player : players.values()) {
            player.addClams(player.income()); // Income based on build level
        }

        for (Map.Entry<String, List<ActionCard>> entry : currentDrafts.entrySet()) {
            String playerId = entry.getKey();
            int selectedIndex = selectedCardIndexes.getOrDefault(playerId, 0);
            if (selectedIndex >= 0 && selectedIndex < entry.getValue().size()) {
                requirePlayer(playerId).addCard(entry.getValue().get(selectedIndex));
            }
        }

        for (String playerId : upgradeRequests) {
            requirePlayer(playerId).upgradeBuild();
        }

        currentDrafts.clear();
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
        if (!action.card().id().equals("dummy")) {
            actor.removeCard(action.card());
        }
        if (pendingActions.size() == players.size()) {
            phase = GamePhase.RESOLUTION;
        }
    }

    public void resolveActions() {
        requirePhase(GamePhase.RESOLUTION);
        Map<String, Double> rewardReductions = collectRewardReductions();

        Set<String> doubleNegativeTargets = new java.util.HashSet<>();
        Map<String, List<String>> altruistLinks = new java.util.HashMap<>();
        Map<String, Integer> opportunistActors = new java.util.HashMap<>();

        // 1. Process Signature Cards first
        for (PlayerAction action : pendingActions.values()) {
            PlayerState actor = requirePlayer(action.playerId());
            
            // Hand management: 
            // If hand is full (3) and player played/passed without keeping a specific card, reset.
            if (actor.hand().size() >= 3 && action.keptCard() == null) {
                actor.clearHand();
            } else if (action.keptCard() != null) {
                // If they chose a specific card to keep, clear and add ONLY that one
                actor.clearHand();
                actor.addCard(action.keptCard());
            }
            // Otherwise, they keep what they have minus what they played (which was removed in submitAction)

            if (action.card().type() == CardType.SIGNATURE_SABOTEUR) {
                if (action.targetPlayerId() != null) doubleNegativeTargets.add(action.targetPlayerId());
            } else if (action.card().type() == CardType.SIGNATURE_ALTRUIST) {
                if (action.targetPlayerId() != null) altruistLinks.computeIfAbsent(action.targetPlayerId(), k -> new ArrayList<>()).add(actor.id());
            } else if (action.card().type() == CardType.SIGNATURE_OPPORTUNIST) {
                opportunistActors.put(actor.id(), 0);
            }
        }

        // 2. Process Base Cards
        for (PlayerAction action : pendingActions.values()) {
            PlayerState actor = requirePlayer(action.playerId());
            switch (action.card().type()) {
                case HELP -> {
                    PlayerState target = requireTarget(action);
                    int actorRepGain = calculatePositiveEffect(40, action.card().rarity(), actor, CardType.HELP);
                    int actorClamsGain = calculatePositiveEffect(15, action.card().rarity(), actor, CardType.HELP);
                    int targetWealthGain = calculatePositiveEffect(30, action.card().rarity(), target, CardType.HELP);

                    actorRepGain = reduceGain(actorRepGain, rewardReductions.getOrDefault(actor.id(), 0.0));
                    actorClamsGain = reduceGain(actorClamsGain, rewardReductions.getOrDefault(actor.id(), 0.0));
                    targetWealthGain = reduceGain(targetWealthGain, rewardReductions.getOrDefault(target.id(), 0.0));

                    applyReputationGain(actor, actorRepGain, opportunistActors);
                    actor.addClams(actorClamsGain);
                    applyWealthGain(target, targetWealthGain, altruistLinks);
                }
                case STEAL -> {
                    PlayerState target = requireTarget(action);
                    int actorWealthGain = calculatePositiveEffect(45, action.card().rarity(), actor, CardType.STEAL);
                    actorWealthGain = reduceGain(actorWealthGain, rewardReductions.getOrDefault(actor.id(), 0.0));

                    int actorRepLoss = calculateNegativeEffect(10, action.card().rarity(), doubleNegativeTargets.contains(actor.id()));
                    int targetClamsLoss = calculateNegativeEffect(35, action.card().rarity(), doubleNegativeTargets.contains(target.id()));
                    int targetWealthLoss = calculateNegativeEffect(10, action.card().rarity(), doubleNegativeTargets.contains(target.id()));

                    applyWealthGain(actor, actorWealthGain, altruistLinks);
                    actor.addReputation(-actorRepLoss);
                    target.deductClams(targetClamsLoss);
                    target.addWealth(-targetWealthLoss);
                }
                case SABOTAGE -> {
                    requireTarget(action);
                    int actorInfamyGain = calculateNegativeEffect(50, action.card().rarity(), false);
                    actor.addInfamy(actorInfamyGain);
                }
            }
        }

        pendingActions.clear();
        phase = GamePhase.EVENT;
    }

    private void applyWealthGain(PlayerState target, int amount, Map<String, List<String>> altruistLinks) {
        if (amount > 0) {
            target.addWealth(amount);
            List<String> links = altruistLinks.get(target.id());
            if (links != null) {
                for (String altruistId : links) {
                    requirePlayer(altruistId).addWealth(amount / 2);
                }
            }
        }
    }

    private void applyReputationGain(PlayerState target, int amount, Map<String, Integer> opportunistActors) {
        if (amount > 0) {
            target.addReputation(amount);
            for (Map.Entry<String, Integer> entry : opportunistActors.entrySet()) {
                String oppId = entry.getKey();
                if (!oppId.equals(target.id())) {
                    int triggers = entry.getValue();
                    if (triggers < 5) {
                        requirePlayer(oppId).addWealth(10);
                        opportunistActors.put(oppId, triggers + 1);
                    }
                }
            }
        }
    }

    public void applyEvent(GameEvent event) {
        requirePhase(GamePhase.EVENT);
        Objects.requireNonNull(event, "event");
        if (event.targetPlayerId() != null) {
            PlayerState target = requirePlayer(event.targetPlayerId());
            applyEventToPlayer(target, event);
        } else if (!event.name().equals("Calm Current") && !event.name().equals("Travelling Shop") && !event.name().equals("Charity Wave")) {
            // Apply to everyone
            for (PlayerState player : players.values()) {
                applyEventToPlayer(player, event);
            }
        }
        
        // Special case for Charity Wave: Set a flag for next round
        if (event.name().equals("Charity Wave")) {
            charityWaveActive = true;
        } else {
            charityWaveActive = false; // Reset if not Charity Wave
        }

        phase = GamePhase.ROUND_COMPLETE;
    }

    private void applyEventToPlayer(PlayerState target, GameEvent event) {
        target.addClams(event.clamsDelta());
        target.addWealth(event.wealthDelta());
        target.addReputation(event.reputationDelta());
        target.addInfamy(event.infamyDelta());
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
        prepareDrafts();
    }

    private Map<String, Double> collectRewardReductions() {
        Map<String, Double> rewardReductions = new LinkedHashMap<>();
        for (PlayerAction action : pendingActions.values()) {
            if (action.card().type() == CardType.SABOTAGE && action.targetPlayerId() != null) {
                PlayerState actor = requirePlayer(action.playerId());
                double reduction = actor.playerClass() == PlayerClass.SABOTEUR ? 0.70 : 0.50;
                rewardReductions.merge(action.targetPlayerId(), reduction, (a, b) -> Math.min(0.70, a + b));
            }
        }

        return rewardReductions;
    }

    private int calculatePositiveEffect(int base, CardRarity rarity, PlayerState actor, CardType cardType) {
        double buildBonus = actor.statBonus();
        double classBonus = 0.0;
        if ((actor.playerClass() == PlayerClass.ALTRUIST && cardType == CardType.HELP) ||
            (actor.playerClass() == PlayerClass.OPPORTUNIST && cardType == CardType.STEAL) ||
            (actor.playerClass() == PlayerClass.SABOTEUR && cardType == CardType.SABOTAGE)) {
            classBonus = 0.15;
        }
        
        if (charityWaveActive && cardType == CardType.HELP) {
            classBonus += 0.25; // Bonus from Charity Wave
        }

        double effect = base * rarity.multiplier() * (1.0 + buildBonus + classBonus);

        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int calculateNegativeEffect(int base, CardRarity rarity, boolean doubleNegative) {
        double effect = base * rarity.multiplier();
        if (crabPeakActive) {
            effect *= 2.0;
        }
        if (doubleNegative) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int reduceGain(int effect, double reduction) {
        return Math.round((float) (effect * (1.0 - reduction)));
    }

    private ActionCard drawCard(PlayerState player) {
        if (!deck.isEmpty()) {
            return deck.remove();
        }
        if (!deckTemplate.isEmpty()) {
            deck.addAll(deckTemplate);
            return deck.remove();
        }
        return generateCard(player);
    }

    public ActionCard generateCard(PlayerState player) {
        java.util.Random random = new java.util.Random();
        double roll = random.nextDouble();
        CardType generatedType;
        if (roll < 0.05) { // 5% Signature
            generatedType = switch (player.playerClass()) {
                case ALTRUIST -> CardType.SIGNATURE_ALTRUIST;
                case OPPORTUNIST -> CardType.SIGNATURE_OPPORTUNIST;
                case SABOTEUR -> CardType.SIGNATURE_SABOTEUR;
            };
        } else if (roll < 0.70) { // 65% Matching Class
            generatedType = switch (player.playerClass()) {
                case ALTRUIST -> CardType.HELP;
                case OPPORTUNIST -> CardType.STEAL;
                case SABOTEUR -> CardType.SABOTAGE;
            };
        } else { // 30% Off-Class
            CardType[] offClasses = switch (player.playerClass()) {
                case ALTRUIST -> new CardType[]{CardType.STEAL, CardType.SABOTAGE};
                case OPPORTUNIST -> new CardType[]{CardType.HELP, CardType.SABOTAGE};
                case SABOTEUR -> new CardType[]{CardType.HELP, CardType.STEAL};
            };
            generatedType = offClasses[random.nextInt(offClasses.length)];
        }

        double rarityRoll = random.nextDouble();
        CardRarity rarity;
        if (rarityRoll < 0.50) {
            rarity = CardRarity.COMMON;
        } else if (rarityRoll < 0.80) {
            rarity = CardRarity.UNCOMMON;
        } else if (rarityRoll < 0.95) {
            rarity = CardRarity.RARE;
        } else {
            rarity = CardRarity.EPIC;
        }

        String id = java.util.UUID.randomUUID().toString();
        String name = rarity.name().substring(0, 1) + rarity.name().substring(1).toLowerCase() + " " +
                      generatedType.name().replace("SIGNATURE_", "Sig: ");
        return new ActionCard(id, name, generatedType, rarity);
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
