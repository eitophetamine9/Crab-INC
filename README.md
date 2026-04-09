# Project Title: Crab Inc.

## Group Members
- CARILLO, SAMUEL (flurgerburger)
- EPILAN, GIAN CEDRICK (gOps132)
- ORTIZ, KRISTIAN ENRICO (Odnerk)
- ROMALES, RAY CHRISTIAN (chanie12)
- SANTIZO, JOHN ERICK (eitophetamine9)

## Project Description
Crab Inc. is a multiplayer, turn-based tycoon desktop application that gamifies the socio-economic concept of "Crab Mentality." Players manage competing local businesses and must choose between investing in the community (co-op) or sabotaging rivals (PvP) to reach the top. It serves as both a strategic resource management game and a social commentary on local business practices.

## Proposed Features
- **User Authentication:** Players can register, log in, and track their all-time win/loss records.
- **Multiplayer Lobby System:** 3 to 7 players can join a shared session.
- **Interactive Dashboard:** A live UI showing personal business stats, rival stats, and global "Market Health."
- **Action Phase Form:** Players execute daily actions (e.g., "Invest in Infrastructure" or "Bribe Health Inspector").
- **Real-Time Market Calculation:** Server processes simultaneous player turns and updates the global economy.
- **End-Game Reports:** Detailed database-driven summary of who sabotaged whom and the final market state.

## Planned Technologies
- **Language:** Java
- **GUI:** JavaFX (FXML)
- **Database:** MySQL / SQLite via JDBC
- **Networking:** Java Sockets (for multiplayer communication)

## Evaluation Criteria Mapping (Initial)
- **OOP:** Core entities encapsulated into classes (`Player`, `Business`, `ActionManager`).
- **GUI:** Dashboard and forms built using JavaFX layout containers (BorderPane, VBox) and updated via Controllers.
- **UML:** Use Case and Class Diagrams included in the `/diagrams` folder.
- **Design Pattern:** - *Singleton:* Managing the single Database Connection.
  - *Observer:* Notifying the JavaFX UI when the server updates game states.
- **Multithreading:** JavaFX `Task` and `Platform.runLater` used to handle background server polling without freezing the UI, plus multithreaded socket handling for multiple players.
