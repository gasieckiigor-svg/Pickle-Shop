package pl.pickleshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PickleShop extends JavaPlugin implements Listener {

    private static final String SHOP_TITLE = "§2§lPickleShop";
    private static final String BANK_TITLE = "§5§lPickle Bank";

    private final Map<UUID, Long> bankBalances = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("sklep")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Ta komenda jest tylko dla graczy.");
                return true;
            }

            openShop(player, 0);
            return true;
        });

        Objects.requireNonNull(getCommand("bank")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Ta komenda jest tylko dla graczy.");
                return true;
            }

            openBank(player);
            return true;
        });

        Objects.requireNonNull(getCommand("saldo")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Ta komenda jest tylko dla graczy.");
                return true;
            }

            long balance = getBalance(player);

            player.sendMessage(
                    "§2§lPICKLE BANK §8» §fMasz §a"
                            + formatPickles(balance)
                            + " §fpickli."
            );

            return true;
        });

        getLogger().info("PickleShop zostal wlaczony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PickleShop zostal wylaczony!");
    }

    // =========================================================
    // SHOP
    // =========================================================

    private void openShop(Player player, int page) {

        ConfigurationSection items = getConfig().getConfigurationSection("items");

        if (items == null) {
            player.sendMessage("§cBrak itemow w config.yml!");
            return;
        }

        List<String> keys = new ArrayList<>(items.getKeys(false));

        int itemsPerPage = 45;
        int maxPages = Math.max(1, (int) Math.ceil(keys.size() / (double) itemsPerPage));

        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                SHOP_TITLE + " §8(" + (page + 1) + "/" + maxPages + ")"
        );

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, keys.size());

        for (int i = start; i < end; i++) {

            String key = keys.get(i);
            ConfigurationSection section = items.getConfigurationSection(key);

            if (section == null) continue;

            ItemStack item = createShopItem(section);

            if (item != null) {
                inventory.setItem(i - start, item);
            }
        }

        // Dolny pasek
        ItemStack info = createItem(
                Material.PAPER,
                "§e§lInformacje",
                List.of(
                        "§7Kliknij przedmiot, aby go kupić.",
                        "",
                        "§a1 shulker = 1728 pickli",
                        "§7Cena jest podana w pełnych shulkerach.",
                        "",
                        "§fTwój bank: §a" + formatPickles(getBalance(player)) + " pickli"
                )
        );

        inventory.setItem(49, info);

        if (page > 0) {
            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "§aPoprzednia strona",
                            List.of("§7Kliknij, aby wrócić.")
                    )
            );
        }

        if (page < maxPages - 1) {
            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "§aNastępna strona",
                            List.of("§7Kliknij, aby przejść dalej.")
                    )
            );
        }

        inventory.setItem(
                48,
                createItem(
                        Material.CHEST,
                        "§6§lPickle Bank",
                        List.of(
                                "§7Twoje saldo:",
                                "§a" + formatPickles(getBalance(player)) + " pickli",
                                "",
                                "§eKliknij, aby otworzyć bank."
                        )
                )
        );

        player.openInventory(inventory);
    }

    private ItemStack createShopItem(ConfigurationSection section) {

        String name = section.getString("name", "&fItem");
        String materialName = section.getString("material", "STONE");

        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            getLogger().warning("Nieznany material: " + materialName);
            return null;
        }

        int amount = section.getInt("amount", 1);
        long price = section.getLong("price", 1);

        ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(color(name));

            List<String> lore = new ArrayList<>();

            lore.add("§8━━━━━━━━━━━━━━━━");
            lore.add("§fIlość: §e" + amount);
            lore.add("§fCena: §a" + price + " shulkerów pickli");
            lore.add("§7(" + formatPickles(price * 1728L) + " pickli)");
            lore.add("");

            ConfigurationSection enchants =
                    section.getConfigurationSection("enchants");

            if (enchants != null && !enchants.getKeys(false).isEmpty()) {

                lore.add("§d§lEnchanty:");

                for (String enchant : enchants.getKeys(false)) {
                    int level = enchants.getInt(enchant);
                    lore.add(
                            "§7• §f" + prettyEnchant(enchant)
                                    + " " + level
                    );
                }

                lore.add("");
            }

            lore.add("§a§lKLIKNIJ, ABY KUPIĆ");
            lore.add("§8━━━━━━━━━━━━━━━━");

            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================================
    // BANK
    // =========================================================

    private void openBank(Player player) {

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                BANK_TITLE
        );

        long balance = getBalance(player);

        inventory.setItem(
                11,
                createItem(
                        Material.CHEST,
                        "§a§lTwoje saldo",
                        List.of(
                                "",
                                "§fPickle: §a" + formatPickles(balance),
                                "",
                                "§71 shulker = 1728 pickli"
                        )
                )
        );

        inventory.setItem(
                13,
                createItem(
                        Material.SEA_PICKLE,
                        "§2§lPickle Bank",
                        List.of(
                                "",
                                "§fSaldo:",
                                "§a" + formatPickles(balance) + " pickli",
                                "",
                                "§7W sklepie możesz płacić",
                                "§7picklami znajdującymi się",
                                "§7w ekwipunku oraz shulkerach."
                        )
                )
        );

        inventory.setItem(
                15,
                createItem(
                        Material.GOLD_INGOT,
                        "§e§lSaldo w shulkerach",
                        List.of(
                                "",
                                "§fPełne shulkery:",
                                "§a" + (balance / 1728),
                                "",
                                "§7Pozostałe pickles:",
                                "§a" + (balance % 1728)
                        )
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // CLICK
    // =========================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.startsWith(SHOP_TITLE)) {

            event.setCancelled(true);

            int slot = event.getRawSlot();

            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (slot == 45 || slot == 53) {

                int currentPage = getPage(title);

                if (slot == 45) {
                    openShop(player, currentPage - 1);
                } else {
                    openShop(player, currentPage + 1);
                }

                return;
            }

            if (slot == 48) {
                openBank(player);
                return;
            }

            if (slot == 49) {
                return;
            }

            if (slot >= 45) {
                return;
            }

            ConfigurationSection items =
                    getConfig().getConfigurationSection("items");

            if (items == null) return;

            List<String> keys = new ArrayList<>(items.getKeys(false));

            int currentPage = getPage(title);
            int index = currentPage * 45 + slot;

            if (index < 0 || index >= keys.size()) return;

            String key = keys.get(index);

            ConfigurationSection section =
                    items.getConfigurationSection(key);

            if (section == null) return;

            buyItem(player, section);

            return;
        }

        if (title.equals(BANK_TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        String title = event.getView().getTitle();

        if (title.startsWith(SHOP_TITLE) ||
                title.equals(BANK_TITLE)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // BUY
    // =========================================================

    private void buyItem(Player player, ConfigurationSection section) {

        String materialName = section.getString("material", "STONE");

        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            player.sendMessage("§cTen przedmiot jest nieprawidłowy.");
            return;
        }

        int amount = section.getInt("amount", 1);

        long priceShulkers = section.getLong("price", 1);

        long pricePickles = priceShulkers * 1728L;

        long balance = getBalance(player);

        if (balance < pricePickles) {

            player.sendMessage(
                    "§cNie masz wystarczającej liczby pickli!"
            );

            player.sendMessage(
                    "§7Potrzebujesz: §a"
                            + formatPickles(pricePickles)
                            + " pickli."
            );

            player.sendMessage(
                    "§7Masz: §a"
                            + formatPickles(balance)
                            + " pickli."
            );

            return;
        }

        ItemStack reward =
                createRewardItem(section, material, amount);

        if (!canFit(player, reward)) {

            player.sendMessage(
                    "§cNie masz wystarczająco miejsca w ekwipunku!"
            );

            return;
        }

        setBalance(player, balance - pricePickles);

        HashMap<Integer, ItemStack> leftovers =
                player.getInventory().addItem(reward);

        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    leftover
            );
        }

        String itemName =
                section.getString("name", material.name());

        player.sendMessage(
                "§2§lPICKLE SHOP §8» §aKupiono "
                        + color(itemName)
                        + " §7x"
                        + amount
        );

        player.sendMessage(
                "§7Zapłacono: §a"
                        + priceShulkers
                        + " shulkerów pickli."
        );
    }

    private ItemStack createRewardItem(
            ConfigurationSection section,
            Material material,
            int amount
    ) {

        ItemStack item =
                new ItemStack(
                        material,
                        Math.min(amount, material.getMaxStackSize())
                );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        String name =
                section.getString("name", "&fItem");

        meta.setDisplayName(color(name));

        ConfigurationSection enchants =
                section.getConfigurationSection("enchants");

        if (enchants != null) {

            for (String enchantName : enchants.getKeys(false)) {

                int level =
                        enchants.getInt(enchantName);

                Enchantment enchant =
                        findEnchantment(enchantName);

                if (enchant != null) {

                    try {
                        meta.addEnchant(
                                enchant,
                                level,
                                true
                        );
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        item.setItemMeta(meta);

        return item;
    }

    // =========================================================
    // BALANCE
    // =========================================================

    private long getBalance(Player player) {
        return bankBalances.getOrDefault(
                player.getUniqueId(),
                0L
        );
    }

    private void setBalance(Player player, long balance) {

        if (balance < 0) balance = 0;

        bankBalances.put(
                player.getUniqueId(),
                balance
        );
    }

    // =========================================================
    // UTILITY
    // =========================================================

    private boolean canFit(Player player, ItemStack item) {

        int remaining = item.getAmount();

        for (ItemStack slot :
                player.getInventory().getStorageContents()) {

            if (slot == null || slot.getType() == Material.AIR) {

                remaining -= item.getMaxStackSize();

            } else if (slot.isSimilar(item)) {

                remaining -=
                        slot.getMaxStackSize() - slot.getAmount();
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private int getPage(String title) {

        try {

            int open = title.lastIndexOf("(");
            int slash = title.lastIndexOf("/");

            if (open == -1 || slash == -1) return 0;

            return Integer.parseInt(
                    title.substring(open + 1, slash)
            ) - 1;

        } catch (Exception ignored) {
            return 0;
        }
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);
            meta.setLore(lore);

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

    private String formatPickles(long amount) {

        if (amount >= 1_000_000_000) {
            return String.format(
                    "%.2f mld",
                    amount / 1_000_000_000.0
            );
        }

        if (amount >= 1_000_000) {
            return String.format(
                    "%.2f mln",
                    amount / 1_000_000.0
            );
        }

        if (amount >= 1000) {
            return String.format(
                    "%.1f tys.",
                    amount / 1000.0
            );
        }

        return String.valueOf(amount);
    }

    private String prettyEnchant(String enchant) {

        String[] words =
                enchant.toLowerCase(Locale.ROOT)
                        .split("_");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {

            if (word.isEmpty()) continue;

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(
                        word.substring(1)
                );
            }

            result.append(" ");
        }

        return result.toString().trim();
    }

    private Enchantment findEnchantment(String name) {

        String normalized =
                name.toLowerCase(Locale.ROOT);

        for (Enchantment enchantment :
                Enchantment.values()) {

            String key =
                    enchantment.getKey()
                            .getKey()
                            .toLowerCase(Locale.ROOT);

            if (key.equals(normalized)) {
                return enchantment;
            }
        }

        return null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        bankBalances.putIfAbsent(
                event.getPlayer().getUniqueId(),
                0L
        );
    }
}
