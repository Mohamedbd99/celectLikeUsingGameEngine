# Features and Commands Documentation
## Celeste-like Platformer Game Engine

---

## Table of Contents
1. [Game Features](#game-features)
2. [Controls](#controls)
3. [Gradle Commands](#gradle-commands)
4. [Build Commands](#build-commands)
5. [Game Systems](#game-systems)
6. [Tools](#tools)

---

## Game Features

### Core Gameplay
- **2D Platformer**: Smooth platforming with wall-jumping, dashing, and air control
- **Combat System**: Three-hit ground combo, air attacks, special attacks, and defense
- **Enemy AI**: Multiple enemy types with different behaviors and attack patterns
- **Health System**: Segmented health bars, damage system, and death/respawn mechanics
- **Power-ups**: Shield, Speed, and Weapon power-ups with duration tracking
- **Score System**: Points awarded for defeating enemies
- **Timer**: Play time tracking

### Visual Features
- **Tile-based Map**: Customizable tile-based level system
- **Animation System**: Frame-based animations for player and enemies
- **HUD Overlay**: Health bar, score, timer, and active power-ups display
- **State Overlays**: Menu, pause, game over, and victory screens
- **Health Bars**: Visual health indicators for enemies

### Technical Features
- **VSync**: Smooth 60 FPS gameplay
- **Fixed Camera**: Deterministic camera behavior
- **Collision Detection**: Tile-based collision system
- **State Management**: Complex state machine for character and game flow
- **Logging**: Comprehensive event logging to `logs/game.log`

---

## Controls

### Movement
| Key | Action | Description |
|-----|--------|-------------|
| `W` / `↑` | Move Up | Aim upward / Climb |
| `A` / `←` | Move Left | Move left |
| `S` / `↓` | Move Down | Crouch / Aim downward |
| `D` / `→` | Move Right | Move right |
| `SPACE` | Jump | Jump / Wall-jump |

### Combat
| Key | Action | Description |
|-----|--------|-------------|
| `J` / `Left Mouse` | Attack | Three-hit ground combo / Air slash |
| `E` / `Middle Mouse` | Special Attack | Ground-only special attack |
| `Right Mouse` | Defend | Hold to block attacks |
| `SHIFT` | Dash | Quick dash in movement direction |

### Game Controls
| Key | Action | Description |
|-----|--------|-------------|
| `ESC` | Pause/Resume | Toggle pause menu |
| `ENTER` | Confirm | Confirm menu selections / Start game |
| `M` | Menu | Return to main menu from pause/game over |
| `R` | Retry | Restart from game over screen |

### Debug/QA Controls
| Key | Action | Description |
|-----|--------|-------------|
| `1` | Grant Shield | Add shield power-up (for testing) |
| `2` | Grant Speed | Add speed power-up (for testing) |
| `3` | Grant Weapon | Add weapon power-up (for testing) |

---

## Gradle Commands

### Running the Game

#### Main Game
```bash
# Windows
.\gradlew.bat run

# Linux/Mac
./gradlew run
```
Launches the main game (`DesktopLauncher` → `CelesteGame`)

#### Map Editor
```bash
# Windows
.\gradlew.bat runViewEditor

# Linux/Mac
./gradlew runViewEditor
```
Launches the map editor (`EditorLauncher` → `MapEditorApp`)
- Opens two windows: map view and detachable palette
- Edit tiles, place enemies, configure collision

#### Map Inspector
```bash
# Windows
.\gradlew.bat runInspector

# Linux/Mac
./gradlew runInspector
```
Launches the map inspector (`InspectorLauncher` → `MapInspector`)
- View map data, collision masks, enemy spawns
- Export/import map data

### Building

#### Build JAR
```bash
.\gradlew.bat jar
```
Creates executable JAR in `build/libs/`

#### Build Distribution
```bash
.\gradlew.bat distZip
```
Creates ZIP distribution in `build/distributions/`
- Includes all dependencies
- Includes executable scripts for Windows/Unix
- Ready for distribution

#### Clean Build
```bash
.\gradlew.bat clean build
```
Cleans previous build and rebuilds everything

#### Clean Distribution
```bash
.\gradlew.bat clean distZip
```
Cleans and creates fresh distribution

### Testing

#### Run Tests
```bash
.\gradlew.bat test
```
Runs all unit tests

#### Test Report
```bash
.\gradlew.bat test
# Reports in build/reports/tests/
```

### Tools

#### Extract Tiles from TSX
```bash
.\gradlew.bat extractTiles
```
Extracts individual tile images from a TSX tileset file
- Configured in `build.gradle.kts`
- Outputs: `tile_0000.png`, `tile_0001.png`, etc.

---

## Build Commands

### PowerShell Scripts

#### Build Executable Distribution
```powershell
.\build-executable.ps1
```
- Builds the game distribution
- Extracts to `game-executable/` folder
- Ready-to-run executable

### Manual Build Steps

1. **Build Distribution**:
   ```powershell
   .\gradlew.bat distZip
   ```

2. **Extract ZIP**:
   ```powershell
   Expand-Archive -Path build\distributions\celectLikeUsingGameEngine-1.0-SNAPSHOT.zip -DestinationPath game-executable
   ```

3. **Run Game**:
   ```powershell
   cd game-executable\celectLikeUsingGameEngine-1.0-SNAPSHOT\bin
   .\celectLikeUsingGameEngine.bat
   ```

---

## Game Systems

### Character System

#### Samurai Character
- **States**: 11 different states (idle, run, jump, dash, attack, defend, etc.)
- **Physics**: Custom kinematic controller with gravity, friction, and collision
- **Combat**: Multi-hit combo system, air attacks, special attacks
- **Power-ups**: Stackable decorators (shield, speed, weapon)
- **Health**: Observable health component with listeners

#### Movement Mechanics
- **Ground Movement**: Left/right with acceleration/deceleration
- **Jumping**: Variable jump height, wall-jumping
- **Dashing**: Quick dash in movement direction
- **Wall Interaction**: Wall sliding, wall jumping, wall contact detection

### Enemy System

#### Enemy Types
- **redDeon**: Flying enemy with attack animations
- **skeletonEnemie**: Ground enemy with shield and walk animations
- **deathBoss**: Boss enemy with multiple attack patterns

#### Enemy Behavior
- **AI**: Pathfinding and attack patterns
- **Health**: Individual health bars per enemy
- **Combat**: Melee damage system with range detection
- **Organization**: Composite tree structure by enemy type

### Map System

#### Level Data
- **Blueprint**: Tile-based level representation
- **Collision**: Separate collision map for physics
- **Tileset**: TSX-based tileset loading
- **Export/Import**: JSON-based map data persistence

#### Map Editor Features
- **Tile Painting**: Click to place tiles from palette
- **Enemy Placement**: Place enemies at specific coordinates
- **Collision Editing**: Toggle solid/water/door/key tiles
- **Save/Load**: Export to `editor_blueprint.json` and `inspector_snapshot.json`

### Combat System

#### Attack Types
- **Ground Combo**: Three-hit combo (Attack 1 → 2 → 3)
- **Air Attack**: Single air slash
- **Special Attack**: Ground-only special move
- **Defense**: Block incoming attacks

#### Damage System
- **Base Damage**: Strategy-defined damage values
- **Power-up Modifiers**: Decorator-based damage multipliers
- **Defense**: Shield power-up reduces incoming damage
- **Range Detection**: Melee attack range checking

### Power-up System

#### Power-up Types
- **Shield**: Increases defense multiplier (reduces incoming damage)
- **Speed**: Increases movement speed multiplier
- **Weapon**: Increases attack damage and multiplier

#### Power-up Mechanics
- **Duration**: Each power-up has a time limit
- **Stacking**: Multiple power-ups can be active simultaneously
- **HUD Display**: Shows active power-ups with remaining time
- **Decay**: Power-ups automatically expire after duration

---

## Tools

### Map Editor (`runViewEditor`)
- **Purpose**: Create and edit game levels
- **Features**:
  - Tile painting from palette
  - Enemy placement
  - Collision mask editing
  - Save/load functionality
- **Output Files**:
  - `editor_blueprint.json` - Tile data
  - `inspector_snapshot.json` - Collision and enemy data
  - `enemy_spawns.json` - Enemy spawn coordinates

### Map Inspector (`runInspector`)
- **Purpose**: View and analyze map data
- **Features**:
  - Visualize collision masks
  - View enemy spawns
  - Export map data
  - Validate map structure

### TSX Tile Extractor (`extractTiles`)
- **Purpose**: Extract individual tile images from TSX tileset
- **Usage**: Configure path in `build.gradle.kts`
- **Output**: Individual PNG files for each tile

---

## File Structure

### Important Files
- `editor_blueprint.json` - Level tile data
- `inspector_snapshot.json` - Collision and enemy data
- `enemy_spawns.json` - Enemy spawn coordinates
- `game_config.json` - Game configuration (camera, player settings)
- `logs/game.log` - Game event log

### Asset Locations
- `src/main/resources/assets/` - All game assets
  - `newTileSetManara/` - Main tileset
  - `emenies/` - Enemy sprites
  - `FULL_Samurai 2D Pixel Art v1.2/` - Player sprites
  - `b.tsx` - Tileset definition

---

## Configuration

### Game Config (`game_config.json`)
```json
{
  "camera": {
    "tilesWide": 28,
    "tilesTall": 16,
    "zoom": 1.0
  },
  "player": {
    "colliderWidth": 18.0,
    "colliderHeight": 32.0,
    "colliderOffsetX": 39.0,
    "colliderOffsetY": 2.0
  }
}
```

### Enemy Spawns (`enemy_spawns.json`)
```json
[
  {"name": "deathBoss", "row": 14, "col": 38},
  {"name": "skeletonEnemie", "row": 2, "col": 32},
  {"name": "redDeon", "row": 2, "col": 17}
]
```

---

## Troubleshooting

### Game Won't Start
- Check Java version (requires Java 21+)
- Verify assets are in `src/main/resources/assets/`
- Check `logs/game.log` for errors

### Map Not Loading
- Ensure `editor_blueprint.json` exists in project root or resources
- Check `inspector_snapshot.json` for collision data
- Verify tileset files are present

### Enemies Not Spawning
- Check `enemy_spawns.json` format
- Verify enemy definitions in `EnemyRegistry`
- Check spawn coordinates are within map bounds

### Build Issues
- Run `.\gradlew.bat clean` first
- Check Java version compatibility
- Verify all dependencies are downloaded

---

## Performance Tips

- **VSync**: Enabled by default for smooth 60 FPS
- **Fixed Camera**: Reduces rendering overhead
- **Composite Enemy Tree**: Efficient enemy updates
- **State Machine**: Minimal state transition overhead
- **Logging**: Can be disabled in production builds

---

## Development Workflow

1. **Edit Map**: Use `runViewEditor` to create/edit levels
2. **Test Game**: Use `run` to play the game
3. **Inspect Data**: Use `runInspector` to validate map data
4. **Build Distribution**: Use `distZip` to create distributable package
5. **Test Executable**: Run from `game-executable/` folder

---

## Version Information

- **Game Version**: 1.0-SNAPSHOT
- **LibGDX Version**: 1.12.1
- **Java Version**: 21
- **Build Tool**: Gradle

