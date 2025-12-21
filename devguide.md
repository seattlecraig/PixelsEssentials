# PixelsEssentials Developer Guide

A comprehensive technical reference for developers working with or extending PixelsEssentials.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Class Structure](#class-structure)
3. [Data Flow](#data-flow)
4. [Event System](#event-system)
5. [Command System](#command-system)
6. [Data Persistence](#data-persistence)
7. [Permission System](#permission-system)
8. [External Dependencies](#external-dependencies)
9. [Extending the Plugin](#extending-the-plugin)
10. [Code Conventions](#code-conventions)

---

## Architecture Overview

PixelsEssentials follows a single-class monolithic architecture pattern. All functionality is contained within `PixelsEssentials.java`, implementing three core Bukkit interfaces:

- `CommandExecutor` - Handles all command execution
- `TabCompleter` - Provides tab completion for commands
- `Listener` - Handles Bukkit events

### Design Philosophy

The plugin uses a **command-routing pattern** where `onCommand()` acts as a central router, delegating to specific handler methods based on command name. This keeps the main entry point clean while allowing complex command logic in dedicated methods.

Data persistence uses a **cache-through pattern** where player data is loaded into memory on demand and written back to disk on modification or player disconnect.

---

## Class Structure

### Main Class: PixelsEssentials

```
PixelsEssentials extends JavaPlugin
    implements CommandExecutor, TabCompleter, Listener
```

### Instance Fields

| Field | Type | Purpose |
|-------|------|---------|
| `playerDataFolder` | `File` | Reference to `plugins/PixelsEssentials/playerdata/` |
| `playerDataCache` | `Map<UUID, PlayerData>` | In-memory cache of player data |
| `debugMode` | `boolean` | Controls verbose console logging |
| `deathLocations` | `Map<UUID, Location>` | Temporary storage for keeppos respawn |

### Inner Classes

#### PlayerData

Stores all persistent data for a single player:

```java
private static class PlayerData {
    Map<String, LocationData> homes;      // Home name -> location
    LocationData lastTeleportLocation;     // For /back after teleport
    LocationData lastDeathLocation;        // For /back after death
    boolean lastWasDeath;                  // Determines which location /back uses
    LocationData logoutLocation;           // Where player logged out
    boolean autofeedEnabled;               // Autofeed toggle (default: true)
}
```

#### LocationData

Immutable location storage with world UUID for reliability:

```java
private static class LocationData {
    final String worldUuid;    // Primary world identifier
    final String worldName;    // Fallback/display name
    final double x, y, z;      // Coordinates
    final float yaw, pitch;    // Rotation
    
    static LocationData fromLocation(Location location);  // Factory method
    Location toLocation();                                 // Conversion method
}
```

The dual world identification (UUID + name) ensures locations survive world renames while maintaining human readability.

---

## Data Flow

### Player Data Lifecycle

```
Player Action
     │
     ▼
loadPlayerData(uuid)  ─────────────► Check cache
     │                                    │
     │ (cache miss)                       │ (cache hit)
     ▼                                    │
Load from YAML file                       │
     │                                    │
     ▼                                    │
Store in playerDataCache ◄────────────────┘
     │
     ▼
Modify data in cache
     │
     ▼
savePlayerData(uuid)
     │
     ▼
Write to YAML file
```

### Location Tracking Flow

```
PlayerTeleportEvent
     │
     ▼
Check distance > 1 block?  ──► No ──► Ignore
     │
     │ Yes
     ▼
Save "from" location as lastTeleportLocation
Set lastWasDeath = false
     │
     ▼
savePlayerData()
```

```
PlayerDeathEvent
     │
     ├──► Save death location as lastDeathLocation
     │    Set lastWasDeath = true
     │
     ├──► keepxp permission? ──► setDroppedExp(0), setKeepLevel(true)
     │
     ├──► keepinv permission? ──► setKeepInventory(true), clear drops
     │
     └──► keeppos permission? ──► Store in deathLocations map
```

```
PlayerRespawnEvent
     │
     ▼
Check deathLocations map
     │
     │ (entry exists)
     ▼
setRespawnLocation(deathLocation)
Remove from deathLocations
```

---

## Event System

### Registered Events

| Event | Priority | Purpose |
|-------|----------|---------|
| `PlayerTeleportEvent` | MONITOR, ignoreCancelled | Track last teleport location |
| `PlayerDeathEvent` | LOWEST | Death protections (must run first) |
| `PlayerRespawnEvent` | HIGHEST | Keeppos respawn teleport |
| `FoodLevelChangeEvent` | HIGHEST | Autofeed hunger restoration |
| `PlayerQuitEvent` | MONITOR | Save logout location and persist data |

### Event Priority Rationale

- **LOWEST for death**: Ensures keepinv/keepxp are set before other plugins process the event, preventing item duplication exploits.
- **HIGHEST for respawn/food**: Ensures our modifications happen last and aren't overwritten.
- **MONITOR for teleport/quit**: Observation only, doesn't modify event outcome.

---

## Command System

### Command Routing

The `onCommand()` method routes based on command name:

```java
switch (cmdName) {
    case "repair":           return handleRepairCommand(sender, args);
    case "home":
    case "homes":            return handleHomeCommand(sender, args);
    case "sethome":          return handleSetHomeCommand(sender, args);
    case "delhome":          return handleDelHomeCommand(sender, args);
    case "homeinfo":         return handleHomeInfoCommand(sender, args);
    case "autofeed":         return handleAutofeedCommand(sender, args);
    case "back":             return handleBackCommand(sender);
    case "giveenchanteditem": return handleGiveEnchantedItemCommand(sender, args);
    case "pixelsessentials": return handleMainCommand(sender, args);
}
```

### Handler Method Pattern

Each handler follows this pattern:

1. Check if sender is Player (if required)
2. Check permissions
3. Validate arguments
4. Load player data (if needed)
5. Execute logic
6. Save player data (if modified)
7. Send feedback message
8. Return true

### Tab Completion

Tab completion mirrors the command routing:

```java
switch (cmdName) {
    case "repair":           return tabCompleteRepair(sender, args);
    case "home":
    case "homes":            return tabCompleteHome(sender, args);
    // ... etc
}
```

Each tab completer method:
1. Checks permissions before suggesting options
2. Uses `filterCompletions()` to match partial input
3. Returns context-appropriate suggestions based on argument position

---

## Data Persistence

### File Structure

```
plugins/PixelsEssentials/
├── config.yml              # Plugin configuration
└── playerdata/
    ├── <uuid1>.yml         # Player 1 data
    ├── <uuid2>.yml         # Player 2 data
    └── ...
```

### YAML Schema

```yaml
# Player data file schema
lastteleportlocation:       # Optional
  world: "uuid-string"
  world-name: "string"
  x: double
  y: double
  z: double
  yaw: double
  pitch: double

lastdeathlocation:          # Optional, same structure

last-was-death: boolean     # true = death, false = teleport

logoutlocation:             # Optional, same structure

homes:                      # Map of home name -> location
  <homename>:               # Lowercase key
    world: "uuid-string"
    world-name: "string"
    x: double
    y: double
    z: double
    yaw: double
    pitch: double

autofeed: boolean           # Default: true
```

### Load/Save Methods

**Loading:**
```java
private void loadPlayerData(UUID uuid) {
    // Skip if already cached
    // Read YAML file
    // Parse each section into LocationData objects
    // Store in playerDataCache
}
```

**Saving:**
```java
private void savePlayerData(UUID uuid) {
    // Get from cache
    // Build YamlConfiguration
    // Write each LocationData to config
    // Save to file
}
```

### Cache Management

- Data loaded on-demand (first access)
- Data saved immediately on modification
- Cache cleared on `/pe reload`
- All cached data saved on plugin disable

---

## Permission System

### Home Limit Calculation

The `getMaxHomes()` method implements tiered permissions:

```java
private int getMaxHomes(Player player) {
    ConfigurationSection multipleSection = config.getConfigurationSection("sethome-multiple");
    int maxHomes = 0;
    
    for (String tier : multipleSection.getKeys(false)) {
        String permission = "pixelsessentials.sethome." + tier;
        int tierValue = multipleSection.getInt(tier, 0);
        
        if (player.hasPermission(permission) && tierValue > maxHomes) {
            maxHomes = tierValue;
        }
    }
    
    return Math.max(maxHomes, 1);  // Minimum 1 home
}
```

This allows:
- Multiple tiers defined in config
- Players to have multiple tier permissions
- Highest value wins
- Minimum of 1 home guaranteed

### Permission Hierarchy

```
pixelsessentials.repair.*
├── pixelsessentials.repair.hand
├── pixelsessentials.repair.all
└── pixelsessentials.repair.player

pixelsessentials.back
└── pixelsessentials.back.ondeath   (additional permission for death return)

pixelsessentials.sethome
└── pixelsessentials.sethome.<tier> (config-defined tiers)
```

---

## External Dependencies

### Required

- **Paper/Spigot API 1.21+** - Core server API
- **Adventure API** - Modern text component system (bundled with Paper)

### Optional

- **ItemsAdder** - Custom item support for `/gei` command
  - Soft dependency (plugin works without it)
  - Uses `dev.lone.itemsadder.api.CustomStack`

### API Usage

**Adventure API Components:**
```java
// All messages use Adventure Components
sender.sendMessage(Component.text("Message", NamedTextColor.GREEN));

// Composite messages
sender.sendMessage(Component.text("Prefix: ", NamedTextColor.GREEN)
    .append(Component.text("value", NamedTextColor.YELLOW)));
```

**ItemsAdder Integration:**
```java
CustomStack customStack = CustomStack.getInstance(itemId);
if (customStack != null) {
    ItemStack itemStack = customStack.getItemStack();
    // Modify and give to player
}
```

**Enchantment Registry (1.21+):**
```java
NamespacedKey key = NamespacedKey.minecraft(enchantName);
Enchantment enchantment = Registry.ENCHANTMENT.get(key);
itemStack.addUnsafeEnchantment(enchantment, level);
```

---

## Extending the Plugin

### Adding a New Command

1. Add command to `plugin.yml`
2. Register executor in `onEnable()`
3. Add case to `onCommand()` switch
4. Create handler method following the pattern
5. Add tab completion if needed

### Adding a New Event

1. Create method with `@EventHandler` annotation
2. Choose appropriate priority
3. Events are auto-registered via `registerEvents(this, this)`

### Adding Player Data Fields

1. Add field to `PlayerData` class
2. Add load logic in `loadPlayerData()`
3. Add save logic in `savePlayerData()`
4. Use the field in your feature

---

## Code Conventions

### Naming

- Handler methods: `handle<Command>Command()`
- Tab completers: `tabComplete<Command>()`
- Permission strings: `pixelsessentials.<feature>.<action>`

### Messages

- Success: `NamedTextColor.GREEN`
- Warning/Info: `NamedTextColor.YELLOW`
- Error: `NamedTextColor.RED`
- Values/Names: `NamedTextColor.AQUA`
- Secondary text: `NamedTextColor.GRAY`

### Null Safety

- Always check `sender instanceof Player` before casting
- Check `player.getInventory().getItemInMainHand()` for AIR
- Check `ItemMeta` exists before operations
- Check worlds exist before teleporting

### Debug Logging

```java
if (debugMode) {
    getLogger().info("[DEBUG] Descriptive message: " + variable);
}
```

---

## Build Information

### Maven Dependencies

```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>1.21-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>dev.lone</groupId>
    <artifactId>itemsadder-api</artifactId>
    <version>3.6.1</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml Structure

```yaml
name: PixelsEssentials
version: 1.0.0
main: com.supafloof.pixelsessentials.PixelsEssentials
api-version: '1.21'
softdepend: [ItemsAdder]

commands:
  repair:
    description: Repair items
    aliases: []
  home:
    description: Teleport to home
    aliases: [homes]
  # ... etc

permissions:
  pixelsessentials.repair.hand:
    default: op
  # ... etc
```

---

*PixelsEssentials by SupaFloof Games, LLC*
