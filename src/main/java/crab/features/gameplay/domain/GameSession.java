package crab.features.gameplay.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
    private GamePhase phase = GamePhase.DEVELOPMENT;
    private WinnerResult winner;
    private final Random rng = new Random();
    // Tracks how many Opportunist Signature triggers fired this round
    private int opportunistSigTriggersThisRound = 0;

    private boolean charityWaveActive = false;
    private boolean nextRoundCharityWave = false;
    private boolean travellingShopActive = false;

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

        // Each player starts with 1 card initially
        for (PlayerState player : players) {
            player.addCard(drawCard());
        }
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

    public boolean isTravellingShopActive() {
        return travellingShopActive;
    }

    public void buyRareCard(String playerId) {
        if (!travellingShopActive) return;
        PlayerState p = requirePlayer(playerId);
        if (p.clams() >= 50) {
            p.deductClams(50);
            p.addCard(drawRareCard());
        }
    }

    private ActionCard drawRareCard() {
        return deckTemplate.stream()
                .filter(c -> c.rarity() == CardRarity.RARE)
                .findFirst().orElse(drawCard());
    }

    public void resolveDevelopment(Map<String, Integer> selectedCardIndexes, Set<String> upgradeRequests) {
        requirePhase(GamePhase.DEVELOPMENT);
        Objects.requireNonNull(selectedCardIndexes, "selectedCardIndexes");
        Objects.requireNonNull(upgradeRequests, "upgradeRequests");

        for (PlayerState player : players.values()) {
            player.addClams(player.income()); // Income based on build level
        }

        for (String playerId : upgradeRequests) {
            requirePlayer(playerId).upgradeBuild();
        }

        phase = GamePhase.DRAWING;
    }

    public void resolveDrawing() {
        requirePhase(GamePhase.DRAWING);
        for (PlayerState player : players.values()) {
            player.addCard(drawCard());
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
        // Signature SABOTEUR: mark targets that receive double-negative treatment
        Set<String> doubleNegativeTargets = collectDoubleNegativeTargets();
        opportunistSigTriggersThisRound = 0;

        // First pass: resolve Signature SABOTEUR actions (per spec: Signatures resolve first)
        for (PlayerAction action : pendingActions.values()) {
            if (action.card().type() == CardType.SIGNATURE_SABOTEUR) {
                PlayerState actor = requirePlayer(action.playerId());
                resolveSabotage(actor, action);
            }
        }

        // Second pass: resolve all remaining actions
        for (PlayerAction action : pendingActions.values()) {
            PlayerState actor = requirePlayer(action.playerId());
            
            // Hand management rule:
            // Calculate starting hand size correctly (Pass card is not from hand)
            int startHandSize = actor.hand().size() + (action.card().id().equals("dummy") ? 0 : 1);
            if (startHandSize >= 3) {
                actor.clearHand();
                if (action.keptCard() != null) {
                    actor.addCard(action.keptCard());
                }
            }
            // else: keep existing hand (already done as we don't clear it)

            switch (action.card().type()) {
                case HELP             -> resolveHelp(actor, action, rewardReductions, doubleNegativeTargets, false);
                case SIGNATURE_ALTRUIST -> resolveHelp(actor, action, rewardReductions, doubleNegativeTargets, true);
                case STEAL            -> resolveSteal(actor, action, rewardReductions, doubleNegativeTargets, false);
                case SIGNATURE_OPPORTUNIST -> resolveSteal(actor, action, rewardReductions, doubleNegativeTargets, true);
                case SABOTAGE         -> resolveSabotage(actor, action);
                case SIGNATURE_SABOTEUR -> { /* already handled in first pass */ }
            }
        }

        pendingActions.clear();
        
        // Events trigger every 3 rounds (3, 6, 9, 12, 15)
        if (currentRound % 3 == 0) {
            phase = GamePhase.EVENT;
        } else {
            phase = GamePhase.ROUND_COMPLETE;
        }
    }

    public void applyEvent(GameEvent event) {
        requirePhase(GamePhase.EVENT);
        Objects.requireNonNull(event, "event");
        
        if ("Charity Wave".equals(event.name())) {
            nextRoundCharityWave = true;
        } else if ("Travelling Shop".equals(event.name())) {
            travellingShopActive = true;
        } else if (event.targetPlayerId() != null) {
            PlayerState target = requirePlayer(event.targetPlayerId());
            target.addClams(event.clamsDelta());
            target.addWealth(event.wealthDelta());
            target.addReputation(event.reputationDelta());
            target.addInfamy(event.infamyDelta());
        }

        phase = GamePhase.ROUND_COMPLETE;
    }

    /**
     * Applies an event's deltas to ALL players simultaneously (e.g. Market Crash).
     * Transitions phase to ROUND_COMPLETE after application.
     */
    public void applyEventToAll(GameEvent event) {
        requirePhase(GamePhase.EVENT);
        Objects.requireNonNull(event, "event");
        for (PlayerState p : players.values()) {
            p.addClams(event.clamsDelta());
            p.addWealth(event.wealthDelta());
            p.addReputation(event.reputationDelta());
            p.addInfamy(event.infamyDelta());
        }
        phase = GamePhase.ROUND_COMPLETE;
    }

    public void completeRound() {
        requirePhase(GamePhase.ROUND_COMPLETE);
        
        // Reset/Update per-round event states
        charityWaveActive = nextRoundCharityWave;
        nextRoundCharityWave = false;
        travellingShopActive = false; 
        // User says "buy one Rare card using Clams". Usually implies during the event phase.

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

    /**
     * Help card: +40 Rep + +15 Clams (actor), +30 Wealth (target).
     * Signature Altruist: actor also gains 50% of the Wealth the target receives.
     */
    private void resolveHelp(PlayerState actor, PlayerAction action,
                             Map<String, Double> rewardReductions,
                             Set<String> doubleNegativeTargets,
                             boolean isSignature) {
        PlayerState target = requireTarget(action);
        int actorReputationGain = calculatePositiveEffect(40, action.card().rarity(), actor);
        int actorClamsGain      = calculatePositiveEffect(15, action.card().rarity(), actor);
        int targetWealthGain   = calculatePositiveEffect(30, action.card().rarity(), target);

        // Charity Wave Bonus: Help cards give bonus Reputation (+20) next round
        if (charityWaveActive) {
            actorReputationGain += 20;
        }

        // Apply Saboteur distortion reductions
        actorReputationGain = reduceGain(actorReputationGain, rewardReductions.getOrDefault(actor.id(), 0.0));
        actorClamsGain       = reduceGain(actorClamsGain,       rewardReductions.getOrDefault(actor.id(), 0.0));
        targetWealthGain    = reduceGain(targetWealthGain,    rewardReductions.getOrDefault(target.id(), 0.0));

        // Double-negative from Signature Saboteur (target receives doubled damage)
        if (doubleNegativeTargets.contains(target.id())) {
            targetWealthGain = Math.max(0, targetWealthGain / 2);
        }

        actor.addReputation(actorReputationGain);
        actor.addClams(actorClamsGain);
        target.addWealth(targetWealthGain);

        // Signature Altruist: actor gains 50% of the Wealth given to the target
        if (isSignature) {
            int altruistBonus = Math.round(targetWealthGain * 0.5f);
            actor.addWealth(altruistBonus);
        }

        // Opportunist Signature passive: +10 Wealth for any Opportunist who sees an opponent gain Rep
        triggerOpportunistPassive(actor.id(), actorReputationGain);
    }

    /**
     * Steal card: +45 Wealth (actor), -10 Rep (actor), -35 Clams (target).
     * Signature Opportunist: in addition, actor gains +10 Wealth whenever an opponent gains Reputation
     * (already handled via opportunist passive trigger — here we apply the base steal as normal).
     */
    private void resolveSteal(PlayerState actor, PlayerAction action,
                              Map<String, Double> rewardReductions,
                              Set<String> doubleNegativeTargets,
                              boolean isSignature) {
        PlayerState target = requireTarget(action);
        int actorWealthGain = calculatePositiveEffect(45, action.card().rarity(), actor);
        actorWealthGain = reduceGain(actorWealthGain, rewardReductions.getOrDefault(actor.id(), 0.0));

        int actorReputationLoss = calculateNegativeEffect(10, action.card().rarity());
        int targetClamsLoss      = calculateNegativeEffect(35, action.card().rarity());

        // Double-negative from Signature Saboteur: target loses twice as much Clams
        if (doubleNegativeTargets.contains(target.id())) {
            targetClamsLoss *= 2;
        }

        actor.addWealth(actorWealthGain);
        actor.addReputation(-actorReputationLoss);
        target.deductClams(targetClamsLoss);
    }

    /**
     * Sabotage card: +50 Infamy (actor), 50% reward reduction on target this round.
     * Signature Saboteur: target takes double negative effects for 1 round (collected separately).
     */
    private void resolveSabotage(PlayerState actor, PlayerAction action) {
        requireTarget(action); // validates target exists
        int actorInfamyGain = calculateNegativeEffect(50, action.card().rarity());
        actor.addInfamy(actorInfamyGain);
    }

    /**
     * Opportunist Signature Passive: any Opportunist-class player gains +10 Wealth
     * whenever an opponent gains Reputation, up to 5 triggers per round.
     */
    private void triggerOpportunistPassive(String reputationGainerId, int repGained) {
        if (repGained <= 0) return;
        for (PlayerState p : players.values()) {
            if (p.playerClass() == PlayerClass.OPPORTUNIST
                    && !p.id().equals(reputationGainerId)
                    && opportunistSigTriggersThisRound < 5) {
                // Only triggers if Opportunist had a SIGNATURE_OPPORTUNIST action this round
                boolean hasOpportunistSig = pendingActions.values().stream()
                        .anyMatch(a -> a.playerId().equals(p.id())
                                && a.card().type() == CardType.SIGNATURE_OPPORTUNIST);
                if (hasOpportunistSig) {
                    p.addWealth(10);
                    opportunistSigTriggersThisRound++;
                }
            }
        }
    }

    private Map<String, Double> collectRewardReductions() {
        Map<String, Double> rewardReductions = new LinkedHashMap<>();
        for (PlayerAction action : pendingActions.values()) {
            CardType ct = action.card().type();
            if ((ct == CardType.SABOTAGE || ct == CardType.SIGNATURE_SABOTEUR) && action.targetPlayerId() != null) {
                PlayerState actor = requirePlayer(action.playerId());
                // Saboteur class passive: 70% max; otherwise base 50%
                double reduction = actor.playerClass() == PlayerClass.SABOTEUR ? 0.70 : 0.50;
                rewardReductions.merge(action.targetPlayerId(), reduction, (a, b) -> Math.min(0.70, a + b));
            }
        }
        return rewardReductions;
    }

    /** Collects the set of player IDs that are targeted by a SIGNATURE_SABOTEUR this round. */
    private Set<String> collectDoubleNegativeTargets() {
        Set<String> targets = new java.util.HashSet<>();
        for (PlayerAction action : pendingActions.values()) {
            if (action.card().type() == CardType.SIGNATURE_SABOTEUR && action.targetPlayerId() != null) {
                targets.add(action.targetPlayerId());
            }
        }
        return targets;
    }

    /**
     * Selects an event using hostility-weighted RNG.
     * TargetWeight = (Infamy / TotalInfamy) * 100. Higher infamy = more likely to be targeted.
     */
    public GameEvent selectWeightedEvent() {
        requirePhase(GamePhase.EVENT);
        
        // Choose event type (Round 3, 6, 9, 12, 15)
        int roll = rng.nextInt(100);
        if (roll < 25) {
            // Market Crash: All lose Wealth
            return GameEvent.marketCrash(30 + rng.nextInt(21)); // 30-50 wealth
        } else if (roll < 50) {
            // Charity Wave: Next round Rep bonus
            return GameEvent.charityWave();
        } else if (roll < 75) {
            // Crab Hunt: Highest Wealth loses Clams
            String targetId = players.values().stream()
                    .max(java.util.Comparator.comparingInt(PlayerState::wealth))
                    .map(PlayerState::id)
                    .orElse(players.keySet().iterator().next());
            return GameEvent.crabHunt(targetId, 40 + rng.nextInt(21)); // 40-60 clams
        } else {
            // Travelling Shop: Buy Rare card
            return GameEvent.travellingShop();
        }
    }

    private String selectHostilityTarget(int totalInfamy) {
        if (totalInfamy == 0) {
            // No infamy: pick the wealthiest player as target
            return players.values().stream()
                    .max(java.util.Comparator.comparingInt(PlayerState::wealth))
                    .map(PlayerState::id)
                    .orElse(players.values().iterator().next().id());
        }
        // Weighted random by infamy
        NavigableMap<Integer, String> weightMap = new TreeMap<>();
        int cumulative = 0;
        for (PlayerState p : players.values()) {
            int weight = Math.max(1, (int) ((p.infamy() / (double) totalInfamy) * 100));
            cumulative += weight;
            weightMap.put(cumulative, p.id());
        }
        int pick = rng.nextInt(cumulative);
        return weightMap.higherEntry(pick).getValue();
    }

    private int calculatePositiveEffect(int base, CardRarity rarity, PlayerState actor) {
        double buildBonus = actor.statBonus();
        double classBonus = 0.0;
        double effect = base * rarity.multiplier() * (1.0 + buildBonus + classBonus);

        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int calculateNegativeEffect(int base, CardRarity rarity) {
        double effect = base * rarity.multiplier();
        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int reduceGain(int effect, double reduction) {
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
        int maxWealth = players.values().stream().mapToInt(PlayerState::wealth).max().orElse(0);
        int maxRep = players.values().stream().mapToInt(PlayerState::reputation).max().orElse(0);
        int maxInfamy = players.values().stream().mapToInt(PlayerState::infamy).max().orElse(0);

        // Tie-breaking or priority order if one player is highest in multiple?
        // Usually, these are global highests.
        if (maxWealth >= maxRep && maxWealth >= maxInfamy) {
            PlayerState winner = players.values().stream().filter(p -> p.wealth() == maxWealth).findFirst().get();
            return new WinnerResult(winner.id(), PlayerClass.OPPORTUNIST, winner.wealth());
        } else if (maxRep >= maxWealth && maxRep >= maxInfamy) {
            PlayerState winner = players.values().stream().filter(p -> p.reputation() == maxRep).findFirst().get();
            return new WinnerResult(winner.id(), PlayerClass.ALTRUIST, winner.reputation());
        } else {
            PlayerState winner = players.values().stream().filter(p -> p.infamy() == maxInfamy).findFirst().get();
            return new WinnerResult(winner.id(), PlayerClass.SABOTEUR, winner.infamy());
        }
    }
}
