package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SentinelCommand implements CommandExecutor {

    private final Sentinel plugin;

    public SentinelCommand(Sentinel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reloadlang")) {
            Messages.reloadServerLanguage(plugin);
            sender.sendMessage(Messages.getComponent("sentinel.language_reloaded"));
            return true;
        }

        sender.sendMessage(Messages.getComponent("sentinel.usage"));
        return true;
    }
}
