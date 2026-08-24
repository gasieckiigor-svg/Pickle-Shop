package pl.pickleshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.*;

public class PickleShop extends JavaPlugin implements Listener {

    // =========================================================
    // DANE
    // =========================================================

    private final HashMap<UUID, Long> balances = new HashMap<>();

    private final HashMap<UUID, String> playerCategories = new HashMap<>();

    // Zapamiętuje, jaki item znajduje się w którym slocie sklepu
    private final HashMap<UUID, HashMap<Integer, String>> playerShopItems =
            new HashMap<>();

    private static final String SHOP = "§2§lPICKLE §a§lSHOP";
    private static final String CATEGORY = "§8PickleShop » ";

    // =========================================================
    // START / STOP
    // =========================================================

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
    // BANK - ZAPIS
    // =========================================================

    private void loadBalances() {

        if (getConfig().getConfigurationSection("balances") == null) {
            return;
        }

        for (String uuidString :
                getConfig()
                        .getConfigurationSection("balances")
                        .getKeys(false)) {

            try {

                UUID uuid = UUID.fromString(uuidString);

                long balance =
                        getConfig().getLong(
                                "balances." + uuidString,
                                0L
                        );

                balances.put(
                        uuid,
                        Math.max(0L, balance)
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void saveBalances() {

        getConfig().set("balances", null);

        for (Map.Entry<UUID, Long> entry :
                balances.entrySet()) {

            getConfig().set(
                    "balances." + entry.getKey(),
                    entry.getValue()
            );
        }

        saveConfig();
    }

    // =========================================================
    // SALDO
    // =========================================================

    private long getBalance(Player player) {

        return balances.getOrDefault(
                player.getUniqueId(),
                0L
        );
    }

    private void setBalance(Player player, long amount) {

        balances.put(
                player.getUniqueId(),
                Math.max(0L, amount)
        );
    }

    // =========================================================
    // PICKLE W EKWIPUNKU
    // =========================================================

    private long countPickles(Player player) {

        long amount = 0L;

        for (ItemStack item :
                player.getInventory().getContents()) {

            if (item != null &&
                    item.getType() == Material.SEA_PICKLE) {

                amount += item.getAmount();
            }
        }

        return amount;
    }

    private boolean removePickles(
            Player player,
            long amount) {

        if (amount <= 0) {
            return false;
        }

        if (countPickles(player) < amount) {
            return false;
        }

        long remaining = amount;

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
                    (int) Math.min(
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

    // =========================================================
    // DAWANIE PICKLE
    // =========================================================

    private void givePickles(
            Player player,
            long amount) {

        while (amount > 0) {

            int give =
                    (int) Math.min(
                            amount,
                            64
                    );

            HashMap<Integer, ItemStack> leftover =
                    player.getInventory().addItem(
                            new ItemStack(
                                    Material.SEA_PICKLE,
                                    give
                            )
                    );

            if (!leftover.isEmpty()) {

                for (ItemStack item :
                        leftover.values()) {

                    player.getWorld().dropItemNaturally(
                            player.getLocation(),
                            item
                    );
                }
            }

            amount -= give;
        }
    }

    // =========================================================
    // PICKLE TOP
    // =========================================================

    private void showPickleTop(Player player) {

        List<Map.Entry<UUID, Long>> top =
                new ArrayList<>(
                        balances.entrySet()
                );

        top.sort(
                (a, b) ->
                        Long.compare(
                                b.getValue(),
                                a.getValue()
                        )
        );

        player.sendMessage("");
        player.sendMessage(
                "§2§l🥒 PICKLE TOP §a§l🥒"
        );
        player.sendMessage(
                "§8-------------------------"
        );

        if (top.isEmpty()) {

            player.sendMessage(
                    "§7Brak graczy w rankingu."
            );

        } else {

            int position = 1;

            for (Map.Entry<UUID, Long> entry : top) {

                if (position > 10) {
                    break;
                }

                OfflinePlayer offlinePlayer =
                        Bukkit.getOfflinePlayer(
                                entry.getKey()
                        );

                String name =
                        offlinePlayer.getName();

                if (name == null) {
                    name = "Nieznany";
                }

                player.sendMessage(
                        "§e#" + position
                                + " §f" + name
                                + " §8» §a"
                                + entry.getValue()
                                + " §2pickle"
                );

                position++;
            }
        }

        player.sendMessage(
                "§8-------------------------"
        );

        player.sendMessage("");
    }

    // =========================================================
    // GLOWNE MENU
    // =========================================================

    private void openMainShop(Player player) {

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        SHOP
                );

        // PvP
        inv.setItem(
                10,
                menuItem(
                        Material.COBWEB,
                        "§f§lPvP",
                        "§7Kliknij, aby zobaczyc itemy PvP"
                )
        );

        // Jedzenie
        inv.setItem(
                12,
                menuItem(
                        Material.GOLDEN_APPLE,
                        "§6§lJedzenie",
                        "§7Kliknij, aby zobaczyc jedzenie"
                )
        );

        // Surowce
        inv.setItem(
                14,
                menuItem(
                        Material.DIAMOND,
                        "§b§lSurowce",
                        "§7Kliknij, aby zobaczyc surowce"
                )
        );

        // Utility
        inv.setItem(
                16,
                menuItem(
                        Material.ENDER_CHEST,
                        "§5§lUtility",
                        "§7Kliknij, aby zobaczyc utility"
                )
        );

        // Bronie
        inv.setItem(
                28,
                menuItem(
                        Material.DIAMOND_SWORD,
                        "§c§lBronie",
                        "§7Kliknij, aby zobaczyc bronie"
                )
        );

        // Narzedzia
        inv.setItem(
                30,
                menuItem(
                        Material.DIAMOND_PICKAXE,
                        "§a§lNarzedzia",
                        "§7Kliknij, aby zobaczyc narzedzia"
                )
        );

        // Endgame
        inv.setItem(
                32,
                menuItem(
                        Material.NETHERITE_SWORD,
                        "§4§lENDGAME",
                        "§7Kliknij, aby zobaczyc itemy OP"
                )
        );

        // Elytra
        inv.setItem(
                34,
                menuItem(
                        Material.ELYTRA,
                        "§d§lElytra",
                        "§7Kliknij, aby zobaczyc Elytry"
                )
        );

        // Saldo
        inv.setItem(
                40,
                menuItem(
                        Material.SEA_PICKLE,
                        "§2§lTwoje Pickle",
                        "§7Saldo: §e"
                                + getBalance(player)
        );

        player.openInventory(inv);
    }

    // =========================================================
    // KATEGORIA
    // =========================================================

    private void openCategory(
            Player player,
            String category) {

        playerCategories.put(
                player.getUniqueId(),
                category
        );

        HashMap<Integer, String> slotItems =
                new HashMap<>();

        playerShopItems.put(
                player.getUniqueId(),
                slotItems
        );

        Inventory inv =
                Bukkit.createInventory(
                        null,
                        54,
                        CATEGORY + category
                );

        if (getConfig().getConfigurationSection("items") == null) {

            inv.setItem(
                    49,
                    menuItem(
                            Material.ARROW,
                            "§cPowrot",
                            "§7Wroc do kategorii"
                    )
            );

            player.openInventory(inv);

            return;
        }

        int slot = 0;

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

                // Zapamiętujemy dokładny item
                slotItems.put(
                        slot,
                        key
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
    // TWORZENIE ITEMU
    // =========================================================

    private ItemStack createShopItem(
            String key) {

        String path =
                "items." + key;

        String materialName =
                getConfig().getString(
                        path + ".material"
                );

        if (materialName == null) {
            return null;
        }

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material == null) {

            getLogger().warning(
                    "Nieprawidlowy material: "
                            + materialName
                            + " dla itemu "
                            + key
            );

            return null;
        }

        int amount =
                getConfig().getInt(
                        path + ".amount",
                        1
                );

        long price =
                getConfig().getLong(
                        path + ".price",
                        0L
                );

        String name =
                getConfig().getString(
                        path + ".name",
                        key
                );

        name = color(name);

        if (amount <= 0) {
            amount = 1;
        }

        ItemStack item =
                new ItemStack(
                        material,
                        Math.min(
                                amount,
                                material.getMaxStackSize()
                        )
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
                "§7Cena: §e"
                        + price
                        + " pickle"
        );

        lore.add("");

        lore.add(
                "§aKliknij, aby kupic!"
        );

        meta.setLore(lore);

        // Potki
        applyPotionType(
                key,
                item,
                meta
        );

        // Enchanty
        applyEnchantments(
                path,
                meta
        );

        item.setItemMeta(meta);

        return item;
    }

    // =========================================================
    // POTKI
    // =========================================================

    private void applyPotionType(
            String key,
            ItemStack item,
            ItemMeta meta) {

        if (item.getType() != Material.POTION) {
            return;
        }

        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        PotionType potionType = null;

        switch (key.toLowerCase()) {

            case "potion_strength":
                potionType = PotionType.STRENGTH;
                break;

            case "potion_speed":
                potionType = PotionType.SWIFTNESS;
                break;

            case "potion_fire_resistance":
                potionType = PotionType.FIRE_RESISTANCE;
                break;

            case "potion_regeneration":
                potionType = PotionType.REGENERATION;
                break;

            case "potion_invisibility":
                potionType = PotionType.INVISIBILITY;
                break;

            case "potion_night_vision":
                potionType = PotionType.NIGHT_VISION;
                break;

            case "potion_water_breathing":
                potionType = PotionType.WATER_BREATHING;
                break;

            case "potion_leaping":
                potionType = PotionType.LEAPING;
                break;

            case "potion_turtle_master":
                potionType = PotionType.TURTLE_MASTER;
                break;

            default:
                break;
        }

        if (potionType != null) {

            potionMeta.setBasePotionType(
                    potionType
            );
        }
    }

    // =========================================================
    // ENCHANTY
    // =========================================================

    private void applyEnchantments(
            String path,
            ItemMeta meta) {

        if (getConfig().getConfigurationSection(
                path + ".enchants"
        ) == null) {

            return;
        }

        for (String enchantName :
                getConfig()
                        .getConfigurationSection(
                                path + ".enchants"
                        )
                        .getKeys(false)) {

            int level =
                    getConfig().getInt(
                            path
                                    + ".enchants."
                                    + enchantName
                    );

            if (level <= 0) {
                continue;
            }

            Enchantment enchant =
                    getEnchantment(
                            enchantName
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

    private Enchantment getEnchantment(
            String name) {

        String key =
                name.toLowerCase();

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
    public void onInventoryClick(
            InventoryClickEvent event) {

        String title =
                event.getView().getTitle();

        if (!title.equals(SHOP) &&
                !title.startsWith(CATEGORY)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        ItemStack clicked =
                event.getCurrentItem();

        if (clicked == null ||
                clicked.getType() == Material.AIR) {

            return;
        }

        // =====================================================
        // GLOWNE MENU
        // =====================================================

        if (title.equals(SHOP)) {

            switch (clicked.getType()) {

                case COBWEB:
                    openCategory(
                            player,
                            "pvp"
                    );
                    break;

                case GOLDEN_APPLE:
                    openCategory(
                            player,
                            "food"
                    );
                    break;

                case DIAMOND:
                    openCategory(
                            player,
                            "resources"
                    );
                    break;

                case ENDER_CHEST:
                    openCategory(
                            player,
                            "utility"
                    );
                    break;

                case DIAMOND_SWORD:
                    openCategory(
                            player,
                            "weapons"
                    );
                    break;

                case DIAMOND_PICKAXE:
                    openCategory(
                            player,
                            "tools"
                    );
                    break;

                case NETHERITE_SWORD:
                    openCategory(
                            player,
                            "endgame"
                    );
                    break;

                case ELYTRA:
                    openCategory(
                            player,
                            "elytra"
                    );
                    break;

                default:
                    break;
            }

            return;
        }

        // =====================================================
        // POWROT
        // =====================================================

        if (event.getSlot() == 49) {

            openMainShop(player);

            return;
        }

        // =====================================================
        // KUPNO ITEMU
        // =====================================================

        HashMap<Integer, String> slotItems =
                playerShopItems.get(
                        player.getUniqueId()
                );

        if (slotItems == null) {
            return;
        }

        String key =
                slotItems.get(
                        event.getSlot()
                );

        if (key == null) {
            return;
        }

        buyItem(
                player,
                key
        );
    }

    // =========================================================
    // KUPOWANIE
    // =========================================================

    private void buyItem(
            Player player,
            String key) {

        String path =
                "items." + key;

        long price =
                getConfig().getLong(
                        path + ".price",
                        0L
                );

        int amount =
                getConfig().getInt(
                        path + ".amount",
                        1
                );

        if (price < 0) {

            player.sendMessage(
                    "§cTen przedmiot ma nieprawidlowa cene."
            );

            return;
        }

        if (amount <= 0) {

            player.sendMessage(
                    "§cTen przedmiot ma nieprawidlowa ilosc."
            );

            return;
        }

        long balance =
                getBalance(player);

        if (balance < price) {

            player.sendMessage(
                    getPrefix()
                            + "§cNie masz wystarczajaco pickle!"
            );

            return;
        }

        String materialName =
                getConfig().getString(
                        path + ".material"
                );

        if (materialName == null) {
            return;
        }

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material == null) {

            player.sendMessage(
                    "§cNieprawidlowy material przedmiotu."
            );

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

            // Potka
            applyPotionType(
                    key,
                    item,
                    meta
            );

            // Enchanty
            applyEnchantments(
                    path,
                    meta
            );

            item.setItemMeta(meta);
        }

        // =====================================================
        // ZAPISUJEMY EKWIPUNEK PRZED TRANSAKCJA
        // =====================================================

        ItemStack[] oldContents =
                cloneContents(
                        player.getInventory().getContents()
                );

        HashMap<Integer, ItemStack> leftover =
                player.getInventory().addItem(item);

        // =====================================================
        // JEZELI NIE WSZYSTKO SIE DODALO
        // =====================================================

        if (!leftover.isEmpty()) {

            // Cofamy cala operacje
            player.getInventory().setContents(
                    oldContents
            );

            player.updateInventory();

            player.sendMessage(
                    "§cNie masz wystarczajaco miejsca w ekwipunku!"
            );

            return;
        }

        // =====================================================
        // ITEM DODANY - DOPIERO TERAZ POBIERAMY PIENIADZE
        // =====================================================

        setBalance(
                player,
                balance - price
        );

        saveBalances();

        String itemName =
                ChatColor.stripColor(
                        getConfig().getString(
                                path + ".name",
                                key
                        )
                );

        player.sendMessage(
                getPrefix()
                        + "§aKupiono §f"
                        + amount
                        + "x "
                        + itemName
                        + " §aza §e"
                        + price
                        + " pickle!"
        );
    }

    // =========================================================
    // BEZPIECZNA KOPIA EKWIPUNKU
    // =========================================================

    private ItemStack[] cloneContents(
            ItemStack[] contents) {

        ItemStack[] clone =
                new ItemStack[contents.length];

        for (int i = 0;
             i < contents.length;
             i++) {

            if (contents[i] != null) {

                clone[i] =
                        contents[i].clone();

            } else {

                clone[i] = null;
            }
        }

        return clone;
    }

    // =========================================================
    // PREFIX
    // =========================================================

    private String getPrefix() {

        return color(
                getConfig().getString(
                        "messages.prefix",
                        "&2&lPICKLE &a&lSHOP &8» "
                )
        );
    }

    // =========================================================
    // GUI ITEM
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
                    Collections.singletonList(
                            lore
                    )
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================================
    // KOLOR
    // =========================================================

    private String color(
            String text) {

        if (text == null) {
            return "";
        }

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

        // =====================================================
        // /PICKLETOP
        // =====================================================

        if (cmd.equals("pickletop")) {

            showPickleTop(player);

            return true;
        }

        // =====================================================
        // /SKLEP
        // =====================================================

        if (cmd.equals("sklep")) {

            openMainShop(player);

            return true;
        }

        // =====================================================
        // /SALDO
        // =====================================================

        if (cmd.equals("saldo")) {

            player.sendMessage(
                    getPrefix()
                            + "§aTwoje saldo: §e"
                            + getBalance(player)
                            + " pickle"
            );

            return true;
        }

        // =====================================================
        // /BANK
        // =====================================================

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

            // =================================================
            // WPLAC
            // =================================================

            if (args[0].equalsIgnoreCase("wplac")) {

                if (args.length < 2) {

                    player.sendMessage(
                            "§cUzycie: /bank wplac <ilosc>"
                    );

                    return true;
                }

                long amount;

                try {

                    amount =
                            Long.parseLong(
                                    args[1]
                            );

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

                if (!removePickles(
                        player,
                        amount
                )) {

                    player.sendMessage(
                            "§cNie udalo sie pobrac pickle z ekwipunku!"
                    );

                    return true;
                }

                long oldBalance =
                        getBalance(player);

                // Ochrona przed przepełnieniem
                if (amount >
                        Long.MAX_VALUE - oldBalance) {

                    // W razie ekstremalnego przypadku
                    // zwracamy pickle
                    givePickles(
                            player,
                            amount
                    );

                    player.sendMessage(
                            "§cSaldo jest zbyt duze!"
                    );

                    return true;
                }

                setBalance(
                        player,
                        oldBalance + amount
                );

                saveBalances();

                player.sendMessage(
                        "§aWplacono §e"
                                + amount
                                + " §apickle!"
                );

                return true;
            }

            // =================================================
            // WYPLAC
            // =================================================

            if (args[0].equalsIgnoreCase("wyplac")) {

                if (args.length < 2) {

                    player.sendMessage(
                            "§cUzycie: /bank wyplac <ilosc>"
                    );

                    return true;
                }

                long amount;

                try {

                    amount =
                            Long.parseLong(
                                    args[1]
                            );

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

                long balance =
                        getBalance(player);

                if (balance < amount) {

                    player.sendMessage(
                            "§cNie masz tylu pickle w banku!"
                    );

                    return true;
                }

                // =================================================
                // SPRAWDZAMY EKWIPUNEK PRZED ZMIANA SALDA
                // =================================================

                ItemStack[] oldContents =
                        cloneContents(
                                player.getInventory().getContents()
                        );

                givePickles(
                        player,
                        amount
                );

                // =================================================
                // Uwaga:
                // givePickles wyrzuca nadmiar na ziemie.
                // Jest to celowe - gracz nie traci pickle.
                // =================================================

                setBalance(
                        player,
                        balance - amount
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

    // =========================================================
    // ZAMYKANIE GUI
    // =========================================================

    @EventHandler
    public void onClose(
            InventoryCloseEvent event) {

        if (!(event.getPlayer()
                instanceof Player player)) {

            return;
        }

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    String title =
                            player.getOpenInventory()
                                    .getTitle();

                    if (!title.equals(SHOP) &&
                            !title.startsWith(CATEGORY)) {

                        playerCategories.remove(
                                player.getUniqueId()
                        );

                        playerShopItems.remove(
                                player.getUniqueId()
                        );
                    }
                }
        );
    }

    // =========================================================
    // WYJSCIE GRACZA
    // =========================================================

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event) {

        UUID uuid =
                event.getPlayer().getUniqueId();

        playerCategories.remove(uuid);
        playerShopItems.remove(uuid);
    }
}
