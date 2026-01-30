package com.supafloof.pixelsessentials;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import dev.lone.itemsadder.api.CustomStack;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PixelsEssentials Plugin - A comprehensive Minecraft Paper/Spigot essentials plugin
 * providing core utility commands for server administration and player convenience.
 * 
 * <h2>Plugin Architecture Overview</h2>
 * <p>This plugin follows a single-file architecture with the main class implementing
 * CommandExecutor, TabCompleter, and Listener interfaces. It uses an in-memory cache
 * for player data with YAML file persistence, and integrates with external plugins
 * (Vault, PlaceholderAPI, ItemsAdder) for extended functionality.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><b>Item Repair System:</b> Repairs damageable items in hand, full inventory, or for other players</li>
 *   <li><b>Home Teleportation:</b> Multi-home system with configurable permission-based limits per tier</li>
 *   <li><b>Location Tracking:</b> Tracks last teleport location, death location, and logout location</li>
 *   <li><b>Back Command:</b> Return to previous location before teleport or death (with permission check for death)</li>
 *   <li><b>Death Management:</b> Optional keep inventory (keepinv), keep XP (keepxp), and keep position (keeppos)</li>
 *   <li><b>Autofeed System:</b> Automatically restores hunger to full when it drops below threshold</li>
 *   <li><b>Extended Ender Chest:</b> Permission-based 54-slot ender chest with lazy loading and async saves</li>
 *   <li><b>Balance Leaderboard Signs:</b> Physical signs displaying top player balances with periodic updates</li>
 *   <li><b>Recipe Unlock:</b> Optional automatic recipe discovery on player join</li>
 *   <li><b>PlaceholderAPI Integration:</b> Custom placeholders for health, balance, and armor durability</li>
 *   <li><b>ItemsAdder Integration:</b> Give custom items with enchantments, custom names, and lore</li>
 *   <li><b>Per-Player Data Storage:</b> YAML-based persistence with in-memory caching for performance</li>
 *   <li><b>Tab Completion:</b> Context-aware suggestions for all commands</li>
 *   <li><b>Hot-Reload:</b> Configuration reload without server restart</li>
 * </ul>
 * 
 * <h2>Commands Reference</h2>
 * <table border="1">
 *   <tr><th>Command</th><th>Description</th><th>Permission</th></tr>
 *   <tr><td>/repair hand</td><td>Repairs item in main hand</td><td>pixelsessentials.repair.hand</td></tr>
 *   <tr><td>/repair all</td><td>Repairs all items in inventory, armor, and off-hand</td><td>pixelsessentials.repair.all</td></tr>
 *   <tr><td>/repair player &lt;name&gt;</td><td>Repairs all items for target player</td><td>pixelsessentials.repair.player</td></tr>
 *   <tr><td>/home [name]</td><td>Teleport to home or list all homes</td><td>pixelsessentials.home</td></tr>
 *   <tr><td>/homes</td><td>List all homes (alias for /home)</td><td>pixelsessentials.home</td></tr>
 *   <tr><td>/sethome [name]</td><td>Set home at current location (default: "home")</td><td>pixelsessentials.sethome + tier</td></tr>
 *   <tr><td>/delhome &lt;name&gt;</td><td>Delete a home</td><td>pixelsessentials.delhome</td></tr>
 *   <tr><td>/homeinfo [name]</td><td>Show home count or coordinates of specific home</td><td>pixelsessentials.homeinfo</td></tr>
 *   <tr><td>/back</td><td>Return to last location before teleport or death</td><td>pixelsessentials.back[.ondeath]</td></tr>
 *   <tr><td>/autofeed [on|off]</td><td>Toggle automatic hunger restoration</td><td>pixelsessentials.autofeed</td></tr>
 *   <tr><td>/gei &lt;player&gt; &lt;item&gt; [opts]</td><td>Give ItemsAdder item with enchants/name/lore</td><td>pixelsessentials.giveenchanteditem</td></tr>
 *   <tr><td>/pe reload</td><td>Reload configuration and clear cache</td><td>pixelsessentials.reload</td></tr>
 *   <tr><td>/pe debug on|off</td><td>Toggle verbose debug logging to console</td><td>pixelsessentials.debug</td></tr>
 *   <tr><td>/pe show &lt;place&gt;</td><td>Initiate balance leaderboard sign creation</td><td>pixelsessentials.show</td></tr>
 *   <tr><td>/pe updatesigns</td><td>Force immediate update of all balance signs</td><td>pixelsessentials.show</td></tr>
 * </table>
 * 
 * <h2>Death-Related Permissions</h2>
 * <ul>
 *   <li><b>pixelsessentials.keepxp:</b> Preserve experience points and levels on death</li>
 *   <li><b>pixelsessentials.keepinv:</b> Preserve inventory contents on death (no item drops)</li>
 *   <li><b>pixelsessentials.keeppos:</b> Respawn at death location instead of spawn point</li>
 *   <li><b>pixelsessentials.back.ondeath:</b> Allow /back to return to death location</li>
 * </ul>
 * 
 * <h2>Extended Ender Chest</h2>
 * <p>Players with the pixelsessentials.enderchest.extended permission get a 54-slot ender chest
 * instead of the vanilla 27 slots. The first 27 slots are synced with the vanilla ender chest,
 * while the extra 27 slots are stored separately in playerdata/{uuid}_enderchest.yml.</p>
 * 
 * <p><b>Technical details:</b></p>
 * <ul>
 *   <li><b>Permission:</b> pixelsessentials.enderchest.extended</li>
 *   <li><b>Triggers:</b> Right-clicking ender chest blocks AND /ec commands from other plugins</li>
 *   <li><b>Storage:</b> Base64-encoded ItemStack array for compact, efficient storage</li>
 *   <li><b>Loading:</b> Lazy-loaded only when player opens an ender chest</li>
 *   <li><b>Saving:</b> Asynchronous to avoid main thread lag</li>
 *   <li><b>Persistence:</b> Survives death (like vanilla ender chest)</li>
 * </ul>
 * 
 * <h2>PlaceholderAPI Placeholders</h2>
 * <ul>
 *   <li><b>%pixelsessentials_current_health%</b> - Current health with 1 decimal (e.g., 15.5)</li>
 *   <li><b>%pixelsessentials_max_health%</b> - Maximum health with 1 decimal (e.g., 20.0 or 40.0 with buffs)</li>
 *   <li><b>%pixelsessentials_formatted_balance%</b> - Economy balance formatted with K/M/B/T suffix</li>
 *   <li><b>%pixelsessentials_helmet_total_durability%</b> - Max durability of equipped helmet</li>
 *   <li><b>%pixelsessentials_helmet_current_durability%</b> - Current durability of equipped helmet</li>
 *   <li><b>%pixelsessentials_chestplate_total_durability%</b> - Max durability of equipped chestplate</li>
 *   <li><b>%pixelsessentials_chestplate_current_durability%</b> - Current durability of equipped chestplate</li>
 *   <li><b>%pixelsessentials_leggings_total_durability%</b> - Max durability of equipped leggings</li>
 *   <li><b>%pixelsessentials_leggings_current_durability%</b> - Current durability of equipped leggings</li>
 *   <li><b>%pixelsessentials_boots_total_durability%</b> - Max durability of equipped boots</li>
 *   <li><b>%pixelsessentials_boots_current_durability%</b> - Current durability of equipped boots</li>
 * </ul>
 * 
 * <h2>Home Limit Configuration</h2>
 * <p>Home limits are determined by permissions in the format pixelsessentials.sethome.&lt;tier&gt;
 * where the tier maps to a number in config.yml's sethome-multiple section. If a player has
 * multiple tier permissions, the highest value is used. Players with base sethome permission
 * but no tier get a minimum of 1 home.</p>
 * <pre>
 * sethome-multiple:
 *   default: 1    # pixelsessentials.sethome.default -> 1 home
 *   vip: 3        # pixelsessentials.sethome.vip -> 3 homes
 *   mvp: 5        # pixelsessentials.sethome.mvp -> 5 homes
 *   admin: 100    # pixelsessentials.sethome.admin -> 100 homes
 * </pre>
 * 
 * <h2>Player Data File Format</h2>
 * <p>Stored at plugins/PixelsEssentials/playerdata/{uuid}.yml</p>
 * <pre>
 * lastteleportlocation:
 *   world: &lt;world-uuid&gt;
 *   world-name: world
 *   x: 100.5
 *   y: 64.0
 *   z: -200.3
 *   yaw: 90.0
 *   pitch: 0.0
 * lastdeathlocation:
 *   world: &lt;world-uuid&gt;
 *   world-name: world_nether
 *   x: 50.0
 *   y: 30.0
 *   z: 100.0
 *   yaw: 180.0
 *   pitch: 0.0
 * last-was-death: false
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
 *   base:
 *     world: &lt;world-uuid&gt;
 *     ...
 * autofeed: true
 * </pre>
 * 
 * <h2>External Dependencies</h2>
 * <ul>
 *   <li><b>Vault (optional):</b> Required for balance leaderboard signs and formatted_balance placeholder</li>
 *   <li><b>PlaceholderAPI (optional):</b> Required for custom placeholder expansion</li>
 *   <li><b>ItemsAdder (optional):</b> Required for /giveenchanteditem command to give custom items</li>
 * </ul>
 * 
 * <h2>Technical Details</h2>
 * <ul>
 *   <li>Player data stored in YAML files per-player using UUID as filename</li>
 *   <li>In-memory cache with on-demand loading and save-on-modify for performance</li>
 *   <li>Location data includes world UUID (primary) and world name (fallback/display)</li>
 *   <li>Separate tracking for lastTeleportLocation vs lastDeathLocation with lastWasDeath flag</li>
 *   <li>Repair uses Damageable interface check to avoid processing non-repairable items</li>
 *   <li>Uses Adventure API Components for all player messaging (modern text API)</li>
 *   <li>Balance signs use ConcurrentHashMap for thread-safety with scheduled updates</li>
 *   <li>Balance cache with 5-second validity prevents redundant economy lookups</li>
 * </ul>
 * 
 * @author SupaFloof Games, LLC
 * @version 1.0.0
 * @see <a href="https://playmc.supafloof.com">SupaFloof Minecraft Server</a>
 */
public class PixelsEssentials extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    
    // ==================================================================================
    // INSTANCE VARIABLES - PLAYER DATA MANAGEMENT
    // ==================================================================================
    
    /**
     * Directory containing individual player data YAML files.
     * 
     * <p><b>Location:</b> plugins/PixelsEssentials/playerdata/</p>
     * <p><b>File naming:</b> {player-uuid}.yml (e.g., 550e8400-e29b-41d4-a716-446655440000.yml)</p>
     * 
     * <p>This directory is created during {@link #onEnable()} if it doesn't exist.
     * Each player who uses home commands, triggers teleport/death events, or uses
     * autofeed will have a corresponding YAML file created here.</p>
     */
    private File playerDataFolder;
    
    /**
     * In-memory cache of player data to optimize file I/O performance.
     * 
     * <p><b>Key:</b> Player UUID</p>
     * <p><b>Value:</b> PlayerData object containing homes, locations, autofeed, and more</p>
     * 
     * <p><b>Cache Behavior:</b></p>
     * <ul>
     *   <li>Data is loaded on-demand when first needed via {@link #loadPlayerData(UUID)}</li>
     *   <li>Data is saved to disk immediately on modification for durability</li>
     *   <li>Cache entries persist for online players to minimize file reads</li>
     *   <li>Cache is cleared on plugin disable ({@link #onDisable()}) and config reload</li>
     * </ul>
     * 
     * <p>This cache pattern significantly reduces disk I/O during gameplay while
     * ensuring data persistence through immediate saves and quit event handling.</p>
     */
    private Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    
    /**
     * Debug mode flag controlling verbose console logging.
     * 
     * <p><b>Toggle:</b> /pe debug on|off (requires pixelsessentials.debug permission)</p>
     * 
     * <p>When enabled, outputs detailed information about:</p>
     * <ul>
     *   <li>Player data loading/saving operations with file paths</li>
     *   <li>Permission checks for home limits showing all tier evaluations</li>
     *   <li>Death/respawn handling for keepxp/keepinv/keeppos features</li>
     *   <li>Autofeed trigger events with hunger level details</li>
     *   <li>Balance sign update operations</li>
     *   <li>Recipe unlock counts on player join</li>
     * </ul>
     * 
     * <p>This flag is not persisted to config and resets to false on plugin reload/restart.</p>
     */
    private boolean debugMode = false;
    
    /**
     * Pending death location choices awaiting player GUI selection (keeppos feature).
     * 
     * <p><b>Key:</b> Player UUID</p>
     * <p><b>Value:</b> Cloned Location where the player died</p>
     * 
     * <p>When a player with keeppos permission dies, their death location is stored here.
     * They respawn at lobby spawn, then a GUI opens to let them choose whether to
     * return to their death location or stay safe.</p>
     */
    private Map<UUID, Location> pendingDeathLocationChoices = new HashMap<>();
    
    /**
     * Title for the keeppos death location choice GUI.
     * Used to identify our inventory in InventoryClickEvent.
     */
    private static final String KEEPPOS_GUI_TITLE = "Return to Death Location?";
    
    /**
     * NamespacedKey for storing bank note value in PDC.
     * Used to identify bank notes and store their monetary value.
     */
    private NamespacedKey bankNoteValueKey;
    
    /**
     * NamespacedKey for storing bank note unique ID in PDC.
     * Used to make each bank note unique (unstackable).
     */
    private NamespacedKey bankNoteUniqueKey;
    
    /**
     * Name of the lobby world for keeppos respawning.
     * Players with keeppos will respawn here before seeing the choice GUI.
     * 
     * <p><b>Config:</b> lobby-world (default: "world")</p>
     */
    private String lobbyWorldName = "world";
    
    // ==================================================================================
    // INSTANCE VARIABLES - EXTENDED ENDER CHEST
    // ==================================================================================
    
    /**
     * Title for the extended ender chest inventory (54 slots).
     * Used to identify our custom inventory in InventoryClickEvent and InventoryCloseEvent.
     */
    private static final String EXTENDED_ENDERCHEST_TITLE = "Ender Chest";
    
    /**
     * Cache of extended ender chest contents (the extra 27 slots beyond vanilla).
     * 
     * <p><b>Key:</b> Player UUID</p>
     * <p><b>Value:</b> ItemStack array of size 27 containing the extended slots</p>
     * 
     * <p>This cache is separate from PlayerData for performance reasons:</p>
     * <ul>
     *   <li>Lazy loading - only loaded when player actually opens an ender chest</li>
     *   <li>Not loaded with regular PlayerData to keep that fast</li>
     *   <li>Stored in separate file: playerdata/{uuid}_enderchest.yml</li>
     * </ul>
     * 
     * <p>Contents are saved asynchronously when the inventory is closed to avoid
     * main thread lag from Base64 serialization and file I/O.</p>
     */
    private Map<UUID, ItemStack[]> extendedEnderChestCache = new ConcurrentHashMap<>();
    
    /**
     * Tracks which players currently have an extended ender chest open.
     * Used to identify our custom inventory on close events.
     * 
     * <p><b>Key:</b> Player UUID</p>
     * <p><b>Value:</b> The Inventory instance they have open</p>
     */
    private Map<UUID, Inventory> openExtendedEnderChests = new ConcurrentHashMap<>();
    
    // ==================================================================================
    // INSTANCE VARIABLES - EXTERNAL INTEGRATIONS
    // ==================================================================================
    
    /**
     * Vault economy service provider for balance-related features.
     * 
     * <p><b>Initialized:</b> During {@link #onEnable()} if Vault plugin is present and registered</p>
     * <p><b>Value:</b> null if Vault is not installed or no economy plugin is registered</p>
     * 
     * <p><b>Used by:</b></p>
     * <ul>
     *   <li>Balance leaderboard signs - retrieving and ranking player balances</li>
     *   <li>PlaceholderAPI expansion - %pixelsessentials_formatted_balance% placeholder</li>
     *   <li>{@link #getTopBalances(int)} - querying all player balances for rankings</li>
     * </ul>
     * 
     * <p>The economy instance is obtained via Bukkit's RegisteredServiceProvider pattern.
     * A null check should always be performed before any economy operations since Vault
     * is an optional soft dependency.</p>
     */
    private Economy economy = null;
    
    // ==================================================================================
    // BALANCE LEADERBOARD SIGNS
    // ==================================================================================
    
    /**
     * Registry of balance leaderboard sign locations and their ranking positions.
     * 
     * <p><b>Key:</b> Block location of the sign (world + x,y,z coordinates)</p>
     * <p><b>Value:</b> Placement number (1 = richest player, 2 = second richest, etc.)</p>
     * 
     * <p><b>Persistence:</b> Saved to plugins/PixelsEssentials/signs.yml on shutdown and creation</p>
     * 
     * <p>Uses ConcurrentHashMap for thread-safety since sign updates run on a
     * scheduled async task timer that may interact with player commands concurrently.</p>
     * 
     * <p><b>Sign Lifecycle:</b></p>
     * <ol>
     *   <li>Created via /pe show &lt;place&gt; followed by right-click on a sign</li>
     *   <li>Updated periodically by {@link #startSignUpdateTask()} at configurable interval</li>
     *   <li>Automatically removed from tracking if sign block is destroyed/replaced</li>
     *   <li>Persisted to signs.yml on plugin disable and after each sign creation</li>
     * </ol>
     */
    private Map<Location, Integer> balanceSigns = new ConcurrentHashMap<>();
    
    /**
     * Pending balance sign creation requests awaiting player interaction.
     * 
     * <p><b>Key:</b> Player UUID who executed /pe show &lt;place&gt; command</p>
     * <p><b>Value:</b> Placement number (1-N) to assign to the next right-clicked sign</p>
     * 
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Admin runs /pe show 1 (or any placement number)</li>
     *   <li>Entry added: {admin-uuid} -> 1</li>
     *   <li>Admin receives prompt to right-click a sign</li>
     *   <li>Admin right-clicks any sign block</li>
     *   <li>{@link #onPlayerInteract} converts the sign to a balance leaderboard</li>
     *   <li>Entry removed from this map, sign added to {@link #balanceSigns}</li>
     * </ol>
     * 
     * <p>Entries are automatically cleaned up on player quit via {@link #onPlayerQuit}
     * to prevent stale pending requests affecting future sessions.</p>
     */
    private Map<UUID, Integer> pendingBalanceSigns = new ConcurrentHashMap<>();
    
    /**
     * Interval in seconds between automatic balance leaderboard sign updates.
     * 
     * <p><b>Config key:</b> sign-update-interval</p>
     * <p><b>Default:</b> 60 seconds</p>
     * <p><b>Reloaded:</b> On /pe reload command</p>
     * 
     * <p>Lower values provide more current balance data but increase server load due to
     * economy lookups for all players who have ever joined. The default of 60 seconds
     * balances data freshness with performance. Consider higher values (120-300) for
     * servers with many unique players.</p>
     * 
     * <p>Note: Manual updates can be forced at any time via /pe updatesigns command.</p>
     */
    private int signUpdateInterval = 60;
    
    // ==================================================================================
    // INSTANCE VARIABLES - CONFIGURATION OPTIONS
    // ==================================================================================
    
    /**
     * Flag controlling automatic recipe discovery for players on join.
     * 
     * <p><b>Config key:</b> unlock-recipes</p>
     * <p><b>Default:</b> false (disabled)</p>
     * <p><b>Reloaded:</b> On /pe reload command</p>
     * 
     * <p>When enabled, all registered recipes (vanilla and custom from other plugins)
     * are discovered for players when they join the server. This is useful for servers
     * that want players to have immediate full recipe book access without needing to
     * discover recipes through normal gameplay progression.</p>
     * 
     * <p><b>Note:</b> Only recipes implementing {@link org.bukkit.Keyed} can be discovered.
     * The count of newly unlocked recipes is logged in debug mode.</p>
     */
    private boolean unlockRecipesOnJoin = false;
    
    /**
     * Cached list of top player balances to minimize expensive economy lookups.
     * 
     * <p><b>Structure:</b> List of Map.Entry&lt;UUID, Double&gt; sorted by balance descending</p>
     * <p><b>Index:</b> 0 = richest player, 1 = second richest, etc.</p>
     * 
     * <p>This cache prevents repeated expensive economy lookups when multiple
     * balance signs need updating simultaneously. The cache is shared across all sign
     * updates that occur within the {@link #BALANCE_CACHE_VALIDITY_MS} window.</p>
     * 
     * <p><b>Invalidation:</b></p>
     * <ul>
     *   <li>Automatically after 5 seconds (time-based expiry)</li>
     *   <li>Manually via /pe updatesigns command (sets to null)</li>
     *   <li>On each periodic update cycle (ensures fresh data)</li>
     * </ul>
     * 
     * @see #topBalancesCacheTime
     * @see #BALANCE_CACHE_VALIDITY_MS
     * @see #getTopBalancesCached(int)
     */
    private List<Map.Entry<UUID, Double>> topBalancesCache = null;
    
    /**
     * Timestamp (System.currentTimeMillis) when the top balances cache was last populated.
     * 
     * <p>Used in conjunction with {@link #BALANCE_CACHE_VALIDITY_MS} to determine
     * if the cached balance data is still fresh enough to use. When the current time
     * minus this timestamp exceeds the validity period, the cache is considered stale
     * and will be refreshed on the next access.</p>
     * 
     * <p>Set to 0 when cache is explicitly invalidated via /pe updatesigns.</p>
     * 
     * @see #topBalancesCache
     * @see #getTopBalancesCached(int)
     */
    private long topBalancesCacheTime = 0;
    
    /**
     * Duration in milliseconds that cached balance data remains valid before refresh.
     * 
     * <p><b>Value:</b> 5000ms (5 seconds)</p>
     * 
     * <p>This short validity ensures balance sign data is reasonably current while
     * preventing redundant economy lookups when multiple signs update in quick succession
     * (which happens every {@link #signUpdateInterval} seconds when {@link #updateAllBalanceSigns()}
     * iterates through all registered signs).</p>
     * 
     * <p>The 5-second window is intentionally short because balance changes (trades, shop
     * purchases, job payments) can happen frequently on active servers.</p>
     */
    private static final long BALANCE_CACHE_VALIDITY_MS = 5000;
    
    // ==================================================================================
    // PLUGIN LIFECYCLE METHODS
    // ==================================================================================
    
    /**
     * Plugin initialization method called by Bukkit when the server enables this plugin.
     * 
     * <p><b>Initialization Sequence:</b></p>
     * <ol>
     *   <li>Create playerdata directory structure for per-player YAML files</li>
     *   <li>Save default config.yml from JAR resources if not present</li>
     *   <li>Register all command executors and tab completers for each command</li>
     *   <li>Register this class as event listener (implements Listener interface)</li>
     *   <li>Hook into Vault economy service if Vault plugin is present</li>
     *   <li>Register PlaceholderAPI expansion if PlaceholderAPI is present</li>
     *   <li>Load balance leaderboard signs from signs.yml persistent storage</li>
     *   <li>Start periodic sign update scheduled task (if economy available)</li>
     *   <li>Load configuration options (unlock-recipes, sign-update-interval)</li>
     *   <li>Output colored startup messages to console</li>
     * </ol>
     * 
     * <p><b>External Plugin Integration (all optional soft dependencies):</b></p>
     * <ul>
     *   <li><b>Vault:</b> Enables economy features - balance signs and formatted_balance placeholder</li>
     *   <li><b>PlaceholderAPI:</b> Enables custom placeholders for health, balance, and durability</li>
     *   <li><b>ItemsAdder:</b> Enables /giveenchanteditem command for custom items (checked at runtime)</li>
     * </ul>
     * 
     * <p><b>Commands Registered:</b> repair, home, sethome, delhome, homeinfo, pixelsessentials,
     * autofeed, back, giveenchanteditem</p>
     * 
     * <p><b>Events Handled:</b> PlayerTeleportEvent, PlayerDeathEvent, PlayerRespawnEvent,
     * FoodLevelChangeEvent, PlayerQuitEvent, PlayerInteractEvent, PlayerJoinEvent</p>
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
        
        // Initialize NamespacedKeys for bank notes
        bankNoteValueKey = new NamespacedKey(this, "banknote_value");
        bankNoteUniqueKey = new NamespacedKey(this, "banknote_unique");
        
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
        
        getCommand("giveenchanteditem").setExecutor(this);
        getCommand("giveenchanteditem").setTabCompleter(this);
        
        getCommand("withdraw").setExecutor(this);
        getCommand("withdraw").setTabCompleter(this);
        
        // Register event listener for location tracking
        // Handles PlayerTeleportEvent, PlayerDeathEvent, and PlayerQuitEvent
        getServer().getPluginManager().registerEvents(this, this);
        
        // Setup Vault economy integration
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] Vault economy hooked successfully!", NamedTextColor.GREEN));
            }
        }
        
        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PixelsEssentialsExpansion(this).register();
            getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PlaceholderAPI expansion registered!", NamedTextColor.GREEN));
        }
        
        // Load balance leaderboard signs
        loadBalanceSigns();
        
        // Start sign update task
        signUpdateInterval = getConfig().getInt("sign-update-interval", 60);
        if (economy != null) {
            startSignUpdateTask();
            getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] Balance leaderboard signs enabled (update interval: " + signUpdateInterval + "s)", NamedTextColor.GREEN));
        } else {
            getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] WARNING: Vault economy not found - balance leaderboard signs will not work!", NamedTextColor.YELLOW));
        }
        
        // Load unlock-recipes config option
        unlockRecipesOnJoin = getConfig().getBoolean("unlock-recipes", false);
        if (unlockRecipesOnJoin) {
            getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] Recipe unlock on join enabled", NamedTextColor.GREEN));
        }
        
        // Load lobby world name for keeppos respawning
        lobbyWorldName = getConfig().getString("lobby-world", "world");
        
        // Send startup messages to console with Adventure API colored text
        // Green for main message, light purple (magenta) for author credit
        // Uses Adventure API Component instead of legacy color codes
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Started!", NamedTextColor.GREEN));
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] By SupaFloof Games, LLC", NamedTextColor.LIGHT_PURPLE));
    }
    
    /**
     * Plugin shutdown method called by Bukkit when the server disables this plugin.
     * 
     * <p><b>Shutdown Sequence:</b></p>
     * <ol>
     *   <li>Iterate through all cached player data and save to disk files</li>
     *   <li>Clear player data cache to release memory</li>
     *   <li>Clear death locations map (any pending keeppos respawns are lost)</li>
     *   <li>Save balance leaderboard signs to signs.yml for persistence</li>
     *   <li>Clear balance sign maps (balanceSigns and pendingBalanceSigns)</li>
     *   <li>Output shutdown message to console in red color</li>
     * </ol>
     * 
     * <p>This method ensures no data loss by persisting all in-memory data
     * before the plugin is fully disabled. Any scheduled tasks (like sign updates)
     * are automatically cancelled by Bukkit when the plugin is disabled.</p>
     * 
     * <p><b>Note:</b> Players who die with keeppos and quit before respawning will
     * lose their pending death location choice since it is not persisted.</p>
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
        pendingDeathLocationChoices.clear();
        
        // Save balance leaderboard signs
        saveBalanceSigns();
        balanceSigns.clear();
        pendingBalanceSigns.clear();
        
        // Send shutdown message to console in red
        // Indicates the plugin has stopped cleanly
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] PixelsEssentials Shutting Down", NamedTextColor.RED));
    }
    
    // ==================================================================================
    // EVENT HANDLERS - LOCATION TRACKING AND PLAYER FEATURES
    // ==================================================================================
    
    /**
     * Handles player teleport events to track the player's pre-teleport location.
     * 
     * <p><b>Event Priority:</b> MONITOR - Runs after all other handlers, only if event not cancelled</p>
     * <p><b>Ignore Cancelled:</b> true - Does not process if another plugin cancelled the teleport</p>
     * 
     * <p><b>Purpose:</b> Saves the player's location BEFORE teleportation occurs, enabling
     * the /back command to return them to their previous location. This tracking is distinct
     * from death location tracking.</p>
     * 
     * <p><b>Filtering:</b> Minor movements (distance &lt; 1 block) are ignored to prevent
     * recording trivial position changes from other plugins, vehicle mounting, minecart
     * movement, or game mechanics that cause micro-teleports.</p>
     * 
     * <p><b>Data Flow:</b></p>
     * <ol>
     *   <li>Player initiates teleport (home command, spawn, plugin teleport, etc.)</li>
     *   <li>This handler captures the "from" location before the teleport completes</li>
     *   <li>Location saved to PlayerData.lastTeleportLocation via {@link #setLastTeleportLocation}</li>
     *   <li>PlayerData.lastWasDeath flag set to false (this was a teleport, not death)</li>
     *   <li>Player can now use /back to return to this saved location</li>
     * </ol>
     * 
     * @param event The PlayerTeleportEvent containing from/to locations and teleport cause
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Null check
        if (to == null) return;
        
        // Don't track if teleporting within the same block (minor movements)
        if (from.getWorld().equals(to.getWorld()) && from.distanceSquared(to) < 1.0) {
            return;
        }
        
        // Don't track teleports involving EliteMobs worlds
        // EliteMobs handles its own teleportation and these worlds are transient/instanced
        String fromWorld = from.getWorld().getName();
        String toWorld = to.getWorld().getName();
        if (fromWorld.startsWith("em_") || toWorld.startsWith("em_")) {
            if (debugMode) {
                getLogger().info("[DEBUG] Skipping /back tracking for EliteMobs world teleport: " 
                    + fromWorld + " -> " + toWorld);
            }
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
        
        // KeepPos - Store death location for GUI choice after respawn
        // Player will respawn at lobby, then get a GUI to choose whether to return
        if (player.hasPermission("pixelsessentials.keeppos")) {
            UUID deathUuid = player.getUniqueId();
            pendingDeathLocationChoices.put(deathUuid, deathLocation.clone());
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepPos: Stored death location for " + player.getName() + 
                    " at " + deathLocation.getWorld().getName() + 
                    " (" + String.format("%.1f, %.1f, %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()) + ")");
                getLogger().info("[DEBUG] KeepPos: Stored with UUID: " + deathUuid);
            }
        }
    }
    
    /**
     * Handles player respawn events for KeepPos feature.
     * 
     * <p>Forces respawn to lobby world spawn, then opens a GUI to let player
     * choose whether to return to death location or stay safe.</p>
     * 
     * @param event The PlayerRespawnEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Debug: Always log that respawn handler fired
        if (debugMode) {
            getLogger().info("[DEBUG] KeepPos: onPlayerRespawn fired for " + player.getName());
            getLogger().info("[DEBUG] KeepPos: Checking UUID: " + uuid);
            getLogger().info("[DEBUG] KeepPos: pendingDeathLocationChoices contains UUID? " + pendingDeathLocationChoices.containsKey(uuid));
            getLogger().info("[DEBUG] KeepPos: Map size: " + pendingDeathLocationChoices.size());
        }
        
        // Check if this player has a pending death location choice
        if (pendingDeathLocationChoices.containsKey(uuid)) {
            // FORCE respawn to lobby world spawn - this is the key fix
            World lobby = Bukkit.getWorld(lobbyWorldName);
            if (lobby != null) {
                event.setRespawnLocation(lobby.getSpawnLocation());
            } else {
                // Fallback to overworld spawn if lobby world not found
                World overworld = Bukkit.getWorlds().get(0);
                event.setRespawnLocation(overworld.getSpawnLocation());
                getLogger().warning("[KeepPos] Lobby world '" + lobbyWorldName + "' not found, using " + overworld.getName());
            }
            
            // Open the choice GUI after a short delay to let respawn complete
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && pendingDeathLocationChoices.containsKey(uuid)) {
                    openDeathLocationChoiceGUI(player);
                }
            }, 10L);
            
            if (debugMode) {
                getLogger().info("[DEBUG] KeepPos: Forcing respawn to lobby for " + player.getName());
            }
        }
    }
    
    /**
     * Opens the keeppos death location choice GUI for a player.
     * 
     * <p>Creates a 1-row (9-slot) inventory with green and red concrete options.</p>
     * 
     * @param player The player to show the GUI to
     */
    private void openDeathLocationChoiceGUI(Player player) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 9, 
            Component.text(KEEPPOS_GUI_TITLE));
        
        // Green concrete - Return to death location (slot 2)
        ItemStack returnItem = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta returnMeta = returnItem.getItemMeta();
        returnMeta.displayName(Component.text("Return to Death Location")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        returnMeta.lore(List.of(
            Component.text("Click to teleport back to").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("where you died").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        returnItem.setItemMeta(returnMeta);
        gui.setItem(2, returnItem);
        
        // Red concrete - Stay safe (slot 6)
        ItemStack stayItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta stayMeta = stayItem.getItemMeta();
        stayMeta.displayName(Component.text("Stay Safe")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        stayMeta.lore(List.of(
            Component.text("Click to remain at your").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("current safe location").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        stayItem.setItemMeta(stayMeta);
        gui.setItem(6, stayItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Handles inventory click events for the keeppos death location choice GUI.
     * 
     * @param event The InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Component title = event.getView().title();
        if (title == null) return;
        
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);
        if (!titleStr.equals(KEEPPOS_GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        Location deathLocation = pendingDeathLocationChoices.remove(uuid);
        
        if (clicked.getType() == Material.GREEN_CONCRETE && deathLocation != null) {
            player.closeInventory();
            player.teleport(deathLocation);
            player.sendMessage(Component.text("Returned to your death location.").color(NamedTextColor.GREEN));
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            player.closeInventory();
            player.sendMessage(Component.text("Staying at safe location.").color(NamedTextColor.YELLOW));
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
        
        // Clean up pending death location choice if player quit before choosing
        if (debugMode && pendingDeathLocationChoices.containsKey(uuid)) {
            getLogger().info("[DEBUG] KeepPos: Clearing pending death location for " + player.getName() + " on quit");
        }
        pendingDeathLocationChoices.remove(uuid);
        
        // Clean up any pending sign creation
        pendingBalanceSigns.remove(uuid);
        
        // Clean up extended ender chest tracking
        // Note: If player quits with inventory open, the InventoryCloseEvent
        // should fire first, but we clean up here as a safety measure
        Inventory openEnderChest = openExtendedEnderChests.remove(uuid);
        if (openEnderChest != null && debugMode) {
            getLogger().info("[DEBUG] ExtendedEnderChest: Cleaned up open inventory for " + player.getName() + " on quit");
        }
        
        // Clear extended ender chest from cache (it's already saved or will be)
        extendedEnderChestCache.remove(uuid);
    }
    
    /**
     * Handles player right-click on signs to create balance leaderboard signs.
     * 
     * <p>When a player has a pending sign creation (from /pe show command),
     * right-clicking a sign will convert it to a balance leaderboard sign.</p>
     * 
     * @param event The PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign)) return;
        
        Player player = event.getPlayer();
        Integer pendingPlace = pendingBalanceSigns.get(player.getUniqueId());
        
        if (pendingPlace != null) {
            Location loc = event.getClickedBlock().getLocation();
            balanceSigns.put(loc, pendingPlace);
            updateBalanceSign(loc, pendingPlace);
            pendingBalanceSigns.remove(player.getUniqueId());
            saveBalanceSigns();
            
            player.sendMessage(Component.text("Balance leaderboard sign #" + pendingPlace + " created successfully!", NamedTextColor.GREEN));
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles bank note redemption when player right-clicks with a bank note.
     * 
     * <p>Checks if the item in hand has bank note PDC data and redeems it,
     * depositing the value to the player's economy balance.</p>
     * 
     * @param event The PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBankNoteRedeem(PlayerInteractEvent event) {
        // Only handle right-clicks
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if holding an item
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Check if it's a bank note
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(bankNoteValueKey, PersistentDataType.DOUBLE)) {
            return;
        }
        
        // It's a bank note - redeem it
        event.setCancelled(true);
        redeemBankNote(player, item);
    }
    
    /**
     * Handles player right-click on ender chest blocks for extended ender chest feature.
     * 
     * <p>If the player has the pixelsessentials.enderchest.extended permission,
     * opens a custom 54-slot inventory instead of the vanilla 27-slot ender chest.
     * The first 27 slots mirror the vanilla ender chest, while the second 27 slots
     * are stored in a separate file for persistence.</p>
     * 
     * <p><b>Permission:</b> pixelsessentials.enderchest.extended</p>
     * 
     * @param event The PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        
        Player player = event.getPlayer();
        
        // Only intercept if player has extended permission
        if (!player.hasPermission("pixelsessentials.enderchest.extended")) {
            return;
        }
        
        // Check if player is sneaking with an item (placing block behavior)
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        
        // Cancel vanilla ender chest opening
        event.setCancelled(true);
        
        // Open our extended ender chest
        openExtendedEnderChest(player);
    }
    
    /**
     * Handles inventory close events for extended ender chest saving.
     * 
     * <p>When a player closes an extended ender chest, this handler:</p>
     * <ol>
     *   <li>Syncs the first 27 slots back to the vanilla ender chest</li>
     *   <li>Saves the second 27 slots to the extended storage file asynchronously</li>
     * </ol>
     * 
     * @param event The InventoryCloseEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if this is one of our extended ender chests
        Inventory openInv = openExtendedEnderChests.remove(uuid);
        if (openInv == null || openInv != event.getInventory()) {
            return;
        }
        
        if (debugMode) {
            getLogger().info("[DEBUG] ExtendedEnderChest: Closing for " + player.getName());
        }
        
        // Get the inventory contents
        ItemStack[] contents = event.getInventory().getContents();
        
        // Sync first 27 slots back to vanilla ender chest
        Inventory vanillaEnderChest = player.getEnderChest();
        for (int i = 0; i < 27; i++) {
            vanillaEnderChest.setItem(i, contents[i]);
        }
        
        // Extract extended slots (27-53)
        ItemStack[] extendedSlots = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            extendedSlots[i] = contents[i + 27];
        }
        
        // Update cache
        extendedEnderChestCache.put(uuid, extendedSlots);
        
        // Save asynchronously
        saveExtendedEnderChestAsync(uuid, extendedSlots);
    }
    
    /**
     * Handles inventory open events to intercept /ec commands from other plugins.
     * 
     * <p>When a player with pixelsessentials.enderchest.extended permission opens
     * a vanilla ender chest (via /ec or similar commands), this handler cancels
     * the vanilla inventory and opens our extended 54-slot version instead.</p>
     * 
     * <p>This complements {@link #onEnderChestInteract} which handles physical
     * ender chest block interactions.</p>
     * 
     * @param event The InventoryOpenEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        // Only handle ender chest inventories
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Only intercept if player has extended permission
        if (!player.hasPermission("pixelsessentials.enderchest.extended")) {
            return;
        }
        
        // Check if we already have an extended chest open for this player
        // (prevents recursion if something weird happens)
        if (openExtendedEnderChests.containsKey(player.getUniqueId())) {
            return;
        }
        
        if (debugMode) {
            getLogger().info("[DEBUG] ExtendedEnderChest: Intercepted vanilla ender chest open for " + player.getName() + " (likely /ec command)");
        }
        
        // Cancel the vanilla ender chest
        event.setCancelled(true);
        
        // Open our extended version on next tick to avoid issues with cancelled event
        Bukkit.getScheduler().runTask(this, () -> {
            openExtendedEnderChest(player);
        });
    }
    
    /**
     * Handles player join events to unlock all recipes if configured.
     * 
     * <p>When unlock-recipes is true in config.yml, all registered recipes
     * (vanilla and custom) are discovered for the player on join.</p>
     * 
     * @param event The PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!unlockRecipesOnJoin) return;
        
        Player player = event.getPlayer();
        int unlocked = 0;
        
        // Iterate through all registered recipes
        Iterator<org.bukkit.inventory.Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            org.bukkit.inventory.Recipe recipe = recipeIterator.next();
            
            // Only Keyed recipes can be discovered
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                if (player.discoverRecipe(key)) {
                    unlocked++;
                }
            }
        }
        
        if (debugMode && unlocked > 0) {
            getLogger().info("Unlocked " + unlocked + " new recipes for " + player.getName());
        }
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
            case "giveenchanteditem":
                return handleGiveEnchantedItemCommand(sender, args);
            case "withdraw":
                return handleWithdrawCommand(sender, args);
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
        
        // Block command in EliteMobs worlds - use EliteMobs transporter or /em to leave
        String currentWorld = player.getWorld().getName();
        if (currentWorld.startsWith("em_")) {
            player.sendMessage(Component.text("You cannot use /back in EliteMobs dungeons. Use the ", NamedTextColor.RED)
                .append(Component.text("Transporter NPC", NamedTextColor.GOLD))
                .append(Component.text(" or ", NamedTextColor.RED))
                .append(Component.text("/em quit", NamedTextColor.GOLD))
                .append(Component.text(" to leave.", NamedTextColor.RED)));
            return true;
        }
        
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
            if (sender.hasPermission("pixelsessentials.show")) {
                sender.sendMessage(Component.text("/pe show <place>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Create balance leaderboard sign", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/pe updatesigns", NamedTextColor.YELLOW)
                    .append(Component.text(" - Force update all balance signs", NamedTextColor.GRAY)));
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
            
            // Reload config values
            unlockRecipesOnJoin = getConfig().getBoolean("unlock-recipes", false);
            signUpdateInterval = getConfig().getInt("sign-update-interval", 60);
            lobbyWorldName = getConfig().getString("lobby-world", "world");
            
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
        
        if (args[0].equalsIgnoreCase("show")) {
            // Check permission
            if (!sender.hasPermission("pixelsessentials.show")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            // Must be a player
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return true;
            }
            
            // Check if Vault economy is available
            if (economy == null) {
                sender.sendMessage(Component.text("Vault economy is not available!", NamedTextColor.RED));
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /pe show <place>", NamedTextColor.RED));
                sender.sendMessage(Component.text("Example: /pe show 1 (creates 1st place sign)", NamedTextColor.GRAY));
                return true;
            }
            
            int place;
            try {
                place = Integer.parseInt(args[1]);
                if (place < 1) {
                    sender.sendMessage(Component.text("Place must be 1 or higher!", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid place number!", NamedTextColor.RED));
                return true;
            }
            
            Player player = (Player) sender;
            pendingBalanceSigns.put(player.getUniqueId(), place);
            
            sender.sendMessage(Component.text("Right-click a sign to make it a #" + place + " balance leaderboard sign!", NamedTextColor.GREEN));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("updatesigns")) {
            // Check permission
            if (!sender.hasPermission("pixelsessentials.show")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            
            // Force cache invalidation
            topBalancesCache = null;
            topBalancesCacheTime = 0;
            
            // Update all signs
            updateAllBalanceSigns();
            
            sender.sendMessage(Component.text("Updated " + balanceSigns.size() + " balance leaderboard signs.", NamedTextColor.GREEN));
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
    // EXTENDED ENDER CHEST METHODS
    // ==================================================================================
    
    /**
     * Opens the extended 54-slot ender chest for a player.
     * 
     * <p>Creates a custom inventory containing:</p>
     * <ul>
     *   <li>Slots 0-26: Contents from the player's vanilla ender chest</li>
     *   <li>Slots 27-53: Contents from the extended storage (lazy loaded)</li>
     * </ul>
     * 
     * @param player The player to open the ender chest for
     */
    private void openExtendedEnderChest(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (debugMode) {
            getLogger().info("[DEBUG] ExtendedEnderChest: Opening for " + player.getName());
        }
        
        // Create 54-slot inventory (double chest size)
        Inventory extendedChest = Bukkit.createInventory(player, 54, 
            Component.text(EXTENDED_ENDERCHEST_TITLE));
        
        // Copy vanilla ender chest contents to first 27 slots
        Inventory vanillaEnderChest = player.getEnderChest();
        for (int i = 0; i < 27; i++) {
            extendedChest.setItem(i, vanillaEnderChest.getItem(i));
        }
        
        // Load extended slots (lazy loading)
        ItemStack[] extendedSlots = loadExtendedEnderChest(uuid);
        for (int i = 0; i < 27; i++) {
            extendedChest.setItem(i + 27, extendedSlots[i]);
        }
        
        // Track this inventory so we can identify it on close
        openExtendedEnderChests.put(uuid, extendedChest);
        
        // Open the inventory
        player.openInventory(extendedChest);
        
        // Play ender chest open sound
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
    }
    
    /**
     * Loads extended ender chest contents for a player (lazy loading).
     * 
     * <p>First checks the in-memory cache. If not cached, loads from
     * the player's extended ender chest file: playerdata/{uuid}_enderchest.yml</p>
     * 
     * <p>The file stores contents as Base64-encoded serialized ItemStack array
     * for compact storage.</p>
     * 
     * @param uuid The player's UUID
     * @return ItemStack array of size 27 (may contain null elements for empty slots)
     */
    private ItemStack[] loadExtendedEnderChest(UUID uuid) {
        // Check cache first
        ItemStack[] cached = extendedEnderChestCache.get(uuid);
        if (cached != null) {
            if (debugMode) {
                getLogger().info("[DEBUG] ExtendedEnderChest: Loaded from cache for " + uuid);
            }
            return cached;
        }
        
        // Load from file
        File file = new File(playerDataFolder, uuid.toString() + "_enderchest.yml");
        if (!file.exists()) {
            if (debugMode) {
                getLogger().info("[DEBUG] ExtendedEnderChest: No file exists for " + uuid + ", returning empty");
            }
            ItemStack[] empty = new ItemStack[27];
            extendedEnderChestCache.put(uuid, empty);
            return empty;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String base64 = config.getString("contents");
            
            if (base64 == null || base64.isEmpty()) {
                if (debugMode) {
                    getLogger().info("[DEBUG] ExtendedEnderChest: Empty contents for " + uuid);
                }
                ItemStack[] empty = new ItemStack[27];
                extendedEnderChestCache.put(uuid, empty);
                return empty;
            }
            
            ItemStack[] contents = itemStackArrayFromBase64(base64);
            extendedEnderChestCache.put(uuid, contents);
            
            if (debugMode) {
                int itemCount = 0;
                for (ItemStack item : contents) {
                    if (item != null) itemCount++;
                }
                getLogger().info("[DEBUG] ExtendedEnderChest: Loaded " + itemCount + " items from file for " + uuid);
            }
            
            return contents;
            
        } catch (Exception e) {
            getLogger().severe("Failed to load extended ender chest for " + uuid + ": " + e.getMessage());
            ItemStack[] empty = new ItemStack[27];
            extendedEnderChestCache.put(uuid, empty);
            return empty;
        }
    }
    
    /**
     * Saves extended ender chest contents asynchronously.
     * 
     * <p>Serializes the ItemStack array to Base64 and writes to
     * playerdata/{uuid}_enderchest.yml on an async thread to avoid
     * blocking the main server thread.</p>
     * 
     * @param uuid The player's UUID
     * @param contents The extended slot contents (27 items)
     */
    private void saveExtendedEnderChestAsync(UUID uuid, ItemStack[] contents) {
        // Clone the contents array to avoid concurrent modification
        final ItemStack[] contentsCopy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            contentsCopy[i] = contents[i] != null ? contents[i].clone() : null;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String base64 = itemStackArrayToBase64(contentsCopy);
                
                File file = new File(playerDataFolder, uuid.toString() + "_enderchest.yml");
                YamlConfiguration config = new YamlConfiguration();
                config.set("contents", base64);
                config.save(file);
                
                if (debugMode) {
                    int itemCount = 0;
                    for (ItemStack item : contentsCopy) {
                        if (item != null) itemCount++;
                    }
                    getLogger().info("[DEBUG] ExtendedEnderChest: Saved " + itemCount + " items for " + uuid);
                }
                
            } catch (Exception e) {
                getLogger().severe("Failed to save extended ender chest for " + uuid + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Serializes an ItemStack array to a Base64 string.
     * 
     * <p>Uses BukkitObjectOutputStream for proper serialization of all
     * ItemStack data including enchantments, lore, PDC, and NBT.</p>
     * 
     * @param items The ItemStack array to serialize
     * @return Base64-encoded string representation
     * @throws IOException If serialization fails
     */
    private String itemStackArrayToBase64(ItemStack[] items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    /**
     * Deserializes an ItemStack array from a Base64 string.
     * 
     * <p>Uses BukkitObjectInputStream for proper deserialization of all
     * ItemStack data including enchantments, lore, PDC, and NBT.</p>
     * 
     * @param base64 The Base64-encoded string to deserialize
     * @return The deserialized ItemStack array
     * @throws IOException If deserialization fails
     */
    private ItemStack[] itemStackArrayFromBase64(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                try {
                    items[i] = (ItemStack) dataInput.readObject();
                } catch (ClassNotFoundException e) {
                    items[i] = null;
                }
            }
            return items;
        }
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
            case "giveenchanteditem":
                return tabCompleteGiveEnchantedItem(sender, args);
            case "withdraw":
                return tabCompleteWithdraw(sender, args);
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
    
    // ==================================================================================
    // GIVE ENCHANTED ITEM COMMAND
    // ==================================================================================
    
    /**
     * Handles the /giveenchanteditem (alias: /gei) command for giving ItemsAdder custom items.
     * 
     * <p><b>Requires:</b> ItemsAdder plugin to be installed and loaded</p>
     * <p><b>Permission:</b> pixelsessentials.giveenchanteditem</p>
     * 
     * <p><b>Usage:</b> /gei &lt;player&gt; &lt;ia_item&gt; [count] [name:Custom_Name] [lore:Custom_Lore] [durability:N] [enchant:level]...</p>
     * 
     * <p><b>Arguments (flexible order after item):</b></p>
     * <ul>
     *   <li><b>player:</b> Target player name (must be online)</li>
     *   <li><b>ia_item:</b> ItemsAdder item ID (e.g., "fairyset:fairy_helmet")</li>
     *   <li><b>count:</b> Number of items (1-64, default 1)</li>
     *   <li><b>name:Name:</b> Custom display name (underscores become spaces, supports &amp; color codes)</li>
     *   <li><b>lore:Lore:</b> Custom lore line (underscores become spaces, supports &amp; color codes)</li>
     *   <li><b>durability:N:</b> Set max durability (e.g., "durability:5000" for 5000 max durability)</li>
     *   <li><b>enchant:level:</b> Enchantment with level (e.g., "protection:30", "mending")</li>
     * </ul>
     * 
     * <p><b>Enchantment Notes:</b></p>
     * <ul>
     *   <li>Enchantments without a :level suffix default to level 1</li>
     *   <li>Uses addUnsafeEnchantment() to allow any level on any item</li>
     *   <li>Enchantment names use Minecraft namespace keys (e.g., "sharpness", "fire_protection")</li>
     * </ul>
     * 
     * <p><b>Example:</b> /gei Craig fairyset:fairy_helmet 1 name:&amp;6&amp;oAzathoth's_Helmet durability:5000 protection:30 mending aqua_affinity</p>
     * 
     * <p>If target inventory is full, items are dropped at the player's feet.</p>
     * 
     * @param sender The command sender (player or console)
     * @param args The command arguments
     * @return true always (command was handled)
     */
    private boolean handleGiveEnchantedItemCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("pixelsessentials.giveenchanteditem")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /gei <player> <ia_item> [count] [name:Name] [lore:Lore] [durability:N] [enchant:level]...", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /gei Craig fairyset:fairy_helmet 1 name:&6&oAzathoth's_Helmet durability:5000 protection:30 mending", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Note: Underscores become spaces in name/lore. Enchants without :level default to 1.", NamedTextColor.GRAY));
            return true;
        }
        
        // Get target player
        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }
        
        String itemId = args[1];
        
        // Get the ItemsAdder custom item
        CustomStack customStack = CustomStack.getInstance(itemId);
        if (customStack == null) {
            sender.sendMessage(Component.text("Unknown ItemsAdder item: " + itemId, NamedTextColor.RED));
            return true;
        }
        
        // Get the ItemStack from the custom item and CLONE it to break ItemsAdder reference
        ItemStack itemStack = customStack.getItemStack().clone();
        if (itemStack == null) {
            sender.sendMessage(Component.text("Failed to create item: " + itemId, NamedTextColor.RED));
            return true;
        }
        
        // Parse optional arguments
        int count = 1;
        String customName = null;
        String customLore = null;
        int customDurability = -1;  // -1 means not set
        List<String> appliedEnchants = new ArrayList<>();
        List<String> failedEnchants = new ArrayList<>();
        
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            
            // Check for name: prefix
            if (arg.toLowerCase().startsWith("name:")) {
                customName = arg.substring(5).replace('_', ' ');
                // Translate color codes
                customName = ChatColor.translateAlternateColorCodes('&', customName);
                continue;
            }
            
            // Check for lore: prefix
            if (arg.toLowerCase().startsWith("lore:")) {
                customLore = arg.substring(5).replace('_', ' ');
                // Translate color codes
                customLore = ChatColor.translateAlternateColorCodes('&', customLore);
                continue;
            }
            
            // Check for durability: prefix (sets max durability)
            if (arg.toLowerCase().startsWith("durability:")) {
                try {
                    customDurability = Integer.parseInt(arg.substring(11));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid durability value: " + arg.substring(11), NamedTextColor.RED));
                }
                continue;
            }
            
            // Check if it's a plain number (count)
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
            
            // Otherwise treat as enchantment
            String enchantName;
            int level;
            
            if (arg.contains(":")) {
                // Format: enchant:level
                String[] parts = arg.split(":");
                enchantName = parts[0].toLowerCase();
                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    failedEnchants.add(arg + " (invalid level)");
                    continue;
                }
            } else {
                // Format: enchant (default to level 1)
                enchantName = arg.toLowerCase();
                level = 1;
            }
            
            // Look up enchantment by name using Registry (1.21+)
            NamespacedKey key = NamespacedKey.minecraft(enchantName);
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            
            if (enchantment == null) {
                failedEnchants.add(arg + " (unknown enchantment)");
                continue;
            }
            
            // Apply enchantment (unsafe to allow any level)
            itemStack.addUnsafeEnchantment(enchantment, level);
            appliedEnchants.add(enchantName + ":" + level);
        }
        
        // Apply ALL meta modifications in one shot
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Apply custom name
            if (customName != null) {
                meta.setDisplayName(customName);
            }
            
            // Apply custom lore
            if (customLore != null) {
                List<String> loreList = new ArrayList<>();
                List<String> existingLore = meta.getLore();
                if (existingLore != null) {
                    loreList.addAll(existingLore);
                }
                loreList.add(customLore);
                meta.setLore(loreList);
            }
            
            // Set meta ONCE at the end
            itemStack.setItemMeta(meta);
        }
        
        // Apply custom max durability using Data Component API (Paper 1.21+)
        if (customDurability > 0) {
            itemStack.setData(io.papermc.paper.datacomponent.DataComponentTypes.MAX_DAMAGE, customDurability);
        }
        
        // Set count
        itemStack.setAmount(count);
        
        // Give the item to the target player
        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(itemStack);
        
        if (!leftover.isEmpty()) {
            // Inventory full, drop at player's feet
            for (ItemStack drop : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
            sender.sendMessage(Component.text(target.getName() + "'s inventory full - item dropped at their feet.", NamedTextColor.YELLOW));
        }
        
        // Send result message
        StringBuilder msg = new StringBuilder();
        msg.append("Given ").append(target.getName()).append(" ").append(count).append("x ").append(itemId);
        
        if (customName != null) {
            msg.append(" named \"").append(customName).append("\"");
        }
        
        if (customDurability > 0) {
            msg.append(" with ").append(customDurability).append(" max durability");
        }
        
        if (!appliedEnchants.isEmpty()) {
            msg.append(" with enchantments: ").append(String.join(", ", appliedEnchants));
        }
        
        sender.sendMessage(Component.text(msg.toString(), NamedTextColor.GREEN));
        
        if (!failedEnchants.isEmpty()) {
            sender.sendMessage(Component.text("Failed to apply: " + String.join(", ", failedEnchants), NamedTextColor.RED));
        }
        
        return true;
    }
    
    // ==================================================================================
    // WITHDRAW COMMAND HANDLERS
    // ==================================================================================
    
    /**
     * Handles the /withdraw command to create bank notes from player balance.
     * 
     * <p>Supports amount suffixes: K (thousands), M (millions), B (billions), T (trillions)</p>
     * <p>Examples: /withdraw 5000, /withdraw 5.2M, /withdraw 1.5B</p>
     * 
     * @param sender The command sender (must be a player)
     * @param args Command arguments - expects one argument: the amount
     * @return true if command was handled
     */
    private boolean handleWithdrawCommand(CommandSender sender, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("pixelsessentials.withdraw")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        // Check economy
        if (economy == null) {
            player.sendMessage(Component.text("Economy system is not available.", NamedTextColor.RED));
            return true;
        }
        
        // Check arguments
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /withdraw <amount>", NamedTextColor.RED));
            player.sendMessage(Component.text("Examples: /withdraw 5000, /withdraw 5.2M, /withdraw 1.5B", NamedTextColor.GRAY));
            return true;
        }
        
        // Parse amount with K/M/B/T suffixes
        double amount = parseWithdrawAmount(args[0]);
        if (amount < 0) {
            player.sendMessage(Component.text("Invalid amount: " + args[0], NamedTextColor.RED));
            player.sendMessage(Component.text("Examples: 5000, 5.2M, 1.5B, 2T", NamedTextColor.GRAY));
            return true;
        }
        
        // Load config limits
        double minAmount = getConfig().getDouble("withdraw.min", 10);
        double maxAmount = getConfig().getDouble("withdraw.max", 100000000000000.0);
        double feePercent = getConfig().getDouble("withdraw.fee-percent", 0.0);
        
        // Validate min/max
        if (amount < minAmount) {
            player.sendMessage(Component.text("Minimum withdrawal amount is $" + formatAmountWithCommas(minAmount), NamedTextColor.RED));
            return true;
        }
        
        if (amount > maxAmount) {
            player.sendMessage(Component.text("Maximum withdrawal amount is $" + formatAmountWithCommas(maxAmount), NamedTextColor.RED));
            return true;
        }
        
        // Calculate fee (charged on top)
        double fee = amount * (feePercent / 100.0);
        double totalDeduction = amount + fee;
        
        // Check player balance
        double balance = economy.getBalance(player);
        if (balance < totalDeduction) {
            if (fee > 0) {
                player.sendMessage(Component.text("Insufficient funds. You need $" + formatAmountWithCommas(totalDeduction) + 
                    " ($" + formatAmountWithCommas(amount) + " + $" + formatAmountWithCommas(fee) + " fee)", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("Insufficient funds. You need $" + formatAmountWithCommas(totalDeduction), NamedTextColor.RED));
            }
            return true;
        }
        
        // Deduct from player
        economy.withdrawPlayer(player, totalDeduction);
        
        // Create bank note
        ItemStack bankNote = createBankNote(amount, player.getName());
        
        // Give to player (drop if inventory full)
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(bankNote);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(Component.text("Inventory full - bank note dropped at your feet.", NamedTextColor.YELLOW));
        }
        
        // Success message
        if (fee > 0) {
            player.sendMessage(Component.text("Withdrew $" + formatAmountWithCommas(amount) + 
                " (fee: $" + formatAmountWithCommas(fee) + ")", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Withdrew $" + formatAmountWithCommas(amount), NamedTextColor.GREEN));
        }
        
        if (debugMode) {
            getLogger().info("[DEBUG] Withdraw: " + player.getName() + " withdrew $" + amount + " (fee: $" + fee + ")");
        }
        
        return true;
    }
    
    /**
     * Parses a withdraw amount string with optional K/M/B/T suffix.
     * 
     * <p>Supported formats:</p>
     * <ul>
     *   <li>Plain number: 5000 -> 5000</li>
     *   <li>K suffix: 5K or 5.2K -> 5000 or 5200</li>
     *   <li>M suffix: 5M or 5.2M -> 5000000 or 5200000</li>
     *   <li>B suffix: 5B or 5.2B -> 5000000000 or 5200000000</li>
     *   <li>T suffix: 5T or 1.9T -> 5000000000000 or 1900000000000</li>
     * </ul>
     * 
     * @param input The amount string to parse
     * @return The parsed amount, or -1 if invalid
     */
    private double parseWithdrawAmount(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        
        input = input.toUpperCase().trim();
        
        double multiplier = 1;
        String numPart = input;
        
        // Check for suffix
        if (input.endsWith("K")) {
            multiplier = 1_000;
            numPart = input.substring(0, input.length() - 1);
        } else if (input.endsWith("M")) {
            multiplier = 1_000_000;
            numPart = input.substring(0, input.length() - 1);
        } else if (input.endsWith("B")) {
            multiplier = 1_000_000_000;
            numPart = input.substring(0, input.length() - 1);
        } else if (input.endsWith("T")) {
            multiplier = 1_000_000_000_000L;
            numPart = input.substring(0, input.length() - 1);
        }
        
        try {
            double value = Double.parseDouble(numPart);
            if (value <= 0) {
                return -1;
            }
            return value * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Formats an amount with commas for display.
     * 
     * @param amount The amount to format
     * @return Formatted string with commas (e.g., "1,234,567")
     */
    private String formatAmountWithCommas(double amount) {
        // Use NumberFormat for locale-aware comma formatting
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(amount);
    }
    
    /**
     * Creates a bank note ItemStack with the specified value.
     * 
     * <p>The bank note has:</p>
     * <ul>
     *   <li>Custom display name from config</li>
     *   <li>Lore with amount (comma-formatted) and player name placeholders</li>
     *   <li>Optional glow effect (enchant + hide enchants flag)</li>
     *   <li>PDC data storing the value and unique ID (for unstackability)</li>
     * </ul>
     * 
     * @param amount The monetary value of the note
     * @param playerName The name of the player who created the note
     * @return The created bank note ItemStack
     */
    private ItemStack createBankNote(double amount, String playerName) {
        // Get material from config
        String materialName = getConfig().getString("bank-note.material", "PAPER");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            material = Material.PAPER;
        }
        
        ItemStack note = new ItemStack(material, 1);
        ItemMeta meta = note.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String name = getConfig().getString("bank-note.name", "&aBank Note");
            name = ChatColor.translateAlternateColorCodes('&', name);
            meta.setDisplayName(name);
            
            // Set lore with placeholders
            List<String> configLore = getConfig().getStringList("bank-note.lore");
            List<String> lore = new ArrayList<>();
            String formattedAmount = formatAmountWithCommas(amount);
            
            for (String line : configLore) {
                line = line.replace("%amount%", formattedAmount);
                line = line.replace("%player%", playerName);
                line = ChatColor.translateAlternateColorCodes('&', line);
                lore.add(line);
            }
            meta.setLore(lore);
            
            // Add glow effect if enabled
            if (getConfig().getBoolean("bank-note.glow", true)) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            // Store value in PDC
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(bankNoteValueKey, PersistentDataType.DOUBLE, amount);
            
            // Store unique ID to make notes unstackable
            pdc.set(bankNoteUniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            
            note.setItemMeta(meta);
        }
        
        return note;
    }
    
    /**
     * Handles bank note redemption when a player right-clicks with a bank note.
     * 
     * <p>Called from {@link #onPlayerInteract} when detecting a right-click
     * with an item that has bank note PDC data.</p>
     * 
     * @param player The player redeeming the note
     * @param item The bank note item being redeemed
     * @return true if the note was successfully redeemed
     */
    private boolean redeemBankNote(Player player, ItemStack item) {
        if (economy == null) {
            player.sendMessage(Component.text("Economy system is not available.", NamedTextColor.RED));
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(bankNoteValueKey, PersistentDataType.DOUBLE)) {
            return false;
        }
        
        double value = pdc.get(bankNoteValueKey, PersistentDataType.DOUBLE);
        
        // Deposit to player
        economy.depositPlayer(player, value);
        
        // Remove one note from stack
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        player.sendMessage(Component.text("Redeemed bank note for $" + formatAmountWithCommas(value), NamedTextColor.GREEN));
        
        if (debugMode) {
            getLogger().info("[DEBUG] BankNote: " + player.getName() + " redeemed $" + value);
        }
        
        return true;
    }
    
    /**
     * Tab completion for /withdraw command.
     * Suggests common amounts and suffix examples.
     */
    private List<String> tabCompleteWithdraw(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("pixelsessentials.withdraw")) {
            return completions;
        }
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            
            // Suggest common amounts
            String[] suggestions = {"100", "1000", "10000", "100K", "500K", "1M", "5M", "10M", "100M", "1B"};
            for (String suggestion : suggestions) {
                if (suggestion.toLowerCase().startsWith(partial)) {
                    completions.add(suggestion);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Tab completion for /giveenchanteditem command.
     * 
     * <p>Provides completion for player names, count, name:, lore:, and enchantment names.</p>
     */
    private List<String> tabCompleteGiveEnchantedItem(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("pixelsessentials.giveenchanteditem")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - player names
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
            
            // Suggest name:, lore:, and durability: prefixes
            if ("name:".startsWith(currentArg)) {
                completions.add("name:");
            }
            if ("lore:".startsWith(currentArg)) {
                completions.add("lore:");
            }
            if ("durability:".startsWith(currentArg)) {
                completions.add("durability:");
            }
            
            // Suggest common counts
            if (currentArg.isEmpty() || currentArg.matches("^\\d*$")) {
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
            }
            
            // Suggest common enchantment names
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
                        completions.add(enchant + ":");
                        completions.add(enchant); // Also suggest without colon (defaults to level 1)
                    }
                }
            } else {
                // User has typed enchant:, suggest levels
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
            if (sender.hasPermission("pixelsessentials.show")) {
                completions.add("show");
                completions.add("updatesigns");
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
        
        if (args.length == 2 && args[0].equalsIgnoreCase("show")) {
            if (sender.hasPermission("pixelsessentials.show")) {
                // Suggest common placement numbers
                completions.add("1");
                completions.add("2");
                completions.add("3");
                completions.add("4");
                completions.add("5");
                completions.add("10");
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
    // BALANCE LEADERBOARD SIGN METHODS
    // ==================================================================================
    
    /**
     * Loads balance leaderboard signs from signs.yml file.
     * 
     * <p>File location: plugins/PixelsEssentials/signs.yml</p>
     */
    private void loadBalanceSigns() {
        balanceSigns.clear();
        
        File file = new File(getDataFolder(), "signs.yml");
        if (!file.exists()) return;
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection signs = yaml.getConfigurationSection("signs");
        if (signs == null) return;
        
        for (String key : signs.getKeys(false)) {
            ConfigurationSection signSection = signs.getConfigurationSection(key);
            if (signSection != null) {
                String worldName = signSection.getString("world");
                int x = signSection.getInt("x");
                int y = signSection.getInt("y");
                int z = signSection.getInt("z");
                int place = signSection.getInt("place");
                
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    balanceSigns.put(loc, place);
                }
            }
        }
        
        getServer().getConsoleSender().sendMessage(Component.text("[PixelsEssentials] Loaded " + balanceSigns.size() + " balance leaderboard signs", NamedTextColor.GREEN));
    }
    
    /**
     * Saves balance leaderboard signs to signs.yml file.
     * 
     * <p>File location: plugins/PixelsEssentials/signs.yml</p>
     */
    private void saveBalanceSigns() {
        File file = new File(getDataFolder(), "signs.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        int index = 0;
        for (Map.Entry<Location, Integer> entry : balanceSigns.entrySet()) {
            Location loc = entry.getKey();
            int place = entry.getValue();
            String path = "signs." + index;
            
            yaml.set(path + ".world", loc.getWorld().getName());
            yaml.set(path + ".x", loc.getBlockX());
            yaml.set(path + ".y", loc.getBlockY());
            yaml.set(path + ".z", loc.getBlockZ());
            yaml.set(path + ".place", place);
            index++;
        }
        
        try {
            yaml.save(file);
        } catch (IOException e) {
            getLogger().severe("Failed to save balance leaderboard signs: " + e.getMessage());
        }
    }
    
    /**
     * Starts the repeating task to update all balance leaderboard signs.
     * 
     * <p>Signs are updated at the interval specified by signUpdateInterval (default 60 seconds).</p>
     */
    private void startSignUpdateTask() {
        // Run initial update after 1 second (give server time to fully load)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            updateAllBalanceSigns();
        }, 20L);
        
        // Then run periodic updates
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            updateAllBalanceSigns();
        }, 20L * signUpdateInterval, 20L * signUpdateInterval);
    }
    
    /**
     * Updates all balance leaderboard signs with current data.
     */
    private void updateAllBalanceSigns() {
        if (debugMode) {
            getLogger().info("Updating " + balanceSigns.size() + " balance leaderboard signs...");
        }
        
        // Invalidate cache to force fresh data
        topBalancesCache = null;
        topBalancesCacheTime = 0;
        
        // Create a copy to avoid ConcurrentModificationException if signs are removed
        for (Map.Entry<Location, Integer> entry : new HashMap<>(balanceSigns).entrySet()) {
            Location loc = entry.getKey();
            int place = entry.getValue();
            updateBalanceSign(loc, place);
        }
    }
    
    /**
     * Updates a specific balance leaderboard sign.
     * 
     * <p>Sign format:</p>
     * <pre>
     * Line 1: "BALANCE" (gold, bold)
     * Line 2: "#1" (yellow)
     * Line 3: "PlayerName" (green)
     * Line 4: "1.23 M" (aqua)
     * </pre>
     * 
     * @param location Sign location
     * @param place Placement number (1 = richest, etc.)
     */
    private void updateBalanceSign(Location location, int place) {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign)) {
            // Sign was destroyed, remove from tracking
            balanceSigns.remove(location);
            return;
        }
        
        Sign sign = (Sign) block.getState();
        
        // Get top balances (cached)
        List<Map.Entry<UUID, Double>> topBalances = getTopBalancesCached(Math.max(place, 10));
        
        if (topBalances.size() >= place) {
            Map.Entry<UUID, Double> playerEntry = topBalances.get(place - 1);
            UUID uuid = playerEntry.getKey();
            double balance = playerEntry.getValue();
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName() != null ? player.getName() : "Unknown";
            
            sign.getSide(Side.FRONT).line(0, LegacyComponentSerializer.legacyAmpersand().deserialize("&6&lBALANCE"));
            sign.getSide(Side.FRONT).line(1, LegacyComponentSerializer.legacyAmpersand().deserialize("&e#" + place));
            sign.getSide(Side.FRONT).line(2, LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + playerName));
            sign.getSide(Side.FRONT).line(3, LegacyComponentSerializer.legacyAmpersand().deserialize("&b$" + formatBalanceForSign(balance)));
        } else {
            // No player at this position yet
            sign.getSide(Side.FRONT).line(0, LegacyComponentSerializer.legacyAmpersand().deserialize("&6&lBALANCE"));
            sign.getSide(Side.FRONT).line(1, LegacyComponentSerializer.legacyAmpersand().deserialize("&e#" + place));
            sign.getSide(Side.FRONT).line(2, LegacyComponentSerializer.legacyAmpersand().deserialize("&7No Player"));
            sign.getSide(Side.FRONT).line(3, LegacyComponentSerializer.legacyAmpersand().deserialize("&7$0"));
        }
        
        sign.update();
    }
    
    /**
     * Gets top player balances with caching.
     * 
     * <p>Results are cached for 5 seconds to reduce economy lookups.</p>
     * 
     * @param limit Maximum number of players to return
     * @return List of player UUID and balance entries, sorted by balance descending
     */
    private List<Map.Entry<UUID, Double>> getTopBalancesCached(int limit) {
        long currentTime = System.currentTimeMillis();
        
        // Check if cache is still valid
        if (topBalancesCache != null && (currentTime - topBalancesCacheTime) < BALANCE_CACHE_VALIDITY_MS) {
            // Return cached results, but only up to the requested limit
            if (topBalancesCache.size() >= limit) {
                return topBalancesCache.subList(0, limit);
            }
            return topBalancesCache;
        }
        
        // Cache is invalid or doesn't exist, rebuild it
        topBalancesCache = getTopBalances(limit);
        topBalancesCacheTime = currentTime;
        
        return topBalancesCache;
    }
    
    /**
     * Gets top player balances from the economy.
     * 
     * <p>Iterates through all players who have ever joined the server and
     * retrieves their balance from Vault economy.</p>
     * 
     * @param limit Maximum number of players to return
     * @return List of player UUID and balance entries, sorted by balance descending
     */
    private List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        if (economy == null) {
            if (debugMode) {
                getLogger().warning("getTopBalances called but economy is null!");
            }
            return new ArrayList<>();
        }
        
        Map<UUID, Double> balances = new HashMap<>();
        
        // Get all offline players who have ever joined
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.hasPlayedBefore() || player.isOnline()) {
                double balance = economy.getBalance(player);
                if (balance > 0) {
                    balances.put(player.getUniqueId(), balance);
                }
            }
        }
        
        if (debugMode) {
            getLogger().info("Found " + balances.size() + " players with positive balances");
        }
        
        // Sort by balance descending and limit results
        return balances.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Formats a balance value for display on signs.
     * 
     * <p>Format rules:</p>
     * <ul>
     *   <li>Under 1 million: Integer with commas (e.g., "12,375")</li>
     *   <li>Millions: X.XX M (e.g., "35.45 M")</li>
     *   <li>Billions: X.XX B (e.g., "135.22 B")</li>
     *   <li>Trillions+: X.XX T (e.g., "1.36 T")</li>
     * </ul>
     * 
     * @param balance The balance to format
     * @return The formatted balance string
     */
    private String formatBalanceForSign(double balance) {
        if (balance < 0) {
            return "-" + formatBalanceForSign(-balance);
        }
        
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
    
    // ==================================================================================
    // INTERNAL DATA CLASSES
    // ==================================================================================
    
    /**
     * Internal data class storing all persistent data for a single player.
     * 
     * <p>This class serves as the in-memory representation of a player's YAML data file.
     * Instances are cached in {@link #playerDataCache} and persisted to disk via
     * {@link #savePlayerData(UUID)}.</p>
     * 
     * <p><b>Data Stored:</b></p>
     * <ul>
     *   <li><b>homes:</b> Map of home names to locations (lowercase keys)</li>
     *   <li><b>lastTeleportLocation:</b> Position before most recent teleport</li>
     *   <li><b>lastDeathLocation:</b> Position of most recent death</li>
     *   <li><b>lastWasDeath:</b> Flag indicating if /back should use death or teleport location</li>
     *   <li><b>logoutLocation:</b> Position when player quit (for analytics/spawn-on-join)</li>
     *   <li><b>autofeedEnabled:</b> Per-player toggle for automatic hunger restoration</li>
     * </ul>
     * 
     * <p><b>YAML File Mapping:</b></p>
     * <pre>
     * homes:              -> homes Map
     * lastteleportlocation: -> lastTeleportLocation
     * lastdeathlocation:    -> lastDeathLocation
     * last-was-death:       -> lastWasDeath
     * logoutlocation:       -> logoutLocation
     * autofeed:             -> autofeedEnabled
     * </pre>
     */
    private static class PlayerData {
        /**
         * Map of home name (lowercase) to location data.
         * Keys are always lowercase for case-insensitive matching.
         * Empty map by default, never null.
         */
        Map<String, LocationData> homes = new HashMap<>();
        
        /**
         * Location before the player's most recent teleport.
         * Set by {@link #onPlayerTeleport} for /back command functionality.
         * May be null if player hasn't teleported yet.
         */
        LocationData lastTeleportLocation;
        
        /**
         * Location where the player most recently died.
         * Set by {@link #onPlayerDeath} for /back command functionality.
         * May be null if player hasn't died yet.
         */
        LocationData lastDeathLocation;
        
        /**
         * Flag indicating whether the most recent /back-able event was a death.
         * 
         * <p>When true, /back will attempt to use lastDeathLocation (requires ondeath permission).
         * When false, /back will use lastTeleportLocation.</p>
         * 
         * <p>This flag is set to true in {@link #setLastDeathLocation} and
         * false in {@link #setLastTeleportLocation}.</p>
         */
        boolean lastWasDeath = false;
        
        /**
         * Location where the player was when they logged out.
         * Set by {@link #onPlayerQuit} for future features or analytics.
         * May be null if player data was created before first quit.
         */
        LocationData logoutLocation;
        
        /**
         * Whether automatic hunger restoration is enabled for this player.
         * 
         * <p>Default: true (enabled for new players)</p>
         * <p>Toggled via /autofeed on|off command</p>
         * <p>Requires pixelsessentials.autofeed permission to actually trigger</p>
         */
        boolean autofeedEnabled = true;
    }
    
    /**
     * Immutable data class storing a serializable location with world identification.
     * 
     * <p>This class contains all information needed to teleport a player to a saved location.
     * It stores both world UUID (primary, reliable across renames) and world name
     * (fallback, human-readable for display/debugging).</p>
     * 
     * <p><b>World Resolution:</b> When converting to a Bukkit Location via {@link #toLocation()},
     * the world is first looked up by UUID. If not found (world deleted/recreated), falls back
     * to lookup by name. Returns null if neither succeeds.</p>
     * 
     * <p><b>YAML Storage Format:</b></p>
     * <pre>
     * somepath:
     *   world: "550e8400-e29b-41d4-a716-446655440000"
     *   world-name: "world"
     *   x: 100.5
     *   y: 64.0
     *   z: -200.3
     *   yaw: 90.0
     *   pitch: 0.0
     * </pre>
     * 
     * <p><b>Immutability:</b> All fields are final and set at construction time.
     * This ensures thread-safety and prevents accidental modification of saved locations.</p>
     */
    private static class LocationData {
        /**
         * The UUID of the world as a string.
         * Primary identifier used for world resolution in {@link #toLocation()}.
         * More reliable than world name since UUIDs persist across world renames.
         */
        final String worldUuid;
        
        /**
         * The human-readable name of the world.
         * Used for display purposes and as fallback if UUID lookup fails.
         * Stored for debugging and when world is recreated with new UUID.
         */
        final String worldName;
        
        /**
         * X coordinate in the world (east-west position).
         * Stored as double for sub-block precision.
         */
        final double x;
        
        /**
         * Y coordinate in the world (vertical position/height).
         * Stored as double for sub-block precision.
         * Valid range typically 0-320 in modern Minecraft.
         */
        final double y;
        
        /**
         * Z coordinate in the world (north-south position).
         * Stored as double for sub-block precision.
         */
        final double z;
        
        /**
         * Yaw (horizontal rotation) in degrees.
         * 0 = south, 90 = west, 180 = north, 270 = east.
         * Range: -180 to 180 or 0 to 360 depending on source.
         */
        final float yaw;
        
        /**
         * Pitch (vertical rotation/look angle) in degrees.
         * 0 = horizontal, -90 = straight up, 90 = straight down.
         */
        final float pitch;
        
        /**
         * Constructs a new immutable LocationData instance.
         * 
         * @param worldUuid World UUID as string (from World.getUID().toString())
         * @param worldName World name (from World.getName())
         * @param x X coordinate (east-west)
         * @param y Y coordinate (height)
         * @param z Z coordinate (north-south)
         * @param yaw Horizontal rotation in degrees
         * @param pitch Vertical rotation in degrees
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
    
    // ==================================================================================
    // PLACEHOLDERAPI EXPANSION
    // ==================================================================================
    
    /**
     * PlaceholderAPI expansion for PixelsEssentials.
     * 
     * <p><b>Supported Placeholders:</b></p>
     * <ul>
     *   <li><b>%pixelsessentials_current_health%</b> - Player's current health with 1 decimal (e.g., 20.0, 15.5)</li>
     *   <li><b>%pixelsessentials_max_health%</b> - Player's max health with 1 decimal (e.g., 20.0, 40.0)</li>
     *   <li><b>%pixelsessentials_formatted_balance%</b> - Formatted economy balance:
     *       <ul>
     *         <li>Under 1M: Integer with commas (e.g., 12,375 or 993,113)</li>
     *         <li>Millions: X.XX M (e.g., 35.45 M)</li>
     *         <li>Billions: X.XX B (e.g., 135.22 B)</li>
     *         <li>Trillions+: X.XX T (e.g., 1.36 T)</li>
     *       </ul>
     *   </li>
     *   <li><b>%pixelsessentials_helmet_total_durability%</b> - Max durability of helmet (empty if no helmet)</li>
     *   <li><b>%pixelsessentials_helmet_current_durability%</b> - Current durability of helmet (empty if no helmet)</li>
     *   <li><b>%pixelsessentials_chestplate_total_durability%</b> - Max durability of chestplate (empty if none)</li>
     *   <li><b>%pixelsessentials_chestplate_current_durability%</b> - Current durability of chestplate (empty if none)</li>
     *   <li><b>%pixelsessentials_leggings_total_durability%</b> - Max durability of leggings (empty if none)</li>
     *   <li><b>%pixelsessentials_leggings_current_durability%</b> - Current durability of leggings (empty if none)</li>
     *   <li><b>%pixelsessentials_boots_total_durability%</b> - Max durability of boots (empty if none)</li>
     *   <li><b>%pixelsessentials_boots_current_durability%</b> - Current durability of boots (empty if none)</li>
     * </ul>
     */
    private class PixelsEssentialsExpansion extends PlaceholderExpansion {
        
        /** Reference to the main plugin instance */
        private final PixelsEssentials plugin;
        
        /**
         * Constructs a new expansion instance.
         * 
         * @param plugin The main plugin instance
         */
        public PixelsEssentialsExpansion(PixelsEssentials plugin) {
            this.plugin = plugin;
        }
        
        /**
         * Returns the identifier for this expansion.
         * Placeholders will use: %pixelsessentials_placeholder%
         * 
         * @return The expansion identifier
         */
        @Override
        public @NotNull String getIdentifier() {
            return "pixelsessentials";
        }
        
        /**
         * Returns the author of this expansion.
         * 
         * @return The author name
         */
        @Override
        public @NotNull String getAuthor() {
            return "SupaFloof Games, LLC";
        }
        
        /**
         * Returns the version of this expansion.
         * 
         * @return The version string
         */
        @Override
        public @NotNull String getVersion() {
            return plugin.getDescription().getVersion();
        }
        
        /**
         * Indicates this expansion should persist through PlaceholderAPI reloads.
         * 
         * @return true to persist
         */
        @Override
        public boolean persist() {
            return true;
        }
        
        /**
         * Processes placeholder requests.
         * 
         * @param player The player requesting the placeholder (may be null for non-player contexts)
         * @param params The placeholder identifier (everything after pixelsessentials_)
         * @return The placeholder value, or null if not recognized
         */
        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
            if (player == null) {
                return null;
            }
            
            switch (params.toLowerCase()) {
                case "current_health":
                    // Return current health with exactly 1 decimal point
                    return String.format("%.1f", player.getHealth());
                    
                case "max_health":
                    // Return max health with exactly 1 decimal point
                    // Uses getAttribute for accurate max health including modifiers
                    double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    return String.format("%.1f", maxHealth);
                    
                case "formatted_balance":
                    // Return formatted balance from Vault economy
                    if (plugin.economy == null) {
                        return "0";
                    }
                    double balance = plugin.economy.getBalance(player);
                    return formatBalance(balance);
                    
                case "helmet_total_durability":
                    return getArmorTotalDurability(player.getInventory().getHelmet());
                    
                case "helmet_current_durability":
                    return getArmorCurrentDurability(player.getInventory().getHelmet());
                    
                case "chestplate_total_durability":
                    return getArmorTotalDurability(player.getInventory().getChestplate());
                    
                case "chestplate_current_durability":
                    return getArmorCurrentDurability(player.getInventory().getChestplate());
                    
                case "leggings_total_durability":
                    return getArmorTotalDurability(player.getInventory().getLeggings());
                    
                case "leggings_current_durability":
                    return getArmorCurrentDurability(player.getInventory().getLeggings());
                    
                case "boots_total_durability":
                    return getArmorTotalDurability(player.getInventory().getBoots());
                    
                case "boots_current_durability":
                    return getArmorCurrentDurability(player.getInventory().getBoots());
                    
                default:
                    return null;
            }
        }
        
        /**
         * Formats a balance value according to the specified rules:
         * - Under 1 million: Integer with commas (e.g., 12,375)
         * - Millions: X.XX M (e.g., 35.45 M)
         * - Billions: X.XX B (e.g., 135.22 B)
         * - Trillions+: X.XX T (e.g., 1.36 T)
         * 
         * @param balance The balance to format
         * @return The formatted balance string
         */
        private String formatBalance(double balance) {
            if (balance < 0) {
                // Handle negative balances
                return "-" + formatBalance(-balance);
            }
            
            final double TRILLION = 1_000_000_000_000.0;
            final double BILLION = 1_000_000_000.0;
            final double MILLION = 1_000_000.0;
            
            if (balance >= TRILLION) {
                // Trillions: X.XX T
                return String.format("%.2f T", balance / TRILLION);
            } else if (balance >= BILLION) {
                // Billions: X.XX B
                return String.format("%.2f B", balance / BILLION);
            } else if (balance >= MILLION) {
                // Millions: X.XX M
                return String.format("%.2f M", balance / MILLION);
            } else {
                // Under 1 million: Integer with commas
                return String.format("%,d", (long) balance);
            }
        }
        
        /**
         * Gets the total (max) durability of an armor item.
         * 
         * @param item The armor item (may be null)
         * @return The max durability as a string, or empty string if not applicable
         */
        private String getArmorTotalDurability(ItemStack item) {
            if (item == null || item.getType().isAir()) {
                return "";
            }
            
            // Use Data Component API to get actual max damage (supports custom durability)
            Integer maxDamage = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.MAX_DAMAGE);
            if (maxDamage == null || maxDamage == 0) {
                return "";
            }
            
            return String.valueOf(maxDamage);
        }
        
        /**
         * Gets the current remaining durability of an armor item.
         * 
         * @param item The armor item (may be null)
         * @return The current durability as a string, or empty string if not applicable
         */
        private String getArmorCurrentDurability(ItemStack item) {
            if (item == null || item.getType().isAir()) {
                return "";
            }
            
            // Use Data Component API to get actual max damage (supports custom durability)
            Integer maxDamage = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.MAX_DAMAGE);
            if (maxDamage == null || maxDamage == 0) {
                return "";
            }
            
            // Get current damage from the item
            Integer currentDamage = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.DAMAGE);
            if (currentDamage == null) {
                currentDamage = 0;
            }
            
            int currentDurability = maxDamage - currentDamage;
            
            return String.valueOf(currentDurability);
        }
    }
}