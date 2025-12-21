# PixelsEssentials End User Guide

Welcome to PixelsEssentials! This guide will help you understand all the features available to you as a player.

---

## Table of Contents

1. [Home System](#home-system)
2. [Back Command](#back-command)
3. [Repair Commands](#repair-commands)
4. [Autofeed](#autofeed)
5. [Death Protections](#death-protections)

---

## Home System

The home system lets you save locations and teleport back to them instantly.

### Setting a Home

To save your current location as a home:

```
/sethome
```

This creates a home called "home". You can also give it a custom name:

```
/sethome mybase
/sethome farm
/sethome mineshaft
```

**Rules for home names:**
- Letters, numbers, underscores, and dashes only
- Maximum 32 characters
- Names are case-insensitive ("Farm" and "farm" are the same)

### Teleporting Home

To teleport to a home:

```
/home mybase
```

Or just `/home` to see your list of homes first.

### Listing Your Homes

To see all your saved homes:

```
/home
```
or
```
/homes
```

This shows your homes and how many you've used out of your maximum allowed.

### Getting Home Information

To see the coordinates of a specific home:

```
/homeinfo mybase
```

This shows the world name and X, Y, Z coordinates.

To see a summary of your home usage:

```
/homeinfo
```

### Deleting a Home

To remove a saved home:

```
/delhome mybase
```

### Home Limits

You have a maximum number of homes you can set. This limit is determined by your rank or permissions on the server. When you set a home, you'll see your current usage like "(3/5)" meaning you have 3 homes out of a maximum of 5.

If you've reached your limit, you must delete an existing home before creating a new one. However, you can always update an existing home by using `/sethome` with the same name again.

---

## Back Command

The `/back` command teleports you to your previous location.

### Returning After Teleporting

If you teleport somewhere (using `/home`, a warp, etc.) and want to return:

```
/back
```

This takes you back to where you were standing before the teleport.

### Returning After Death

If you die and want to return to your death location:

```
/back
```

**Note:** Returning to your death location may require additional permissions. If you don't have permission, `/back` will take you to your last teleport location instead.

---

## Repair Commands

The repair system lets you fix damaged tools, weapons, and armor.

### Repairing Your Held Item

To repair the item in your main hand:

```
/repair hand
```

The item must be damageable (tools, weapons, armor). Items that can't take damage (like blocks or food) cannot be repaired.

### Repairing Everything

To repair all items in your inventory and armor:

```
/repair all
```

This repairs:
- All items in your inventory (including hotbar)
- All equipped armor pieces
- Your off-hand item

You'll see a message listing all items that were repaired.

---

## Autofeed

Autofeed automatically restores your hunger when it gets low.

### Enabling Autofeed

```
/autofeed on
```

### Disabling Autofeed

```
/autofeed off
```

### Checking Status

```
/autofeed
```

### How It Works

When autofeed is enabled and your hunger bar drops below 4 bars (8 hunger points / food level 16), your hunger is automatically restored to full. This also restores your saturation.

Autofeed is enabled by default when you first join. Your preference is saved and persists across sessions.

---

## Death Protections

Depending on your permissions, you may have special protections when you die:

### Keep Inventory

If you have this permission, you keep all your items when you die. Nothing drops on the ground.

### Keep Experience

If you have this permission, you keep all your XP levels and progress when you die. No experience orbs are dropped.

### Keep Position

If you have this permission, you respawn exactly where you died instead of at your bed or world spawn.

**Note:** These features depend on server configuration. Ask your server administrator if you have access to these protections.

---

## Tips and Tricks

1. **Use descriptive home names** - Names like "nether_portal" or "diamond_mine" are easier to remember than "home1" or "h2".

2. **Save homes in safe locations** - Make sure there's solid ground where you set a home. Teleporting into lava or the void is not fun!

3. **Use `/back` strategically** - If you accidentally teleport to the wrong place, `/back` is your friend.

4. **Repair before big adventures** - Use `/repair all` before heading into dangerous areas to ensure your gear is in top condition.

5. **Enable autofeed for convenience** - If you have permission, autofeed means you never have to worry about eating.

---

## Command Quick Reference

| Command | Description |
|---------|-------------|
| `/home` | List all your homes |
| `/home <name>` | Teleport to a home |
| `/sethome [name]` | Set a home (default: "home") |
| `/delhome <name>` | Delete a home |
| `/homeinfo [name]` | Show home coordinates |
| `/back` | Return to previous location |
| `/repair hand` | Repair held item |
| `/repair all` | Repair all items |
| `/autofeed on\|off` | Toggle autofeed |

---

*PixelsEssentials by SupaFloof Games, LLC*
