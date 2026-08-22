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
        getLogger().info("PickleShop zostal wlaczony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PickleShop zostal wylaczony!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tej komendy moze uzyc tylko gracz!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("pickle")) {

            if (args.length == 0) {
                player.sendMessage(ChatColor.GREEN + "=== PickleShop ===");
                player.sendMessage(ChatColor.YELLOW + "/pickle balance - sprawdz saldo");
                player.sendMessage(ChatColor.YELLOW + "/pickle give <ilosc> - dodaj pickle");
                return true;
            }

            if (args[0].equalsIgnoreCase("balance")) {
                int balance = balances.getOrDefault(player.getUniqueId(), 0);

                player.sendMessage(
                        ChatColor.GREEN + "Masz " +
                        ChatColor.YELLOW + balance +
                        ChatColor.GREEN + " pickle!"
                );
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uzycie: /pickle give <ilosc>");
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[1]);

                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Ilosc musi byc wieksza od 0!");
                        return true;
                    }

                    UUID uuid = player.getUniqueId();

                    int oldBalance = balances.getOrDefault(uuid, 0);
                    int newBalance = oldBalance + amount;

                    balances.put(uuid, newBalance);

                    player.sendMessage(
                            ChatColor.GREEN + "Dodano " +
                            ChatColor.YELLOW + amount +
                            ChatColor.GREEN + " pickle!"
                    );

                    player.sendMessage(
                            ChatColor.GREEN + "Twoje saldo: " +
                            ChatColor.YELLOW + newBalance +
                            ChatColor.GREEN + " pickle."
                    );

                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Podaj prawidlowa liczbe!");
                }

                return true;
            }

            player.sendMessage(ChatColor.RED + "Nieznana opcja!");
            return true;
        }

        return false;
    }
}
