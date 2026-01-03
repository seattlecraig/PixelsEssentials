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
8. [External Integrations](#external-integrations)
9. [Balance Leaderboard Signs](#balance-leaderboard-signs)
10. [PlaceholderAPI Expansion](#placeholderapi-expansion)
11. [Extending the Plugin](#extending-the-plugin)
12. [Code Conventions](#code-conventions)

---

## Architecture Overview

PixelsEssentials follows a single-class monolithic architecture pattern. All functionality is contained within `PixelsEssentials.java`, implementing three core Bukkit interfaces:

- `CommandExecutor` - Handles all command execution
- `TabCompleter` - Provides tab completion for commands
- `Listener` - Handles Bukkit events

### Design Patterns

**Command Routing Pattern:**
The `onCommand()` method acts as a central router, delegating to specific `handle*Command()` methods based on command name. This keeps the entry point clean while allowing complex logic in dedicated methods.

**Cache-Through Pattern:**
Player data is loaded into memory on demand and written back to disk immediately on modification. This balances performance with data durability.

**Factory Pattern:**
`LocationData.fromLocation()` provides a clean factory method for creating location data from Bukkit Location objects.

### Package Structure

```
com.supafloof.pixelsessentials
└── PixelsEssentials.java      # Main class (single-file architecture)
    ├── PlayerData             # Inner class - player data container
    ├── LocationData           # Inner class - immutable location storage
    └── PixelsEssentialsExpansion  # Inner class - PlaceholderAPI expansion
```

---

## Class Structure

### Main Class

```java
public class PixelsEssentials extends JavaPlugin 
    implements CommandExecutor, TabCompleter, Listener
```

### Instance Fields

| Field | Type | Purpose |
|-------|------|---------|
| `playerDataFolder` | `File` | Reference to `plugins/PixelsEssentials/playerdata/` |
| `playerDataCache` | `Map<UUID, PlayerData>` | In-memory cache of loaded player data |
| `debugMode` | `boolean` | Controls verbose console logging |
| `deathLocations` | `Map<UUID, Location>` | Temporary storage for keeppos respawn locations |
| `economy` | `Economy` | Vault economy provider (nullable) |
| `balanceSigns` | `ConcurrentHashMap<Location, Integer>` | Balance sign locations and rankings |
| `pendingBalanceSigns` | `ConcurrentHashMap<UUID, Integer>` | Pending sign creation requests |
| `signUpdateInterval` | `int` | Sign refresh interval in seconds |
| `unlockRecipesOnJoin` | `boolean` | Recipe auto-unlock toggle |
| `topBalancesCache` | `List<Map.Entry<UUID, Double>>` | Cached balance rankings |
| `topBalancesCacheTime` | `long` | Cache timestamp for validity checking |

### Inner Class: PlayerData

Stores all persistent data for a single player:

```java
private static class PlayerData {
    Map<String, LocationData> homes = new HashMap<>();  // Home name -> location
    LocationData lastTeleportLocation;    // For /back after teleport
    LocationData lastDeathLocation;       // For /back after death  
    boolean lastWasDeath = false;         // Determines which location /back uses
    LocationData logoutLocation;          // Where player logged out
    boolean autofeedEnabled = true;       // Autofeed toggle
}
```

### Inner Class: LocationData

Immutable location storage with world UUID for reliability:

```java
private static class LocationData {
    final String worldUuid;     // Primary world identifier (survives renames)
    final String worldName;     // Fallback/display name
    final double x, y, z;       // Coordinates
    final float yaw, pitch;     // Rotation
    
    // Factory method - creates from Bukkit Location
    static LocationData fromLocation(Location location);
    
    // Conversion method - returns Bukkit Location (null if world missing)
    Location toLocation();
}
```

**World Resolution Logic:**
`toLocation()` first attempts lookup by UUID, then falls back to world name if UUID lookup fails (handles world recreation scenarios).

### Inner Class: PixelsEssentialsExpansion

PlaceholderAPI expansion providing custom placeholders:

```java
private class PixelsEssentialsExpansion extends PlaceholderExpansion {
    // Identifier: "pixelsessentials"
    // Placeholders: current_health, max_health, formatted_balance,
    //               [slot]_total_durability, [slot]_current_durability
}
```

---

## Data Flow

### Player Data Lifecycle

```
Player Action (command, event)
         │
         ▼
    loadPlayerData(uuid)
         │
         ├─── Cache Hit ──────────────────┐
         │                                │
         ▼ Cache Miss                     │
    Load from YAML file                   │
         │                                │
         ▼                                │
    Parse sections into                   │
    PlayerData object                     │
         │                                │
         ▼                                │
    Store in playerDataCache ◄────────────┘
         │
         ▼
    Modify data in cache
         │
         ▼
    savePlayerData(uuid)
         │
         ▼
    Write to YAML file immediately
```

### Location Tracking: Teleport

```
PlayerTeleportEvent (MONITOR priority, ignoreCancelled=true)
         │
         ▼
    Check distance > 1 block?
         │
    No ──┴──► Ignore (minor movement)
         │
    Yes  │
         ▼
    Get "from" location
         │
         ▼
    setLastTeleportLocation(player, from)
         │
         ├── lastTeleportLocation = LocationData.fromLocation(from)
         ├── lastWasDeath = false
         └── savePlayerData(uuid)
```

### Location Tracking: Death

```
PlayerDeathEvent (LOWEST priority)
         │
         ├───► setLastDeathLocation(player, deathLocation)
         │         ├── lastDeathLocation = LocationData.fromLocation(loc)
         │         ├── lastWasDeath = true
         │         └── savePlayerData(uuid)
         │
         ├───► Has keepxp permission?
         │         └── setDroppedExp(0), setKeepLevel(true)
         │
         ├───► Has keepinv permission?
         │         └── setKeepInventory(true), getDrops().clear()
         │
         └───► Has keeppos permission?
                   └── deathLocations.put(uuid, deathLocation.clone())
```

### Location Tracking: Respawn

```
PlayerRespawnEvent (HIGHEST priority)
         │
         ▼
    deathLocations.containsKey(uuid)?
         │
    No ──┴──► Normal respawn
         │
    Yes  │
         ▼
    Location deathLoc = deathLocations.remove(uuid)
         │
         ▼
    event.setRespawnLocation(deathLoc)
```

### Back Command Logic

```
/back command
         │
         ▼
    Load player data
         │
         ▼
    Check lastWasDeath flag
         │
    ├── true (last event was death)
    │       │
    │       ▼
    │   Has back.ondeath permission?
    │       │
    │   Yes ─┴─► Use lastDeathLocation
    │       │
    │   No  │
    │       ▼
    │   Use lastTeleportLocation (fallback)
    │
    └── false (last event was teleport)
            │
            ▼
        Use lastTeleportLocation
```

---

## Event System

### Registered Events

| Event | Priority | ignoreCancelled | Purpose |
|-------|----------|-----------------|---------|
| `PlayerTeleportEvent` | MONITOR | true | Track last teleport location |
| `PlayerDeathEvent` | LOWEST | false | Death protections (runs first) |
| `PlayerRespawnEvent` | HIGHEST | false | Keeppos respawn teleport |
| `FoodLevelChangeEvent` | HIGHEST | false | Autofeed hunger restoration |
| `PlayerQuitEvent` | MONITOR | false | Save logout location and persist data |
| `PlayerInteractEvent` | HIGH | false | Balance sign creation |
| `PlayerJoinEvent` | MONITOR | false | Recipe unlock on join |

### Event Priority Rationale

- **LOWEST for PlayerDeathEvent:** Ensures keepinv/keepxp are set before other plugins process the event, preventing item duplication exploits with plugins that might also modify drops.

- **HIGHEST for PlayerRespawnEvent:** Ensures keeppos location overrides other plugins' respawn location changes.

- **HIGHEST for FoodLevelChangeEvent:** Allows autofeed to cancel and override the food level change.

- **MONITOR for teleport/quit/join:** Observation only - doesn't modify the event outcome, just records data.

- **HIGH for PlayerInteractEvent:** Runs before most handlers to intercept sign clicks for balance sign creation.

---

## Command System

### Command Routing

The `onCommand()` method routes based on command name:

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String cmdName = command.getName().toLowerCase();
    
    switch (cmdName) {
        case "repair":            return handleRepairCommand(sender, args);
        case "home":
        case "homes":             return handleHomeCommand(sender, args);
        case "sethome":           return handleSetHomeCommand(sender, args);
        case "delhome":           return handleDelHomeCommand(sender, args);
        case "homeinfo":          return handleHomeInfoCommand(sender, args);
        case "autofeed":          return handleAutofeedCommand(sender, args);
        case "back":              return handleBackCommand(sender);
        case "giveenchanteditem": return handleGiveEnchantedItemCommand(sender, args);
        case "pixelsessentials":  return handleMainCommand(sender, args);
        default:                  return false;
    }
}
```

### Handler Method Pattern

Each handler follows a consistent pattern:

```java
private boolean handleXxxCommand(CommandSender sender, String[] args) {
    // 1. Check if sender is Player (if required)
    if (!(sender instanceof Player)) {
        sender.sendMessage(Component.text("...", NamedTextColor.RED));
        return true;
    }
    Player player = (Player) sender;
    
    // 2. Check permissions
    if (!player.hasPermission("pixelsessentials.xxx")) {
        player.sendMessage(Component.text("...", NamedTextColor.RED));
        return true;
    }
    
    // 3. Validate arguments
    if (args.length < 1) {
        player.sendMessage(Component.text("Usage: ...", NamedTextColor.RED));
        return true;
    }
    
    // 4. Load player data (if needed)
    loadPlayerData(player.getUniqueId());
    PlayerData data = playerDataCache.get(player.getUniqueId());
    
    // 5. Execute logic
    // ...
    
    // 6. Save player data (if modified)
    savePlayerData(player.getUniqueId());
    
    // 7. Send feedback message
    player.sendMessage(Component.text("...", NamedTextColor.GREEN));
    
    // 8. Return true (command was handled)
    return true;
}
```

### Tab Completion

Tab completion mirrors the command routing pattern:

```java
@Override
public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    String cmdName = command.getName().toLowerCase();
    
    switch (cmdName) {
        case "repair":            return tabCompleteRepair(sender, args);
        case "home":
        case "homes":             return tabCompleteHome(sender, args);
        case "sethome":           return new ArrayList<>();  // No completion needed
        case "delhome":
        case "homeinfo":          return tabCompleteHomeNames(sender, args);
        case "autofeed":          return tabCompleteAutofeed(sender, args);
        case "giveenchanteditem": return tabCompleteGiveEnchantedItem(sender, args);
        case "pixelsessentials":  return tabCompleteMain(sender, args);
        default:                  return new ArrayList<>();
    }
}
```

Each tab completer:
1. Checks permissions before suggesting options
2. Returns context-appropriate suggestions based on argument position
3. Uses `filterCompletions()` to match partial input

```java
private List<String> filterCompletions(List<String> completions, String partial) {
    return completions.stream()
        .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
        .toList();
}
```

---

## Data Persistence

### File Structure

```
plugins/PixelsEssentials/
├── config.yml              # Plugin configuration
├── signs.yml               # Balance leaderboard sign locations
└── playerdata/
    ├── <uuid1>.yml         # Player 1 data
    ├── <uuid2>.yml         # Player 2 data
    └── ...
```

### YAML Schema: Player Data

```yaml
# Last teleport location (for /back)
lastteleportlocation:           # Optional section
  world: "uuid-string"          # World UUID as string
  world-name: "world"           # World name (fallback/display)
  x: 100.5                      # Double
  y: 64.0                       # Double
  z: -200.3                     # Double
  yaw: 90.0                     # Float (stored as double)
  pitch: 0.0                    # Float (stored as double)

lastdeathlocation:              # Optional, same structure as above

last-was-death: false           # Boolean - true if last event was death

logoutlocation:                 # Optional, same structure as above

homes:                          # Map of home name -> location
  <homename>:                   # Lowercase key (case-insensitive storage)
    world: "uuid-string"
    world-name: "world"
    x: 100.0
    y: 64.0
    z: 100.0
    yaw: 0.0
    pitch: 0.0

autofeed: true                  # Boolean, default true
```

### YAML Schema: Signs

```yaml
signs:
  0:
    world: "world"
    x: 100
    y: 64
    z: 200
    place: 1
  1:
    world: "world"
    x: 102
    y: 64
    z: 200
    place: 2
```

### Load Method Implementation

```java
private void loadPlayerData(UUID uuid) {
    // Skip if already cached (performance optimization)
    if (playerDataCache.containsKey(uuid)) {
        return;
    }
    
    File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
    PlayerData data = new PlayerData();
    
    if (playerFile.exists()) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        // Load each section using helper method
        if (config.contains("lastteleportlocation")) {
            data.lastTeleportLocation = loadLocationData(
                config.getConfigurationSection("lastteleportlocation"));
        }
        // ... similar for other location fields
        
        // Load homes map
        ConfigurationSection homesSection = config.getConfigurationSection("homes");
        if (homesSection != null) {
            for (String homeName : homesSection.getKeys(false)) {
                LocationData home = loadLocationData(
                    homesSection.getConfigurationSection(homeName));
                if (home != null) {
                    data.homes.put(homeName.toLowerCase(), home);
                }
            }
        }
        
        // Load autofeed (default true for new players)
        data.autofeedEnabled = config.getBoolean("autofeed", true);
    }
    
    playerDataCache.put(uuid, data);
}
```

### Save Method Implementation

```java
private void savePlayerData(UUID uuid) {
    PlayerData data = playerDataCache.get(uuid);
    if (data == null) return;
    
    File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
    YamlConfiguration config = new YamlConfiguration();
    
    // Save each location using helper method
    if (data.lastTeleportLocation != null) {
        saveLocationData(config, "lastteleportlocation", data.lastTeleportLocation);
    }
    // ... similar for other location fields
    
    // Save homes map
    for (Map.Entry<String, LocationData> entry : data.homes.entrySet()) {
        saveLocationData(config, "homes." + entry.getKey(), entry.getValue());
    }
    
    // Save autofeed setting
    config.set("autofeed", data.autofeedEnabled);
    
    try {
        config.save(playerFile);
    } catch (IOException e) {
        getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
    }
}
```

---

## Permission System

### Home Limit Calculation

The `getMaxHomes()` method implements tiered permissions:

```java
private int getMaxHomes(Player player) {
    ConfigurationSection multipleSection = 
        getConfig().getConfigurationSection("sethome-multiple");
    
    if (multipleSection == null) {
        return 1;  // Default if no config
    }
    
    int maxHomes = 0;
    
    // Check each tier permission
    for (String tier : multipleSection.getKeys(false)) {
        String permission = "pixelsessentials.sethome." + tier;
        int tierValue = multipleSection.getInt(tier, 0);
        
        if (player.hasPermission(permission) && tierValue > maxHomes) {
            maxHomes = tierValue;
        }
    }
    
    // Minimum 1 home if player has base sethome permission
    return Math.max(maxHomes, 1);
}
```

This design allows:
- Unlimited tiers defined in config
- Players can have multiple tier permissions
- Highest value wins
- Minimum of 1 home guaranteed

### Permission Node Structure

```
pixelsessentials.repair.*
├── pixelsessentials.repair.hand
├── pixelsessentials.repair.all
└── pixelsessentials.repair.player

pixelsessentials.back
└── pixelsessentials.back.ondeath   (additional permission)

pixelsessentials.sethome
└── pixelsessentials.sethome.<tier> (config-defined tiers)

pixelsessentials.keep*
├── pixelsessentials.keepxp
├── pixelsessentials.keepinv
└── pixelsessentials.keeppos
```

---

## External Integrations

### Vault Economy

**Initialization:**
```java
if (getServer().getPluginManager().getPlugin("Vault") != null) {
    RegisteredServiceProvider<Economy> rsp = 
        getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp != null) {
        economy = rsp.getProvider();
    }
}
```

**Usage:**
```java
// Get player balance
double balance = economy.getBalance(player);

// Get offline player balance
double balance = economy.getBalance(Bukkit.getOfflinePlayer(uuid));
```

### PlaceholderAPI

**Registration:**
```java
if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
    new PixelsEssentialsExpansion(this).register();
}
```

### ItemsAdder

**Item Retrieval:**
```java
CustomStack customStack = CustomStack.getInstance(itemId);
if (customStack != null) {
    ItemStack itemStack = customStack.getItemStack();
    // Modify and give to player
}
```

**Enchantment Application (1.21+ Registry API):**
```java
NamespacedKey key = NamespacedKey.minecraft(enchantName);
Enchantment enchantment = Registry.ENCHANTMENT.get(key);
if (enchantment != null) {
    itemStack.addUnsafeEnchantment(enchantment, level);
}
```

---

## Balance Leaderboard Signs

### Architecture

Balance signs use three maps:
- `balanceSigns` - Active signs: Location → Ranking position
- `pendingBalanceSigns` - Creation pending: Player UUID → Ranking position
- `topBalancesCache` - Cached rankings: List of UUID → Balance entries

### Sign Update Flow

```
Scheduled Task (every signUpdateInterval seconds)
         │
         ▼
    updateAllBalanceSigns()
         │
         ├── Invalidate topBalancesCache
         │
         └── For each sign in balanceSigns:
                  │
                  ▼
             updateBalanceSign(location, place)
                  │
                  ├── Check if block is still a sign
                  │       │
                  │   No ─┴─► Remove from balanceSigns
                  │
                  └── Get player at ranking via getTopBalancesCached()
                           │
                           ▼
                      Update sign lines:
                      Line 0: "BALANCE" (gold, bold)
                      Line 1: "#N" (yellow)
                      Line 2: PlayerName (green)
                      Line 3: "$X.XX M" (aqua)
```

### Cache Validity

```java
private static final long BALANCE_CACHE_VALIDITY_MS = 5000;  // 5 seconds

private List<Map.Entry<UUID, Double>> getTopBalancesCached(int limit) {
    long currentTime = System.currentTimeMillis();
    
    // Check cache validity
    if (topBalancesCache != null && 
        (currentTime - topBalancesCacheTime) < BALANCE_CACHE_VALIDITY_MS) {
        return topBalancesCache.subList(0, Math.min(limit, topBalancesCache.size()));
    }
    
    // Rebuild cache
    topBalancesCache = getTopBalances(limit);
    topBalancesCacheTime = currentTime;
    
    return topBalancesCache;
}
```

---

## PlaceholderAPI Expansion

### Implementation

```java
private class PixelsEssentialsExpansion extends PlaceholderExpansion {
    
    @Override
    public String getIdentifier() {
        return "pixelsessentials";
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return null;
        
        switch (params.toLowerCase()) {
            case "current_health":
                return String.format("%.1f", player.getHealth());
                
            case "max_health":
                double maxHealth = player.getAttribute(
                    Attribute.GENERIC_MAX_HEALTH).getValue();
                return String.format("%.1f", maxHealth);
                
            case "formatted_balance":
                if (economy == null) return "0";
                return formatBalance(economy.getBalance(player));
                
            // Armor durability cases...
            
            default:
                return null;
        }
    }
}
```

### Balance Formatting Logic

```java
private String formatBalance(double balance) {
    if (balance < 0) return "-" + formatBalance(-balance);
    
    final double TRILLION = 1_000_000_000_000.0;
    final double BILLION = 1_000_000_000.0;
    final double MILLION = 1_000_000.0;
    
    if (balance >= TRILLION) {
        return String.format("%.2f T", balance / TRILLION);
    } else if (balance >= BILLION) {
        return String.format("%.2f B", balance / BILLION);
    } else if (balance >= MILLION) {
        return String.format("%.2f M", balance / MILLION);
    } else {
        return String.format("%,d", (long) balance);
    }
}
```

---

## Extending the Plugin

### Adding a New Command

1. **Add to plugin.yml:**
```yaml
commands:
  newcommand:
    description: Description here
    usage: /<command> [args]
```

2. **Register in onEnable():**
```java
getCommand("newcommand").setExecutor(this);
getCommand("newcommand").setTabCompleter(this);
```

3. **Add case to onCommand():**
```java
case "newcommand": return handleNewCommand(sender, args);
```

4. **Create handler method:**
```java
private boolean handleNewCommand(CommandSender sender, String[] args) {
    // Follow the handler pattern
}
```

5. **Add tab completion (if needed):**
```java
case "newcommand": return tabCompleteNewCommand(sender, args);
```

### Adding Player Data Fields

1. **Add field to PlayerData class:**
```java
boolean newSetting = false;
```

2. **Add load logic:**
```java
data.newSetting = config.getBoolean("new-setting", false);
```

3. **Add save logic:**
```java
config.set("new-setting", data.newSetting);
```

### Adding a New Event Handler

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onNewEvent(SomeEvent event) {
    // Event handling logic
}
```

Events are auto-registered via `registerEvents(this, this)` in onEnable().

---

## Code Conventions

### Naming

- Handler methods: `handle<Command>Command()`
- Tab completers: `tabComplete<Command>()`
- Location setters: `setLast<Type>Location()`
- Permission strings: `pixelsessentials.<feature>.<action>`

### Message Colors (Adventure API)

| Color | Usage |
|-------|-------|
| `NamedTextColor.GREEN` | Success messages |
| `NamedTextColor.YELLOW` | Warnings, info, secondary success |
| `NamedTextColor.RED` | Errors, permission denied |
| `NamedTextColor.AQUA` | Highlighted values, player names |
| `NamedTextColor.GRAY` | Secondary/help text |
| `NamedTextColor.GOLD` | Headers, decorative borders |
| `NamedTextColor.LIGHT_PURPLE` | Author credit |

### Null Safety Patterns

```java
// Check sender type
if (!(sender instanceof Player)) {
    sender.sendMessage(Component.text("...", NamedTextColor.RED));
    return true;
}

// Check held item
ItemStack item = player.getInventory().getItemInMainHand();
if (item == null || item.getType() == Material.AIR) { ... }

// Check damageable
ItemMeta meta = item.getItemMeta();
if (!(meta instanceof Damageable)) { ... }

// Check world exists
Location location = locationData.toLocation();
if (location == null || location.getWorld() == null) { ... }
```

### Debug Logging Pattern

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

<dependency>
    <groupId>net.milkbowl.vault</groupId>
    <artifactId>VaultAPI</artifactId>
    <version>1.7</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>me.clip</groupId>
    <artifactId>placeholderapi</artifactId>
    <version>2.11.3</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml Structure

```yaml
name: PixelsEssentials
version: 1.0.0
main: com.supafloof.pixelsessentials.PixelsEssentials
api-version: '1.21'
softdepend: [ItemsAdder, Vault, PlaceholderAPI]

commands:
  repair:
    description: Repair items
  home:
    description: Teleport to home
    aliases: [homes]
  sethome:
    description: Set a home
  delhome:
    description: Delete a home
  homeinfo:
    description: View home information
  back:
    description: Return to previous location
  autofeed:
    description: Toggle autofeed
  giveenchanteditem:
    description: Give custom item with enchantments
    aliases: [gei]
  pixelsessentials:
    description: Plugin management
    aliases: [pe]

permissions:
  pixelsessentials.repair.hand:
    default: op
  pixelsessentials.repair.all:
    default: op
  pixelsessentials.repair.player:
    default: op
  # ... additional permissions
```

---

*PixelsEssentials by SupaFloof Games, LLC*
