package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LogoutCommand implements CommandExecutor {

    private final AuthManager authManager;

    public LogoutCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.getComponent("server.only_players"));
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(Messages.getComponent(sender, "logout.usage"));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (!authManager.isRegistered(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.must_authenticate"));
            return true;
        }

        if (!authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.must_authenticate"));
            return true;
        }

        authManager.logout(uuid);

        player.kick();

        return true;
    }
}
