package crab.features.gameplay.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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

        // Each player starts with 1 card initially
        for (PlayerState player : players) {
            player.addCard(drawCard(player));
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
        // Shop is active during EVENT phase if it was rolled
        if (!travellingShopActive) return;
        PlayerState p = requirePlayer(playerId);
        if (p.clams() >= 50) {
            p.addClams(-50); // Direct deduct
            p.addCard(drawRareCard(p));
        }
    }

    private ActionCard drawRareCard(PlayerState player) {
        // 10% chance for a Signature card for the player's class
        int roll = rng.nextInt(100);
        CardRarity targetRarity = (roll < 10) ? CardRarity.SIGNATURE : CardRarity.RARE;

        List<ActionCard> candidates = deckTemplate.stream()
                .filter(c -> c.rarity() == targetRarity && isStrictlyCompatible(c, player.playerClass()))
                .collect(java.util.stream.Collectors.toList());

        if (candidates.isEmpty()) {
            // Fallback to RARE if no signatures available, still strictly compatible
            candidates = deckTemplate.stream()
                    .filter(c -> c.rarity() == CardRarity.RARE && isStrictlyCompatible(c, player.playerClass()))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (candidates.isEmpty()) return drawCard(player);
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private boolean isStrictlyCompatible(ActionCard card, PlayerClass playerClass) {
        return switch (playerClass) {
            case ALTRUIST -> card.type() == CardType.HELP || card.type() == CardType.SIGNATURE_ALTRUIST;
            case OPPORTUNIST -> card.type() == CardType.STEAL || card.type() == CardType.SIGNATURE_OPPORTUNIST;
            case SABOTEUR -> card.type() == CardType.SABOTAGE || card.type() == CardType.SIGNATURE_SABOTEUR;
        };
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
            player.addCard(drawCard(player));
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

        if (currentRound >= maxRounds || isCompletedCrabPeakFinalRound() || hasPlayerReachedThreshold()) {
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

    private boolean hasPlayerReachedThreshold() {
        for (PlayerState p : players.values()) {
            int score = switch (p.playerClass()) {
                case OPPORTUNIST -> p.wealth();
                case ALTRUIST -> p.reputation();
                case SABOTEUR -> p.infamy();
            };
            if (score >= winThreshold) return true;
        }
        return false;
    }

    /**
     * Help card: +40 Rep, +15 Clams (actor), +30 Wealth (target).
     * Signature Altruist: +80 Rep, +50% Shared Wealth.
     */
    private void resolveHelp(PlayerState actor, PlayerAction action,
                             Map<String, Double> rewardReductions,
                             Set<String> doubleNegativeTargets,
                             boolean isSignature) {
        PlayerState target = requireTarget(action);
        
        int baseRep = isSignature ? 80 : 40;
        int actorReputationGain = calculatePositiveEffect(baseRep, action.card().rarity(), actor);
        int actorClamsGain      = calculatePositiveEffect(15, action.card().rarity(), actor);
        int targetWealthGain    = calculatePositiveEffect(30, action.card().rarity(), target);

        // Charity Wave Bonus: Help cards give bonus Reputation (+20) next round
        if (charityWaveActive) {
            actorReputationGain += 20;
        }

        // Apply Saboteur distortion reductions
        actorReputationGain = reduceGain(actorReputationGain, rewardReductions.getOrDefault(actor.id(), 0.0));
        actorClamsGain       = reduceGain(actorClamsGain,       rewardReductions.getOrDefault(actor.id(), 0.0));
        targetWealthGain    = reduceGain(targetWealthGain,    rewardReductions.getOrDefault(target.id(), 0.0));

        // Double-negative logic: target wealth gain is halved? 
        // User says "Double negative effects". Helping is positive. 
        // But usually sabotage makes "gains" smaller. I'll halve the gain as before.
        if (doubleNegativeTargets.contains(target.id())) {
            targetWealthGain = Math.max(0, targetWealthGain / 2);
        }

        actor.addReputation(actorReputationGain);
        actor.addClams(actorClamsGain);
        target.addWealth(targetWealthGain);

        // Signature Altruist: Shared Wealth Gain (50% of target Wealth gain)
        if (isSignature) {
            int sharedWealth = Math.round(targetWealthGain * 0.5f);
            actor.addWealth(sharedWealth);
        }

        // Opportunist Signature passive: +10 Wealth for any Opportunist who sees an opponent gain Rep
        triggerOpportunistPassive(actor.id(), actorReputationGain);
    }

    /**
     * Steal card: +45 Wealth, -10 Rep (actor), -35 Clams (target).
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
     * Sabotage card: +50 Infamy (actor), 50% reward reduction on target (Capped at 70%).
     * Signature Saboteur: +100 Infamy, Double negative effects on target for 1 round.
     */
    private void resolveSabotage(PlayerState actor, PlayerAction action) {
        requireTarget(action);
        boolean isSignature = action.card().type() == CardType.SIGNATURE_SABOTEUR;
        int baseInfamy = isSignature ? 100 : 50;
        int actorInfamyGain = calculateNegativeEffect(baseInfamy, action.card().rarity());
        actor.addInfamy(actorInfamyGain);
    }

    /**
     * Opportunist Signature Passive: any Opportunist-class player gains +10 Wealth
     * whenever an opponent gains Reputation, up to 5 triggers per round.
     */
    private void triggerOpportunistPassive(String gainerId, int repGained) {
        if (repGained <= 0) return;
        for (PlayerState p : players.values()) {
            if (p.playerClass() == PlayerClass.OPPORTUNIST
                    && !p.id().equals(gainerId)) {
                
                boolean hasOpportunistSig = pendingActions.values().stream()
                        .anyMatch(a -> a.playerId().equals(p.id())
                                && a.card().type() == CardType.SIGNATURE_OPPORTUNIST);
                
                if (hasOpportunistSig && opportunistSigTriggersThisRound < 5) {
                    p.addWealth(10);
                    opportunistSigTriggersThisRound++;
                }
            }
        }
    }

    private Map<String, Double> collectRewardReductions() {
        Map<String, Double> reductions = new LinkedHashMap<>();
        for (PlayerAction action : pendingActions.values()) {
            CardType ct = action.card().type();
            if ((ct == CardType.SABOTAGE || ct == CardType.SIGNATURE_SABOTEUR) && action.targetPlayerId() != null) {
                // Distortion Formula: Base Distortion (50%) * RarityMultiplier
                double baseDistortion = 0.50;
                double multiplier = action.card().rarity().multiplier();
                double distortion = baseDistortion * multiplier;
                
                // Distortion Cap: 70%
                distortion = Math.min(0.70, distortion);
                
                reductions.merge(action.targetPlayerId(), distortion, (a, b) -> Math.min(0.70, a + b));
            }
        }
        return reductions;
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
        
        // Equal 25% chance for each of the 4 events
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
            travellingShopActive = true;
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

        // Round-based multipliers
        if (currentRound >= 14) {
            effect *= 2.0;
        } else if (currentRound >= 12) {
            effect *= 1.5;
        }

        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int calculateNegativeEffect(int base, CardRarity rarity) {
        double effect = base * rarity.multiplier();

        // Round-based multipliers for negative effects too? 
        // User says "points gained", but usually games scale everything.
        // I'll apply it to negative as well to keep the pressure.
        if (currentRound >= 14) {
            effect *= 2.0;
        } else if (currentRound >= 12) {
            effect *= 1.5;
        }

        if (crabPeakActive) {
            effect *= 2.0;
        }

        return Math.round((float) effect);
    }

    private int reduceGain(int effect, double reduction) {
        return Math.round((float) (effect * (1.0 - reduction)));
    }

    private ActionCard drawCard(PlayerState player) {
        int roll = rng.nextInt(100);
        CardRarity rarity;
        if (roll < 60) rarity = CardRarity.COMMON;
        else if (roll < 85) rarity = CardRarity.UNCOMMON;
        else if (roll < 95) rarity = CardRarity.RARE;
        else rarity = CardRarity.SIGNATURE;

        List<ActionCard> candidates = deckTemplate.stream()
                .filter(c -> c.rarity() == rarity && isCompatible(c, player.playerClass()))
                .collect(java.util.stream.Collectors.toList());

        if (candidates.isEmpty()) {
            // Fallback: draw any compatible card if specific rarity is empty (shouldn't happen with standard deck)
            candidates = deckTemplate.stream()
                    .filter(c -> isCompatible(c, player.playerClass()))
                    .collect(java.util.stream.Collectors.toList());
        }

        return candidates.get(rng.nextInt(candidates.size()));
    }

    private boolean isCompatible(ActionCard card, PlayerClass playerClass) {
        if (card.rarity() != CardRarity.SIGNATURE) {
            return true;
        }
        return switch (playerClass) {
            case ALTRUIST -> card.type() == CardType.SIGNATURE_ALTRUIST;
            case OPPORTUNIST -> card.type() == CardType.SIGNATURE_OPPORTUNIST;
            case SABOTEUR -> card.type() == CardType.SIGNATURE_SABOTEUR;
        };
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
            int score = switch (player.playerClass()) {
                case OPPORTUNIST -> player.wealth();
                case ALTRUIST -> player.reputation();
                case SABOTEUR -> player.infamy();
            };
            highest = Math.max(highest, score);
        }
        return highest;
    }
    private WinnerResult determineWinner() {
        // Priority 1: Who met their goal?
        for (PlayerState p : players.values()) {
            int score = switch (p.playerClass()) {
                case OPPORTUNIST -> p.wealth();
                case ALTRUIST -> p.reputation();
                case SABOTEUR -> p.infamy();
            };
            if (score >= winThreshold) {
                return new WinnerResult(p.id(), p.playerClass(), score);
            }
        }

        // Priority 2: Closest to goal if time ran out
        PlayerState best = null;
        double bestProgress = -1.0;
        for (PlayerState p : players.values()) {
            int score = switch (p.playerClass()) {
                case OPPORTUNIST -> p.wealth();
                case ALTRUIST -> p.reputation();
                case SABOTEUR -> p.infamy();
            };
            double progress = (double) score / winThreshold;
            if (progress > bestProgress) {
                bestProgress = progress;
                best = p;
            }
        }
        
        int finalScore = switch (best.playerClass()) {
            case OPPORTUNIST -> best.wealth();
            case ALTRUIST -> best.reputation();
            case SABOTEUR -> best.infamy();
        };
        return new WinnerResult(best.id(), best.playerClass(), finalScore);
    }
}
