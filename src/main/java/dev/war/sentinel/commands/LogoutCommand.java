package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthService;
import dev.war.sentinel.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogoutCommand implements CommandExecutor {

    private final AuthService authService;

    public LogoutCommand(AuthService authService) {
        this.authService = authService;
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

        authService.logout(player);
        return true;
    }
}
