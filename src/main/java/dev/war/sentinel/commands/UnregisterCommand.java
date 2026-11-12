package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthService;
import dev.war.sentinel.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnregisterCommand implements CommandExecutor {

    private final AuthService authService;

    public UnregisterCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Messages.getComponent(sender, "unregister.usage"));
            return true;
        }

        authService.unregister(sender, args[0]);
        return true;
    }
}
