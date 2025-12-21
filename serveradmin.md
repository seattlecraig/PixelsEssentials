# PixelsEssentials Server Administrator Guide

This guide covers installation, configuration, permissions, and administration of PixelsEssentials.

---

## Table of Contents

1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Permissions Reference](#permissions-reference)
4. [Admin Commands](#admin-commands)
5. [Player Data Management](#player-data-management)
6. [Integration with ItemsAdder](#integration-with-itemsadder)
7. [Troubleshooting](#troubleshooting)

---

## Installation

### Requirements

- Paper or Spigot server 1.21+
- Java 21+
- (Optional) ItemsAdder plugin for `/gei` command

### Installation Steps

1. Download `PixelsEssentials.jar`
2. Place it in your server's `plugins/` folder
3. Start or restart the server
4. The plugin creates its data folder at `plugins/PixelsEssentials/`

### First Run

On first run, the plugin creates:
- `plugins/PixelsEssentials/config.yml` - Main configuration
- `plugins/PixelsEssentials/playerdata/` - Per-player data storage

---

## Configuration

### config.yml

The main configuration file controls home limits:

```yaml
# Home Limits Configuration
# Players need pixelsessentials.sethome.<tier> permission to get the corresponding limit
sethome-multiple:
  default: 1
  vip: 3
  mvp: 5
  elite: 10
  admin: 100
```

### How Home Limits Work

1. Define tiers in `sethome-multiple` section
2. Each tier name becomes a permission: `pixelsessentials.sethome.<tier>`
3. Players get the highest value from all tiers they have permission for
4. Players always get at least 1 home if they have base `pixelsessentials.sethome`

**Example Permission Setup (LuckPerms):**

```
# Default group gets 1 home
/lp group default permission set pixelsessentials.sethome true
/lp group default permission set pixelsessentials.sethome.default true

# VIP group gets 3 homes
/lp group vip permission set pixelsessentials.sethome.vip true

# Admin group gets 100 homes
/lp group admin permission set pixelsessentials.sethome.admin true
```

### Reloading Configuration

After editing config.yml:

```
/pe reload
```

This reloads the configuration and clears the player data cache.

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
| `pixelsessentials.home` | Use /home command | true |
| `pixelsessentials.sethome` | Set homes (base permission) | true |
| `pixelsessentials.sethome.<tier>` | Tier-based home limit | varies |
| `pixelsessentials.delhome` | Delete homes | true |
| `pixelsessentials.homeinfo` | View home information | true |

### Back Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.back` | Use /back command | op |
| `pixelsessentials.back.ondeath` | Return to death location | op |

### Death Protection Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.keepxp` | Keep XP on death | op |
| `pixelsessentials.keepinv` | Keep inventory on death | op |
| `pixelsessentials.keeppos` | Respawn at death location | op |

### Utility Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `pixelsessentials.autofeed` | Use autofeed feature | op |
| `pixelsessentials.giveenchanteditem` | Use /gei command | op |
| `pixelsessentials.reload` | Reload plugin configuration | op |
| `pixelsessentials.debug` | Toggle debug mode | op |

### Recommended Permission Groups

**Default Players:**
```
pixelsessentials.home
pixelsessentials.sethome
pixelsessentials.sethome.default
pixelsessentials.delhome
pixelsessentials.homeinfo
```

**VIP Players:**
```
# All default permissions plus:
pixelsessentials.sethome.vip
pixelsessentials.repair.hand
pixelsessentials.back
pixelsessentials.autofeed
```

**Staff/Moderators:**
```
# All VIP permissions plus:
pixelsessentials.sethome.elite
pixelsessentials.repair.all
pixelsessentials.repair.player
pixelsessentials.back.ondeath
pixelsessentials.keepinv
pixelsessentials.keepxp
```

**Administrators:**
```
pixelsessentials.*
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

Shows plugin information and available subcommands.

### Reload Configuration

```
/pe reload
```

- Reloads `config.yml`
- Clears player data cache (data reloads from disk on next access)
- Does NOT reload plugin.yml (requires server restart)

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

Debug mode logs:
- Home permission calculations
- Player data file loading/saving
- Death protection triggers
- Autofeed triggers
- Back command location resolution

### Repair Player Items

Admins can repair another player's entire inventory:

```
/repair player <playername>
```

This repairs all items in their inventory, armor, and off-hand.

### Give Enchanted Item (Requires ItemsAdder)

Give custom items to players:

```
/gei <player> <ia_item> [count] [name:Name] [lore:Lore] [enchant:level]...
```

**Examples:**

```
# Give basic item
/gei Steve fairyset:fairy_sword

# Give with count
/gei Steve fairyset:fairy_sword 5

# Give with custom name (underscores become spaces)
/gei Steve fairyset:fairy_sword 1 name:&6&oLegendary_Blade

# Give with enchantments
/gei Steve fairyset:fairy_sword 1 sharpness:10 unbreaking:5 mending

# Full example
/gei Steve fairyset:fairy_helmet 1 name:&6&oHelmet_of_Power lore:&7&oA_powerful_helm protection:30 unbreaking:25 mending
```

**Notes:**
- Item IDs are in format `namespace:item_id`
- Underscores in name/lore become spaces
- Enchantments without `:level` default to level 1
- Enchantments bypass normal level limits (unsafe enchanting)

---

## Player Data Management

### Data Location

Player data is stored in:
```
plugins/PixelsEssentials/playerdata/<uuid>.yml
```

Each player has their own file named by their UUID.

### Data File Format

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
  world-name: "world"
  x: 150.0
  y: 32.0
  z: 100.0
  yaw: 0.0
  pitch: 0.0

# Was the last event a death or teleport?
last-was-death: true

# Logout location
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

# Autofeed setting
autofeed: true
```

### Managing Player Data

**Reset a player's homes:**
Delete the `homes:` section from their data file.

**Reset all player data:**
Delete the entire `playerdata/` directory.

**Migrate data:**
Player files use UUIDs, so they work across name changes.

### Data Caching

- Player data is cached in memory when accessed
- Cache is saved to disk on:
  - Player logout
  - Server shutdown
  - After any modification (home set/delete, etc.)
- Cache is cleared on `/pe reload`

---

## Integration with ItemsAdder

### Dependency Configuration

PixelsEssentials has a soft dependency on ItemsAdder. The `/gei` command only works when ItemsAdder is installed.

### How /gei Works

1. Retrieves item from ItemsAdder registry using `CustomStack.getInstance()`
2. Gets the Bukkit ItemStack from the custom item
3. Applies custom name and lore (preserving existing lore)
4. Applies enchantments using unsafe enchanting (bypasses level limits)
5. Gives item to player (drops at feet if inventory full)

### Compatibility Notes

- ItemsAdder must be loaded before PixelsEssentials uses the `/gei` command
- Item IDs must match exactly what's defined in ItemsAdder configs
- Custom attribute modifiers from ItemsAdder are preserved

---

## Troubleshooting

### Homes Not Saving

1. Check file permissions on `plugins/PixelsEssentials/playerdata/`
2. Enable debug mode: `/pe debug on`
3. Check console for file save errors

### Home Limits Not Working

1. Enable debug mode: `/pe debug on`
2. Have the player try `/sethome`
3. Check console for permission check output
4. Verify `config.yml` has correct `sethome-multiple` section
5. Verify player has correct tier permission

### /back Not Working

1. Check player has `pixelsessentials.back` permission
2. For death location, check `pixelsessentials.back.ondeath`
3. Enable debug mode to see location resolution

### /gei Command Not Found

1. Verify ItemsAdder is installed and enabled
2. Check console for any errors on startup
3. Make sure ItemsAdder loads before PixelsEssentials

### Keep Inventory Not Working

1. Verify player has `pixelsessentials.keepinv` permission
2. Check priority with other plugins (use EventPriority LOWEST)
3. Enable debug mode to confirm trigger

### Performance Concerns

- Player data is cached to minimize disk I/O
- Location tracking only triggers on significant teleports (>1 block)
- Autofeed only triggers when food level is decreasing

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| "World no longer exists" | Home is in deleted world | Delete the home with `/delhome` |
| "Unknown ItemsAdder item" | Invalid item ID in /gei | Check ItemsAdder item registry |
| "You have reached your home limit" | Player at max homes | Delete a home or upgrade permissions |

---

## Console Messages

### Startup
```
[PixelsEssentials] PixelsEssentials Started!
[PixelsEssentials] By SupaFloof Games, LLC
```

### Shutdown
```
[PixelsEssentials] PixelsEssentials Shutting Down
```

### Errors
```
[SEVERE] Failed to save player data for <uuid>: <error>
```

---

*PixelsEssentials by SupaFloof Games, LLC*
