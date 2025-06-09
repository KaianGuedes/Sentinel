package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnregisterCommand implements CommandExecutor {

    private final AuthManager authManager;

    public UnregisterCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Apenas operadores podem usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Uso correto: /unregister <player>", NamedTextColor.RED));
            return true;
        }

        String target = args[0];

        if (!authManager.isRegistered(Bukkit.getPlayerUniqueId(target))) {
            sender.sendMessage(Component.text("Este jogador não está registrado.", NamedTextColor.RED));
            return true;
        }

        authManager.unregister(Bukkit.getPlayerUniqueId(target));
        sender.sendMessage(Component.text("Conta de " + target + " removida com sucesso.", NamedTextColor.LIGHT_PURPLE));

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer != null) {
            targetPlayer.kick(Component.text("Sua conta foi desregistrada.", NamedTextColor.LIGHT_PURPLE));
        }

        return true;
    }
}
