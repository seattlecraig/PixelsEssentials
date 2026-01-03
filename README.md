# PixelsEssentials

A lightweight, feature-rich essentials plugin for Paper/Spigot 1.21+ servers.

[![Paper](https://img.shields.io/badge/Paper-1.21%2B-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red)](LICENSE)

---

## Overview

PixelsEssentials provides essential utility commands that every Minecraft server needs, without the bloat. Built from the ground up for modern Paper servers, it offers a clean, efficient implementation of core features with optional integrations for extended functionality.

### Why PixelsEssentials?

- **Lightweight** - Single JAR, minimal footprint, efficient caching
- **Modern** - Built for Paper 1.21+ with Adventure API text components
- **Reliable** - UUID-based player data survives name changes; dual world ID (UUID + name) handles world renames
- **Flexible** - Permission-based home limits with unlimited configurable tiers
- **Integrated** - Optional Vault, PlaceholderAPI, and ItemsAdder support

---

## Features

### 🏠 Home System
Save and teleport to personal locations with configurable permission-based limits.

- Multiple named homes per player
- Permission tier system for home limits (config-driven)
- View home coordinates with `/homeinfo`
- Update existing homes without using limit slots

### 🔙 Back Command
Return to your previous location after teleporting or dying.

- Tracks last teleport location separately from death location
- Optional permission for death location return
- Intelligent fallback when death permission is missing

### 🔧 Repair System
Fix damaged tools, weapons, and armor instantly.

- Repair held item or entire inventory
- Repair other players' items (admin feature)
- Smart detection of damageable items only

### 🍖 Autofeed
Never worry about hunger with automatic food restoration.

- Per-player toggle that persists across sessions
- Triggers when hunger drops below threshold
- Restores both hunger and saturation to full

### ☠️ Death Protections
Configurable death behavior with separate permissions.

- **Keep Inventory** - Items stay in inventory on death
- **Keep XP** - Experience preserved on death
- **Keep Position** - Respawn at death location

### 📊 Balance Leaderboard Signs
Physical signs displaying top player balances with automatic updates.

- Create signs for any ranking position (#1, #2, #3, etc.)
- Configurable update interval
- Formatted balance display (K/M/B/T suffixes)
- Requires Vault + Economy plugin

### 🎁 Give Enchanted Item
Give ItemsAdder custom items with enchantments, names, and lore.

- Works with any ItemsAdder item
- Unlimited enchantment levels (unsafe enchanting)
- Custom display names and lore with color codes
- Requires ItemsAdder plugin

### 📈 PlaceholderAPI Support
Custom placeholders for use in other plugins.

- Current/max health with formatting
- Formatted economy balance
- Armor durability (current/max for each slot)
- Requires PlaceholderAPI + Vault

### 🍳 Recipe Unlock
Optional automatic recipe discovery on player join.

- Unlocks all vanilla and custom recipes
- Configurable via config.yml

---

## Quick Start

### Installation

1. Download `PixelsEssentials.jar`
2. Place in your `plugins/` folder
3. Restart the server
4. Configure permissions for your players

### Basic Permission Setup (LuckPerms)

```bash
# Give all players basic home access
/lp group default permission set pixelsessentials.home true
/lp group default permission set pixelsessentials.sethome true
/lp group default permission set pixelsessentials.sethome.default true
/lp group default permission set pixelsessentials.delhome true
/lp group default permission set pixelsessentials.homeinfo true
```

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/home [name]` | Teleport to home or list homes | `pixelsessentials.home` |
| `/sethome [name]` | Set a home (default: "home") | `pixelsessentials.sethome` |
| `/delhome <name>` | Delete a home | `pixelsessentials.delhome` |
| `/homeinfo [name]` | View home info/coordinates | `pixelsessentials.homeinfo` |
| `/back` | Return to previous location | `pixelsessentials.back` |
| `/repair hand` | Repair held item | `pixelsessentials.repair.hand` |
| `/repair all` | Repair all inventory items | `pixelsessentials.repair.all` |
| `/repair player <name>` | Repair player's items | `pixelsessentials.repair.player` |
| `/autofeed [on\|off]` | Toggle autofeed | `pixelsessentials.autofeed` |
| `/gei <player> <item> ...` | Give ItemsAdder item | `pixelsessentials.giveenchanteditem` |
| `/pe reload` | Reload configuration | `pixelsessentials.reload` |
| `/pe debug on\|off` | Toggle debug logging | `pixelsessentials.debug` |
| `/pe show <place>` | Create balance sign | `pixelsessentials.show` |
| `/pe updatesigns` | Force update signs | `pixelsessentials.show` |

---

## Configuration

### Home Limits (config.yml)

```yaml
sethome-multiple:
  default: 1    # pixelsessentials.sethome.default
  vip: 3        # pixelsessentials.sethome.vip
  mvp: 5        # pixelsessentials.sethome.mvp
  elite: 10     # pixelsessentials.sethome.elite
  admin: 100    # pixelsessentials.sethome.admin

sign-update-interval: 60    # Seconds between balance sign updates
unlock-recipes: false       # Auto-unlock recipes on join
```

Players receive the highest limit from all tiers they have permission for.

---

## Death Protection Permissions

| Permission | Effect |
|------------|--------|
| `pixelsessentials.keepinv` | Keep inventory on death |
| `pixelsessentials.keepxp` | Keep experience on death |
| `pixelsessentials.keeppos` | Respawn at death location |
| `pixelsessentials.back.ondeath` | Allow `/back` to death location |

---

## PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%pixelsessentials_current_health%` | Current health (1 decimal) |
| `%pixelsessentials_max_health%` | Max health (1 decimal) |
| `%pixelsessentials_formatted_balance%` | Formatted balance (requires Vault) |
| `%pixelsessentials_helmet_total_durability%` | Helmet max durability |
| `%pixelsessentials_helmet_current_durability%` | Helmet current durability |
| `%pixelsessentials_chestplate_total_durability%` | Chestplate max durability |
| `%pixelsessentials_chestplate_current_durability%` | Chestplate current durability |
| `%pixelsessentials_leggings_total_durability%` | Leggings max durability |
| `%pixelsessentials_leggings_current_durability%` | Leggings current durability |
| `%pixelsessentials_boots_total_durability%` | Boots max durability |
| `%pixelsessentials_boots_current_durability%` | Boots current durability |

---

## Documentation

- **[End User Guide](docs/enduser.md)** - Player-focused feature guide
- **[Server Admin Guide](docs/serveradmin.md)** - Installation, configuration, permissions, and administration
- **[Developer Guide](docs/devguide.md)** - Code architecture, data flow, and extension guide

---

## Requirements

- Paper or Spigot 1.21+
- Java 21+
- (Optional) Vault + Economy plugin - for balance features
- (Optional) PlaceholderAPI - for custom placeholders
- (Optional) ItemsAdder - for `/gei` command

---

## Support

For issues, feature requests, or questions:
- Create an issue on GitHub
- Contact SupaFloof Games support

---

## License

Copyright © 2024 SupaFloof Games, LLC. All rights reserved.

---

*Made with ❤️ by SupaFloof Games, LLC*
