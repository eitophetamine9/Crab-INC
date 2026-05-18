# Project Title: Crab Inc.
> **“Crab your way to the top”**

## Group Members
- CARILLO, SAMUEL (flurgerburger)
- EPILAN, GIAN CEDRICK (gOps132)
- ORTIZ, KRISTIAN ENRICO (Odnerk)
- ROMALES, RAY CHRISTIAN (chanie12)
- SANTIZO, JOHN ERICK (eitophetamine9)

## Project Description
Crab Inc. is a beach-themed, phase-based strategy card game that explores the socio-economic phenomenon of **"Crab Mentality."** Set in a vibrant seabed district, players manage resources and navigate four distinct phases (Development, Action, Resolution, and Event). The game challenges players to balance their individual growth against community health, choosing between cooperation (Bayanihan) and destructive competition (Crab Actions).

---

## Technical Architecture

Crab Inc. uses **FXGL** as the runtime foundation and **JavaFX/FXML** for UI.
> **Core Architecture Rule:** FXGL is the runtime. Crab Inc. owns the architecture.

### Layered Structure
- **`Main`**: Entry point that starts the application.
- **`crab.app`**: Adapts FXGL lifecycle hooks (bootstrap, init, update) into project bootstrap code.
- **`crab.appcore`**: Centralized engine services including the Module Lifecycle, `GameContext` Service Locator, Database, and concurrent asset loading utilities.
- **`crab.features`**: Modular, decoupled gameplay mechanics (Card System, Build System, Events, Character Selection, Menus, Battle Visuals).
- **Domain layer**: Holds plain-old Java objects (`Card`, `PlayerState`, `GameSession`, `GameEvent`) representing the pure, immutable game rules without FXGL/JavaFX UI dependencies.

---

## Finalized Gameplay Mechanics

1. **Four-Phase Gameplay Loop**:
   - **Development Phase**: Players collect passive round income (Clams) and choose to spend Clams to upgrade their beach shelters (Build level 1 to 5).
   - **Drawing Phase**: Players draw cards to refill their hand. Hand drawing has a **60% bias for own-class cards** vs. **40% for cross-class cards**.
   - **Action Phase**: Secret submission of actions (Help, Steal, Sabotage). Players target opponents concurrently.
   - **Resolution Phase**: Simultaneous execution of all submitted actions. Cards are consumed upon play.
   
2. **Build System & Playstyles**:
   Players select a character matching one of three distinct classes, each with specialized passive bonuses that scale with the build level:
   - **Opportunist (Saboteur/Scavenger)**: Focuses on **Wealth** score.
   - **Altruist (Helper/Community Builder)**: Focuses on **Reputation** score.
   - **Saboteur (Distorter/Competitor)**: Focuses on **Infamy** score.
   
   | Build Level | Upgrade Cost (Clams) | Passive Income (Clams/round) | Stat Mult. Bonus |
   |:---:|:---:|:---:|:---:|
   | **1** | 40 | 10 | +0% |
   | **2** | 80 | 25 | +5% |
   | **3** | 150 | 50 | +10% |
   | **4** | 250 | 90 | +15% |
   | **5 (Max)** | — | 150 | +25% |

3. **Card and Deck Dynamics**:
   - Class-compatible tiered rarities: **Common (1.0x)**, **Uncommon (1.5x)**, and **Rare (2.5x)**.
   - **Signature Cards**: Special, powerful cards with custom gameplay-altering mechanics:
     - *Altruist Signature*: Giver receives +80 Reputation and shares 50% of the target's Wealth gain.
     - *Opportunist Signature*: Gains +10 Wealth whenever an opponent gains Reputation (up to 5 passive triggers per round).
     - *Saboteur Signature*: Grudge/double-negative effect (targets suffer double negative losses/halved positive gains).
   - **Hand Size & FIFO Mechanics**: Maximum hand size is capped at 3. Re-drawing past the limit triggers **FIFO (First-In, First-Out)** card replacement (the oldest card is discarded automatically to ensure active hand management).

4. **Hostility-Weighted Event System**:
   Triggered every 3 rounds (Rounds 3, 6, 9, 12, 15) to keep matches dynamic:
   - **Calm Current**: Passive round; no global effects.
   - **Market Crash**: Global downturn. All players lose 30–50 Wealth.
   - **Charity Wave**: A wave of goodwill. All Help cards grant a bonus +20 Reputation next round.
   - **Crab Hunt**: Targets the wealthiest player (with a hostility weight scaled by their Infamy score) causing them to lose 40–60 Clams.
   - **Travelling Shop**: The rare merchant arrives! Allows active player turns to buy Rare cards (50 Clams) or Signature cards (120 Clams) directly from the shop stock.

5. **Win Threshold and Endings**:
   - **Victory Trigger**: Immediate win check. A player achieves victory the instant they hit the **1000 point milestone** in their class-specific score (Wealth, Reputation, or Infamy).
   - **Ending screens**: Personalized animated ending screens matching the winner's class (*Altruist*, *Opportunist*, or *Saboteur*) with distinct narrative results.

---

## Design Patterns & SOLID Compliance

- **Flyweight Pattern (`AssetFlyweightFactory`)**:
  Manages heavy visual assets (dynamic idle/damage animated GIF crab models, heavy background and ending gifs). Memory is preserved by downsampling loaded `Image` instances based on extrinsic view slots (e.g. `human`, `ai_0`). This bounds heap usage and completely resolves the classic JavaFX shared-animation freeze issue.
- **Strategy Pattern (`GameEvent` / `CardEffects`)**:
  Separates specific positive/negative state delta actions and event conditions from the core `GameSession` engine.
- **Command Pattern (`PlayerAction`)**:
  Buffers, serializes, and encapsulates secret actions submitted by players during the Action Phase so they can be simultaneously resolved.
- **Singleton & Service Locator (`GameContext` / `ScreenManager`)**:
  Centralizes access to decoupled modules, audio players, save game adapters, and databases without strong couplings.
- **Single Responsibility Principle (SRP)**:
  Decoupled UI presentation logic (FXML Controllers) from domain and persistence states.

---

## Persistence & Multithreaded Database

Crab Inc. uses a hybrid database stack for offline and online persistence:
- **SQLite (Local File `crabinc.db`)**: Handles quick offline storage for local player statistics, high scores, and active card libraries.
- **Dockerized MySQL (Production/Auth)**: Manages secure user authentication (salted SHA-256 hashed password credentials), player logins, accounts, and server-side cloud game saves.

### Asynchronous Pipeline
To prevent the FXGL / JavaFX Application Thread from stuttering during database network calls:
- All queries, database updates, login validations, and signups are executed asynchronously inside background threads (`DatabaseManager`).
- Visual seaglass/cartoon transition spinners play smoothly while waiting for off-thread logins.

---

## Run and Setup

### 1. Requirements
- **Java Platform**: JDK 25
- **Build tool**: Maven

### 2. Environment Variables
To override default database endpoints, the Java runtime reads the following variables:
```bash
CRABINC_DB_URL=jdbc:mysql://localhost:3306/crabinc?serverTimezone=UTC
CRABINC_DB_USER=crabinc
CRABINC_DB_PASSWORD=crabinc
CRABINC_DEV_USERNAME=demo
CRABINC_DEV_PASSWORD=demo
```

### 3. Docker Compose Stack
Crab Inc. bundles a complete local Docker stack for MySQL and phpMyAdmin.

**Start the containers**:
```bash
docker compose up -d mysql phpmyadmin
```

| Service | Port / URL | Credentials |
|:---:|:---:|:---:|
| **MySQL** | `localhost:3306` | DB: `crabinc` / User: `crabinc` / Pass: `crabinc` |
| **phpMyAdmin** | `http://localhost:8081` | Root user: `root` / Pass: `crabinc-root` |

**Default credentials for game login**:
- Username: `demo`
- Password: `demo`

### 4. Running the Game
- **IntelliJ**: Launch using the shared run configuration: `Crab Inc FXGL`
- **Terminal**: Run the Maven wrapper:
```bash
./mvnw javafx:run
```

---

## Evaluation Criteria Mapping

- **Object-Oriented Programming (OOP)**: Clear domain-model encapsulation. Clear hierarchy for classes and properties, strictly compliant with SOLID principles. Fully commented with Doxygen standard comments.
- **Graphical User Interface (GUI)**: Fluid FXGL render engine integrated with modular FXML overlays. Themed using CSS (`cartoon-style.css`), cinematic underwater video backgrounds, bubble particle systems, and 3D rotated labels.
- **Unified Modeling Language (UML)**: Updated system diagrams are kept in [ClassDiagram.png](file:///c:/Users/Erick/IdeaProjects/Crab-Inc-/diagrams/ClassDiagram.png) and [UseCaseDiagram.png](file:///c:/Users/Erick/IdeaProjects/Crab-Inc-/diagrams/UseCaseDiagram.png).
- **Multithreading**: Concurrent database authentication tasks, off-thread auto-saves, separate task timers, and simultaneous resolution loops.
