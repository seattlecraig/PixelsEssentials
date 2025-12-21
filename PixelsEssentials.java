package com.supafloof.pixelsessentials;

// ============================================================================================
// IMPORTS - Adventure API for modern text components
// ============================================================================================
import net.kyori.adventure.text.Component;           // Modern text component system
import net.kyori.adventure.text.format.NamedTextColor; // Predefined color constants
import net.kyori.adventure.text.format.TextDecoration; // Text styling (bold, italic, etc.)

// ============================================================================================
// IMPORTS - Bukkit/Paper Core API
// ============================================================================================
import org.bukkit.Bukkit;                            // Server access and utilities
import org.bukkit.ChatColor;                         // Legacy color code translation
import org.bukkit.Location;                          // World position with rotation
import org.bukkit.Material;                          // Block/item types
import org.bukkit.World;                             // World reference
import org.bukkit.command.Command;                   // Command object from plugin.yml
import org.bukkit.command.CommandExecutor;           // Interface for handling commands
import org.bukkit.command.CommandSender;             // Player, console, or command block
import org.bukkit.command.TabCompleter;              // Interface for tab completion
import org.bukkit.configuration.ConfigurationSection; // YAML section access
import org.bukkit.configuration.file.FileConfiguration; // Main config access
import org.bukkit.configuration.file.YamlConfiguration; // YAML file operations
import org.bukkit.entity.Player;                     // Player entity
import org.bukkit.event.EventHandler;                // Event listener annotation
import org.bukkit.event.EventPriority;               // Event execution order
import org.bukkit.event.Listener;                    // Interface for event handling
import org.bukkit.event.entity.FoodLevelChangeEvent; // Hunger change event
import org.bukkit.event.entity.PlayerDeathEvent;     // Player death event
import org.bukkit.event.player.PlayerQuitEvent;      // Player disconnect event
import org.bukkit.event.player.PlayerRespawnEvent;   // Player respawn event
import org.bukkit.event.player.PlayerTeleportEvent;  // Player teleport event
import org.bukkit.inventory.ItemStack;               // Item instance
import org.bukkit.inventory.PlayerInventory;         // Player's inventory
import org.bukkit.inventory.meta.Damageable;         // Interface for items with durability
import org.bukkit.inventory.meta.ItemMeta;           // Item metadata (name, lore, etc.)
import org.bukkit.plugin.java.JavaPlugin;            // Base class for plugins
import org.bukkit.NamespacedKey;                     // Namespaced identifier for registry
import org.bukkit.Registry;                          // Bukkit registry for enchantments
import org.bukkit.enchantments.Enchantment;          // Enchantment type

// ============================================================================================
// IMPORTS - ItemsAdder API (Optional Dependency)
// ============================================================================================
import dev.lone.itemsadder.api.CustomStack;          // ItemsAdder custom item API

// ============================================================================================
// IMPORTS - Java Standard Library
// ============================================================================================
import java.io.*;                                    // File I/O operations
import java.util.*;                                  // Collections and utilities

/**
 * ============================================================================================
 * PIXELSESSENTIALS - Main Plugin Class
 * ============================================================================================
 * 
 * A comprehensive essentials plugin providing core utility commands for Minecraft servers.
 * This single-class architecture implements all functionality within one file for simplicity
 * and ease of maintenance.
 * 
 * ARCHITECTURE OVERVIEW:
 * ---------------------
 * - Implements CommandExecutor for handling all plugin commands
 * - Implements TabCompleter for providing intelligent tab completion
 * - Implements Listener for handling Bukkit events (teleport, death, food, quit)
 * 
 * FEATURE MODULES:
 * ---------------
 * 1. REPAIR SYSTEM      - Fix damaged items (hand, all, player)
 * 2. HOME SYSTEM        - Save and teleport to personal locations
 * 3. BACK COMMAND       - Return to previous location (teleport or death)
 * 4. AUTOFEED          - Automatic hunger restoration
 * 5. DEATH PROTECTIONS  - Keep inventory, XP, or position on death
 * 6. GIVE ENCHANTED    - Give ItemsAdder items with custom enchantments
 * 7. ADMIN COMMANDS    - Reload config, toggle debug mode
 * 
 * DATA PERSISTENCE:
 * ----------------
 * Player data is stored in YAML files at: plugins/PixelsEssentials/playerdata/{uuid}.yml
 * Data is cached in memory and saved to disk on modification or player disconnect.
 * 
 * @author SupaFloof Games, LLC
 * @version 1.0.0
 */
public class PixelsEssentials extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    
    // ========================================================================================
    // INSTANCE FIELDS - Plugin State and Data Storage
    // ========================================================================================
    
    /**
     * Directory reference for player data files.
     * Location: plugins/PixelsEssentials/playerdata/
     * Each player has a file named {uuid}.yml containing their homes, locations, and settings.
     */
    private File playerDataFolder;
    
    /**
     * In-memory cache for player data to minimize disk I/O.
     * 
     * KEY: Player's UUID (survives name changes)
     * VALUE: PlayerData object containing all persistent data for that player
     * 
     * CACHE LIFECYCLE:
     * - Loaded on-demand when player data is first accessed
     * - Saved to disk immediately on any modification
     * - Cleared on /pe reload command
     * - All entries saved to disk on plugin disable
     */
    private Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    
    /**
     * Debug mode toggle for verbose console logging.
     * When true, outputs detailed information about:
     * - Permission checks and tier matching
     * - File load/save operations
     * - Event handling decisions
     * - Location tracking
     * 
     * Toggled via: /pe debug on|off
     */
    private boolean debugMode = false;
    
    /**
     * Temporary storage for death locations used by the keeppos feature.
     * 
     * KEY: Player's UUID
     * VALUE: Clone of the death location
     * 
     * LIFECYCLE:
     * - Added in PlayerDeathEvent when player has keeppos permission
     * - Removed in PlayerRespawnEvent after teleporting player
     * - Also cleaned up in PlayerQuitEvent if player disconnects before respawning
     * 
     * This map is NOT persisted to disk - it only tracks pending respawns.
     */
    private Map<UUID, Location> deathLocations = new HashMap<>();
    
    // ========================================================================================
    // LIFECYCLE METHODS - Plugin Enable/Disable
    // ========================================================================================
    
    /**
     * Called by Bukkit when the plugin is enabled (server start or /reload).
     * 
     * INITIALIZATION SEQUENCE:
     * 1. Create playerdata directory structure
     * 2. Save default config.yml if not exists
     * 3. Register all commands with their executors and tab completers
     * 4. Register this class as an event listener
     * 5. Output startup messages to console
     */
    @Override
    public void onEnable() {
        // ================================================================================
        // STEP 1: Create directory structure for player data storage
        // ================================================================================
        // The playerdata folder stores individual YAML files for each player
        // File naming uses UUIDs to survive player name changes
        playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            // mkdirs() creates all parent directories as well (plugins/PixelsEssentials/)
            playerDataFolder.mkdirs();
        }
        
        // ================================================================================
        // STEP 2: Initialize configuration file
        // ================================================================================
        // saveDefaultConfig() copies config.yml from JAR to plugin folder if not exists
        // This preserves user modifications while ensuring defaults are available
        saveDefaultConfig();
        
        // ================================================================================
        // STEP 3: Register command handlers
        // ================================================================================
        // Each command defined in plugin.yml needs an executor (handler) and optionally
        // a tab completer. The 'this' reference works because this class implements both
        // CommandExecutor and TabCompleter interfaces.
        
        // Repair command - fix damaged items
        getCommand("repair").setExecutor(this);
        getCommand("repair").setTabCompleter(this);
        
        // Home command - teleport to saved homes (also handles 'homes' alias)
        getCommand("home").setExecutor(this);
        getCommand("home").setTabCompleter(this);
        
        // Sethome command - save current location as a home
        getCommand("sethome").setExecutor(this);
        getCommand("sethome").setTabCompleter(this);
        
        // Delhome command - delete a saved home
        getCommand("delhome").setExecutor(this);
        getCommand("delhome").setTabCompleter(this);
        
        // Homeinfo command - display home coordinates
        getCommand("homeinfo").setExecutor(this);
        getCommand("homeinfo").setTabCompleter(this);
        
        // Main plugin command (/pixelsessentials or /pe)
        getCommand("pixelsessentials").setExecutor(this);
        getCommand("pixelsessentials").setTabCompleter(this);
        
        // Autofeed command - toggle automatic hunger restoration
        getCommand("autofeed").setExecutor(this);
        getCommand("autofeed").setTabCompleter(this);
        
        // Back command - return to previous location
        getCommand("back").setExecutor(this);
        getCommand("back").setTabCompleter(this);
        
        // Give enchanted item command - ItemsAdder integration
        getCommand("giveenchanteditem").setExecutor(this);
        getCommand("giveenchanteditem").setTabCompleter(this);
        
        // ================================================================================
        // STEP 4: Register event listeners
        // ================================================================================
        // registerEvents() tells Bukkit to call our @EventHandler methods
        // First parameter: the listener object (this class)
        // Second parameter: the plugin (for cleanup on disable)
        getServer().getPluginManager().registerEvents(this, this);
        
        // ================================================================================
        // STEP 5: Console startup messages
        // ================================================================================
        // Using Adventure API Component for colored console output
        // This is the modern replacement for ChatColor
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Started!", NamedTextColor.GREEN));
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] By SupaFloof Games, LLC", NamedTextColor.LIGHT_PURPLE));
    }
    
    /**
     * Called by Bukkit when the plugin is disabled (server stop or /reload).
     * 
     * SHUTDOWN SEQUENCE:
     * 1. Save all cached player data to disk
     * 2. Clear all caches to free memory
     * 3. Output shutdown message to console
     */
    @Override
    public void onDisable() {
        // ================================================================================
        // STEP 1: Persist all cached player data
        // ================================================================================
        // This ensures no data loss if there are cached modifications that haven't
        // been written to disk yet (though normally saves happen immediately)
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
        
        // ================================================================================
        // STEP 2: Clear memory caches
        // ================================================================================
        // Helps garbage collection and prevents stale data on reload
        playerDataCache.clear();
        deathLocations.clear();
        
        // ================================================================================
        // STEP 3: Console shutdown message
        // ================================================================================
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Shutting Down", NamedTextColor.RED));
    }
    
    // ========================================================================================
    // EVENT HANDLERS - Location Tracking and Death Protections
    // ========================================================================================
    
    /**
     * Handles player teleportation to track the "from" location for /back command.
     * 
     * EVENT PRIORITY: MONITOR
     * - Runs after all other plugins have processed the event
     * - ignoreCancelled=true means we skip if another plugin cancelled the teleport
     * 
     * BEHAVIOR:
     * - Ignores minor movements (less than 1 block) to avoid tracking things like
     *   vehicle dismounts or minor position corrections
     * - Saves the "from" location as the player's lastTeleportLocation
     * - Sets lastWasDeath=false so /back knows this was a teleport, not death
     * 
     * @param event The teleport event containing from/to locations and cause
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        
        // ================================================================================
        // Filter: Ignore minor teleports (< 1 block distance)
        // ================================================================================
        // This prevents tracking things like:
        // - Vehicle dismounts (player moves slightly)
        // - Plugin position corrections
        // - Ender pearl landing adjustments
        Location to = event.getTo();
        if (to != null && from.getWorld().equals(to.getWorld()) 
            && from.distanceSquared(to) < 1.0) {
            return;
        }
        
        // ================================================================================
        // Save the origin location for /back command
        // ================================================================================
        setLastTeleportLocation(player, from);
    }
    
    /**
     * Handles player death for multiple features:
     * 1. Tracks death location for /back command
     * 2. KeepXP - Preserves experience points
     * 3. KeepInv - Preserves inventory items
     * 4. KeepPos - Stores location for respawn teleport
     * 
     * EVENT PRIORITY: LOWEST
     * - Runs FIRST before all other plugins
     * - Critical for keepinv/keepxp to work correctly
     * - If we run after other plugins, items may already be duplicated
     * 
     * IMPORTANT: Using LOWEST priority ensures that when we set keepInventory=true,
     * other plugins see that flag and don't process drops. Running at NORMAL or higher
     * can cause item duplication bugs with grave plugins, death chest plugins, etc.
     * 
     * @param event The death event containing drops, XP, and death message
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        
        // ================================================================================
        // FEATURE: Track death location for /back command
        // ================================================================================
        // This is always saved regardless of permissions
        // The /back command checks permissions when determining if player can use it
        setLastDeathLocation(player, deathLocation);
        
        // ================================================================================
        // FEATURE: Keep Experience (keepxp permission)
        // ================================================================================
        // When enabled, player retains all XP and levels on death
        // setDroppedExp(0) prevents XP orbs from spawning
        // setKeepLevel(true) tells Paper to preserve the level/progress
        if (player.hasPermission("pixelsessentials.keepxp")) {
            event.setDroppedExp(0);       // No XP drops
            event.setKeepLevel(true);      // Preserve level bar
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepXP: Preserved XP for " + player.getName());
            }
        }
        
        // ================================================================================
        // FEATURE: Keep Inventory (keepinv permission)
        // ================================================================================
        // When enabled, player keeps all items on death
        // setKeepInventory(true) is the Paper/Bukkit flag for this
        // We also clear drops as a safety measure against other plugins
        if (player.hasPermission("pixelsessentials.keepinv")) {
            event.setKeepInventory(true);  // Paper handles the rest
            event.getDrops().clear();       // Clear any drops (safety)
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepInv: Preserved inventory for " + player.getName());
            }
        }
        
        // ================================================================================
        // FEATURE: Keep Position (keeppos permission)
        // ================================================================================
        // When enabled, player respawns at their death location
        // We store the location now and use it in PlayerRespawnEvent
        // Must clone the location to prevent issues if the original is modified
        if (player.hasPermission("pixelsessentials.keeppos")) {
            deathLocations.put(player.getUniqueId(), deathLocation.clone());
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepPos: Stored death location for " + player.getName() + 
                    " at " + deathLocation.getWorld().getName() + 
                    " (" + String.format("%.1f, %.1f, %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()) + ")");
            }
        }
    }
    
    /**
     * Handles player respawn to implement the keeppos feature.
     * 
     * EVENT PRIORITY: HIGHEST
     * - Runs LAST after all other plugins
     * - Ensures our respawn location isn't overwritten
     * 
     * BEHAVIOR:
     * - Checks if player has a pending death location from keeppos
     * - If so, sets the respawn location to their death point
     * - Removes the entry from deathLocations map after use
     * 
     * @param event The respawn event containing spawn location
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // ================================================================================
        // Check for pending keeppos respawn
        // ================================================================================
        // If the player died with keeppos permission, their death location
        // was stored in deathLocations map. Retrieve and apply it.
        if (deathLocations.containsKey(uuid)) {
            Location deathLocation = deathLocations.remove(uuid);  // Get and remove
            
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
     * Handles food level changes to implement the autofeed feature.
     * 
     * EVENT PRIORITY: HIGHEST
     * - Runs last to override any other food changes
     * - Ensures autofeed has final say on hunger
     * 
     * BEHAVIOR:
     * - Only affects players (not other entities)
     * - Only triggers when food is DECREASING
     * - Only triggers when food drops to 16 or below (lost 2+ hunger icons)
     * - Restores hunger to 20 (full) and saturation to 20 (max)
     * - Cancels the event to prevent the decrease
     * 
     * TRIGGER THRESHOLD: Food level 16 (4 icons missing from max of 10)
     * This prevents constant triggering on every tiny food loss.
     * 
     * @param event The food level change event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // ================================================================================
        // Filter: Only process player food changes
        // ================================================================================
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // ================================================================================
        // Permission check: Player must have autofeed permission
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.autofeed")) {
            return;
        }
        
        // ================================================================================
        // Setting check: Player must have autofeed enabled in their data
        // ================================================================================
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        if (data == null || !data.autofeedEnabled) {
            return;
        }
        
        int newFoodLevel = event.getFoodLevel();
        int currentFoodLevel = player.getFoodLevel();
        
        // ================================================================================
        // Direction check: Only act when food is decreasing
        // ================================================================================
        // This prevents interfering with food consumption (eating)
        if (newFoodLevel >= currentFoodLevel) {
            return;
        }
        
        // ================================================================================
        // Threshold check: Only restore when hunger gets low enough
        // ================================================================================
        // Food level 16 = 8 hunger points = 4 icons lost from max
        // This prevents constant triggering on every small decrease
        if (newFoodLevel <= 16) {
            // Cancel the decrease
            event.setCancelled(true);
            // Restore to maximum
            player.setFoodLevel(20);        // Full hunger bar
            player.setSaturation(20.0f);    // Max saturation (hidden hunger buffer)
            
            if (debugMode) {
                getLogger().info("[DEBUG] Autofeed: Restored hunger for " + player.getName() + 
                    " (was at " + currentFoodLevel + ", going to " + newFoodLevel + ")");
            }
        }
    }
    
    /**
     * Handles player disconnection for data persistence and cleanup.
     * 
     * EVENT PRIORITY: MONITOR
     * - Runs after all other plugins
     * - Pure observation/logging, doesn't affect event
     * 
     * BEHAVIOR:
     * - Saves the player's current location as their logout location
     * - Persists all cached data to disk
     * - Cleans up any pending death location (if player quit before respawning)
     * 
     * @param event The quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // ================================================================================
        // Load player data into cache if not already loaded
        // ================================================================================
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            // ============================================================================
            // Save logout location
            // ============================================================================
            // This could be used for features like returning to logout spot
            // Currently stored but not actively used
            data.logoutLocation = LocationData.fromLocation(player.getLocation());
            
            // ============================================================================
            // Persist all data to disk
            // ============================================================================
            savePlayerData(uuid);
        }
        
        // ================================================================================
        // Cleanup: Remove any pending death respawn
        // ================================================================================
        // If player had keeppos and quit before clicking respawn,
        // clean up the temporary death location
        deathLocations.remove(uuid);
    }
    
    // ========================================================================================
    // LOCATION TRACKING HELPERS
    // ========================================================================================
    
    /**
     * Saves the player's current location as their last teleport location.
     * Convenience overload that calls the main method with player.getLocation().
     * 
     * @param player The player whose location to save
     */
    private void setLastTeleportLocation(Player player) {
        setLastTeleportLocation(player, player.getLocation());
    }
    
    /**
     * Saves a specific location as the player's last teleport location.
     * Also sets lastWasDeath=false to indicate this was a teleport, not death.
     * 
     * @param player The player to save location for
     * @param location The location to save
     */
    private void setLastTeleportLocation(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        
        // Ensure player data is loaded
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            // Convert Bukkit Location to our serializable LocationData
            data.lastTeleportLocation = LocationData.fromLocation(location);
            // Mark that the last tracked event was a teleport, not death
            data.lastWasDeath = false;
            // Persist immediately
            savePlayerData(uuid);
        }
    }
    
    /**
     * Saves a location as the player's last death location.
     * Also sets lastWasDeath=true to indicate this was a death.
     * 
     * @param player The player who died
     * @param location The death location
     */
    private void setLastDeathLocation(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        
        // Ensure player data is loaded
        loadPlayerData(uuid);
        
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            // Convert Bukkit Location to our serializable LocationData
            data.lastDeathLocation = LocationData.fromLocation(location);
            // Mark that the last tracked event was a death
            data.lastWasDeath = true;
            // Persist immediately
            savePlayerData(uuid);
        }
    }
    
    // ========================================================================================
    // COMMAND ROUTING - Central command handler that delegates to specific handlers
    // ========================================================================================
    
    /**
     * Main command entry point. Routes commands to their specific handlers.
     * 
     * This method is called by Bukkit whenever a player or console executes
     * any command registered to this plugin.
     * 
     * ROUTING PATTERN:
     * Uses a switch statement to delegate to specific handler methods.
     * This keeps the routing logic clean and each handler self-contained.
     * 
     * @param sender The command executor (Player, Console, CommandBlock)
     * @param command The Command object from plugin.yml
     * @param label The alias used to call the command
     * @param args Array of arguments after the command
     * @return true if command was handled, false to show usage from plugin.yml
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Get command name in lowercase for consistent matching
        String cmdName = command.getName().toLowerCase();
        
        // Route to appropriate handler based on command name
        switch (cmdName) {
            case "repair":
                return handleRepairCommand(sender, args);
            case "home":
            case "homes":  // Alias handling - both go to same handler
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
            case "giveenchanteditem":
                return handleGiveEnchantedItemCommand(sender, args);
            case "pixelsessentials":
                return handleMainCommand(sender, args);
            default:
                return false;  // Unknown command, show usage
        }
    }
    
    // ========================================================================================
    // REPAIR COMMAND HANDLERS
    // ========================================================================================
    
    /**
     * Routes /repair subcommands to their handlers.
     * 
     * SUBCOMMANDS:
     * - hand: Repair item in main hand
     * - all: Repair all items in inventory
     * - player <n>: Repair another player's items
     * 
     * @param sender Command executor
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleRepairCommand(CommandSender sender, String[] args) {
        // Require at least one argument (the subcommand)
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /repair <hand|all|player>", NamedTextColor.RED));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // Route to appropriate subcommand handler
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
     * PERMISSION: pixelsessentials.repair.hand
     * 
     * BEHAVIOR:
     * - Must be executed by a player (not console)
     * - Item must be damageable (implements Damageable interface)
     * - Sets item damage to 0 (full durability)
     * - Shows item name in success message
     * 
     * @param sender Command executor (must be Player)
     * @return true if handled
     */
    private boolean handleRepairHand(CommandSender sender) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.repair.hand")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Get the item in main hand
        // ================================================================================
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if actually holding something
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Component.text("You are not holding an item.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Check if item can be repaired
        // ================================================================================
        // Only items that implement Damageable have durability
        // This includes tools, weapons, armor, but not blocks, food, etc.
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            player.sendMessage(Component.text("This item cannot be repaired.", NamedTextColor.RED));
            return true;
        }
        
        Damageable damageable = (Damageable) meta;
        
        // Check if item is already at full durability
        if (damageable.getDamage() == 0) {
            player.sendMessage(Component.text("This item is already fully repaired.", NamedTextColor.YELLOW));
            return true;
        }
        
        // ================================================================================
        // Get item name for feedback message
        // ================================================================================
        String itemName;
        if (meta.hasDisplayName()) {
            // Use custom display name if set
            // PlainTextComponentSerializer strips color codes for clean output
            itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        } else {
            // Format material name: DIAMOND_SWORD -> Diamond Sword
            String materialName = item.getType().name().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder formatted = new StringBuilder();
            for (String word : words) {
                if (formatted.length() > 0) formatted.append(" ");
                // Capitalize first letter of each word
                formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            itemName = formatted.toString();
        }
        
        // ================================================================================
        // Repair the item
        // ================================================================================
        damageable.setDamage(0);  // 0 damage = full durability
        item.setItemMeta(meta);    // Apply the modified meta back to item
        
        // ================================================================================
        // Success feedback
        // ================================================================================
        player.sendMessage(Component.text("Repaired: ", NamedTextColor.GREEN)
            .append(Component.text(itemName, NamedTextColor.YELLOW)));
        
        return true;
    }
    
    /**
     * Handles /repair all - repairs all items in player's inventory and armor.
     * 
     * PERMISSION: pixelsessentials.repair.all
     * 
     * BEHAVIOR:
     * - Repairs main inventory (36 slots including hotbar)
     * - Repairs armor slots (helmet, chestplate, leggings, boots)
     * - Repairs off-hand item
     * - Shows list of all repaired items
     * 
     * @param sender Command executor (must be Player)
     * @return true if handled
     */
    private boolean handleRepairAll(CommandSender sender) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.repair.all")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Repair all items and collect names
        // ================================================================================
        List<String> repairedItems = repairAllItems(player);
        
        // ================================================================================
        // Feedback message
        // ================================================================================
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
     * Handles /repair player &lt;name&gt; - repairs another player's items.
     * 
     * PERMISSION: pixelsessentials.repair.player
     * 
     * BEHAVIOR:
     * - Target player must be online
     * - Repairs target's entire inventory and armor
     * - Notifies both the executor and the target player
     * 
     * @param sender Command executor
     * @param args Command arguments (expects player name at index 1)
     * @return true if handled
     */
    private boolean handleRepairPlayer(CommandSender sender, String[] args) {
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!sender.hasPermission("pixelsessentials.repair.player")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Argument validation: Need player name
        // ================================================================================
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /repair player <playername>", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Find the target player
        // ================================================================================
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: ", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW)));
            return true;
        }
        
        // ================================================================================
        // Repair target's items
        // ================================================================================
        List<String> repairedItems = repairAllItems(target);
        
        // ================================================================================
        // Feedback messages
        // ================================================================================
        if (repairedItems.isEmpty()) {
            sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" had no items that needed repair.", NamedTextColor.YELLOW)));
        } else {
            String itemList = String.join(", ", repairedItems);
            
            // Message to executor
            sender.sendMessage(Component.text("Repaired ", NamedTextColor.GREEN)
                .append(Component.text(repairedItems.size(), NamedTextColor.AQUA))
                .append(Component.text(" item" + (repairedItems.size() == 1 ? "" : "s") + " for ", NamedTextColor.GREEN))
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GREEN))
                .append(Component.text(itemList, NamedTextColor.YELLOW)));
            
            // Notification to target player
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
     * @param player The player whose items to repair
     * @return List of item names that were repaired
     */
    private List<String> repairAllItems(Player player) {
        List<String> repairedItems = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        
        // ================================================================================
        // Repair main inventory (includes hotbar, slots 0-35)
        // ================================================================================
        for (ItemStack item : inventory.getContents()) {
            String itemName = repairItem(item);
            if (itemName != null) {
                repairedItems.add(itemName);
            }
        }
        
        // ================================================================================
        // Repair armor slots (helmet, chestplate, leggings, boots)
        // ================================================================================
        for (ItemStack item : inventory.getArmorContents()) {
            String itemName = repairItem(item);
            if (itemName != null) {
                repairedItems.add(itemName);
            }
        }
        
        // ================================================================================
        // Repair off-hand item
        // ================================================================================
        String offhandItem = repairItem(inventory.getItemInOffHand());
        if (offhandItem != null) {
            repairedItems.add(offhandItem);
        }
        
        return repairedItems;
    }
    
    /**
     * Repairs a single item if it is damageable and has taken damage.
     * 
     * @param item The item to repair (may be null or air)
     * @return The item's display name if repaired, null if not repaired
     */
    private String repairItem(ItemStack item) {
        // ================================================================================
        // Null/Air check
        // ================================================================================
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        // ================================================================================
        // Check if item can be damaged
        // ================================================================================
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return null;
        }
        
        Damageable damageable = (Damageable) meta;
        
        // ================================================================================
        // Check if item actually has damage
        // ================================================================================
        if (damageable.getDamage() == 0) {
            return null;
        }
        
        // ================================================================================
        // Repair the item
        // ================================================================================
        damageable.setDamage(0);
        item.setItemMeta(meta);
        
        // ================================================================================
        // Return friendly name for feedback
        // ================================================================================
        if (meta.hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        } else {
            // Format: DIAMOND_SWORD -> Diamond Sword
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
    
    // ========================================================================================
    // HOME COMMAND HANDLERS
    // ========================================================================================
    
    /**
     * Handles /home and /homes commands.
     * 
     * USAGE:
     * - /home (no args) - Lists all homes
     * - /homes - Same as /home (alias)
     * - /home &lt;name&gt; - Teleports to specified home
     * 
     * PERMISSION: pixelsessentials.home
     * 
     * @param sender Command executor (must be Player)
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleHomeCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.home")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        Map<String, LocationData> homes = data.homes;
        
        // ================================================================================
        // No arguments - list homes
        // ================================================================================
        if (args.length < 1) {
            return listHomes(player, homes);
        }
        
        // ================================================================================
        // Argument provided - teleport to home
        // ================================================================================
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
        
        // ================================================================================
        // Convert LocationData to Bukkit Location
        // ================================================================================
        LocationData home = homes.get(homeName);
        Location location = home.toLocation();
        
        // Check if world still exists (might have been deleted)
        if (location == null || location.getWorld() == null) {
            player.sendMessage(Component.text("Cannot teleport to home '", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW))
                .append(Component.text("' - world no longer exists.", NamedTextColor.RED)));
            return true;
        }
        
        // ================================================================================
        // Teleport the player
        // ================================================================================
        // Note: The teleport event handler will automatically save this as lastTeleportLocation
        player.teleport(location);
        player.sendMessage(Component.text("Teleported to home: ", NamedTextColor.GREEN)
            .append(Component.text(homeName, NamedTextColor.AQUA)));
        
        return true;
    }
    
    /**
     * Lists all homes for a player.
     * 
     * @param player Player to show homes to
     * @param homes Map of home names to locations
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
        
        // Sort home names alphabetically
        List<String> homeNames = new ArrayList<>(homes.keySet());
        Collections.sort(homeNames);
        String homeList = String.join(", ", homeNames);
        
        // Display: "Your homes (3/5): home, farm, mine"
        player.sendMessage(Component.text("Your homes (", NamedTextColor.GREEN)
            .append(Component.text(homes.size(), NamedTextColor.AQUA))
            .append(Component.text("/", NamedTextColor.GREEN))
            .append(Component.text(maxHomes, NamedTextColor.AQUA))
            .append(Component.text("): ", NamedTextColor.GREEN))
            .append(Component.text(homeList, NamedTextColor.YELLOW)));
        
        return true;
    }
    
    /**
     * Handles /sethome command - saves current location as a home.
     * 
     * USAGE:
     * - /sethome - Creates home named "home"
     * - /sethome &lt;name&gt; - Creates home with custom name
     * 
     * PERMISSION: pixelsessentials.sethome
     * HOME LIMIT: Determined by pixelsessentials.sethome.&lt;tier&gt; permissions
     * 
     * NAME VALIDATION:
     * - Only alphanumeric, underscore, and dash
     * - Maximum 32 characters
     * - Case-insensitive (converted to lowercase)
     * 
     * @param sender Command executor (must be Player)
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleSetHomeCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.sethome")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Get home name (default to "home" if not provided)
        // ================================================================================
        String homeName = (args.length > 0) ? args[0].toLowerCase() : "home";
        
        // ================================================================================
        // Validate home name format
        // ================================================================================
        // Regex: ^[a-zA-Z0-9_-]{1,32}$
        // - Only letters, numbers, underscores, dashes
        // - 1 to 32 characters
        if (!homeName.matches("^[a-zA-Z0-9_-]{1,32}$")) {
            player.sendMessage(Component.text("Invalid home name. Use only letters, numbers, underscores, and dashes (max 32 chars).", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // ================================================================================
        // Check if this is an update or new creation
        // ================================================================================
        boolean isUpdate = data.homes.containsKey(homeName);
        
        // ================================================================================
        // Check home limit (only for new homes)
        // ================================================================================
        if (!isUpdate) {
            int maxHomes = getMaxHomes(player);
            if (data.homes.size() >= maxHomes) {
                player.sendMessage(Component.text("You have reached your home limit (", NamedTextColor.RED)
                    .append(Component.text(maxHomes, NamedTextColor.YELLOW))
                    .append(Component.text(").", NamedTextColor.RED)));
                return true;
            }
        }
        
        // ================================================================================
        // Create and store the home
        // ================================================================================
        LocationData home = LocationData.fromLocation(player.getLocation());
        data.homes.put(homeName, home);
        
        // ================================================================================
        // Persist to disk
        // ================================================================================
        savePlayerData(player.getUniqueId());
        
        // ================================================================================
        // Success message
        // ================================================================================
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
     * Handles /delhome command - deletes a saved home.
     * 
     * USAGE: /delhome &lt;name&gt;
     * 
     * PERMISSION: pixelsessentials.delhome
     * 
     * @param sender Command executor (must be Player)
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleDelHomeCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.delhome")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Argument validation: Need home name
        // ================================================================================
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /delhome <n>", NamedTextColor.RED));
            return true;
        }
        
        String homeName = args[0].toLowerCase();
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // ================================================================================
        // Check if home exists
        // ================================================================================
        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Component.text("Home not found: ", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW)));
            return true;
        }
        
        // ================================================================================
        // Delete the home
        // ================================================================================
        data.homes.remove(homeName);
        
        // ================================================================================
        // Persist to disk
        // ================================================================================
        savePlayerData(player.getUniqueId());
        
        // ================================================================================
        // Success message
        // ================================================================================
        player.sendMessage(Component.text("Home '", NamedTextColor.GREEN)
            .append(Component.text(homeName, NamedTextColor.AQUA))
            .append(Component.text("' deleted!", NamedTextColor.GREEN)));
        
        return true;
    }
    
    /**
     * Handles /homeinfo command - displays home information.
     * 
     * USAGE:
     * - /homeinfo - Shows home count summary
     * - /homeinfo &lt;name&gt; - Shows coordinates of specific home
     * 
     * PERMISSION: pixelsessentials.homeinfo
     * 
     * @param sender Command executor (must be Player)
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleHomeInfoCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.homeinfo")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(player.getUniqueId());
        
        PlayerData data = playerDataCache.get(player.getUniqueId());
        
        // ================================================================================
        // No arguments - show summary
        // ================================================================================
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
        
        // ================================================================================
        // Show specific home info
        // ================================================================================
        String homeName = args[0].toLowerCase();
        
        // Check if home exists
        if (!data.homes.containsKey(homeName)) {
            player.sendMessage(Component.text("Home not found: ", NamedTextColor.RED)
                .append(Component.text(homeName, NamedTextColor.YELLOW)));
            return true;
        }
        
        LocationData home = data.homes.get(homeName);
        
        // Display formatted home information
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
    
    // ========================================================================================
    // AUTOFEED COMMAND HANDLER
    // ========================================================================================
    
    /**
     * Handles /autofeed command - toggles automatic hunger restoration.
     * 
     * USAGE:
     * - /autofeed - Shows current status
     * - /autofeed on - Enables autofeed
     * - /autofeed off - Disables autofeed
     * 
     * PERMISSION: pixelsessentials.autofeed
     * 
     * @param sender Command executor (must be Player)
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleAutofeedCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.autofeed")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        
        // ================================================================================
        // No arguments - show current status
        // ================================================================================
        if (args.length < 1) {
            boolean enabled = data.autofeedEnabled;
            player.sendMessage(Component.text("Autofeed is currently: ", NamedTextColor.GRAY)
                .append(Component.text(enabled ? "ON" : "OFF", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
            player.sendMessage(Component.text("Usage: /autofeed <on|off>", NamedTextColor.YELLOW));
            return true;
        }
        
        // ================================================================================
        // Enable autofeed
        // ================================================================================
        if (args[0].equalsIgnoreCase("on")) {
            data.autofeedEnabled = true;
            savePlayerData(uuid);
            player.sendMessage(Component.text("Autofeed enabled!", NamedTextColor.GREEN));
            return true;
        }
        
        // ================================================================================
        // Disable autofeed
        // ================================================================================
        if (args[0].equalsIgnoreCase("off")) {
            data.autofeedEnabled = false;
            savePlayerData(uuid);
            player.sendMessage(Component.text("Autofeed disabled.", NamedTextColor.YELLOW));
            return true;
        }
        
        // ================================================================================
        // Invalid argument
        // ================================================================================
        player.sendMessage(Component.text("Usage: /autofeed <on|off>", NamedTextColor.RED));
        return true;
    }
    
    // ========================================================================================
    // BACK COMMAND HANDLER
    // ========================================================================================
    
    /**
     * Handles /back command - returns player to their previous location.
     * 
     * PERMISSION: pixelsessentials.back (required)
     * PERMISSION: pixelsessentials.back.ondeath (required for death location)
     * 
     * BEHAVIOR:
     * - If lastWasDeath is true:
     *   - With ondeath permission: Teleport to death location
     *   - Without ondeath permission: Teleport to last teleport location (if available)
     * - If lastWasDeath is false:
     *   - Teleport to last teleport location
     * 
     * @param sender Command executor (must be Player)
     * @return true if handled
     */
    private boolean handleBackCommand(CommandSender sender) {
        // ================================================================================
        // Validation: Must be a player
        // ================================================================================
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!player.hasPermission("pixelsessentials.back")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Load player data
        // ================================================================================
        loadPlayerData(uuid);
        PlayerData data = playerDataCache.get(uuid);
        
        if (data == null) {
            player.sendMessage(Component.text("No previous location to return to.", NamedTextColor.RED));
            return true;
        }
        
        Location targetLocation = null;
        String message = null;
        
        // ================================================================================
        // Determine which location to use
        // ================================================================================
        if (data.lastWasDeath) {
            // Last tracked event was a death
            if (player.hasPermission("pixelsessentials.back.ondeath")) {
                // Player can return to death location
                if (data.lastDeathLocation != null) {
                    targetLocation = data.lastDeathLocation.toLocation();
                    message = "Teleported to your death location.";
                }
            } else {
                // Player cannot return to death - try teleport location instead
                if (data.lastTeleportLocation != null) {
                    targetLocation = data.lastTeleportLocation.toLocation();
                    message = "You don't have permission to return to your death point; returning to your last known location.";
                } else {
                    player.sendMessage(Component.text("You don't have permission to return to your death point.", NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            // Last tracked event was a teleport
            if (data.lastTeleportLocation != null) {
                targetLocation = data.lastTeleportLocation.toLocation();
                message = "Teleported to your previous location.";
            }
        }
        
        // ================================================================================
        // Validate target location
        // ================================================================================
        if (targetLocation == null) {
            player.sendMessage(Component.text("No previous location to return to.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Execute teleport
        // ================================================================================
        player.teleport(targetLocation);
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
        
        return true;
    }
    
    // ========================================================================================
    // MAIN PLUGIN COMMAND HANDLER
    // ========================================================================================
    
    /**
     * Handles /pixelsessentials (or /pe) command for plugin administration.
     * 
     * SUBCOMMANDS:
     * - (none) - Shows plugin info and available commands
     * - reload - Reloads configuration (pixelsessentials.reload)
     * - debug on|off - Toggles debug mode (pixelsessentials.debug)
     * 
     * @param sender Command executor
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleMainCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // No arguments - show plugin info
        // ================================================================================
        if (args.length < 1) {
            sender.sendMessage(Component.text("PixelsEssentials ", NamedTextColor.GREEN)
                .append(Component.text("v" + getDescription().getVersion(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("By ", NamedTextColor.GRAY)
                .append(Component.text("SupaFloof Games, LLC", NamedTextColor.LIGHT_PURPLE)));
            
            // Show available commands based on permissions
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
        
        // ================================================================================
        // Reload subcommand
        // ================================================================================
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pixelsessentials.reload")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            // Reload config from disk
            reloadConfig();
            
            // Clear player data cache so data reloads on next access
            playerDataCache.clear();
            
            sender.sendMessage(Component.text("[PixelsEssentials] ", NamedTextColor.GREEN)
                .append(Component.text("Configuration and player data cache reloaded!", NamedTextColor.GREEN)));
            return true;
        }
        
        // ================================================================================
        // Debug subcommand
        // ================================================================================
        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("pixelsessentials.debug")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            // No second arg - show status
            if (args.length < 2) {
                sender.sendMessage(Component.text("Debug mode is currently: ", NamedTextColor.GRAY)
                    .append(Component.text(debugMode ? "ON" : "OFF", debugMode ? NamedTextColor.GREEN : NamedTextColor.RED)));
                sender.sendMessage(Component.text("Usage: /pe debug <on|off>", NamedTextColor.YELLOW));
                return true;
            }
            
            // Toggle debug mode
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
        
        // ================================================================================
        // Unknown subcommand
        // ================================================================================
        sender.sendMessage(Component.text("Unknown command. Use /pe for help.", NamedTextColor.RED));
        return true;
    }
    
    // ========================================================================================
    // PLAYER DATA PERSISTENCE
    // ========================================================================================
    
    /**
     * Loads a player's data from their YAML file into the cache.
     * 
     * FILE LOCATION: plugins/PixelsEssentials/playerdata/{uuid}.yml
     * 
     * This method is idempotent - if data is already cached, it returns immediately.
     * This prevents unnecessary disk I/O during rapid command execution.
     * 
     * @param uuid The player's UUID
     */
    private void loadPlayerData(UUID uuid) {
        // ================================================================================
        // Skip if already cached
        // ================================================================================
        if (playerDataCache.containsKey(uuid)) {
            return;
        }
        
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData();
        
        // ================================================================================
        // Load from file if exists
        // ================================================================================
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
            
            // Load death/teleport flag
            data.lastWasDeath = config.getBoolean("last-was-death", false);
            
            // Load logout location
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
            
            // Load autofeed setting (defaults to true for new players)
            data.autofeedEnabled = config.getBoolean("autofeed", true);
        } else {
            if (debugMode) {
                getLogger().info("[DEBUG] No player data file found at: " + playerFile.getAbsolutePath());
            }
        }
        
        // ================================================================================
        // Store in cache
        // ================================================================================
        playerDataCache.put(uuid, data);
    }
    
    /**
     * Parses a LocationData object from a YAML configuration section.
     * 
     * EXPECTED STRUCTURE:
     * world: "uuid-string"
     * world-name: "world_name"
     * x: 0.0
     * y: 64.0
     * z: 0.0
     * yaw: 0.0
     * pitch: 0.0
     * 
     * @param section Configuration section to parse
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
     * FILE LOCATION: plugins/PixelsEssentials/playerdata/{uuid}.yml
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
        
        // Save death/teleport flag
        config.set("last-was-death", data.lastWasDeath);
        
        // Save logout location
        if (data.logoutLocation != null) {
            saveLocationData(config, "logoutlocation", data.logoutLocation);
        }
        
        // Save all homes
        for (Map.Entry<String, LocationData> entry : data.homes.entrySet()) {
            saveLocationData(config, "homes." + entry.getKey(), entry.getValue());
        }
        
        // Save autofeed setting
        config.set("autofeed", data.autofeedEnabled);
        
        // Write to disk
        try {
            config.save(playerFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Writes a LocationData object to a configuration path.
     * 
     * @param config Configuration to write to
     * @param path Base path (e.g., "homes.home" or "lastlocation")
     * @param location Location data to save
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
     * Calculates the maximum number of homes a player can have.
     * 
     * HOME LIMIT SYSTEM:
     * - Tiers are defined in config.yml under sethome-multiple
     * - Each tier creates a permission: pixelsessentials.sethome.<tier>
     * - Player's max homes = highest tier value they have permission for
     * - Minimum return value is 1 (guaranteed at least 1 home)
     * 
     * EXAMPLE CONFIG:
     * sethome-multiple:
     *   default: 1    -> pixelsessentials.sethome.default
     *   vip: 3        -> pixelsessentials.sethome.vip
     *   admin: 100    -> pixelsessentials.sethome.admin
     * 
     * @param player The player to check
     * @return Maximum homes allowed (minimum 1)
     */
    private int getMaxHomes(Player player) {
        FileConfiguration config = getConfig();
        ConfigurationSection multipleSection = config.getConfigurationSection("sethome-multiple");
        
        // Fallback if config section missing
        if (multipleSection == null) {
            if (debugMode) {
                getLogger().warning("[DEBUG] sethome-multiple section not found in config.yml!");
            }
            return 1;
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
            
            // Track highest tier player has
            if (hasPermission && tierValue > maxHomes) {
                maxHomes = tierValue;
                matchedTier = tier;
            }
        }
        
        if (debugMode) {
            if (matchedTier != null) {
                getLogger().info("[DEBUG] Player " + player.getName() + " best match: '" + matchedTier + "' with " + maxHomes + " homes");
            } else {
                getLogger().info("[DEBUG] Player " + player.getName() + " matched no sethome tiers, defaulting to 1");
            }
        }
        
        // Ensure minimum of 1 home
        return Math.max(maxHomes, 1);
    }
    
    // ========================================================================================
    // TAB COMPLETION
    // ========================================================================================
    
    /**
     * Main tab completion entry point. Routes to specific handlers.
     * 
     * @param sender Command executor
     * @param command Command object
     * @param alias Alias used
     * @param args Current arguments
     * @return List of completion suggestions
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
                // No suggestions for sethome - user types custom name
                return completions;
            case "delhome":
            case "homeinfo":
                return tabCompleteHomeNames(sender, args);
            case "autofeed":
                return tabCompleteAutofeed(sender, args);
            case "giveenchanteditem":
                return tabCompleteGiveEnchantedItem(sender, args);
            case "pixelsessentials":
                return tabCompleteMain(sender, args);
            default:
                return completions;
        }
    }
    
    /**
     * Tab completion for /repair command.
     * Suggests subcommands and player names based on permissions.
     */
    private List<String> tabCompleteRepair(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommand options
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
            // Second argument for "player" - online player names
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
     * Tab completion for /home - shows player's home names.
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
     * Tab completion for /autofeed - suggests on/off.
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
    
    // ========================================================================================
    // GIVE ENCHANTED ITEM COMMAND (ItemsAdder Integration)
    // ========================================================================================
    
    /**
     * Handles /giveenchanteditem (alias: /gei) command.
     * 
     * Gives an ItemsAdder custom item to a player with optional:
     * - Count (1-64)
     * - Custom display name (name:Custom_Name, underscores become spaces)
     * - Custom lore line (lore:Custom_Lore, underscores become spaces)
     * - Enchantments (enchant:level or just enchant for level 1)
     * 
     * PERMISSION: pixelsessentials.giveenchanteditem
     * 
     * REQUIRES: ItemsAdder plugin
     * 
     * EXAMPLES:
     * /gei Steve fairyset:fairy_sword
     * /gei Steve fairyset:fairy_sword 5
     * /gei Steve fairyset:fairy_sword 1 name:&6&oBlade_of_Fire
     * /gei Steve fairyset:fairy_sword 1 sharpness:10 fire_aspect:5 mending
     * 
     * @param sender Command executor
     * @param args Command arguments
     * @return true if handled
     */
    private boolean handleGiveEnchantedItemCommand(CommandSender sender, String[] args) {
        // ================================================================================
        // Permission check
        // ================================================================================
        if (!sender.hasPermission("pixelsessentials.giveenchanteditem")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Argument validation
        // ================================================================================
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /gei <player> <ia_item> [count] [name:Name] [lore:Lore] [enchant:level]...", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /gei Craig fairyset:fairy_helmet 1 name:&6&oAzathoth's_Helmet protection:30 mending", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Note: Underscores become spaces in name/lore. Enchants without :level default to 1.", NamedTextColor.GRAY));
            return true;
        }
        
        // ================================================================================
        // Find target player
        // ================================================================================
        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }
        
        String itemId = args[1];
        
        // ================================================================================
        // Get ItemsAdder custom item
        // ================================================================================
        // CustomStack.getInstance() returns null if item doesn't exist
        CustomStack customStack = CustomStack.getInstance(itemId);
        if (customStack == null) {
            sender.sendMessage(Component.text("Unknown ItemsAdder item: " + itemId, NamedTextColor.RED));
            return true;
        }
        
        // Get the Bukkit ItemStack from ItemsAdder
        ItemStack itemStack = customStack.getItemStack();
        if (itemStack == null) {
            sender.sendMessage(Component.text("Failed to create item: " + itemId, NamedTextColor.RED));
            return true;
        }
        
        // ================================================================================
        // Parse optional arguments
        // ================================================================================
        int count = 1;
        String customName = null;
        String customLore = null;
        List<String> appliedEnchants = new ArrayList<>();
        List<String> failedEnchants = new ArrayList<>();
        
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            
            // ----------------------------------------------------------------------------
            // Check for name: prefix - custom display name
            // ----------------------------------------------------------------------------
            if (arg.toLowerCase().startsWith("name:")) {
                customName = arg.substring(5).replace('_', ' ');
                // Translate &-codes to Minecraft color codes
                customName = ChatColor.translateAlternateColorCodes('&', customName);
                continue;
            }
            
            // ----------------------------------------------------------------------------
            // Check for lore: prefix - custom lore line
            // ----------------------------------------------------------------------------
            if (arg.toLowerCase().startsWith("lore:")) {
                customLore = arg.substring(5).replace('_', ' ');
                customLore = ChatColor.translateAlternateColorCodes('&', customLore);
                continue;
            }
            
            // ----------------------------------------------------------------------------
            // Check for plain number - item count
            // ----------------------------------------------------------------------------
            if (arg.matches("^\\d+$")) {
                try {
                    count = Integer.parseInt(arg);
                    if (count < 1) count = 1;
                    if (count > 64) count = 64;
                } catch (NumberFormatException e) {
                    // Ignore, keep default count
                }
                continue;
            }
            
            // ----------------------------------------------------------------------------
            // Otherwise treat as enchantment
            // ----------------------------------------------------------------------------
            String enchantName;
            int level;
            
            if (arg.contains(":")) {
                // Format: enchantment:level
                String[] parts = arg.split(":");
                enchantName = parts[0].toLowerCase();
                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    failedEnchants.add(arg + " (invalid level)");
                    continue;
                }
            } else {
                // Format: enchantment (default to level 1)
                enchantName = arg.toLowerCase();
                level = 1;
            }
            
            // Look up enchantment in registry (1.21+ method)
            NamespacedKey key = NamespacedKey.minecraft(enchantName);
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            
            if (enchantment == null) {
                failedEnchants.add(arg + " (unknown enchantment)");
                continue;
            }
            
            // Apply enchantment using unsafe method (allows any level)
            itemStack.addUnsafeEnchantment(enchantment, level);
            appliedEnchants.add(enchantName + ":" + level);
        }
        
        // ================================================================================
        // Apply custom name and lore
        // ================================================================================
        if (customName != null || customLore != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                // Preserve existing lore when adding new lore line
                List<String> existingLore = meta.getLore();
                
                if (customName != null) {
                    meta.setDisplayName(customName);
                }
                if (customLore != null) {
                    List<String> loreList = new ArrayList<>();
                    if (existingLore != null) {
                        loreList.addAll(existingLore);
                    }
                    loreList.add(customLore);
                    meta.setLore(loreList);
                }
                
                itemStack.setItemMeta(meta);
            }
        }
        
        // ================================================================================
        // Set stack count
        // ================================================================================
        itemStack.setAmount(count);
        
        // ================================================================================
        // Give item to player
        // ================================================================================
        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(itemStack);
        
        // If inventory was full, drop at feet
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
            sender.sendMessage(Component.text(target.getName() + "'s inventory full - item dropped at their feet.", NamedTextColor.YELLOW));
        }
        
        // ================================================================================
        // Success message
        // ================================================================================
        StringBuilder msg = new StringBuilder();
        msg.append("Given ").append(target.getName()).append(" ").append(count).append("x ").append(itemId);
        
        if (customName != null) {
            msg.append(" named \"").append(customName).append("\"");
        }
        
        if (!appliedEnchants.isEmpty()) {
            msg.append(" with enchantments: ").append(String.join(", ", appliedEnchants));
        }
        
        sender.sendMessage(Component.text(msg.toString(), NamedTextColor.GREEN));
        
        // Report failed enchantments
        if (!failedEnchants.isEmpty()) {
            sender.sendMessage(Component.text("Failed to apply: " + String.join(", ", failedEnchants), NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Tab completion for /giveenchanteditem command.
     * 
     * Suggests:
     * - Player names (arg 1)
     * - name: and lore: prefixes (arg 3+)
     * - Common counts (1, 16, 32, 64)
     * - Enchantment names with optional :level suffix
     */
    private List<String> tabCompleteGiveEnchantedItem(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("pixelsessentials.giveenchanteditem")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return filterCompletions(completions, args[0]);
        }
        
        if (args.length == 2) {
            // Second argument - item ID (no completion, user types it)
            return completions;
        }
        
        if (args.length >= 3) {
            String currentArg = args[args.length - 1].toLowerCase();
            
            // Suggest name: and lore: prefixes
            if ("name:".startsWith(currentArg)) {
                completions.add("name:");
            }
            if ("lore:".startsWith(currentArg)) {
                completions.add("lore:");
            }
            
            // Suggest common counts
            if (currentArg.isEmpty() || currentArg.matches("^\\d*$")) {
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
            }
            
            // Common enchantment names
            String[] commonEnchants = {
                "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect",
                "looting", "sweeping_edge", "efficiency", "silk_touch", "unbreaking",
                "fortune", "power", "punch", "flame", "infinity", "luck_of_the_sea",
                "lure", "loyalty", "impaling", "riptide", "channeling", "multishot",
                "quick_charge", "piercing", "mending", "vanishing_curse", "binding_curse",
                "protection", "fire_protection", "feather_falling", "blast_protection",
                "projectile_protection", "respiration", "aqua_affinity", "thorns",
                "depth_strider", "frost_walker", "soul_speed", "swift_sneak"
            };
            
            // If user hasn't typed a colon yet, suggest enchantment names
            if (!currentArg.contains(":") || currentArg.startsWith("name:") || currentArg.startsWith("lore:")) {
                for (String enchant : commonEnchants) {
                    if (enchant.startsWith(currentArg)) {
                        completions.add(enchant + ":");    // With colon for level
                        completions.add(enchant);          // Without colon (level 1)
                    }
                }
            } else {
                // User has typed "enchant:", suggest levels
                String enchantPart = currentArg.split(":")[0];
                for (int level = 1; level <= 10; level++) {
                    completions.add(enchantPart + ":" + level);
                }
            }
            
            return filterCompletions(completions, currentArg);
        }
        
        return completions;
    }
    
    /**
     * Tab completion for /pixelsessentials command.
     * Shows subcommands based on permissions.
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
     * Filters a list of completions to only include those starting with the partial input.
     * Case-insensitive matching.
     * 
     * @param completions Full list of possible completions
     * @param partial What the user has typed so far
     * @return Filtered list matching the partial input
     */
    private List<String> filterCompletions(List<String> completions, String partial) {
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
            .toList();
    }
    
    // ========================================================================================
    // INTERNAL DATA CLASSES
    // ========================================================================================
    
    /**
     * Container for all persistent data associated with a single player.
     * 
     * This class is intentionally simple with public fields for easy access.
     * It's only used internally within PixelsEssentials.
     */
    private static class PlayerData {
        /**
         * Map of home names (lowercase) to their locations.
         * Key: Home name (always lowercase for case-insensitive lookup)
         * Value: LocationData with world and coordinates
         */
        Map<String, LocationData> homes = new HashMap<>();
        
        /**
         * Location before the player's last teleport.
         * Used by /back command when lastWasDeath is false.
         */
        LocationData lastTeleportLocation;
        
        /**
         * Location where the player last died.
         * Used by /back command when lastWasDeath is true
         * and player has pixelsessentials.back.ondeath permission.
         */
        LocationData lastDeathLocation;
        
        /**
         * Flag indicating whether the most recent tracked event was a death.
         * true = last event was death (use lastDeathLocation for /back)
         * false = last event was teleport (use lastTeleportLocation for /back)
         */
        boolean lastWasDeath = false;
        
        /**
         * Location where the player was when they logged out.
         * Currently stored but not actively used - available for future features.
         */
        LocationData logoutLocation;
        
        /**
         * Whether autofeed is enabled for this player.
         * Defaults to true for new players.
         */
        boolean autofeedEnabled = true;
    }
    
    /**
     * Immutable container for location data that can be serialized to/from YAML.
     * 
     * WORLD IDENTIFICATION:
     * Stores both world UUID and world name for reliability:
     * - UUID is the primary identifier (survives world renames)
     * - Name is used for display and as a fallback
     * 
     * When converting back to a Bukkit Location, first tries UUID lookup,
     * then falls back to name lookup if UUID fails.
     */
    private static class LocationData {
        /** World UUID as a string. Primary identifier for world lookup. */
        final String worldUuid;
        
        /** World name. Used for display and fallback lookup. */
        final String worldName;
        
        /** X coordinate in the world. */
        final double x;
        
        /** Y coordinate (height) in the world. */
        final double y;
        
        /** Z coordinate in the world. */
        final double z;
        
        /** Yaw (horizontal rotation). 0 = South, 90 = West, 180 = North, 270 = East. */
        final float yaw;
        
        /** Pitch (vertical rotation). -90 = up, 0 = forward, 90 = down. */
        final float pitch;
        
        /**
         * Constructs a new LocationData with all fields.
         * 
         * @param worldUuid World UUID string
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
         * Factory method to create LocationData from a Bukkit Location.
         * 
         * @param location Bukkit Location to convert
         * @return New LocationData instance
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
         * Converts this LocationData back to a Bukkit Location.
         * 
         * LOOKUP ORDER:
         * 1. Try to find world by UUID (most reliable)
         * 2. Fall back to world name (handles edge cases)
         * 
         * @return Bukkit Location, or null if world no longer exists
         */
        Location toLocation() {
            World bukkitWorld = null;
            
            // First try UUID lookup (most reliable)
            try {
                UUID uuid = UUID.fromString(worldUuid);
                bukkitWorld = Bukkit.getWorld(uuid);
            } catch (IllegalArgumentException e) {
                // Invalid UUID format, will try name fallback
            }
            
            // Fallback to name lookup
            if (bukkitWorld == null) {
                bukkitWorld = Bukkit.getWorld(worldName);
            }
            
            // World doesn't exist anymore
            if (bukkitWorld == null) {
                return null;
            }
            
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }
}