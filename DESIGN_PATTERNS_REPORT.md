# Design Patterns Report
## Celeste-like Platformer Game Engine

This document provides a comprehensive analysis of all design patterns implemented in the Celeste-like Platformer game engine.

---

## 1. Command Pattern

### Purpose
Encapsulates user input actions as objects, allowing for flexible input handling, remapping, and potential undo/redo functionality.

### Implementation
- **Interface**: `SamuraiCommand`
- **Concrete Commands**:
  - `MoveLeftCommand`, `MoveRightCommand`, `MoveUpCommand`, `MoveDownCommand`
  - `JumpCommand`
  - `DashCommand`
  - `AttackCommand`
  - `DefendCommand`
  - `SpecialAttackCommand`

### Key Classes
```java
interface SamuraiCommand {
    void execute(SamuraiCharacter samurai, float delta);
    void release(SamuraiCharacter samurai);
}
```

### Benefits
- Decouples input handling from game logic
- Enables easy key remapping
- Supports command queuing and history
- Facilitates testing of game actions

### Usage Location
- `CelesteGame.handlePlayingInput()` - Maps keyboard input to command execution
- All movement, combat, and special actions are handled through commands

---

## 2. State Pattern

### Purpose
Manages complex state-dependent behavior for the Samurai character and game flow, allowing states to encapsulate their own logic.

### Implementation

#### A. Samurai Character States
- **Interface**: `SamuraiState`
- **Concrete States**:
  - `SamuraiIdleState` - Default standing state
  - `SamuraiRunState` - Horizontal movement
  - `SamuraiJumpState` - Jumping and falling
  - `SamuraiDashState` - Quick dash movement
  - `SamuraiAttackState` - Attack animations and logic
  - `SamuraiDefendState` - Blocking/defending
  - `SamuraiSpecialAttackState` - Special attack moves
  - `SamuraiHurtState` - Damage reaction
  - `SamuraiDeathState` - Death animation
  - `SamuraiWallContactState` - Wall interaction
  - `SamuraiWallSlideState` - Sliding down walls
  - `SamuraiWallJumpState` - Wall jumping

#### B. Game States
- **Enum**: `GameState`
- **States**: `MENU`, `PLAYING`, `PAUSED`, `GAME_OVER`, `VICTORY`

### Key Classes
```java
interface SamuraiState {
    void enter(SamuraiCharacter samurai);
    void update(SamuraiCharacter samurai, float delta);
    void exit(SamuraiCharacter samurai);
    SamuraiState handleInput(SamuraiCharacter samurai);
    SamuraiState checkTransitions(SamuraiCharacter samurai);
}
```

### Benefits
- Clean separation of state-specific behavior
- Easy to add new states without modifying existing code
- State transitions are explicit and traceable
- Logging of state changes via `GameLogger.stateTransition()`

### Usage Location
- `SamuraiCharacter` - Manages current state and transitions
- `CelesteGame` - Manages game-level states (menu, playing, paused, etc.)

---

## 3. Decorator Pattern

### Purpose
Dynamically adds power-up effects (shield, speed, weapon) to the Samurai character without modifying the base class.

### Implementation
- **Component Interface**: `SamuraiAttributes`
- **Concrete Component**: `BaseSamuraiAttributes`
- **Base Decorator**: `PowerUpDecorator`
- **Concrete Decorators**:
  - `ShieldPowerUp` - Increases defense multiplier
  - `SpeedPowerUp` - Increases movement speed multiplier
  - `WeaponPowerUp` - Increases attack damage and multiplier

### Key Classes
```java
interface SamuraiAttributes {
    float attackMultiplier();
    int attackBonus();
    float defenseMultiplier();
    float speedMultiplier();
}

abstract class PowerUpDecorator implements SamuraiAttributes {
    protected final SamuraiAttributes delegate;
    // Forwards calls to delegate, can override specific methods
}
```

### Benefits
- Power-ups can be stacked dynamically at runtime
- New power-up types can be added without modifying existing code
- Each decorator modifies specific attributes independently
- HUD displays active power-ups with remaining duration

### Usage Location
- `SamuraiCharacter.grantPowerUp()` - Applies decorators
- `SamuraiCharacter.getActivePowerUps()` - Returns current decorator chain
- Combat calculations use decorated attributes for damage/defense

---

## 4. Composite Pattern

### Purpose
Organizes enemy entities in a tree structure, allowing uniform treatment of individual enemies and enemy groups.

### Implementation
- **Component Interface**: `EnemyComponent`
- **Composite**: `EnemyGroupNode` - Groups enemies by type
- **Leaf**: `EnemyLeafNode` - Wraps individual `EnemyInstance`

### Key Classes
```java
interface EnemyComponent {
    void update(float delta);
    void draw(SpriteBatch batch);
    boolean applyMeleeDamage(...);
    boolean isEmpty();
}

class EnemyGroupNode implements EnemyComponent {
    private List<EnemyComponent> children;
    // Recursively delegates to children
}

class EnemyLeafNode implements EnemyComponent {
    private EnemyInstance instance;
    // Delegates to wrapped instance
}
```

### Benefits
- Efficient hierarchical organization of enemies
- Recursive operations (update, render, collision) traverse the tree
- Easy to group enemies by type for batch operations
- Simplifies enemy management and cleanup

### Usage Location
- `EnemyManager` - Maintains root `EnemyGroupNode`
- All enemy operations (update, draw, damage) traverse the composite tree
- Enemies are automatically grouped by their definition ID

---

## 5. Strategy Pattern

### Purpose
Defines a family of attack algorithms, making them interchangeable and allowing the attack system to vary independently.

### Implementation
- **Strategy Interface**: `SamuraiAttackStrategy`
- **Concrete Strategies**:
  - `GroundAttackOneStrategy` - First hit of ground combo
  - `GroundAttackTwoStrategy` - Second hit of ground combo
  - `GroundAttackThreeStrategy` - Third hit of ground combo
  - `AirAttackStrategy` - Airborne attack

### Key Classes
```java
interface SamuraiAttackStrategy {
    SamuraiAnimationKey animationKey();
    boolean canExecute(SamuraiCharacter samurai);
    void onEnter(SamuraiCharacter samurai);
    void onUpdate(SamuraiCharacter samurai, float delta);
    void onExit(SamuraiCharacter samurai);
    boolean shouldEnd(SamuraiCharacter samurai);
    int damage();
}
```

### Benefits
- Each attack has its own behavior and animation
- Easy to add new attack types
- Attack coordinator selects appropriate strategy based on context
- Combo system automatically progresses through strategies

### Usage Location
- `SamuraiAttackCoordinator` - Selects and manages attack strategies
- `SamuraiAttackState` - Executes the selected strategy
- Combo system chains strategies together

---

## 6. Observer Pattern

### Purpose
Implements a publish-subscribe mechanism for health and death events, allowing multiple listeners to react to state changes.

### Implementation
- **Subject**: `HealthComponent`
- **Observers**: `HealthListener`, `DeathListener`
- **Concrete Observers**: Game systems that react to health changes

### Key Classes
```java
class HealthComponent {
    private Array<HealthListener> listeners;
    
    public void damage(int amount) {
        // Notify all listeners
        for (HealthListener listener : listeners) {
            listener.onDamageTaken(...);
            listener.onHealthChanged(...);
        }
    }
}

interface HealthListener {
    void onDamageTaken(int amount, int current, int max);
    void onHealed(int amount, int current, int max);
    void onHealthChanged(int current, int max);
    void onDeath();
}
```

### Benefits
- Decouples health management from UI and game logic
- Multiple systems can react to health changes (HUD, game over, logging)
- Easy to add new observers without modifying `HealthComponent`
- Supports both player and enemy health systems

### Usage Location
- `SamuraiCharacter` - Health component notifies listeners on damage/death
- `EnemyInstance` - Uses health component for enemy health
- `CelesteGame` - Listens for player death to trigger game over

---

## 7. Factory/Registry Pattern

### Purpose
Centralizes enemy type definitions and provides a lookup mechanism for creating enemy instances.

### Implementation
- **Registry**: `EnemyRegistry` - Static registry of enemy definitions
- **Factory Method**: `EnemyDefinition.builder()` - Builds enemy definitions
- **Product**: `EnemyDefinition` - Contains stats, animations, and metadata

### Key Classes
```java
public final class EnemyRegistry {
    private static final Map<String, EnemyDefinition> DEFINITIONS = new HashMap<>();
    
    public static void register(EnemyDefinition definition);
    public static EnemyDefinition definition(String id);
    public static void registerDefaults();
}

public record EnemyDefinition(
    String id,
    String assetBase,
    EnemyStats stats,
    Map<EnemyAnimationKey, EnemyAnimationSpec> animations
) {
    public static Builder builder(String id, String assetBase) { ... }
}
```

### Benefits
- Centralized enemy configuration
- Easy to add new enemy types
- Type-safe enemy lookup
- Enemy definitions loaded from JSON spawn data
- Supports multiple enemy variants (normal, bosses)

### Usage Location
- `EnemyRegistry.registerDefaults()` - Initializes default enemy types
- `EnemySpawnLoader` - Loads spawn data and looks up definitions
- `EnemyManager.spawnAll()` - Creates instances from definitions

---

## 8. Facade Pattern

### Purpose
Provides a simplified interface to the complex Samurai character subsystem, hiding the complexity of state management, physics, animation, combat, and power-ups.

### Implementation
- **Facade**: `SamuraiCharacter`
- **Subsystems**: 
  - State management (`SamuraiState`)
  - Physics (`SamuraiKinematicController`)
  - Animation system
  - Combat system (`HealthComponent`, `SamuraiAttributes`)
  - Power-up system (Decorators)
  - Input handling (Commands)

### Key Classes
```java
public class SamuraiCharacter {
    // Simplified public API
    public void update(float delta);
    public void draw(SpriteBatch batch);
    public void grantPowerUp(SamuraiPowerUpType type);
    public void takeDamage(int amount);
    public boolean isDead();
    // ... many more simplified methods
}
```

### Benefits
- Simple, unified interface for game logic
- Hides complexity of multiple subsystems
- Easier to use and maintain
- Changes to subsystems don't affect client code
- Centralized logging and state management

### Usage Location
- `CelesteGame` - Uses `SamuraiCharacter` facade for all player interactions
- Commands interact with the facade, not individual subsystems
- Game systems query facade for player state

---

## 9. Template Method Pattern

### Purpose
Defines the skeleton of an algorithm in `PowerUpDecorator`, allowing subclasses to override specific steps.

### Implementation
- **Abstract Class**: `PowerUpDecorator`
- **Template Methods**: Attribute getters that delegate to wrapped component
- **Concrete Classes**: Override specific methods to modify behavior

### Key Classes
```java
abstract class PowerUpDecorator implements SamuraiAttributes {
    protected final SamuraiAttributes delegate;
    
    // Template methods - can be overridden
    @Override
    public float attackMultiplier() {
        return delegate.attackMultiplier(); // Default: forward
    }
    
    // Subclasses override to modify specific attributes
}
```

### Benefits
- Consistent structure across all decorators
- Easy to add new decorator types
- Default behavior is forwarding, can override selectively

---

## Pattern Interactions

### Command → State
Commands trigger state transitions in the Samurai character.

### State → Strategy
Attack states use strategy pattern to select attack behavior.

### Decorator → Facade
Decorators modify attributes that the facade exposes.

### Composite → Observer
Enemy composite tree uses observer pattern for death events.

### Factory → Composite
Factory creates enemy instances that become leaves in the composite tree.

---

## Design Principles Applied

1. **Single Responsibility Principle**: Each class has one clear purpose
2. **Open/Closed Principle**: New states, commands, strategies can be added without modifying existing code
3. **Liskov Substitution Principle**: All state implementations are interchangeable
4. **Interface Segregation**: Small, focused interfaces (Command, State, Strategy)
5. **Dependency Inversion**: Depend on abstractions (interfaces) not concretions

---

## Summary

The game engine successfully implements 9 major design patterns, creating a flexible, maintainable, and extensible codebase. Each pattern addresses specific design challenges:

- **Command**: Input handling and remapping
- **State**: Character and game state management
- **Decorator**: Dynamic power-up system
- **Composite**: Enemy organization
- **Strategy**: Attack system flexibility
- **Observer**: Event-driven health system
- **Factory/Registry**: Enemy type management
- **Facade**: Simplified character API
- **Template Method**: Decorator structure

These patterns work together to create a cohesive, well-architected game engine that is both powerful and maintainable.

