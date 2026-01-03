# PixelsEssentials End User Guide

Welcome to PixelsEssentials! This guide covers all the features available to you as a player.

---

## Table of Contents

1. [Home System](#home-system)
2. [Back Command](#back-command)
3. [Repair Commands](#repair-commands)
4. [Autofeed](#autofeed)
5. [Death Protections](#death-protections)
6. [Command Quick Reference](#command-quick-reference)

---

## Home System

The home system lets you save locations and teleport back to them instantly.

### Setting a Home

Stand where you want your home and type:

```
/sethome
```

This creates a home called "home". To give it a custom name:

```
/sethome base
/sethome farm
/sethome nether_hub
```

**Home Name Rules:**
- Letters, numbers, underscores, and dashes only
- Maximum 32 characters
- Names are case-insensitive ("Farm" and "farm" are the same)

### Updating a Home

To move an existing home to your current location, use `/sethome` with the same name:

```
/sethome farm
```

If "farm" already exists, it updates to your new location without counting against your home limit.

### Teleporting Home

To teleport to a specific home:

```
/home farm
```

To teleport to your default home:

```
/home home
```

### Listing Your Homes

To see all your saved homes:

```
/home
```

or

```
/homes
```

This displays your homes in a comma-separated list along with your current usage (e.g., "3/5" meaning 3 homes out of 5 maximum).

### Getting Home Information

To see the coordinates of a specific home:

```
/homeinfo farm
```

This shows the world name and X, Y, Z coordinates.

To see a summary of your home count:

```
/homeinfo
```

### Deleting a Home

To remove a saved home:

```
/delhome farm
```

### Home Limits

You have a maximum number of homes based on your permissions. When you set a home, you'll see your usage displayed like "(3/5)".

If you've reached your limit:
- Delete an existing home with `/delhome`
- Or update an existing home by using `/sethome` with the same name

---

## Back Command

The `/back` command returns you to your previous location.

### Returning After Teleporting

After any teleport (using `/home`, warps, etc.):

```
/back
```

This takes you back to where you were standing before the teleport.

### Returning After Death

If you die and want to return to your death location:

```
/back
```

**Note:** Returning to death locations requires the `pixelsessentials.back.ondeath` permission. Without it, `/back` will take you to your last teleport location instead.

### How It Works

The plugin tracks two separate locations:
- **Last Teleport Location** - Where you were before your most recent teleport
- **Last Death Location** - Where you died most recently

The `/back` command uses whichever event happened most recently, subject to your permissions.

---

## Repair Commands

Fix damaged tools, weapons, and armor instantly.

### Repairing Your Held Item

To repair the item in your main hand:

```
/repair hand
```

Requirements:
- You must be holding an item
- The item must be damageable (tools, weapons, armor)
- Items at full durability show a message instead

### Repairing Everything

To repair all damaged items in your inventory:

```
/repair all
```

This repairs:
- All items in your main inventory (36 slots)
- All equipped armor pieces (helmet, chestplate, leggings, boots)
- Your off-hand item (shield slot)

You'll see a message listing all repaired items.

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

When autofeed is enabled:
- Your hunger is monitored as it decreases
- When it drops to 16 or below (4+ hunger points lost from maximum of 20), it triggers
- Your hunger bar is restored to full (20)
- Your saturation is also restored to full (20)

Autofeed is enabled by default for new players. Your preference is saved and persists across sessions.

---

## Death Protections

Depending on your permissions, you may have special protections when you die:

### Keep Inventory (pixelsessentials.keepinv)

When you die:
- All items stay in your inventory
- Nothing drops on the ground

### Keep Experience (pixelsessentials.keepxp)

When you die:
- Your XP level is preserved
- Your XP progress bar stays the same
- No experience orbs are dropped

### Keep Position (pixelsessentials.keeppos)

When you die:
- You respawn exactly where you died
- Bypasses bed spawn and world spawn

These permissions are typically granted to specific ranks or groups.

---

## Command Quick Reference

### Home Commands

| Command | Description |
|---------|-------------|
| `/home` | List all your homes |
| `/home <name>` | Teleport to a home |
| `/homes` | List all your homes (alias) |
| `/sethome` | Set home named "home" |
| `/sethome <name>` | Set a named home |
| `/delhome <name>` | Delete a home |
| `/homeinfo` | Show home count summary |
| `/homeinfo <name>` | Show home coordinates |

### Other Commands

| Command | Description |
|---------|-------------|
| `/back` | Return to previous location |
| `/repair hand` | Repair item in main hand |
| `/repair all` | Repair all inventory items |
| `/autofeed` | Check autofeed status |
| `/autofeed on` | Enable autofeed |
| `/autofeed off` | Disable autofeed |

---

## Tips

1. **Use descriptive home names** - Names like "nether_portal" or "diamond_mine" are easier to remember than "h1" or "home2".

2. **Save homes in safe locations** - Ensure there's solid ground where you set a home.

3. **Use `/back` strategically** - Accidentally teleport to the wrong place? `/back` returns you.

4. **Repair before adventures** - Use `/repair all` before dangerous expeditions.

5. **Tab completion** - All commands support tab completion for home names.

---

*PixelsEssentials by SupaFloof Games, LLC*
