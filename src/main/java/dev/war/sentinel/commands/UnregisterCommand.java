package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnregisterCommand implements CommandExecutor {

    private final AuthManager authManager;

    public UnregisterCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Messages.getComponent(sender, "unregister.usage"));
            return true;
        }

        String target = args[0];
        UUID uuid = UUIDUtils.getCorrectUUID(target);

        if (!authManager.isRegistered(uuid)) {
            sender.sendMessage(Messages.getComponent("unregister.not_registered"));

            return true;
        }

        authManager.unregister(uuid);
        Component message = Messages.getComponent(sender, "unregister.success").replaceText(
                TextReplacementConfig.builder()
                        .matchLiteral("%player%")
                        .replacement(target)
                        .build()
        );

        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(Component.text("[Sentinel] ").append(message));
        }

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer != null) {
            targetPlayer.kick(Messages.getComponent(sender, "unregister.kicked"));
        }

        return true;
    }
}
