# PixelsEssentials

A lightweight, feature-rich essentials plugin for Paper/Spigot 1.21+ servers.

[![Paper](https://img.shields.io/badge/Paper-1.21%2B-blue)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-Proprietary-red)](LICENSE)

---

## Overview

PixelsEssentials provides core utility commands that every Minecraft server needs, without the bloat. Built from the ground up for modern Paper servers, it offers a clean, efficient implementation of essential features.

### Why PixelsEssentials?

- **Lightweight** - Single JAR, minimal dependencies, low memory footprint
- **Modern** - Built for Paper 1.21+ with Adventure API text components
- **Reliable** - UUID-based player data survives name changes
- **Configurable** - Permission-based home limits with unlimited tiers
- **ItemsAdder Integration** - Give custom items with enchantments via `/gei`

---

## Features

### 🏠 Home System
Save and teleport to personal locations with configurable limits per permission tier.

- Set unlimited home names
- Permission-based home limits
- Coordinates display with `/homeinfo`

### 🔙 Back Command
Return to your previous location after teleporting or dying.

- Tracks last teleport location
- Optional death location return (separate permission)

### 🔧 Repair System
Fix damaged tools, weapons, and armor instantly.

- Repair held item
- Repair entire inventory
- Repair other players' items (admin)

### 🍖 Autofeed
Never worry about hunger again with automatic food restoration.

- Toggle on/off per player
- Triggers when hunger drops below threshold
- Restores both hunger and saturation

### ☠️ Death Protections
Keep your items, XP, or position on death.

- Keep Inventory - Items stay in inventory
- Keep XP - Experience preserved
- Keep Position - Respawn at death location

### 🎁 Give Enchanted Item
Give ItemsAdder custom items with enchantments, custom names, and lore.

- Works with any ItemsAdder item
- Unlimited enchantment levels
- Custom display names and lore
- Color code support

---

## Quick Start

### Installation

1. Download `PixelsEssentials.jar`
2. Place in your `plugins/` folder
3. Restart the server
4. Configure permissions for your players

### Basic Setup

Give all players access to homes:
```
/lp group default permission set pixelsessentials.home true
/lp group default permission set pixelsessentials.sethome true
/lp group default permission set pixelsessentials.sethome.default true
```

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/home [name]` | Teleport to a home | `pixelsessentials.home` |
| `/sethome [name]` | Set a home | `pixelsessentials.sethome` |
| `/delhome <name>` | Delete a home | `pixelsessentials.delhome` |
| `/homeinfo [name]` | View home info | `pixelsessentials.homeinfo` |
| `/back` | Return to previous location | `pixelsessentials.back` |
| `/repair hand` | Repair held item | `pixelsessentials.repair.hand` |
| `/repair all` | Repair all items | `pixelsessentials.repair.all` |
| `/repair player <name>` | Repair player's items | `pixelsessentials.repair.player` |
| `/autofeed [on\|off]` | Toggle autofeed | `pixelsessentials.autofeed` |
| `/gei <player> <item> ...` | Give custom item | `pixelsessentials.giveenchanteditem` |
| `/pe reload` | Reload config | `pixelsessentials.reload` |

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
```

Players receive the highest limit from all tiers they have permission for.

---

## Documentation

- **[End User Guide](docs/enduser.md)** - Player-focused feature guide
- **[Server Admin Guide](docs/serveradmin.md)** - Installation, configuration, and permissions
- **[Developer Guide](docs/devguide.md)** - Code architecture and extension guide

---

## Requirements

- Paper or Spigot 1.21+
- Java 21+
- (Optional) ItemsAdder for `/gei` command

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
