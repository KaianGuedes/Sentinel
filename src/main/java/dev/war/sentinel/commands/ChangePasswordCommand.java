package dev.war.sentinel.commands;

import dev.war.sentinel.utils.CommandMatcher;
import dev.war.sentinel.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {

    private final AuthCommands authCommands;

    public ChangePasswordCommand(AuthCommands authCommands) {
        this.authCommands = authCommands;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.getComponent("server.only_players"));
            return true;
        }

        authCommands.dispatch(CommandMatcher.Type.CHANGEPASSWORD, player, args);
        return true;
    }
}
