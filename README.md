# Project Title: Crab Inc.

## Group Members
- CARILLO, SAMUEL (flurgerburger)
- EPILAN, GIAN CEDRICK (gOps132)
- ORTIZ, KRISTIAN ENRICO (Odnerk)
- ROMALES, RAY CHRISTIAN (chanie12)
- SANTIZO, JOHN ERICK (eitophetamine9)

## Project Description
Crab Inc. is a multiplayer, turn-based tycoon game about "crab mentality." Players manage competing local businesses and choose whether to build the community, sabotage rivals, or do both.

## Current Architecture
Crab Inc. uses FXGL as the runtime foundation and JavaFX/FXML for UI.

Core rule:

> FXGL is the runtime. Crab Inc. owns the architecture.

Current layers:

- `Main` starts the app.
- `crab.app` adapts FXGL lifecycle hooks into project bootstrap code.
- `crab.appcore` holds small app-lifetime services such as module lifecycle and game context.
- `crab.features` holds feature-oriented code.
- Domain/game state should stay plain Java and should not depend on FXGL or JavaFX.

The current scaffold proves:

- FXGL startup
- FXML UI loading
- JavaFX 3D rendering inside the FXGL scene
- basic module registration through `GameContext`

## Planned Features
- 3 to 7 player sessions
- character selection
- map/session settings
- simultaneous turn command submission
- local play with bots
- LAN host/join multiplayer
- optional host authentication
- end-game reports

## Planned Technologies
- Java 17
- FXGL 17.3
- JavaFX 21.0.2 / FXML
- Maven
- Future local networking through Java sockets or a project-owned transport adapter

## Run
Use JDK 17 for Maven and IntelliJ.

Use the shared IntelliJ run configuration:

- `Crab Inc FXGL`

Or run from terminal:

```bash
./mvnw javafx:run
```

If IntelliJ logs `J:24` or crashes with `NSTrackingRectTag`, the IDE is running the game with JDK 24. Set the project SDK and Maven runner JRE to the installed JDK 17.

## Evaluation Criteria Mapping
- **OOP:** Use small classes with clear responsibilities and Doxygen class comments for intentional patterns/SOLID usage.
- **GUI:** Use FXGL for runtime scene integration and JavaFX/FXML for UI panels.
- **UML:** Keep diagrams in `/Diagrams`.
- **Design Patterns:** Use Adapter, Facade, Strategy, Command, State, Observer/Event Bus, and Service Registry only where they clarify ownership.
- **Multithreading:** Keep background work behind appcore services such as future task, save/load, LAN discovery, or networking adapters.

## Architecture Credits
- Vertical Slice Architecture: Jimmy Bogard
- Clean Architecture / Dependency Rule: Robert C. Martin
- Service Locator pattern: Robert Nystrom, *Game Programming Patterns*
- Engine subsystem examples: Unreal Engine Subsystems, Godot Autoloads, libGDX AssetManager
