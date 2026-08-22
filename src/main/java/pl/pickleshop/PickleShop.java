package pl.pickleshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PickleShop extends JavaPlugin implements Listener {

    private final HashMap<UUID, Integer> balances = new HashMap<>();

    private static final String SHOP = "§2§lPICKLE §a§lSHOP";
    private static final String CATEGORY = "§8PickleShop » ";
    private static final String BUY_CONFIRM = "§8Kupowanie: ";

    private final HashMap<UUID, String> playerCategories = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadBalances();

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("PickleShop zostal wlaczony!");
    }

    @Override
    public void onDisable() {

        saveBalances();

        getLogger().info("PickleShop zostal wylaczony!");
    }

    // =========================================================
    // BANK
    // =========================================================

    private void loadBalances() {

        if (getConfig().getConfigurationSection("balances") == null) {
            return;
        }

        for (String uuidString :
                getConfig().getConfigurationSection("balances").getKeys(false)) {

            try {

                UUID uuid = UUID.fromString(uuidString);

                int balance =
                        getConfig().getInt("balances." + uuidString);

                balances.put(uuid, balance);

            } catch (Exception ignored) {
            }
        }
    }

    private void saveBalances() {

        getConfig().set("balances", null);

        for (UUID uuid : balances.keySet()) {

            getConfig().set(
                    "balances." + uuid,
                    balances.get(uuid)
            );
        }

        saveConfig();
    }

    private int getBalance(Player player) {

        return balances.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    private void setBalance(Player player, int amount) {

        balances.put(
                player.getUniqueId(),
                Math.max(0, amount)
        );
    }

    // =========================================================
    // PICKLE
    // =========================================================

    private int countPickles(Player player) {

        int amount = 0;

        for (ItemStack item :
                player.getInventory().getContents()) {

            if (item != null &&
                    item.getType() == Material.SEA_PICKLE) {

                amount += item.getAmount();
            }
        }

        return amount;
    }

    private boolean removePickles(Player player, int amount) {

        int remaining = amount;

        for (int slot = 0;
             slot < player.getInventory().getSize();
             slot++) {

            ItemStack item =
                    player.getInventory().getItem(slot);

            if (item == null ||
                    item.getType() != Material.SEA_PICKLE) {

                continue;
            }

            int remove =
                    Math.min(
                            item.getAmount(),
                            remaining
                    );

            item.setAmount(
                    item.getAmount() - remove
            );

            remaining -= remove;

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private void givePickles(Player player, int amount) {

        while (amount > 0) {

            int give = Math.min(amount, 64);

            player.getInventory().addItem(
                    new ItemStack(
                            Material.SEA_PICKLE,
                            give
                    )
            );

            amount -= give;
        }
    }

    // =========================================================
    // SKLEP
    // =========================================================

    private void openMainShop(Player player) {

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        SHOP
                );

        // Kategorie

        inv.setItem(
                10,
                menuItem(
                        Material.COBWEB,
                        "§f§lPvP",
                        "§7Kliknij, aby zobaczyc itemy PvP"
                )
        );

        inv.setItem(
                12,
                menuItem(
                        Material.GOLDEN_APPLE,
                        "§6§lJedzenie",
                        "§7Kliknij, aby zobaczyc jedzenie"
                )
        );

        inv.setItem(
                14,
                menuItem(
                        Material.DIAMOND,
                        "§b§lSurowce",
                        "§7Kliknij, aby zobaczyc surowce"
                )
        );

        inv.setItem(
                16,
                menuItem(
                        Material.ENDER_CHEST,
                        "§5§lUtility",
                        "§7Kliknij, aby zobaczyc utility"
                )
        );

        inv.setItem(
                28,
                menuItem(
                        Material.DIAMOND_SWORD,
                        "§c§lBronie",
                        "§7Kliknij, aby zobaczyc bronie"
                )
        );

        inv.setItem(
                30,
                menuItem(
                        Material.DIAMOND_PICKAXE,
                        "§a§lNarzedzia",
                        "§7Kliknij, aby zobaczyc narzedzia"
                )
        );

        inv.setItem(
                32,
                menuItem(
                        Material.NETHERITE_SWORD,
                        "§4§lENDGAME",
                        "§7Kliknij, aby zobaczyc itemy OP"
                )
        );

        inv.setItem(
                34,
                menuItem(
                        Material.ELYTRA,
                        "§d§lElytra",
                        "§7Kliknij, aby zobaczyc Elytry"
                )
        );

        inv.setItem(
                40,
                menuItem(
                        Material.SEA_PICKLE,
                        "§2§lTwoje Pickle",
                        "§7Saldo: §e" + getBalance(player)
                )
        );

        player.openInventory(inv);
    }

    private void openCategory(Player player, String category) {

        playerCategories.put(
                player.getUniqueId(),
                category
        );

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        CATEGORY + category
                );

        if (getConfig().getConfigurationSection("items") == null) {
            player.openInventory(inv);
            return;
        }

        int slot = 0;

        for (String key :
                getConfig()
                        .getConfigurationSection("items")
                        .getKeys(false)) {

            String path = "items." + key;

            String itemCategory =
                    getConfig().getString(
                            path + ".category",
                            ""
                    );

            if (!itemCategory.equalsIgnoreCase(category)) {
                continue;
            }

            if (slot >= 45) {
                break;
            }

            ItemStack item =
                    createShopItem(key);

            if (item != null) {

                inv.setItem(
                        slot,
                        item
                );

                slot++;
            }
        }

        inv.setItem(
                49,
                menuItem(
                        Material.ARROW,
                        "§cPowrot",
                        "§7Wroc do kategorii"
                )
        );

        player.openInventory(inv);
    }

    // =========================================================
    // TWORZENIE ITEMU Z CONFIG
    // =========================================================

    private ItemStack createShopItem(String key) {

        String path = "items." + key;

        String materialName =
                getConfig().getString(
                        path + ".material"
                );

        if (materialName == null) {
            return null;
        }

        Material material =
                Material.matchMaterial(materialName);

        if (material == null) {
            getLogger().warning(
                    "Nieprawidlowy material: " + materialName
            );
            return null;
        }

        int amount =
                getConfig().getInt(
                        path + ".amount",
                        1
                );

        int price =
                getConfig().getInt(
                        path + ".price",
                        0
                );

        String name =
                getConfig().getString(
                        path + ".name",
                        key
                );

        name = color(name);

        ItemStack item =
                new ItemStack(
                        material,
                        Math.min(amount, material.getMaxStackSize())
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);

        List<String> lore =
                new ArrayList<>();

        lore.add("");
        lore.add(
                "§7Ilosc: §f" + amount
        );

        lore.add(
                "§7Cena: §e" + price + " pickle"
        );

        lore.add("");
        lore.add(
                "§aKliknij, aby kupic!"
        );

        meta.setLore(lore);

        // Enchanty z configu

        if (getConfig().getConfigurationSection(
                path + ".enchants"
        ) != null) {

            for (String enchantName :
                    getConfig()
                            .getConfigurationSection(
                                    path + ".enchants"
                            )
                            .getKeys(false)) {

                int level =
                        getConfig().getInt(
                                path + ".enchants." + enchantName
                        );

                Enchantment enchant =
                        getEnchantment(enchantName);

                if (enchant != null) {

                    meta.addEnchant(
                            enchant,
                            level,
                            true
                    );
                }
            }
        }

        item.setItemMeta(meta);

        return item;
    }

    // =========================================================
    // ENCHANTY
    // =========================================================

    private Enchantment getEnchantment(String name) {

        String key = name.toLowerCase();

        switch (key) {

            case "sharpness":
                return Enchantment.SHARPNESS;

            case "looting":
                return Enchantment.LOOTING;

            case "sweeping_edge":
                return Enchantment.SWEEPING_EDGE;

            case "unbreaking":
                return Enchantment.UNBREAKING;

            case "mending":
                return Enchantment.MENDING;

            case "efficiency":
                return Enchantment.EFFICIENCY;

            case "fortune":
                return Enchantment.FORTUNE;

            case "protection":
                return Enchantment.PROTECTION;

            case "respiration":
                return Enchantment.RESPIRATION;

            case "aqua_affinity":
                return Enchantment.AQUA_AFFINITY;

            case "thorns":
                return Enchantment.THORNS;

            case "feather_falling":
                return Enchantment.FEATHER_FALLING;

            case "depth_strider":
                return Enchantment.DEPTH_STRIDER;

            case "soul_speed":
                return Enchantment.SOUL_SPEED;

            case "power":
                return Enchantment.POWER;

            case "punch":
                return Enchantment.PUNCH;

            case "flame":
                return Enchantment.FLAME;

            case "infinity":
                return Enchantment.INFINITY;

            case "impaling":
                return Enchantment.IMPALING;

            case "loyalty":
                return Enchantment.LOYALTY;

            case "channeling":
                return Enchantment.CHANNELING;

            case "fire_aspect":
                return Enchantment.FIRE_ASPECT;

            case "knockback":
                return Enchantment.KNOCKBACK;

            default:
                return null;
        }
    }

    // =========================================================
    // KLIKANIE
    // =========================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        String title =
                event.getView().getTitle();

        if (!title.equals(SHOP) &&
                !title.startsWith(CATEGORY)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked =
                event.getCurrentItem();

        if (clicked == null ||
                clicked.getType() == Material.AIR) {

            return;
        }

        // GLOWNA STRONA

        if (title.equals(SHOP)) {

            switch (clicked.getType()) {

                case COBWEB:
                    openCategory(player, "pvp");
                    break;

                case GOLDEN_APPLE:
                    openCategory(player, "food");
                    break;

                case DIAMOND:
                    openCategory(player, "resources");
                    break;

                case ENDER_CHEST:
                    openCategory(player, "utility");
                    break;

                case DIAMOND_SWORD:
                    openCategory(player, "weapons");
                    break;

                case DIAMOND_PICKAXE:
                    openCategory(player, "tools");
                    break;

                case NETHERITE_SWORD:
                    openCategory(player, "endgame");
                    break;

                case ELYTRA:
                    openCategory(player, "endgame");
                    break;

                default:
                    break;
            }

            return;
        }

        // KATEGORIA

        if (event.getSlot() == 49) {

            openMainShop(player);
            return;
        }

        String category =
                playerCategories.get(
                        player.getUniqueId()
                );

        if (category == null) {
            return;
        }

        String clickedName =
                ChatColor.stripColor(
                        clicked.getItemMeta() != null &&
                        clicked.getItemMeta().getDisplayName() != null
                                ? clicked.getItemMeta().getDisplayName()
                                : ""
                );

        String keyFound = null;

        for (String key :
                getConfig()
                        .getConfigurationSection("items")
                        .getKeys(false)) {

            String path =
                    "items." + key;

            String itemCategory =
                    getConfig().getString(
                            path + ".category",
                            ""
                    );

            if (!itemCategory.equalsIgnoreCase(category)) {
                continue;
            }

            String configName =
                    color(
                            getConfig().getString(
                                    path + ".name",
                                    key
                            )
                    );

            if (ChatColor.stripColor(configName)
                    .equals(clickedName)) {

                keyFound = key;
                break;
            }
        }

        if (keyFound == null) {
            return;
        }

        buyItem(player, keyFound);
    }

    // =========================================================
    // KUPOWANIE
    // =========================================================

    private void buyItem(Player player, String key) {

        String path =
                "items." + key;

        int price =
                getConfig().getInt(
                        path + ".price"
                );

        int amount =
                getConfig().getInt(
                        path + ".amount",
                        1
                );

        int balance =
                getBalance(player);

        if (balance < price) {

            player.sendMessage(
                    color(
                            getConfig().getString(
                                    "messages.prefix",
                                    "&2&lPICKLE &a&lSHOP &8» "
                            )
                    )
                            + "§cNie masz wystarczajaco pickle!"
            );

            return;
        }

        Material material =
                Material.matchMaterial(
                        getConfig().getString(
                                path + ".material"
                        )
                );

        if (material == null) {
            return;
        }

        ItemStack item =
                new ItemStack(
                        material,
                        amount
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            String name =
                    color(
                            getConfig().getString(
                                    path + ".name",
                                    key
                            )
                    );

            meta.setDisplayName(name);

            if (getConfig().getConfigurationSection(
                    path + ".enchants"
            ) != null) {

                for (String enchantName :
                        getConfig()
                                .getConfigurationSection(
                                        path + ".enchants"
                                )
                                .getKeys(false)) {

                    Enchantment enchant =
                            getEnchantment(enchantName);

                    int level =
                            getConfig().getInt(
                                    path + ".enchants." + enchantName
                            );

                    if (enchant != null) {

                        meta.addEnchant(
                                enchant,
                                level,
                                true
                        );
                    }
                }
            }

            item.setItemMeta(meta);
        }

        setBalance(
                player,
                balance - price
        );

        HashMap<Integer, ItemStack> leftover =
                player.getInventory().addItem(item);

        if (!leftover.isEmpty()) {

            // Jesli ekwipunek jest pelny,
            // zwracamy pieniadze.

            setBalance(
                    player,
                    balance
            );

            player.sendMessage(
                    "§cMasz pelny ekwipunek!"
            );

            return;
        }

        saveBalances();

        player.sendMessage(
                color(
                        getConfig().getString(
                                "messages.prefix",
                                "&2&lPICKLE &a&lSHOP &8» "
                        )
                )
                        + "§aKupiono §f"
                        + amount
                        + "x "
                        + ChatColor.stripColor(
                                getConfig().getString(
                                        path + ".name",
                                        key
                                )
                        )
                        + " §aza §e"
                        + price
                        + " pickle!"
        );
    }

    // =========================================================
    // GUI
    // =========================================================

    private ItemStack menuItem(
            Material material,
            String name,
            String lore) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            meta.setLore(
                    Collections.singletonList(lore)
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    // =========================================================
    // KOMENDY
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Tej komendy moze uzyc tylko gracz!"
            );

            return true;
        }

        String cmd =
                command.getName().toLowerCase();

        // /SKLEP

        if (cmd.equals("sklep")) {

            openMainShop(player);

            return true;
        }

        // /SALDO

        if (cmd.equals("saldo")) {

            player.sendMessage(
                    color(
                            getConfig().getString(
                                    "messages.prefix",
                                    "&2&lPICKLE &a&lSHOP &8» "
                            )
                    )
                            + "§aTwoje saldo: §e"
                            + getBalance(player)
                            + " pickle"
            );

            return true;
        }

        // /BANK

        if (cmd.equals("bank")) {

            if (args.length == 0) {

                player.sendMessage(
                        "§2§lPICKLE BANK"
                );

                player.sendMessage(
                        "§e/bank wplac <ilosc>"
                );

                player.sendMessage(
                        "§e/bank wyplac <ilosc>"
                );

                player.sendMessage(
                        "§e/saldo"
                );

                return true;
            }

            if (args[0].equalsIgnoreCase("wplac")) {

                if (args.length < 2) {

                    player.sendMessage(
                            "§cUzycie: /bank wplac <ilosc>"
                    );

                    return true;
                }

                int amount;

                try {

                    amount =
                            Integer.parseInt(args[1]);

                } catch (NumberFormatException e) {

                    player.sendMessage(
                            "§cPodaj prawidlowa liczbe!"
                    );

                    return true;
                }

                if (amount <= 0) {

                    player.sendMessage(
                            "§cIlosc musi byc wieksza od 0!"
                    );

                    return true;
                }

                if (countPickles(player) < amount) {

                    player.sendMessage(
                            "§cNie masz tylu Sea Pickle!"
                    );

                    return true;
                }

                removePickles(
                        player,
                        amount
                );

                setBalance(
                        player,
                        getBalance(player) + amount
                );

                saveBalances();

                player.sendMessage(
                        "§aWplacono §e"
                                + amount
                                + " §apickle!"
                );

                return true;
            }

            if (args[0].equalsIgnoreCase("wyplac")) {

                if (args.length < 2) {

                    player.sendMessage(
                            "§cUzycie: /bank wyplac <ilosc>"
                    );

                    return true;
                }

                int amount;

                try {

                    amount =
                            Integer.parseInt(args[1]);

                } catch (NumberFormatException e) {

                    player.sendMessage(
                            "§cPodaj prawidlowa liczbe!"
                    );

                    return true;
                }

                if (amount <= 0) {
                    return true;
                }

                if (getBalance(player) < amount) {

                    player.sendMessage(
                            "§cNie masz tylu pickle w banku!"
                    );

                    return true;
                }

                setBalance(
                        player,
                        getBalance(player) - amount
                );

                givePickles(
                        player,
                        amount
                );

                saveBalances();

                player.sendMessage(
                        "§aWyplacono §e"
                                + amount
                                + " §apickle!"
                );

                return true;
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        if (event.getPlayer() instanceof Player player) {

            playerCategories.remove(
                    player.getUniqueId()
            );
        }
    }
}
