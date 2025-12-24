# Celeste-like Platformer Game Engine

## Description

Un moteur de jeu 2D plateforme développé avec LibGDX, inspiré de Celeste. Le jeu propose un système de combat avancé avec un personnage Samurai, une IA d'ennemis sophistiquée, un système de power-ups dynamiques, et un éditeur de carte intégré. Le runtime charge les cartes produites par l'éditeur et l'inspecteur intégrés, offrant une expérience de jeu fluide avec des animations complexes, des combos d'attaque, et une gestion d'état avancée.

### Fonctionnalités Principales

- **Système de Combat** : Combos au sol à trois coups, attaques aériennes, attaques spéciales et défense
- **Mouvement Avancé** : Saut, dash, wall-jump, wall-slide avec contrôle aérien précis
- **Système de Power-ups** : Bouclier, vitesse et arme avec durée limitée et empilement
- **IA d'Ennemis** : Plusieurs types d'ennemis avec comportements et patterns d'attaque uniques
- **Éditeur de Carte** : Outil intégré pour créer et éditer les niveaux
- **Système de Santé** : Barres de vie segmentées, système de dégâts et mécaniques de mort/respawn
- **Logging Complet** : Journalisation de tous les événements de jeu dans `logs/game.log`

## Membres du Groupe

- Mohamed Ben Daamar 

## Technologies Utilisées

- **Langage** : Java 21
- **Framework Game** : LibGDX 1.12.1
- **Build Tool** : Gradle
- **Logging** : Système de logging personnalisé (`GameLogger`)
- **Format de Données** : JSON pour les cartes et configurations

## Design Patterns Implémentés

1. **Command Pattern** : Encapsulation des actions utilisateur (mouvement, saut, attaque, dash, défense) permettant la remappage des touches et un système de commandes flexible.

2. **State Pattern** : Gestion des états du personnage Samurai (idle, run, jump, dash, attack, defend, hurt, death, wall interactions) et des états du jeu (menu, playing, paused, game-over, victory).

3. **Decorator Pattern** : Système de power-ups dynamiques (Shield, Speed, Weapon) qui modifient les attributs du personnage sans modifier la classe de base.

4. **Composite Pattern** : Structure hiérarchique pour organiser les ennemis en arbre, permettant des opérations récursives efficaces (update, render, collision).

5. **Strategy Pattern** : Système d'attaque flexible avec différentes stratégies (GroundAttackOne, GroundAttackTwo, GroundAttackThree, AirAttack) pour gérer les combos.

6. **Observer Pattern** : Système de santé observable avec des listeners pour les événements de dégâts, guérison et mort, permettant à plusieurs systèmes de réagir.

7. **Factory/Registry Pattern** : Création centralisée des types d'ennemis via `EnemyRegistry` avec chargement depuis des fichiers JSON.

8. **Facade Pattern** : Interface simplifiée `SamuraiCharacter` qui cache la complexité des sous-systèmes (physique, animation, combat, power-ups).

9. **Template Method Pattern** : Structure de base pour les décorateurs de power-ups avec méthodes template.

> Pour plus de détails sur chaque pattern, consultez [DESIGN_PATTERNS_REPORT.md](DESIGN_PATTERNS_REPORT.md)

## Installation

### Prérequis

- **JDK 21** ou supérieur
- **Gradle** (inclus via wrapper : `gradlew` ou `gradlew.bat`)

### Étapes

1. **Cloner le dépôt** :
   ```bash
   git clone https://github.com/Mohamedbd99/celectLikeUsingGameEngine.git
   cd celectLikeUsingGameEngine
   ```

2. **Compiler le projet** :
   ```bash
   # Windows
   .\gradlew.bat build
   
   # Linux/Mac
   ./gradlew build
   ```

3. **Exécuter le jeu** :
   ```bash
   # Windows
   .\gradlew.bat run
   
   # Linux/Mac
   ./gradlew run
   ```

4. **Créer une distribution exécutable** :
   ```bash
   # Windows
   .\gradlew.bat distZip
   # ou utiliser le script PowerShell
   .\build-executable.ps1
   
   # Linux/Mac
   ./gradlew distZip
   ```

## Utilisation

### Contrôles du Jeu

#### Mouvement
- `W` / `↑` : Se déplacer vers le haut / Viser vers le haut
- `A` / `←` : Se déplacer vers la gauche
- `S` / `↓` : Se déplacer vers le bas / S'accroupir
- `D` / `→` : Se déplacer vers la droite
- `SPACE` : Sauter / Wall-jump

#### Combat
- `J` / `Clic Gauche` : Attaquer (combo au sol à 3 coups / attaque aérienne)
- `E` / `Clic Milieu` : Attaque spéciale (uniquement au sol)
- `Clic Droit` : Défendre (maintenir pour bloquer)
- `SHIFT` : Dash (direction déterminée par les touches de mouvement)

#### Contrôles du Jeu
- `ESC` : Pause/Reprendre
- `ENTER` : Confirmer / Commencer le jeu
- `M` : Retour au menu principal
- `R` : Recommencer depuis l'écran de game over

#### Contrôles Debug/QA (pour les tests)
- `1` : Accorder un power-up Bouclier
- `2` : Accorder un power-up Vitesse
- `3` : Accorder un power-up Arme

### Commandes Gradle

| Commande | Description |
|----------|-------------|
| `./gradlew run` | Lance le jeu principal |
| `./gradlew runViewEditor` | Lance l'éditeur de carte |
| `./gradlew runInspector` | Lance l'inspecteur de carte |
| `./gradlew build` | Compile le projet |
| `./gradlew distZip` | Crée une distribution ZIP exécutable |
| `./gradlew test` | Exécute les tests unitaires |
| `./gradlew extractTiles` | Extrait les tuiles d'un fichier TSX |

> Pour plus de détails, consultez [FEATURES_AND_COMMANDS.md](FEATURES_AND_COMMANDS.md)

## Structure du Projet

```
celectLikeUsingGameEngine/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/celestelike/
│   │   │       ├── desktop/          # Launchers (Game, Editor, Inspector)
│   │   │       ├── game/
│   │   │       │   ├── entity/
│   │   │       │   │   ├── samurai/  # Personnage principal
│   │   │       │   │   │   ├── input/        # Command Pattern
│   │   │       │   │   │   ├── state/        # State Pattern
│   │   │       │   │   │   ├── attack/       # Strategy Pattern
│   │   │       │   │   │   ├── powerup/      # Decorator Pattern
│   │   │       │   │   │   └── combat/       # Observer Pattern
│   │   │       │   │   └── enemy/    # Ennemis (Composite, Factory)
│   │   │       │   ├── world/        # Gestion des niveaux
│   │   │       │   ├── config/       # Configuration
│   │   │       │   ├── logging/     # Système de logging
│   │   │       │   └── state/        # États du jeu
│   │   │       └── tools/            # Outils (Editor, Inspector)
│   │   └── resources/
│   │       └── assets/               # Assets du jeu
│   └── test/                         # Tests unitaires
├── build/                            # Fichiers de build
├── docs/                             # Documentation
│   └── architecture.puml            # Diagramme PlantUML
├── game-executable/                  # Distribution exécutable
├── build.gradle.kts                  # Configuration Gradle
├── DESIGN_PATTERNS_REPORT.md        # Rapport détaillé des patterns
├── FEATURES_AND_COMMANDS.md         # Documentation complète
└── ARCHITECTURE_DIAGRAM.md          # Diagrammes Mermaid
```

## Architecture

Le jeu utilise une architecture modulaire avec plusieurs couches :

1. **Couche Application** : `DesktopLauncher`, `EditorLauncher`, `InspectorLauncher`
2. **Couche Game Core** : `CelesteGame` (boucle principale)
3. **Couche Entity** : `SamuraiCharacter` (Facade), systèmes d'ennemis
4. **Couche World** : Gestion des niveaux, collisions, tilesets
5. **Couche Presentation** : Rendu, HUD, overlays

> Pour les diagrammes détaillés, consultez [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)

## Fichiers de Configuration

- `game_config.json` : Configuration de la caméra et du joueur
- `editor_blueprint.json` : Données des tuiles de la carte
- `inspector_snapshot.json` : Masques de collision et données d'ennemis
- `enemy_spawns.json` : Coordonnées de spawn des ennemis

## Logging

Tous les événements de jeu sont journalisés dans `logs/game.log` :
- Transitions d'état
- Application/retrait de power-ups
- Création/destruction d'entités
- Événements de combat

## Build et Distribution

### Build Local
```bash
.\gradlew.bat build
```

### Distribution Exécutable
```bash
.\build-executable.ps1
```
Cela crée un dossier `game-executable/` avec tout le nécessaire pour exécuter le jeu.

### Structure de Distribution
```
celectLikeUsingGameEngine-1.0-SNAPSHOT/
├── bin/
│   ├── celectLikeUsingGameEngine.bat  # Windows
│   └── celectLikeUsingGameEngine      # Linux/Mac
├── lib/                                # Toutes les dépendances JAR
└── README                              # Info de distribution
```

## Tests

```bash
.\gradlew.bat test
```

Les rapports de test sont générés dans `build/reports/tests/`.

## Documentation Supplémentaire

- **[DESIGN_PATTERNS_REPORT.md](DESIGN_PATTERNS_REPORT.md)** : Analyse détaillée de tous les design patterns
- **[FEATURES_AND_COMMANDS.md](FEATURES_AND_COMMANDS.md)** : Documentation complète des fonctionnalités et commandes
- **[ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)** : Diagrammes Mermaid de l'architecture

## Contribution

Ce projet a été développé dans le cadre du module "Design Patterns". Pour contribuer :

1. Créer une branche feature (`feature/nom-feature`)
2. Utiliser des commits conventionnels (`feat:`, `fix:`, `docs:`)
3. Soumettre une pull request

## Licence

[Spécifier la licence du projet]

## Auteurs

- [Votre Nom 1] - Développement initial
- [Votre Nom 2] - [Contribution]
- [Votre Nom 3] - [Contribution]

---

**Note** : Ce projet démontre l'implémentation de 9 design patterns majeurs dans un moteur de jeu 2D complet, créant une architecture flexible, maintenable et extensible.

