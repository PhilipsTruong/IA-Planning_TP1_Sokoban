# Sokoban AI Agent — PDDL4J

Projet de planification automatique résolvant le jeu Sokoban à l'aide de la bibliothèque **PDDL4J** (planificateur `HSP` avec l'heuristique `FAST_FORWARD`).

## Nom et Prénom
- **TRUONG TRAN PHUC TIN** — M2-IC

## Fichiers créés ou modifiés

### 1. Fichiers créés
- `src/main/java/sokoban/SokobanPDDLSolver.java`
- `sokoban-domain.pddl`

### 2. Fichiers modifiés
- `src/main/java/sokoban/Agent.java`
- `src/main/java/sokoban/SokobanMain.java`
- `pom.xml`
  - Configuration des dépendances Maven (ajout de `fr.uga.pddl4j`).

### 3. Fichiers non modifiés
- `src/main/java/sokoban/Board.java`, `Cell.java`, `Box.java`, `Pusher.java`.

## Exécution

Compiler et empaqueter le projet avec Maven :
```bash
mvn clean package