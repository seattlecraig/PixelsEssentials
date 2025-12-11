package com.supafloof.pixelsessentials;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

/**
 * PixelsEssentials Plugin
 * 
 * A comprehensive Minecraft Paper/Spigot essentials plugin providing core utility
 * commands for server administration and player convenience.
 * 
 * <p><b>Core Features:</b></p>
 * <ul>
 *   <li>Item repair system with hand, all inventory, and other player repair options</li>
 *   <li>Home teleportation system with configurable limits per permission tier</li>
 *   <li>Per-player data storage with persistence across server restarts</li>
 *   <li>Last location tracking (before teleport or death)</li>
 *   <li>Logout location tracking</li>
 *   <li>Permission-based home limits with tiered permission groups</li>
 *   <li>Tab completion for all commands and home names</li>
 *   <li>Hot-reload capability for configuration changes</li>
 * </ul>
 * 
 * <p><b>Commands:</b></p>
 * <ul>
 *   <li><b>/repair hand</b> - Repairs item in main hand (pixelsessentials.repair.hand)</li>
 *   <li><b>/repair all</b> - Repairs all items in inventory and armor (pixelsessentials.repair.all)</li>
 *   <li><b>/repair player &lt;name&gt;</b> - Repairs all items for target player (pixelsessentials.repair.player)</li>
 *   <li><b>/home</b> - Lists all homes (pixelsessentials.home)</li>
 *   <li><b>/home &lt;name&gt;</b> - Teleports to named home (pixelsessentials.home)</li>
 *   <li><b>/homes</b> - Lists all homes (alias for /home)</li>
 *   <li><b>/sethome [name]</b> - Sets a home at current location (pixelsessentials.sethome)</li>
 *   <li><b>/delhome &lt;name&gt;</b> - Deletes a home (pixelsessentials.delhome)</li>
 *   <li><b>/homeinfo &lt;name&gt;</b> - Shows coordinates of a home (pixelsessentials.homeinfo)</li>
 *   <li><b>/pixelsessentials reload</b> - Reloads config (pixelsessentials.reload)</li>
 * </ul>
 * 
 * <p><b>Permission Tiers for Home Limits:</b></p>
 * <p>Home limits are determined by permissions in the format pixelsessentials.sethome.&lt;tier&gt;
 * where the tier maps to a number in config.yml. If a player has multiple tier permissions,
 * the highest value is used.</p>
 * 
 * <p><b>Player Data File Format:</b></p>
 * <pre>
 * lastlocation:
 *   world: &lt;world-uuid&gt;
 *   world-name: world
 *   x: 100.5
 *   y: 64.0
 *   z: -200.3
 *   yaw: 90.0
 *   pitch: 0.0
 * logoutlocation:
 *   world: &lt;world-uuid&gt;
 *   world-name: lobby
 *   x: 0.0
 *   y: 64.0
 *   z: 0.0
 *   yaw: 0.0
 *   pitch: 0.0
 * homes:
 *   home:
 *     world: &lt;world-uuid&gt;
 *     world-name: world
 *     x: 100.0
 *     y: 64.0
 *     z: 100.0
 *     yaw: 0.0
 *     pitch: 0.0
 * </pre>
 * 
 * <p><b>Technical Details:</b></p>
 * <ul>
 *   <li>Player data stored in YAML files per-player in plugins/PixelsEssentials/playerdata/</li>
 *   <li>File names use player UUIDs for reliable identification</li>
 *   <li>Location data includes world UUID, world name, x, y, z, yaw, and pitch</li>
 *   <li>Tracks lastlocation (before teleport or death) and logoutlocation</li>
 *   <li>Repair checks for Damageable items to avoid processing non-repairable items</li>
 *   <li>Uses Adventure API Components for all player messaging</li>
 * </ul>
 * 
 * @author SupaFloof Games, LLC
 * @version 1.0.0
 */
public class PixelsEssentials extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    
    /**
     * Directory where player data files are stored.
     * Location: plugins/PixelsEssentials/playerdata/
     * Each file is named by player UUID: {uuid}.yml
     */
    private File playerDataFolder;
    
    /**
     * Cache of player data to avoid constant file reads.
     * Key: Player UUID
     * Value: PlayerData object containing homes, lastlocation, logoutlocation
     * 
     * <p>This cache is populated on demand and saved on changes and player quit
     * to minimize memory usage while providing fast access during gameplay.</p>
     */
    private Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    
    /**
     * Debug mode flag. When true, verbose logging is output to console.
     * Toggled via /pe debug on|off command.
     */
    private boolean debugMode = false;
    
    /**
     * Tracks death locations for players with keeppos permission.
     * Key: Player UUID
     * Value: Death location to respawn at
     * 
     * <p>Entries are added on death and removed after respawn teleport.</p>
     */
    private Map<UUID, Location> deathLocations = new HashMap<>();
    
    /**
     * Plugin initialization method called by Bukkit when the plugin is enabled.
     * 
     * <p><b>Initialization Steps:</b></p>
     * <ol>
     *   <li>Create the playerdata directory if it doesn't exist</li>
     *   <li>Save default config.yml if it doesn't exist</li>
     *   <li>Register command executors and tab completers</li>
     *   <li>Register event listeners for location tracking</li>
     *   <li>Send colored startup messages to console</li>
     * </ol>
     */
    @Override
    public void onEnable() {
        // Create the playerdata directory if it doesn't exist
        // This is where per-player data files are stored
        playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            // mkdirs() creates parent directories if needed (plugins/PixelsEssentials/playerdata/)
            playerDataFolder.mkdirs();
        }
        
        // Save default config.yml if it doesn't exist
        // This creates the configuration file with default sethome-multiple values
        saveDefaultConfig();
        
        // Register command executors
        // Each command is defined in plugin.yml
        getCommand("repair").setExecutor(this);
        getCommand("repair").setTabCompleter(this);
        
        getCommand("home").setExecutor(this);
        getCommand("home").setTabCompleter(this);
        
        getCommand("sethome").setExecutor(this);
        getCommand("sethome").setTabCompleter(this);
        
        getCommand("delhome").setExecutor(this);
        getCommand("delhome").setTabCompleter(this);
        
        getCommand("homeinfo").setExecutor(this);
        getCommand("homeinfo").setTabCompleter(this);
        
        getCommand("pixelsessentials").setExecutor(this);
        getCommand("pixelsessentials").setTabCompleter(this);
        
        getCommand("autofeed").setExecutor(this);
        getCommand("autofeed").setTabCompleter(this);
        
        getCommand("back").setExecutor(this);
        getCommand("back").setTabCompleter(this);
        
        // Register event listener for location tracking
        // Handles PlayerTeleportEvent, PlayerDeathEvent, and PlayerQuitEvent
        getServer().getPluginManager().registerEvents(this, this);
        
        // Send startup messages to console with Adventure API colored text
        // Green for main message, light purple (magenta) for author credit
        // Uses Adventure API Component instead of legacy color codes
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Started!", NamedTextColor.GREEN));
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] By SupaFloof Games, LLC", NamedTextColor.LIGHT_PURPLE));
    }
    
    /**
     * Plugin shutdown method called by Bukkit when the plugin is disabled.
     * 
     * <p>Saves all cached player data and clears the cache to prevent memory leaks.</p>
     */
    @Override
    public void onDisable() {
        // Save all cached player data before shutdown
        // This ensures no data is lost if data was modified but not yet saved
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
        
        // Clear caches
        playerDataCache.clear();
        deathLocations.clear();
        
        // Send shutdown message to console in red
        // Indicates the plugin has stopped cleanly
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Shutting Down", NamedTextColor.RED));
    }
    
    // ==================================================================================
    // EVENT HANDLERS - LOCATION TRACKING
    // ==================================================================================
    
    /**
     * Handles player teleport events to track last location.
     * 
     * <p>Saves the player's location before teleportation as their lastlocation.
     * This allows players to return to where they were before teleporting.</p>
     * 
     * @param event The PlayerTeleportEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        
        // Don't track if teleporting within the same block (minor movements)
        Location to = event.getTo();
        if (to != null && from.getWorld().equals(to.getWorld()) 
            && from.distanceSquared(to) < 1.0) {
            return;
        }
        
        // Save the "from" location as last teleport location
        setLastTeleportLocation(player, from);
    }
    
    /**
     * Handles player death events for multiple features:
     * - Tracks last location (death location saved as lastlocation)
     * - KeepXP: Preserves player XP if they have pixelsessentials.keepxp permission
     * - KeepInv: Preserves inventory if they have pixelsessentials.keepinv permission
     * - KeepPos: Stores death location for respawn if they have pixelsessentials.keeppos permission
     * 
     * <p>Uses LOWEST priority to ensure keepInventory/keepLevel are set before any other plugins
     * process the death event, preventing item duplication issues.</p>
     * 
     * @param event The PlayerDeathEvent
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        
        // Save death location
        setLastDeathLocation(player, deathLocation);
        
        // KeepXP - Preserve experience on death
        // If player has permission, keep their XP unchanged
        if (player.hasPermission("pixelsessentials.keepxp")) {
            // Set dropped XP to 0 - player keeps their XP
            event.setDroppedExp(0);
            // Keep the XP level and progress
            event.setKeepLevel(true);
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepXP: Preserved XP for " + player.getName());
            }
        }
        
        // KeepInv - Preserve inventory on death
        // If player has permission, keep their items
        if (player.hasPermission("pixelsessentials.keepinv")) {
            // setKeepInventory(true) tells Paper to:
            // 1. Not clear the player's inventory on death
            // 2. Not add inventory items to drops
            // We must ALSO clear drops in case other plugins added items
            event.setKeepInventory(true);
            event.getDrops().clear();
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepInv: Preserved inventory for " + player.getName());
            }
        }
        
        // KeepPos - Respawn at death location
        // If player has permission, store their death location for respawn
        if (player.hasPermission("pixelsessentials.keeppos")) {
            // Store death location for respawn handler
            deathLocations.put(player.getUniqueId(), deathLocation.clone());
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepPos: Stored death location for " + player.getName() + 
                    " at " + deathLocation.getWorld().getName() + 
                    " (" + String.format("%.1f, %.1f, %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()) + ")");
            }
        }
    }
    
    /**
     * Handles player respawn events for KeepPos feature.
     * 
     * <p>If the player died with pixelsessentials.keeppos permission,
     * teleports them back to their death location after respawn.</p>
     * 
     * @param event The PlayerRespawnEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if this player has a stored death location
        if (deathLocations.containsKey(uuid)) {
            Location deathLocation = deathLocations.remove(uuid);
            
            // Set respawn location to death location
            event.setRespawnLocation(deathLocation);
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepPos: Respawning " + player.getName() + 
                    " at death location: " + deathLocation.getWorld().getName() + 
                    " (" + String.format("%.1f, %.1f, %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()) + ")");
            }
        }
    }
    
    /**
     * Handles food level change events for autofeed feature.
     * 
     * <p>If the player has autofeed enabled and the pixelsessentials.autofeed permission,
     * their hunger will be restored to full (20) when it drops to 18 or below (2+ bars lost).</p>
     * 
     * @param event The FoodLevelChangeEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // Check if player has autofeed permission
        if (!player.hasPermission("pixelsessentials.autofeed")) {
            return;
        }
        
        // Load player data and check if autofeed is enabled
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        if (data == null || !data.autofeedEnabled) {
            return;
        }
        
        int newFoodLevel = event.getFoodLevel();
        int currentFoodLevel = player.getFoodLevel();
        
        // Only act when food is decreasing
        if (newFoodLevel >= currentFoodLevel) {
            return;
        }
        
        // If food level is dropping to 16 or below (lost 2+ bars / 4+ points from max of 20)
        if (newFoodLevel <= 16) {
            // Cancel the event and restore to full
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            
            if (debugMode) {
                getLogger().info("[DEBUG] Autofeed: Restored hunger for " + player.getName() + 
                    " (was at " + currentFoodLevel + ", going to " + newFoodLevel + ")");
            }
        }
    }
    
    /**
     * Handles player quit events to track logout location.
     * 
     * <p>Saves the player's current location as their logoutlocation
     * and persists all cached data to file.</p>
     * 
     * @param event The PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Load player data if not cached
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            // Save logout location
            data.logoutLocation = LocationData.fromLocation(player.getLocation());
            
            // Persist to file
            savePlayerData(uuid);
        }
        
        // Clean up death location if player quit before respawning
        deathLocations.remove(uuid);
    }
    
    /**
     * Sets the player's last teleport location from their current position.
     * 
     * @param player The player whose last teleport location to set
     */
    private void setLastTeleportLocation(Player player) {
        setLastTeleportLocation(player, player.getLocation());
    }
    
    /**
     * Sets the player's last teleport location to a specific location.
     * 
     * @param player The player whose last teleport location to set
     * @param location The location to save
     */
    private void setLastTeleportLocation(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        
        // Load player data if not cached
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.lastTeleportLocation = LocationData.fromLocation(location);
            data.lastWasDeath = false;
            savePlayerData(uuid);
        }
    }
    
    /**
     * Sets the player's last death location.
     * 
     * @param player The player whose last death location to set
     * @param location The death location to save
     */
    private void setLastDeathLocation(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        
        // Load player data if not cached
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.lastDeathLocation = LocationData.fromLocation(location);
            data.lastWasDeath = true;
            savePlayerData(uuid);
        }
    }
    
    // ==================================================================================
    // COMMAND ROUTING
    // ==================================================================================
    
    /**
     * Main command handler for all plugin commands.
     * Routes to appropriate handler method based on command name.
     * 
     * @param sender The command sender (player or console)
     * @param command The command object
     * @param label The command alias used
     * @param args The command arguments
     * @return true if command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();
        
        switch (cmdName) {
            case "repair":
                return handleRepairCommand(sender, args);
            case "home":
            case "homes":
                return handleHomeCommand(sender, args);
            case "sethome":
                return handleSetHomeCommand(sender, args);
            case "delhome":
                return handleDelHomeCommand(sender, args);
            case "homeinfo":
                return handleHomeInfoCommand(sender, args);
            case "autofeed":
                return handleAutofeedCommand(sender, args);
            case "back":
                return handleBackCommand(sender);
            case "pixelsessentials":
                return handleMainCommand(sender, args);
            default:
                return false;
        }
    }
    
    // ==================================================================================
    // REPAIR COMMAND HANDLERS
    // ==================================================================================
    
    /**
     * Handles the /repair command and its subcommands.
     * 
     * <p><b>Subcommands:</b></p>
     * <ul>
     *   <li>hand - Repairs item in main hand</li>
     *   <li>all - Repairs all inventory items</li>
     *   <li>player &lt;name&gt; - Repairs another player's items</li>
     * </ul>
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleRepairCommand(CommandSender sender, String[] args) {
        // Check for arguments
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /repair <hand|all|player>", NamedTextColor.RED));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "hand":
                return handleRepairHand(sender);
            case "all":
                return handleRepairAll(sender);
            case "player":
                return handleRepairPlayer(sender, args);
            default:
                sender.sendMessage(Component.text("Usage: /repair <hand|all|player>", NamedTextColor.RED));
                return true;
        }
    }
    
    /**
     * Handles /repair hand - repairs the item in the player's main hand.
     * 
     * <p><b>Permission:</b> pixelsessentials.repair.hand</p>
     * 
     * <p>Only repairs items that implement the Damageable interface.
     * Items that cannot take damage (like blocks) are not affected.</p>
     * 
     * @param sender The command sender (must be a player)
     * @return true if command was handled
     */
    private boolean handleRepairHand(CommandSender sender) {
        // Must be a player to repair hand item
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.repair.hand")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Get item in main hand
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if holding an item
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Component.text("You are not holding an item.", NamedTextColor.RED));
            return true;
        }
        
        // Check if item is repairable (has durability)
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            player.sendMessage(Component.text("This item cannot be repaired.", NamedTextColor.RED));
            return true;
        }
        
        Damageable damageable = (Damageable) meta;
        
        // Check if item is already fully repaired
        if (damageable.getDamage() == 0) {
            player.sendMessage(Component.text("This item is already fully repaired.", NamedTextColor.YELLOW));
            return true;
        }
        
        // Get item name before repairing
        String itemName;
        if (meta.hasDisplayName()) {
            itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        } else {
            String materialName = item.getType().name().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder formatted = new StringBuilder();
            for (String word : words) {
                if (formatted.length() > 0) formatted.append(" ");
                formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            itemName = formatted.toString();
        }
        
        // Set damage to 0 (fully repaired)
        damageable.setDamage(0);
        item.setItemMeta(meta);
        
        // Success message with item name
        player.sendMessage(Component.text("Repaired: ", NamedTextColor.GREEN)
            .append(Component.text(itemName, NamedTextColor.YELLOW)));
        
        return true;
    }
    
    /**
     * Handles /repair all - repairs all items in the player's inventory and armor slots.
     * 
     * <p><b>Permission:</b> pixelsessentials.repair.all</p>
     * 
     * <p>Repairs all Damageable items in:</p>
     * <ul>
     *   <li>Main inventory (36 slots)</li>
     *   <li>Armor slots (helmet, chestplate, leggings, boots)</li>
     *   <li>Off-hand slot</li>
     * </ul>
     * 
     * @param sender The command sender (must be a player)
     * @return true if command was handled
     */
    private boolean handleRepairAll(CommandSender sender) {
        // Must be a player to repair inventory
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.repair.all")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Repair all items and get list of repaired item names
        List<String> repairedItems = repairAllItems(player);
        
        // Send result message
        if (repairedItems.isEmpty()) {
            player.sendMessage(Component.text("No items needed repair.", NamedTextColor.YELLOW));
        } else {
            String itemList = String.join(", ", repairedItems);
            player.sendMessage(Component.text("Repaired ", NamedTextColor.GREEN)
                .append(Component.text(repairedItems.size(), NamedTextColor.AQUA))
                .append(Component.text(" item" + (repairedItems.size() == 1 ? "" : "s") + ": ", NamedTextColor.GREEN))
                .append(Component.text(itemList, NamedTextColor.YELLOW)));
        }
        
        return true;
    }
    
    /**
     * Handles /repair player &lt;name&gt; - repairs all items for a target player.
     * 
     * <p><b>Permission:</b> pixelsessentials.repair.player</p>
     * 
     * <p>Allows operators/admins to repair another player's entire inventory
     * and armor. Useful for helping players who had items damaged.</p>
     * 
     * @param sender The command sender
     * @param args The command arguments (expects player name at args[1])
     * @return true if command was handled
     */
    private boolean handleRepairPlayer(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("pixelsessentials.repair.player")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Check for player name argument
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /repair player <playername>", NamedTextColor.RED));
            return true;
        }
        
        // Find the target player
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: ", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW)));
            return true;
        }
        
        // Repair all items for target player
        List<String> repairedItems = repairAllItems(target);
        
        // Send result messages
        if (repairedItems.isEmpty()) {
            sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" had no items that needed repair.", NamedTextColor.YELLOW)));
        } else {
            String itemList = String.join(", ", repairedItems);
            sender.sendMessage(Component.text("Repaired ", NamedTextColor.GREEN)
                .append(Component.text(repairedItems.size(), NamedTextColor.AQUA))
                .append(Component.text(" item" + (repairedItems.size() == 1 ? "" : "s") + " for ", NamedTextColor.GREEN))
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GREEN))
                .append(Component.text(itemList, NamedTextColor.YELLOW)));
            
            // Notify target player
            target.sendMessage(Component.text("Your items have been repaired by ", NamedTextColor.GREEN)
                .append(Component.text(sender.getName(), NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GREEN))
                .append(Component.text(itemList, NamedTextColor.YELLOW)));
        }
        
        return true;
    }
    
    /**
     * Repairs all damageable items in a player's inventory and armor.
     * 
     * <p>Iterates through all inventory contents, armor slots, and off-hand,
     * repairing any item that has the Damageable interface and has taken damage.</p>
     * 
     * @param player The player whose items to repair
     * @return List of item names that were repaired
     */
    private List<String> repairAllItems(Player player) {
        List<String> repairedItems = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        
        // Repair main inventory contents (includes hotbar)
        for (ItemStack item : inventory.getContents()) {
            String itemName = repairItem(item);
            if (itemName != null) {
                repairedItems.add(itemName);
            }
        }
        
        // Repair armor contents
        for (ItemStack item : inventory.getArmorContents()) {
            String itemName = repairItem(item);
            if (itemName != null) {
                repairedItems.add(itemName);
            }
        }
        
        // Repair off-hand
        String offhandItem = repairItem(inventory.getItemInOffHand());
        if (offhandItem != null) {
            repairedItems.add(offhandItem);
        }
        
        return repairedItems;
    }
    
    /**
     * Repairs a single item if it is damageable and has taken damage.
     * 
     * @param item The item to repair (may be null)
     * @return The item's display name if repaired, null otherwise
     */
    private String repairItem(ItemStack item) {
        // Skip null or air items
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        // Check if item is damageable
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return null;
        }
        
        Damageable damageable = (Damageable) meta;
        
        // Check if item has damage
        if (damageable.getDamage() == 0) {
            return null;
        }
        
        // Repair by setting damage to 0
        damageable.setDamage(0);
        item.setItemMeta(meta);
        
        // Return a friendly name for the item
        // Use custom display name if it has one, otherwise format the material name
        if (meta.hasDisplayName()) {
            // Strip color codes for cleaner output
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        } else {
            // Convert DIAMOND_SWORD to Diamond Sword
            String materialName = item.getType().name().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder formatted = new StringBuilder();
            for (String word : words) {
                if (formatted.length() > 0) formatted.append(" ");
                formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            return formatted.toString();
        }
    }
    
    // ==================================================================================
    // HOME COMMAND HANDLERS
    // ==================================================================================
    
    /**
     * Handles the /home and /homes commands.
     * 
     * <p><b>Usage:</b></p>
     * <ul>
     *   <li>/home - Lists all homes</li>
     *   <li>/homes - Lists all homes (alias)</li>
     *   <li>/home &lt;name&gt; - Teleports to named home</li>
     * </ul>
     * 
     * <p><b>Permission:</b> pixelsessentials.home</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleHomeCommand(CommandSender sender, String[] args) {
        // Must be a player to use homes
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.home")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Load player data if not cached
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        Map<String, LocationData> homes = data.homes;
        
        // No arguments - list homes
        if (args.length < 1) {
            return listHomes(player, homes);
        }
        
        // Argument provided - teleport to home
        String homeName = args[0].toLowerCase();
        
        // Check if home exists
        if (homes == null || !homes.containsKey(homeName)) {
            player.sendMessage(Component.text("Home not found: ", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                .append(Component.text("/home", NamedTextColor.AQUA))
                .append(Component.text(" to see your homes.", NamedTextColor.GRAY)));
            return true;
        }
        
        // Get home data and teleport
        LocationData home = homes.get(homeName);
        Location location = home.toLocation();
        
        // Check if world exists
        if (location == null || location.getWorld() == null) {
            player.sendMessage(Component.text("Cannot teleport to home '", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW))
                .append(Component.text("' - world no longer exists.", NamedTextColor.RED)));
            return true;
        }
        
        // Teleport player (lastlocation is set by the teleport event handler)
        player.teleport(location);
        player.sendMessage(Component.text("Teleported to home: ", NamedTextColor.GREEN)
            .append(Component.text(homeName, NamedTextColor.AQUA)));
        
        return true;
    }
    
    /**
     * Lists all homes for a player in a comma-separated format.
     * 
     * @param player The player to show homes to
     * @param homes The player's home map (may be null or empty)
     * @return true always
     */
    private boolean listHomes(Player player, Map<String, LocationData> homes) {
        if (homes == null || homes.isEmpty()) {
            player.sendMessage(Component.text("You have no homes set.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                .append(Component.text("/sethome <n>", NamedTextColor.AQUA))
                .append(Component.text(" to set a home.", NamedTextColor.GRAY)));
            return true;
        }
        
        // Get max homes for display
        int maxHomes = getMaxHomes(player);
        
        // Build comma-separated list
        List<String> homeNames = new ArrayList<>(homes.keySet());
        Collections.sort(homeNames);
        String homeList = String.join(", ", homeNames);
        
        player.sendMessage(Component.text("Your homes (", NamedTextColor.GREEN)
            .append(Component.text(homes.size(), NamedTextColor.AQUA))
            .append(Component.text("/", NamedTextColor.GREEN))
            .append(Component.text(maxHomes, NamedTextColor.AQUA))
            .append(Component.text("): ", NamedTextColor.GREEN))
            .append(Component.text(homeList, NamedTextColor.YELLOW)));
        
        return true;
    }
    
    /**
     * Handles the /sethome command.
     * 
     * <p><b>Usage:</b> /sethome [name]</p>
     * <p>If no name is provided, defaults to "home".</p>
     * 
     * <p><b>Permission:</b> pixelsessentials.sethome</p>
     * <p>Home limit is determined by pixelsessentials.sethome.&lt;tier&gt; permissions.</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleSetHomeCommand(CommandSender sender, String[] args) {
        // Must be a player to set homes
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check base permission
        if (!player.hasPermission("pixelsessentials.sethome")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Get home name (default to "home" if not provided)
        String homeName = (args.length > 0) ? args[0].toLowerCase() : "home";
        
        // Validate home name (alphanumeric only, max 32 chars)
        if (!homeName.matches("^[a-zA-Z0-9_-]{1,32}$")) {
            player.sendMessage(Component.text("Invalid home name. Use only letters, numbers, underscores, and dashes (max 32 chars).", NamedTextColor.RED));
            return true;
        }
        
        // Load player data if not cached
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // Check if updating existing home or creating new
        boolean isUpdate = data.homes.containsKey(homeName);
        
        // If creating new home, check limit
        if (!isUpdate) {
            int maxHomes = getMaxHomes(player);
            if (data.homes.size() >= maxHomes) {
                player.sendMessage(Component.text("You have reached your home limit (", NamedTextColor.RED)
                    .append(Component.text(maxHomes, NamedTextColor.YELLOW))
                    .append(Component.text(").", NamedTextColor.RED)));
                return true;
            }
        }
        
        // Create location data from current location
        LocationData home = LocationData.fromLocation(player.getLocation());
        
        // Store home
        data.homes.put(homeName, home);
        
        // Save to file
        savePlayerData(player.getUniqueId());
        
        // Success message
        if (isUpdate) {
            player.sendMessage(Component.text("Home '", NamedTextColor.GREEN)
                .append(Component.text(homeName, NamedTextColor.AQUA))
                .append(Component.text("' updated!", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Home '", NamedTextColor.GREEN)
                .append(Component.text(homeName, NamedTextColor.AQUA))
                .append(Component.text("' set! (", NamedTextColor.GREEN))
                .append(Component.text(data.homes.size(), NamedTextColor.YELLOW))
                .append(Component.text("/", NamedTextColor.GREEN))
                .append(Component.text(getMaxHomes(player), NamedTextColor.YELLOW))
                .append(Component.text(")", NamedTextColor.GREEN)));
        }
        
        return true;
    }
    
    /**
     * Handles the /delhome command.
     * 
     * <p><b>Usage:</b> /delhome &lt;name&gt;</p>
     * 
     * <p><b>Permission:</b> pixelsessentials.delhome</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleDelHomeCommand(CommandSender sender, String[] args) {
        // Must be a player to delete homes
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.delhome")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Check for home name argument
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /delhome <n>", NamedTextColor.RED));
            return true;
        }
        
        String homeName = args[0].toLowerCase();
        
        // Load player data if not cached
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // Check if home exists
        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Component.text("Home not found: ", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW)));
            return true;
        }
        
        // Remove home
        data.homes.remove(homeName);
        
        // Save to file
        savePlayerData(player.getUniqueId());
        
        // Success message
        player.sendMessage(Component.text("Home '", NamedTextColor.GREEN)
            .append(Component.text(homeName, NamedTextColor.AQUA))
            .append(Component.text("' deleted!", NamedTextColor.GREEN)));
        
        return true;
    }
    
    /**
     * Handles the /homeinfo command.
     * 
     * <p><b>Usage:</b></p>
     * <ul>
     *   <li>/homeinfo - Shows home count summary (current/max)</li>
     *   <li>/homeinfo &lt;name&gt; - Shows coordinates of specified home</li>
     * </ul>
     * 
     * <p><b>Permission:</b> pixelsessentials.homeinfo</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleHomeInfoCommand(CommandSender sender, String[] args) {
        // Must be a player to view home info
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.homeinfo")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Load player data if not cached
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // No arguments - show home count summary
        if (args.length < 1) {
            int currentHomes = data.homes.size();
            int maxHomes = getMaxHomes(player);
            
            player.sendMessage(Component.text("━━━━━━ Home Info ━━━━━━", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Homes set: ", NamedTextColor.GRAY)
                .append(Component.text(currentHomes, NamedTextColor.AQUA))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(maxHomes, NamedTextColor.GREEN)));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            
            return true;
        }
        
        String homeName = args[0].toLowerCase();
        
        // Check if home exists
        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Component.text("Home not found: ", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW)));
            return true;
        }
        
        // Get home data
        LocationData home = data.homes.get(homeName);
        
        // Display home info
        player.sendMessage(Component.text("━━━━━━ Home: ", NamedTextColor.GOLD)
            .append(Component.text(homeName, NamedTextColor.AQUA))
            .append(Component.text(" ━━━━━━", NamedTextColor.GOLD)));
        
        player.sendMessage(Component.text("World: ", NamedTextColor.GRAY)
            .append(Component.text(home.worldName, NamedTextColor.GREEN)));
        
        player.sendMessage(Component.text("Coordinates: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("X: %.1f, Y: %.1f, Z: %.1f", home.x, home.y, home.z), NamedTextColor.YELLOW)));
        
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        
        return true;
    }
    
    // ==================================================================================
    // AUTOFEED COMMAND HANDLER
    // ==================================================================================
    
    /**
     * Handles the /autofeed command.
     * 
     * <p><b>Usage:</b></p>
     * <ul>
     *   <li>/autofeed on - Enables autofeed</li>
     *   <li>/autofeed off - Disables autofeed</li>
     *   <li>/autofeed - Shows current status</li>
     * </ul>
     * 
     * <p><b>Permission:</b> pixelsessentials.autofeed</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleAutofeedCommand(CommandSender sender, String[] args) {
        // Must be a player to use autofeed
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.autofeed")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Load player data
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        
        // No arguments - show current status
        if (args.length < 1) {
            boolean enabled = data.autofeedEnabled;
            player.sendMessage(Component.text("Autofeed is currently: ", NamedTextColor.GRAY)
                .append(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
            player.sendMessage(Component.text("Usage: /autofeed <on|off>", NamedTextColor.YELLOW));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("on")) {
            data.autofeedEnabled = true;
            savePlayerData(uuid);
            player.sendMessage(Component.text("Autofeed enabled!", NamedTextColor.GREEN));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("off")) {
            data.autofeedEnabled = false;
            savePlayerData(uuid);
            player.sendMessage(Component.text("Autofeed disabled.", NamedTextColor.YELLOW));
            return true;
        }
        
        player.sendMessage(Component.text("Usage: /autofeed <on|off>", NamedTextColor.RED));
        return true;
    }
    
    // ==================================================================================
    // BACK COMMAND HANDLER
    // ==================================================================================
    
    /**
     * Handles the /back command.
     * 
     * <p>Teleports the player to their last location before teleporting or their death location.
     * If the last event was a death and the player doesn't have pixelsessentials.back.ondeath,
     * they will be teleported to their last teleport location instead (if available).</p>
     * 
     * <p><b>Permissions:</b></p>
     * <ul>
     *   <li>pixelsessentials.back - Required to use the command</li>
     *   <li>pixelsessentials.back.ondeath - Required to return to death location</li>
     * </ul>
     * 
     * @param sender The command sender (must be a player)
     * @return true if command was handled
     */
    private boolean handleBackCommand(CommandSender sender) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.back")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Load player data
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        
        if (data == null) {
            player.sendMessage(Component.text("No previous location to return to.", NamedTextColor.RED));
            return true;
        }
        
        Location targetLocation = null;
        String message = null;
        
        // Determine which location to use
        if (data.lastWasDeath) {
            // Last event was death
            if (player.hasPermission("pixelsessentials.back.ondeath")) {
                // Player can return to death location
                if (data.lastDeathLocation != null) {
                    targetLocation = data.lastDeathLocation.toLocation();
                    message = "Teleported to your death location.";
                }
            } else {
                // Player cannot return to death location, try last teleport location
                if (data.lastTeleportLocation != null) {
                    targetLocation = data.lastTeleportLocation.toLocation();
                    message = "You don't have permission to return to your death point; returning to your last known location.";
                } else {
                    player.sendMessage(Component.text("You don't have permission to return to your death point.", NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            // Last event was teleport
            if (data.lastTeleportLocation != null) {
                targetLocation = data.lastTeleportLocation.toLocation();
                message = "Teleported to your previous location.";
            }
        }
        
        // Check if we have a valid location
        if (targetLocation == null) {
            player.sendMessage(Component.text("No previous location to return to.", NamedTextColor.RED));
            return true;
        }
        
        // Teleport the player
        player.teleport(targetLocation);
        
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
        
        return true;
    }
    
    // ==================================================================================
    // MAIN PLUGIN COMMAND HANDLER
    // ==================================================================================
    
    /**
     * Handles the /pixelsessentials command for plugin management.
     * 
     * <p><b>Subcommands:</b></p>
     * <ul>
     *   <li>reload - Reloads the configuration file</li>
     *   <li>debug on|off - Toggles debug logging</li>
     * </ul>
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if command was handled
     */
    private boolean handleMainCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            // Show plugin info
            sender.sendMessage(Component.text("PixelsEssentials ", NamedTextColor.GREEN)
                .append(Component.text("v" + getDescription().getVersion(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("By ", NamedTextColor.GRAY)
                .append(Component.text("SupaFloof Games, LLC", NamedTextColor.LIGHT_PURPLE)));
            
            // Only show commands the player has permission for
            if (sender.hasPermission("pixelsessentials.reload")) {
                sender.sendMessage(Component.text("/pe reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
            }
            if (sender.hasPermission("pixelsessentials.debug")) {
                sender.sendMessage(Component.text("/pe debug <on|off>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Toggle debug logging", NamedTextColor.GRAY)));
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            // Check permission
            if (!sender.hasPermission("pixelsessentials.reload")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            // Reload config
            reloadConfig();
            
            // Clear player data cache so it reloads from disk on next access
            playerDataCache.clear();
            
            sender.sendMessage(Component.text("[PixelsEssentials] ", NamedTextColor.GREEN)
                .append(Component.text("Configuration and player data cache reloaded!", NamedTextColor.GREEN)));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("debug")) {
            // Check permission
            if (!sender.hasPermission("pixelsessentials.debug")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text("Debug mode is currently: ", NamedTextColor.GRAY)
                    .append(Component.text(debugMode ? "ON" : "OFF", debugMode ? NamedTextColor.GREEN : NamedTextColor.RED)));
                sender.sendMessage(Component.text("Usage: /pe debug <on|off>", NamedTextColor.YELLOW));
                return true;
            }
            
            if (args[1].equalsIgnoreCase("on")) {
                debugMode = true;
                sender.sendMessage(Component.text("[PixelsEssentials] ", NamedTextColor.GREEN)
                    .append(Component.text("Debug mode enabled!", NamedTextColor.GREEN)));
            } else if (args[1].equalsIgnoreCase("off")) {
                debugMode = false;
                sender.sendMessage(Component.text("[PixelsEssentials] ", NamedTextColor.GREEN)
                    .append(Component.text("Debug mode disabled!", NamedTextColor.YELLOW)));
            } else {
                sender.sendMessage(Component.text("Usage: /pe debug <on|off>", NamedTextColor.RED));
            }
            return true;
        }
        
        sender.sendMessage(Component.text("Unknown command. Use /pe for help.", NamedTextColor.RED));
        return true;
    }
    
    // ==================================================================================
    // PLAYER DATA PERSISTENCE
    // ==================================================================================
    
    /**
     * Loads a player's data from their YAML file into the cache.
     * 
     * <p>File location: plugins/PixelsEssentials/playerdata/{uuid}.yml</p>
     * 
     * <p>File format:</p>
     * <pre>
     * lastlocation:
     *   world: &lt;world-uuid&gt;
     *   world-name: world
     *   x: 100.0
     *   ...
     * logoutlocation:
     *   ...
     * homes:
     *   home:
     *     world: &lt;world-uuid&gt;
     *     world-name: world
     *     ...
     * </pre>
     * 
     * @param uuid The player's UUID
     */
    private void loadPlayerData(UUID uuid) {
        // Skip if already cached
        if (playerDataCache.containsKey(uuid)) {
            return;
        }
        
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData();
        
        if (playerFile.exists()) {
            if (debugMode) {
                getLogger().info("[DEBUG] Loading player data from: " + playerFile.getAbsolutePath());
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            
            // Load last teleport location
            if (config.contains("lastteleportlocation")) {
                data.lastTeleportLocation = loadLocationData(config.getConfigurationSection("lastteleportlocation"));
            }
            
            // Load last death location
            if (config.contains("lastdeathlocation")) {
                data.lastDeathLocation = loadLocationData(config.getConfigurationSection("lastdeathlocation"));
            }
            
            // Load whether last event was death
            data.lastWasDeath = config.getBoolean("last-was-death", false);
            
            // Load logoutlocation
            if (config.contains("logoutlocation")) {
                data.logoutLocation = loadLocationData(config.getConfigurationSection("logoutlocation"));
            }
            
            // Load homes
            ConfigurationSection homesSection = config.getConfigurationSection("homes");
            if (homesSection != null) {
                if (debugMode) {
                    getLogger().info("[DEBUG] Found homes section with keys: " + homesSection.getKeys(false));
                }
                for (String homeName : homesSection.getKeys(false)) {
                    ConfigurationSection homeSection = homesSection.getConfigurationSection(homeName);
                    if (homeSection != null) {
                        LocationData home = loadLocationData(homeSection);
                        if (home != null) {
                            data.homes.put(homeName.toLowerCase(), home);
                        }
                    }
                }
                if (debugMode) {
                    getLogger().info("[DEBUG] Loaded " + data.homes.size() + " homes for player " + uuid);
                }
            } else {
                if (debugMode) {
                    getLogger().warning("[DEBUG] No 'homes' section found in player file: " + playerFile.getName());
                }
            }
            
            // Load autofeed setting (default true if not present)
            data.autofeedEnabled = config.getBoolean("autofeed", true);
        } else {
            if (debugMode) {
                getLogger().info("[DEBUG] No player data file found at: " + playerFile.getAbsolutePath());
            }
        }
        
        playerDataCache.put(uuid, data);
    }
    
    /**
     * Loads a LocationData object from a configuration section.
     * 
     * @param section The configuration section containing location data
     * @return LocationData object, or null if section is null
     */
    private LocationData loadLocationData(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        
        String worldUuid = section.getString("world", "");
        String worldName = section.getString("world-name", "world");
        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 64);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        return new LocationData(worldUuid, worldName, x, y, z, yaw, pitch);
    }
    
    /**
     * Saves a player's data from the cache to their YAML file.
     * 
     * <p>File location: plugins/PixelsEssentials/playerdata/{uuid}.yml</p>
     * 
     * @param uuid The player's UUID
     */
    private void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) {
            return;
        }
        
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        // Save last teleport location
        if (data.lastTeleportLocation != null) {
            saveLocationData(config, "lastteleportlocation", data.lastTeleportLocation);
        }
        
        // Save last death location
        if (data.lastDeathLocation != null) {
            saveLocationData(config, "lastdeathlocation", data.lastDeathLocation);
        }
        
        // Save whether last event was death
        config.set("last-was-death", data.lastWasDeath);
        
        // Save logoutlocation
        if (data.logoutLocation != null) {
            saveLocationData(config, "logoutlocation", data.logoutLocation);
        }
        
        // Save homes
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
    
    /**
     * Saves a LocationData object to a configuration path.
     * 
     * @param config The configuration to save to
     * @param path The path prefix (e.g., "homes.home" or "lastlocation")
     * @param location The location data to save
     */
    private void saveLocationData(YamlConfiguration config, String path, LocationData location) {
        config.set(path + ".world", location.worldUuid);
        config.set(path + ".world-name", location.worldName);
        config.set(path + ".x", location.x);
        config.set(path + ".y", location.y);
        config.set(path + ".z", location.z);
        config.set(path + ".yaw", location.yaw);
        config.set(path + ".pitch", location.pitch);
    }
    
    /**
     * Determines the maximum number of homes a player can have based on their permissions.
     * 
     * <p>Checks all pixelsessentials.sethome.&lt;tier&gt; permissions and returns the
     * highest value from the sethome-multiple configuration section.</p>
     * 
     * @param player The player to check
     * @return The maximum number of homes allowed (minimum 1)
     */
    private int getMaxHomes(Player player) {
        FileConfiguration config = getConfig();
        ConfigurationSection multipleSection = config.getConfigurationSection("sethome-multiple");
        
        if (multipleSection == null) {
            if (debugMode) {
                getLogger().warning("[DEBUG] sethome-multiple section not found in config.yml!");
            }
            return 1; // Default if no config
        }
        
        int maxHomes = 0;
        String matchedTier = null;
        
        if (debugMode) {
            getLogger().info("[DEBUG] Checking sethome permissions for player: " + player.getName());
            getLogger().info("[DEBUG] Available tiers in config: " + multipleSection.getKeys(false));
        }
        
        // Check each tier permission
        for (String tier : multipleSection.getKeys(false)) {
            String permission = "pixelsessentials.sethome." + tier;
            boolean hasPermission = player.hasPermission(permission);
            int tierValue = multipleSection.getInt(tier, 0);
            
            if (debugMode) {
                getLogger().info("[DEBUG]   " + permission + " = " + hasPermission + " (value: " + tierValue + ")");
            }
            
            if (hasPermission && tierValue > maxHomes) {
                maxHomes = tierValue;
                matchedTier = tier;
            }
        }
        
        // Debug logging summary
        if (debugMode) {
            if (matchedTier != null) {
                getLogger().info("[DEBUG] Player " + player.getName() + " best match: '" + matchedTier + "' with " + maxHomes + " homes");
            } else {
                getLogger().info("[DEBUG] Player " + player.getName() + " matched no sethome tiers, defaulting to 1");
            }
        }
        
        // Return at least 1 if player has base sethome permission but no tier
        return Math.max(maxHomes, 1);
    }
    
    // ==================================================================================
    // TAB COMPLETION
    // ==================================================================================
    
    /**
     * Provides tab completion for all plugin commands.
     * 
     * @param sender The command sender
     * @param command The command object
     * @param alias The command alias used
     * @param args The current arguments
     * @return List of tab completion options
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();
        
        switch (cmdName) {
            case "repair":
                return tabCompleteRepair(sender, args);
            case "home":
            case "homes":
                return tabCompleteHome(sender, args);
            case "sethome":
                // No tab completion needed for sethome
                return completions;
            case "delhome":
            case "homeinfo":
                return tabCompleteHomeNames(sender, args);
            case "autofeed":
                return tabCompleteAutofeed(sender, args);
            case "pixelsessentials":
                return tabCompleteMain(sender, args);
            default:
                return completions;
        }
    }
    
    /**
     * Tab completion for /repair command.
     */
    private List<String> tabCompleteRepair(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            if (sender.hasPermission("pixelsessentials.repair.hand")) {
                completions.add("hand");
            }
            if (sender.hasPermission("pixelsessentials.repair.all")) {
                completions.add("all");
            }
            if (sender.hasPermission("pixelsessentials.repair.player")) {
                completions.add("player");
            }
            
            return filterCompletions(completions, args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            // Second argument for player - online player names
            if (sender.hasPermission("pixelsessentials.repair.player")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
            
            return filterCompletions(completions, args[1]);
        }
        
        return completions;
    }
    
    /**
     * Tab completion for /home command - shows player's home names.
     */
    private List<String> tabCompleteHome(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        if (args.length == 1) {
            Player player = (Player) sender;
            loadPlayerData(player.getUniqueId());
            
            PlayerData data = playerDataCache.get(player.getUniqueId());
            if (data != null) {
                completions.addAll(data.homes.keySet());
            }
            
            return filterCompletions(completions, args[0]);
        }
        
        return completions;
    }
    
    /**
     * Tab completion for /delhome and /homeinfo - shows player's home names.
     */
    private List<String> tabCompleteHomeNames(CommandSender sender, String[] args) {
        return tabCompleteHome(sender, args);
    }
    
    /**
     * Tab completion for /autofeed command.
     */
    private List<String> tabCompleteAutofeed(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            return filterCompletions(completions, args[0]);
        }
        
        return completions;
    }
    
    /**
     * Tab completion for /pixelsessentials command.
     * Only shows commands the player has permission for.
     */
    private List<String> tabCompleteMain(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("pixelsessentials.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("pixelsessentials.debug")) {
                completions.add("debug");
            }
            
            return filterCompletions(completions, args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("pixelsessentials.debug")) {
                completions.add("on");
                completions.add("off");
            }
            return filterCompletions(completions, args[1]);
        }
        
        return completions;
    }
    
    /**
     * Filters completions based on what the user has typed so far.
     */
    private List<String> filterCompletions(List<String> completions, String partial) {
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
            .toList();
    }
    
    // ==================================================================================
    // INTERNAL CLASSES
    // ==================================================================================
    
    /**
     * Internal class to store all data for a player.
     * 
     * <p>Contains homes, last locations, and logout location.</p>
     */
    private static class PlayerData {
        /** Map of home name (lowercase) to location data */
        Map<String, LocationData> homes = new HashMap<>();
        
        /** Last location before teleport (may be null) */
        LocationData lastTeleportLocation;
        
        /** Last death location (may be null) */
        LocationData lastDeathLocation;
        
        /** Whether the most recent event was a death (true) or teleport (false) */
        boolean lastWasDeath = false;
        
        /** Location when player logged out (may be null) */
        LocationData logoutLocation;
        
        /** Whether autofeed is enabled for this player (default true) */
        boolean autofeedEnabled = true;
    }
    
    /**
     * Internal class to store location data with world UUID and name.
     * 
     * <p>Contains all information needed to teleport a player to a saved location,
     * including both world UUID (for reliable identification) and world name
     * (for human-readable display and fallback).</p>
     */
    private static class LocationData {
        /** The UUID of the world as a string */
        final String worldUuid;
        
        /** The name of the world (for display and fallback) */
        final String worldName;
        
        /** X coordinate */
        final double x;
        
        /** Y coordinate */
        final double y;
        
        /** Z coordinate */
        final double z;
        
        /** Yaw (horizontal rotation) */
        final float yaw;
        
        /** Pitch (vertical rotation) */
        final float pitch;
        
        /**
         * Constructs a new LocationData object.
         * 
         * @param worldUuid World UUID as string
         * @param worldName World name
         * @param x X coordinate
         * @param y Y coordinate
         * @param z Z coordinate
         * @param yaw Horizontal rotation
         * @param pitch Vertical rotation
         */
        LocationData(String worldUuid, String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldUuid = worldUuid;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        /**
         * Creates a LocationData from a Bukkit Location.
         * 
         * @param location The Bukkit Location
         * @return LocationData object
         */
        static LocationData fromLocation(Location location) {
            World world = location.getWorld();
            return new LocationData(
                world.getUID().toString(),
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
            );
        }
        
        /**
         * Converts this location data to a Bukkit Location.
         * 
         * <p>First tries to find the world by UUID, then falls back to world name.</p>
         * 
         * @return Location object, or null if world doesn't exist
         */
        Location toLocation() {
            World bukkitWorld = null;
            
            // First try to find by UUID
            try {
                UUID uuid = UUID.fromString(worldUuid);
                bukkitWorld = Bukkit.getWorld(uuid);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, try by name
            }
            
            // Fallback to world name
            if (bukkitWorld == null) {
                bukkitWorld = Bukkit.getWorld(worldName);
            }
            
            if (bukkitWorld == null) {
                return null;
            }
            
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }
}