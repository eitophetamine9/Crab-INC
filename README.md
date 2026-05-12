# Project Title: Crab Inc.
> **“Crab your way to the top”**

## Group Members
- CARILLO, SAMUEL (flurgerburger)
- EPILAN, GIAN CEDRICK (gOps132)
- ORTIZ, KRISTIAN ENRICO (Odnerk)
- ROMALES, RAY CHRISTIAN (chanie12)
- SANTIZO, JOHN ERICK (eitophetamine9)

## Project Description
Crab Inc. is a beach themed, phase-based strategy card game that explores the socio-economic phenomenon of **"Crab Mentality."** Set in a vibrant seabed district, players manage resources and navigate four distinct phases (Development, Action, Resolution, and Event). The game challenges players to balance their individual growth against community health, choosing between cooperation (Bayanihan) and destructive competition (Crab Actions).

## Current Architecture
Crab Inc. uses FXGL as the runtime foundation and JavaFX/FXML for UI.

Core rule:
> FXGL is the runtime. Crab Inc. owns the architecture.

Current layers:
- `Main` starts the app.
- `crab.app` adapts FXGL lifecycle hooks into project bootstrap code.
- `crab.appcore` holds small app-lifetime services such as module lifecycle, game context, and the **Phase Engine**.
- `crab.features` holds feature-oriented code (Card System, Build System, Event Logic).
- Domain/game state (e.g., `Card`, `Player`, `GamePhase`) should stay plain Java and should not depend on FXGL or JavaFX.

The current scaffold proves:
- FXGL startup and scene integration.
- FXML UI loading for card hands and player dashboards.
- JavaFX 3D rendering for underwater environmental assets.
- Simultaneous action submission logic through the `GameContext`.

## Planned Features
- **Phase-Based Gameplay:** Development (resource gathering), Action (secret submission), Resolution (simultaneous reveal), and Event (global modifiers).
- **Build System:** Three distinct playstyles (Saboteur, Altruist, Opportunist) with unique passive bonuses.
- **Card Mechanics:** Tiered rarity system (Common, Uncommon, Rare) for Help, Steal, and Sabotage actions.
- **Status Indicators:** Dynamic visual feedback using Clams (Wealth/Pearls) and Statistic board for each player/ai (Wealth/Reputation/Infamy).
- **Multiplayer:** 3 to 7 player sessions with LAN host/join capabilities and local bot support (CURRENTLY ONLY AI ENEMIES).
- **End-Game Reports:** Summary of the "Action Ledger" detailing who sabotaged whom. (TO BE ADDED)

## Planned Technologies
- Java 25
- FXGL 25.0.1
- JavaFX 25.0.1 / FXML
- Maven
- SQLite (via JDBC) for Card Library and Action History.
- Future local networking through Java sockets or a project-owned transport adapter.

## Run
Use JDK 25 for Maven and IntelliJ.

Use the shared IntelliJ run configuration:
- `Crab Inc FXGL`

Or run from terminal:
```bash
./mvnw javafx:run
```

If Maven reports that the project requires Java 25, set the project SDK and Maven runner JRE to an installed JDK 25.

## Local MySQL
Crab Inc. includes a local Docker Compose database stack based on the Space.h setup.

Start MySQL and phpMyAdmin:
```bash
docker compose up -d mysql phpmyadmin
```

Local endpoints:

| Service | URL |
| --- | --- |
| MySQL | `localhost:3306` |
| phpMyAdmin | `http://localhost:8081` |

Default local credentials:

| Key | Value |
| --- | --- |
| Database | `crabinc` |
| User | `crabinc` |
| Password | `crabinc` |
| Root password | `crabinc-root` |

Default game sign-in:

| Username | Password |
| --- | --- |
| `demo` | `demo` |

The Java app reads these optional environment variables:
```bash
CRABINC_DB_URL=jdbc:mysql://localhost:3306/crabinc?serverTimezone=UTC
CRABINC_DB_USER=crabinc
CRABINC_DB_PASSWORD=crabinc
CRABINC_DEV_USERNAME=demo
CRABINC_DEV_PASSWORD=demo
```

## Evaluation Criteria Mapping
- **OOP:** Use of inheritance for card types and strategy-based event effects. Small classes with clear responsibilities and Doxygen class comments for intentional patterns/SOLID usage.
- **GUI:** Use FXGL for runtime scene integration and JavaFX/FXML for UI panels.
- **UML:** Keep diagrams in `/Diagrams`.
- **Design Patterns:** Use Strategy for resolution of card effects/events, Command for buffering secret player actions, and Singleton/Service Registry for centralized managers via GameContext.
- **Multithreading:** Keep background work behind appcore services such as phase timers, simultaneous resolution calculations, and LAN discovery/networking adapters.

## Architecture Credits
- Vertical Slice Architecture: Jimmy Bogard
- Clean Architecture / Dependency Rule: Robert C. Martin
- Service Locator pattern: Robert Nystrom, *Game Programming Patterns*
- Engine subsystem examples: Unreal Engine Subsystems, Godot Autoloads, libGDX AssetManager
