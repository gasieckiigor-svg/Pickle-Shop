package pl.pickleshop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class PickleShop extends JavaPlugin {

    private final HashMap<UUID, Integer> balances = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBalances();

        getLogger().info("PickleShop zostal wlaczony!");
    }

    @Override
    public void onDisable() {
        saveBalances();

        getLogger().info("PickleShop zostal wylaczony!");
    }

    private void loadBalances() {
        if (getConfig().getConfigurationSection("balances") == null) {
            return;
        }

        for (String uuidString : getConfig().getConfigurationSection("balances").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                int balance = getConfig().getInt("balances." + uuidString);
                balances.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Nieprawidlowe UUID: " + uuidString);
            }
        }
    }

    private void saveBalances() {
        getConfig().set("balances", null);

        for (UUID uuid : balances.keySet()) {
            getConfig().set("balances." + uuid, balances.get(uuid));
        }

        saveConfig();
    }

    private int getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), 0);
    }

    private void setBalance(Player player, int amount) {
        balances.put(player.getUniqueId(), amount);
    }

    private int countPickles(Player player) {
        int amount = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SEA_PICKLE) {
                amount += item.getAmount();
            }
        }

        return amount;
    }

    private boolean removePickles(Player player, int amount) {
        int remaining = amount;

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);

            if (item == null || item.getType() != Material.SEA_PICKLE) {
                continue;
            }

            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);

            remaining -= take;

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private void givePickles(Player player, int amount) {
        int remaining = amount;

        while (remaining > 0) {
            int give = Math.min(remaining, 64);

            ItemStack pickles = new ItemStack(Material.SEA_PICKLE, give);
            player.getInventory().addItem(pickles);

            remaining -= give;
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tej komendy moze uzyc tylko gracz!");
            return true;
        }

        String commandName = command.getName().toLowerCase();

        // /saldo
        if (commandName.equals("saldo")) {

            int balance = getBalance(player);

            player.sendMessage(
                    ChatColor.GREEN + "Twoje saldo: "
                            + ChatColor.YELLOW + balance
                            + ChatColor.GREEN + " pickle."
            );

            return true;
        }

        // /bank
        if (commandName.equals("bank")) {

            if (args.length == 0) {
                player.sendMessage(ChatColor.GREEN + "=== Pickle Bank ===");
                player.sendMessage(ChatColor.YELLOW + "/bank wplac <ilosc>");
                player.sendMessage(ChatColor.YELLOW + "/bank wyplac <ilosc>");
                player.sendMessage(ChatColor.YELLOW + "/saldo");
                return true;
            }

            // /bank wplac
            if (args[0].equalsIgnoreCase("wplac")) {

                if (args.length < 2) {
                    player.sendMessage(
                            ChatColor.RED + "Uzycie: /bank wplac <ilosc>"
                    );
                    return true;
                }

                int amount;

                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(
                            ChatColor.RED + "Podaj prawidlowa liczbe!"
                    );
                    return true;
                }

                if (amount <= 0) {
                    player.sendMessage(
                            ChatColor.RED + "Ilosc musi byc wieksza od 0!"
                    );
                    return true;
                }

                int inventoryPickles = countPickles(player);

                if (inventoryPickles < amount) {
                    player.sendMessage(
                            ChatColor.RED + "Nie masz tylu Sea Pickle!"
                    );
                    player.sendMessage(
                            ChatColor.GRAY + "Masz w ekwipunku: "
                                    + ChatColor.YELLOW + inventoryPickles
                    );
                    return true;
                }

                if (!removePickles(player, amount)) {
                    player.sendMessage(
                            ChatColor.RED + "Nie udalo sie pobrac pickle z ekwipunku."
                    );
                    return true;
                }

                int newBalance = getBalance(player) + amount;
                setBalance(player, newBalance);
                saveBalances();

                player.sendMessage(
                        ChatColor.GREEN + "Wplaciles "
                                + ChatColor.YELLOW + amount
                                + ChatColor.GREEN + " pickle do banku!"
                );

                player.sendMessage(
                        ChatColor.GREEN + "Saldo: "
                                + ChatColor.YELLOW + newBalance
                                + ChatColor.GREEN + " pickle."
                );

                return true;
            }

            // /bank wyplac
            if (args[0].equalsIgnoreCase("wyplac")) {

                if (args.length < 2) {
                    player.sendMessage(
                            ChatColor.RED + "Uzycie: /bank wyplac <ilosc>"
                    );
                    return true;
                }

                int amount;

                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(
                            ChatColor.RED + "Podaj prawidlowa liczbe!"
                    );
                    return true;
                }

                if (amount <= 0) {
                    player.sendMessage(
                            ChatColor.RED + "Ilosc musi byc wieksza od 0!"
                    );
                    return true;
                }

                int balance = getBalance(player);

                if (balance < amount) {
                    player.sendMessage(
                            ChatColor.RED + "Nie masz tylu pickle w banku!"
                    );
                    return true;
                }

                setBalance(player, balance - amount);
                givePickles(player, amount);
                saveBalances();

                player.sendMessage(
                        ChatColor.GREEN + "Wyplaciles "
                                + ChatColor.YELLOW + amount
                                + ChatColor.GREEN + " pickle!"
                );

                player.sendMessage(
                        ChatColor.GREEN + "Saldo: "
                                + ChatColor.YELLOW + (balance - amount)
                                + ChatColor.GREEN + " pickle."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED + "Uzycie: /bank <wplac|wyplac> <ilosc>"
            );

            return true;
        }

        // /sklep
        if (commandName.equals("sklep")) {

            player.sendMessage(ChatColor.GREEN + "=== PickleShop ===");
            player.sendMessage(
                    ChatColor.YELLOW + "Sklep zostanie dodany w kolejnym kroku."
            );

            return true;
        }

        return false;
    }
}
