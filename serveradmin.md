# PixelsEssentials Server Administrator Guide

Complete installation, configuration, permissions, and administration reference for PixelsEssentials.

---

## Table of Contents

1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Permissions Reference](#permissions-reference)
4. [Admin Commands](#admin-commands)
5. [Balance Leaderboard Signs](#balance-leaderboard-signs)
6. [PlaceholderAPI Integration](#placeholderapi-integration)
7. [Player Data Management](#player-data-management)
8. [Integration with ItemsAdder](#integration-with-itemsadder)
9. [Debug Mode](#debug-mode)

---

## Installation

### Requirements

- Paper or Spigot server 1.21+
- Java 21+
- (Optional) Vault + Economy plugin for balance features
- (Optional) PlaceholderAPI for custom placeholders
- (Optional) ItemsAdder for `/gei` command

### Installation Steps

1. Download `PixelsEssentials.jar`
2. Place it in your server's `plugins/` folder
3. Start or restart the server
4. The plugin creates its data folder at `plugins/PixelsEssentials/`

### First Run

On first run, the plugin creates:
- `plugins/PixelsEssentials/config.yml` - Main configuration
- `plugins/PixelsEssentials/playerdata/` - Per-player YAML data files
- `plugins/PixelsEssentials/signs.yml` - Balance leaderboard sign locations (when created)

### Console Output

Successful startup shows:
```
[PixelsEssentials] Vault economy hooked successfully!
[PixelsEssentials] PlaceholderAPI expansion registered!
[PixelsEssentials] Loaded X balance leaderboard signs
[PixelsEssentials] Balance leaderboard signs enabled (update interval: 60s)
[PixelsEssentials] Recipe unlock on join enabled
[PixelsEssentials] PixelsEssentials Started!
[PixelsEssentials] By SupaFloof Games, LLC
```

---

## Configuration

### config.yml

```yaml
# Home Limits Configuration
# Players need pixelsessentials.sethome.<tier> permission to get the corresponding limit
sethome-multiple:
  default: 1
  vip: 3
  mvp: 5
  elite: 10
  admin: 100

# Balance leaderboard sign update interval in seconds
sign-update-interval: 60

# Unlock all recipes for players when they join
unlock-recipes: false
```

### How Home Limits Work

1. Define tiers in `sethome-multiple` section
2. Each tier name becomes a permission: `pixelsessentials.sethome.<tier>`
3. Players receive the highest value from all tiers they have permission for
4. Players always get at least 1 home if they have base `pixelsessentials.sethome`

**Example:**
A player with both `pixelsessentials.sethome.vip` (3 homes) and `pixelsessentials.sethome.mvp` (5 homes) gets 5 homes.

### Reloading Configuration

After editing config.yml:

```
/pe reload
```

This:
- Reloads `config.yml`
- Reloads `sign-update-interval` and `unlock-recipes` settings
- Clears player data cache (data reloads from disk on next access)

---

## Permissions Reference

### Repair Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.repair.hand` | Repair item in main hand | op |
| `pixelsessentials.repair.all` | Repair all items in inventory | op |
| `pixelsessentials.repair.player` | Repair another player's items | op |

### Home Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.home` | Use `/home` to teleport and list | op |
| `pixelsessentials.sethome` | Base permission to set homes | op |
| `pixelsessentials.sethome.<tier>` | Tier-based home limit (config-defined) | op |
| `pixelsessentials.delhome` | Delete homes | op |
| `pixelsessentials.homeinfo` | View home information and coordinates | op |

### Back Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.back` | Use `/back` to return to last teleport location | op |
| `pixelsessentials.back.ondeath` | Allow `/back` to return to death location | op |

### Death Protection Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.keepxp` | Keep experience points on death | op |
| `pixelsessentials.keepinv` | Keep inventory on death | op |
| `pixelsessentials.keeppos` | Respawn at death location | op |

### Utility Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.autofeed` | Use autofeed feature | op |
| `pixelsessentials.giveenchanteditem` | Use `/gei` command | op |
| `pixelsessentials.reload` | Reload plugin configuration | op |
| `pixelsessentials.debug` | Toggle debug mode | op |
| `pixelsessentials.show` | Create and manage balance leaderboard signs | op |

### LuckPerms Examples

**Default Players:**
```
/lp group default permission set pixelsessentials.home true
/lp group default permission set pixelsessentials.sethome true
/lp group default permission set pixelsessentials.sethome.default true
/lp group default permission set pixelsessentials.delhome true
/lp group default permission set pixelsessentials.homeinfo true
```

**VIP Players:**
```
/lp group vip permission set pixelsessentials.sethome.vip true
/lp group vip permission set pixelsessentials.repair.hand true
/lp group vip permission set pixelsessentials.back true
/lp group vip permission set pixelsessentials.autofeed true
```

**Staff/Moderators:**
```
/lp group staff permission set pixelsessentials.sethome.elite true
/lp group staff permission set pixelsessentials.repair.all true
/lp group staff permission set pixelsessentials.repair.player true
/lp group staff permission set pixelsessentials.back.ondeath true
/lp group staff permission set pixelsessentials.keepinv true
/lp group staff permission set pixelsessentials.keepxp true
```

---

## Admin Commands

### Main Admin Command

```
/pixelsessentials
```
or
```
/pe
```

Shows plugin version and available subcommands based on your permissions.

### Reload Configuration

```
/pe reload
```

**Permission:** `pixelsessentials.reload`

### Debug Mode

Enable verbose logging:
```
/pe debug on
```

Disable debug logging:
```
/pe debug off
```

Check current status:
```
/pe debug
```

**Permission:** `pixelsessentials.debug`

### Repair Player Items

```
/repair player <playername>
```

Repairs all items in the target player's inventory, armor, and off-hand. Both the admin and target player receive confirmation messages.

**Permission:** `pixelsessentials.repair.player`

### Give Enchanted Item (ItemsAdder Required)

```
/gei <player> <ia_item> [count] [name:Name] [lore:Lore] [enchant:level]...
```

**Permission:** `pixelsessentials.giveenchanteditem`

**Arguments:**
- `<player>` - Target player (must be online)
- `<ia_item>` - ItemsAdder item ID (e.g., `fairyset:fairy_sword`)
- `[count]` - Number of items (1-64, default 1)
- `[name:Name]` - Custom display name (underscores become spaces)
- `[lore:Lore]` - Custom lore line (underscores become spaces)
- `[enchant:level]` - Enchantment with level (e.g., `sharpness:10`)

**Examples:**

```
# Give basic item
/gei Steve fairyset:fairy_sword

# Give with count
/gei Steve fairyset:fairy_sword 5

# Give with custom name (underscores become spaces, & for colors)
/gei Steve fairyset:fairy_sword 1 name:&6&oLegendary_Blade

# Give with enchantments (without level defaults to 1)
/gei Steve fairyset:fairy_sword 1 sharpness:10 unbreaking:5 mending

# Full example with all options
/gei Steve fairyset:fairy_helmet 1 name:&6&oHelm_of_Power lore:&7Ancient_artifact protection:30 unbreaking:25 mending
```

**Notes:**
- Enchantments bypass normal level limits (unsafe enchanting)
- Enchantments without `:level` default to level 1
- Custom lore is appended to existing ItemsAdder lore
- If inventory is full, items drop at the player's feet

---

## Balance Leaderboard Signs

Display top player balances on physical signs that update automatically.

**Requirements:** Vault + Economy plugin

### Creating a Sign

1. Place a sign in the world
2. Run `/pe show <place>` where `<place>` is the ranking position (1, 2, 3, etc.)
3. Right-click the sign

The sign converts to a balance leaderboard showing the player at that rank.

**Permission:** `pixelsessentials.show`

### Sign Format

```
     BALANCE     (gold, bold)
       #1        (yellow)
   PlayerName   (green)
    $1.23 M     (aqua)
```

### Force Update Signs

```
/pe updatesigns
```

Forces an immediate refresh of all balance signs.

### Sign Update Interval

Configure in `config.yml`:
```yaml
sign-update-interval: 60
```

Value is in seconds. Default is 60 seconds.

### Sign Persistence

Signs are saved to `plugins/PixelsEssentials/signs.yml` and persist across restarts. Signs are automatically removed from tracking if the sign block is destroyed.

---

## PlaceholderAPI Integration

When PlaceholderAPI is installed, the following placeholders are available:

### Health Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%pixelsessentials_current_health%` | Current health (1 decimal) | `15.5` |
| `%pixelsessentials_max_health%` | Maximum health (1 decimal) | `20.0` |

### Balance Placeholder

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%pixelsessentials_formatted_balance%` | Formatted balance | `12,375` or `35.45 M` |

**Requirements:** Vault + Economy plugin

**Format Rules:**
- Under 1 million: Integer with commas (e.g., `993,113`)
- Millions: X.XX M (e.g., `35.45 M`)
- Billions: X.XX B (e.g., `135.22 B`)
- Trillions+: X.XX T (e.g., `1.36 T`)

### Armor Durability Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%pixelsessentials_helmet_total_durability%` | Max durability of helmet |
| `%pixelsessentials_helmet_current_durability%` | Current durability of helmet |
| `%pixelsessentials_chestplate_total_durability%` | Max durability of chestplate |
| `%pixelsessentials_chestplate_current_durability%` | Current durability of chestplate |
| `%pixelsessentials_leggings_total_durability%` | Max durability of leggings |
| `%pixelsessentials_leggings_current_durability%` | Current durability of leggings |
| `%pixelsessentials_boots_total_durability%` | Max durability of boots |
| `%pixelsessentials_boots_current_durability%` | Current durability of boots |

Returns empty string if no item is equipped in that slot.

---

## Player Data Management

### Data Location

Player data is stored in:
```
plugins/PixelsEssentials/playerdata/<uuid>.yml
```

Each player has their own file named by their UUID.

### Data File Structure

```yaml
# Last teleport location (for /back)
lastteleportlocation:
  world: "uuid-string"
  world-name: "world"
  x: 100.5
  y: 64.0
  z: -200.3
  yaw: 90.0
  pitch: 0.0

# Last death location (for /back)
lastdeathlocation:
  world: "uuid-string"
  world-name: "world_nether"
  x: 150.0
  y: 32.0
  z: 100.0
  yaw: 0.0
  pitch: 0.0

# Was the last /back-able event a death?
last-was-death: true

# Logout location (for analytics)
logoutlocation:
  world: "uuid-string"
  world-name: "lobby"
  x: 0.0
  y: 64.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

# Player's homes
homes:
  home:
    world: "uuid-string"
    world-name: "world"
    x: 100.0
    y: 64.0
    z: 100.0
    yaw: 0.0
    pitch: 0.0
  farm:
    world: "uuid-string"
    world-name: "world"
    x: 500.0
    y: 70.0
    z: -300.0
    yaw: 180.0
    pitch: 0.0

# Autofeed toggle
autofeed: true
```

### Managing Player Data

**Reset a player's homes:**
Delete the `homes:` section from their data file (or delete specific home entries).

**Reset all player data:**
Delete the contents of the `playerdata/` directory.

**Data uses UUIDs:**
Player files are named by UUID, so data survives name changes.

### Data Caching

- Player data is cached in memory when first accessed
- Cache is saved to disk immediately on modification
- Cache is saved on player logout
- All cached data is saved on plugin disable
- Cache is cleared on `/pe reload`

---

## Integration with ItemsAdder

### Soft Dependency

PixelsEssentials has ItemsAdder as a soft dependency. The `/gei` command only functions when ItemsAdder is installed and loaded.

### How /gei Works

1. Retrieves item from ItemsAdder using `CustomStack.getInstance(itemId)`
2. Obtains the Bukkit ItemStack from the custom item
3. Applies custom display name if specified (with color code translation)
4. Appends custom lore if specified (preserves existing lore)
5. Applies enchantments using `addUnsafeEnchantment()` (bypasses level limits)
6. Sets item count
7. Gives item to player (drops at feet if inventory full)

### Enchantment Names

Use Minecraft namespace keys for enchantments:
- `sharpness`, `smite`, `bane_of_arthropods`
- `protection`, `fire_protection`, `blast_protection`, `projectile_protection`
- `efficiency`, `silk_touch`, `fortune`, `unbreaking`
- `mending`, `infinity`, `flame`, `punch`, `power`
- `respiration`, `aqua_affinity`, `depth_strider`, `frost_walker`
- `thorns`, `feather_falling`, `soul_speed`, `swift_sneak`
- And all other vanilla enchantments

---

## Debug Mode

Enable debug mode to see verbose logging:

```
/pe debug on
```

### What Debug Mode Logs

- **Player data operations:** File paths for loading/saving
- **Home permission calculations:** All tier checks and final result
- **Death protections:** When keepxp/keepinv/keeppos trigger
- **Respawn handling:** Death location restoration for keeppos
- **Autofeed triggers:** Hunger level changes and restoration events
- **Recipe unlocks:** Count of newly discovered recipes on join
- **Balance sign updates:** Update cycle information

### Example Debug Output

```
[DEBUG] Loading player data from: plugins/PixelsEssentials/playerdata/550e8400-e29b-41d4-a716-446655440000.yml
[DEBUG] Found homes section with keys: [home, farm, base]
[DEBUG] Loaded 3 homes for player 550e8400-e29b-41d4-a716-446655440000
[DEBUG] Checking sethome permissions for player: Steve
[DEBUG] Available tiers in config: [default, vip, mvp, elite, admin]
[DEBUG]   pixelsessentials.sethome.default = true (value: 1)
[DEBUG]   pixelsessentials.sethome.vip = true (value: 3)
[DEBUG]   pixelsessentials.sethome.mvp = false (value: 5)
[DEBUG] Player Steve best match: 'vip' with 3 homes
[DEBUG] KeepXP: Preserved XP for Steve
[DEBUG] KeepInv: Preserved inventory for Steve
[DEBUG] KeepPos: Stored death location for Steve at world (100.5, 64.0, -200.0)
[DEBUG] Autofeed: Restored hunger for Steve (was at 18, going to 15)
```

---

*PixelsEssentials by SupaFloof Games, LLC*
