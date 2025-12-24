# Architecture Diagram
## Celeste-like Platformer Game Engine

This document contains Mermaid diagrams illustrating the system architecture and design patterns.

---

## System Architecture Overview

```mermaid
graph TB
    subgraph "Application Layer"
        DL[DesktopLauncher]
        EL[EditorLauncher]
        IL[InspectorLauncher]
    end
    
    subgraph "Game Core"
        CG[CelesteGame]
        GS[GameState]
    end
    
    subgraph "Character System"
        SC[SamuraiCharacter<br/>Facade]
        SKC[SamuraiKinematicController]
        HC[HealthComponent]
        SA[SamuraiAttributes]
    end
    
    subgraph "State System"
        SS[SamuraiState Interface]
        SIS[SamuraiIdleState]
        SRS[SamuraiRunState]
        SAS[SamuraiAttackState]
        SJS[SamuraiJumpState]
        SDS[SamuraiDashState]
    end
    
    subgraph "Command System"
        CMD[SamuraiCommand Interface]
        MLC[MoveLeftCommand]
        MRC[MoveRightCommand]
        JC[JumpCommand]
        AC[AttackCommand]
        DC[DashCommand]
    end
    
    subgraph "Combat System"
        SAC[SamuraiAttackCoordinator]
        SAS2[SamuraiAttackStrategy]
        GA1[GroundAttackOneStrategy]
        GA2[GroundAttackTwoStrategy]
        GA3[GroundAttackThreeStrategy]
        AA[AirAttackStrategy]
    end
    
    subgraph "Power-up System"
        PSA[PowerUpDecorator]
        SPU[ShieldPowerUp]
        SPU2[SpeedPowerUp]
        WPU[WeaponPowerUp]
        BSA[BaseSamuraiAttributes]
    end
    
    subgraph "Enemy System"
        EM[EnemyManager]
        EC[EnemyComponent Interface]
        EGN[EnemyGroupNode<br/>Composite]
        ELN[EnemyLeafNode<br/>Leaf]
        EI[EnemyInstance]
        ER[EnemyRegistry<br/>Factory]
    end
    
    subgraph "World System"
        LD[LevelData]
        LCM[LevelCollisionMap]
        TIO[TilesetIO]
    end
    
    subgraph "Logging"
        GL[GameLogger]
    end
    
    DL --> CG
    EL --> MapEditorApp
    IL --> MapInspector
    
    CG --> SC
    CG --> EM
    CG --> LD
    CG --> GS
    
    SC --> SKC
    SC --> HC
    SC --> SA
    SC --> SS
    
    SS --> SIS
    SS --> SRS
    SS --> SAS
    SS --> SJS
    SS --> SDS
    
    CG --> CMD
    CMD --> MLC
    CMD --> MRC
    CMD --> JC
    CMD --> AC
    CMD --> DC
    
    CMD --> SC
    
    SAS --> SAC
    SAC --> SAS2
    SAS2 --> GA1
    SAS2 --> GA2
    SAS2 --> GA3
    SAS2 --> AA
    
    SA --> PSA
    PSA --> SPU
    PSA --> SPU2
    PSA --> WPU
    SA --> BSA
    
    EM --> EC
    EC --> EGN
    EC --> ELN
    ELN --> EI
    EM --> ER
    
    HC --> HealthListener
    SC --> GL
    EM --> GL
    
    style SC fill:#e1f5ff
    style EM fill:#ffe1f5
    style CG fill:#f5ffe1
    style SS fill:#fff5e1
    style CMD fill:#e1fff5
```

---

## Design Patterns Diagram

```mermaid
graph LR
    subgraph "Command Pattern"
        CMD_INT[SamuraiCommand<br/>Interface]
        CMD_IMPL[Command<br/>Implementations]
        CMD_INT --> CMD_IMPL
    end
    
    subgraph "State Pattern"
        STATE_INT[SamuraiState<br/>Interface]
        STATE_IMPL[State<br/>Implementations]
        STATE_INT --> STATE_IMPL
    end
    
    subgraph "Decorator Pattern"
        ATTR[SamuraiAttributes<br/>Interface]
        BASE[BaseSamuraiAttributes]
        DEC[PowerUpDecorator<br/>Abstract]
        SPU_DEC[ShieldPowerUp]
        SPU2_DEC[SpeedPowerUp]
        WPU_DEC[WeaponPowerUp]
        
        ATTR --> BASE
        ATTR --> DEC
        DEC --> SPU_DEC
        DEC --> SPU2_DEC
        DEC --> WPU_DEC
        SPU_DEC -.decorates.-> BASE
        SPU2_DEC -.decorates.-> BASE
        WPU_DEC -.decorates.-> BASE
    end
    
    subgraph "Composite Pattern"
        COMP_INT[EnemyComponent<br/>Interface]
        COMP_GROUP[EnemyGroupNode<br/>Composite]
        COMP_LEAF[EnemyLeafNode<br/>Leaf]
        
        COMP_INT --> COMP_GROUP
        COMP_INT --> COMP_LEAF
        COMP_GROUP --> COMP_INT
    end
    
    subgraph "Strategy Pattern"
        STRAT_INT[SamuraiAttackStrategy<br/>Interface]
        STRAT_IMPL[Strategy<br/>Implementations]
        STRAT_INT --> STRAT_IMPL
    end
    
    subgraph "Observer Pattern"
        SUBJ[HealthComponent<br/>Subject]
        OBS[HealthListener<br/>Observer]
        SUBJ --> OBS
    end
    
    subgraph "Factory Pattern"
        FACTORY[EnemyRegistry<br/>Factory]
        PRODUCT[EnemyDefinition<br/>Product]
        FACTORY --> PRODUCT
    end
    
    subgraph "Facade Pattern"
        FACADE[SamuraiCharacter<br/>Facade]
        SUBSYS1[State System]
        SUBSYS2[Physics System]
        SUBSYS3[Combat System]
        SUBSYS4[Power-up System]
        
        FACADE --> SUBSYS1
        FACADE --> SUBSYS2
        FACADE --> SUBSYS3
        FACADE --> SUBSYS4
    end
    
    style CMD_INT fill:#ffcccc
    style STATE_INT fill:#ccffcc
    style ATTR fill:#ccccff
    style COMP_INT fill:#ffffcc
    style STRAT_INT fill:#ffccff
    style SUBJ fill:#ccffff
    style FACTORY fill:#ffcccc
    style FACADE fill:#cccccc
```

---

## Class Relationships

```mermaid
classDiagram
    class SamuraiCharacter {
        -SamuraiState currentState
        -HealthComponent health
        -SamuraiAttributes attributes
        +update(float delta)
        +grantPowerUp(SamuraiPowerUpType)
        +takeDamage(int)
    }
    
    class SamuraiState {
        <<interface>>
        +enter(SamuraiCharacter)
        +update(SamuraiCharacter, float)
        +exit(SamuraiCharacter)
    }
    
    class SamuraiCommand {
        <<interface>>
        +execute(SamuraiCharacter, float)
        +release(SamuraiCharacter)
    }
    
    class SamuraiAttributes {
        <<interface>>
        +attackMultiplier() float
        +defenseMultiplier() float
        +speedMultiplier() float
    }
    
    class PowerUpDecorator {
        <<abstract>>
        #SamuraiAttributes delegate
    }
    
    class EnemyManager {
        -EnemyGroupNode rootGroup
        +spawnAll(List~EnemySpawn~)
        +update(float)
    }
    
    class EnemyComponent {
        <<interface>>
        +update(float)
        +draw(SpriteBatch)
        +applyMeleeDamage(...)
    }
    
    class EnemyGroupNode {
        -List~EnemyComponent~ children
    }
    
    class EnemyLeafNode {
        -EnemyInstance instance
    }
    
    class HealthComponent {
        -List~HealthListener~ listeners
        +damage(int)
        +heal(int)
    }
    
    class SamuraiAttackStrategy {
        <<interface>>
        +animationKey() SamuraiAnimationKey
        +canExecute(SamuraiCharacter) boolean
        +damage() int
    }
    
    SamuraiCharacter --> SamuraiState
    SamuraiCharacter --> HealthComponent
    SamuraiCharacter --> SamuraiAttributes
    SamuraiCommand --> SamuraiCharacter
    SamuraiAttributes <|.. PowerUpDecorator
    PowerUpDecorator <|-- ShieldPowerUp
    PowerUpDecorator <|-- SpeedPowerUp
    PowerUpDecorator <|-- WeaponPowerUp
    EnemyManager --> EnemyComponent
    EnemyComponent <|.. EnemyGroupNode
    EnemyComponent <|.. EnemyLeafNode
    EnemyGroupNode --> EnemyComponent
    HealthComponent --> HealthListener
    SamuraiAttackState --> SamuraiAttackStrategy
```

---

## Data Flow Diagram

```mermaid
flowchart TD
    START[User Input] --> CMD[Command Pattern]
    CMD --> SC[SamuraiCharacter Facade]
    
    SC --> STATE[State Pattern]
    STATE --> PHYSICS[Physics System]
    STATE --> COMBAT[Combat System]
    
    COMBAT --> STRAT[Strategy Pattern]
    STRAT --> ANIM[Animation System]
    
    SC --> DECOR[Decorator Pattern]
    DECOR --> ATTR[Attributes]
    ATTR --> COMBAT
    
    SC --> HEALTH[Health Component]
    HEALTH --> OBS[Observer Pattern]
    OBS --> HUD[HUD Update]
    OBS --> GAMEOVER[Game Over Check]
    
    SC --> ENEMY[Enemy System]
    ENEMY --> COMP[Composite Pattern]
    COMP --> AI[Enemy AI]
    COMP --> RENDER[Enemy Rendering]
    
    ENEMY --> FACTORY[Factory Pattern]
    FACTORY --> SPAWN[Enemy Spawning]
    
    SC --> LOG[GameLogger]
    ENEMY --> LOG
    STATE --> LOG
    
    style START fill:#ffcccc
    style SC fill:#ccccff
    style STATE fill:#ccffcc
    style COMBAT fill:#ffffcc
    style ENEMY fill:#ffccff
```

---

## Sequence Diagram: Attack Flow

```mermaid
sequenceDiagram
    participant User
    participant Command as AttackCommand
    participant Character as SamuraiCharacter
    participant State as SamuraiAttackState
    participant Coordinator as AttackCoordinator
    participant Strategy as AttackStrategy
    participant Health as HealthComponent
    participant Enemy as EnemyManager
    
    User->>Command: Press J (Attack)
    Command->>Character: execute(attack)
    Character->>State: handleInput()
    State->>Coordinator: requestStrategy()
    Coordinator->>Strategy: Select strategy
    Strategy-->>Coordinator: Return strategy
    Coordinator-->>State: Return strategy
    State->>Character: switchTo(ATTACK)
    Character->>State: enter()
    State->>Strategy: onEnter()
    
    loop Attack Animation
        Character->>State: update()
        State->>Strategy: onUpdate()
        Strategy->>Character: Check animation
    end
    
    Strategy->>Character: shouldEnd() = true
    State->>Strategy: onExit()
    State->>Character: switchTo(IDLE)
    Character->>Enemy: applyMeleeDamage()
    Enemy->>Health: damage(amount)
    Health->>Enemy: onDeath() if health = 0
```

---

## Component Interaction

```mermaid
graph TB
    subgraph "Input Layer"
        INPUT[Keyboard/Mouse Input]
    end
    
    subgraph "Command Layer"
        CMD[Commands]
    end
    
    subgraph "Character Layer"
        FACADE[SamuraiCharacter Facade]
        STATE[State Machine]
        PHYSICS[Physics Controller]
    end
    
    subgraph "Combat Layer"
        ATTACK[Attack System]
        STRAT[Attack Strategies]
        HEALTH[Health System]
    end
    
    subgraph "Enemy Layer"
        MANAGER[EnemyManager]
        COMPOSITE[Enemy Composite]
        FACTORY[Enemy Registry]
    end
    
    subgraph "World Layer"
        LEVEL[Level Data]
        COLLISION[Collision Map]
        TILESET[Tileset]
    end
    
    subgraph "Presentation Layer"
        RENDER[Renderer]
        HUD[HUD System]
        UI[UI Overlays]
    end
    
    INPUT --> CMD
    CMD --> FACADE
    FACADE --> STATE
    FACADE --> PHYSICS
    FACADE --> ATTACK
    FACADE --> HEALTH
    
    ATTACK --> STRAT
    HEALTH --> HUD
    
    FACADE --> MANAGER
    MANAGER --> COMPOSITE
    MANAGER --> FACTORY
    
    FACADE --> LEVEL
    PHYSICS --> COLLISION
    LEVEL --> TILESET
    
    FACADE --> RENDER
    COMPOSITE --> RENDER
    LEVEL --> RENDER
    RENDER --> HUD
    RENDER --> UI
    
    style FACADE fill:#e1f5ff
    style MANAGER fill:#ffe1f5
    style LEVEL fill:#f5ffe1
```

---

## Power-up Decorator Chain

```mermaid
graph LR
    BASE[BaseSamuraiAttributes<br/>attack: 1.0x<br/>defense: 1.0x<br/>speed: 1.0x]
    
    SHIELD[ShieldPowerUp<br/>defense: 1.5x]
    SHIELD -.decorates.-> BASE
    
    SPEED[SpeedPowerUp<br/>speed: 1.3x]
    SPEED -.decorates.-> SHIELD
    
    WEAPON[WeaponPowerUp<br/>attack: 1.5x]
    WEAPON -.decorates.-> SPEED
    
    FINAL[Final Attributes<br/>attack: 1.5x<br/>defense: 1.5x<br/>speed: 1.3x]
    WEAPON --> FINAL
    
    style BASE fill:#ccccff
    style SHIELD fill:#ffcccc
    style SPEED fill:#ccffcc
    style WEAPON fill:#ffffcc
    style FINAL fill:#ffccff
```

---

## Enemy Composite Structure

```mermaid
graph TD
    ROOT[EnemyManager<br/>rootGroup]
    
    ROOT --> GROUP1[EnemyGroupNode<br/>redDeon]
    ROOT --> GROUP2[EnemyGroupNode<br/>skeletonEnemie]
    ROOT --> GROUP3[EnemyGroupNode<br/>deathBoss]
    
    GROUP1 --> LEAF1[EnemyLeafNode<br/>redDeon #1]
    GROUP1 --> LEAF2[EnemyLeafNode<br/>redDeon #2]
    
    GROUP2 --> LEAF3[EnemyLeafNode<br/>skeleton #1]
    
    GROUP3 --> LEAF4[EnemyLeafNode<br/>boss #1]
    
    LEAF1 --> INST1[EnemyInstance]
    LEAF2 --> INST2[EnemyInstance]
    LEAF3 --> INST3[EnemyInstance]
    LEAF4 --> INST4[EnemyInstance]
    
    style ROOT fill:#ffcccc
    style GROUP1 fill:#ccffcc
    style GROUP2 fill:#ccffcc
    style GROUP3 fill:#ccffcc
    style LEAF1 fill:#ccccff
    style LEAF2 fill:#ccccff
    style LEAF3 fill:#ccccff
    style LEAF4 fill:#ccccff
```

---

These diagrams illustrate the complete architecture of the game engine, showing how all design patterns interact and how the system components are organized.

