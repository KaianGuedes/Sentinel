package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.utils.IPUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

    private final AuthManager authManager;
    private final PlayerStateManager playerStateManager;

    public RegisterCommand(AuthManager authManager, PlayerStateManager playerStateManager) {
        this.authManager = authManager;
        this.playerStateManager = playerStateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Comando apenas para jogadores.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Uso correto: /register <senha>", NamedTextColor.RED));
            return true;
        }

        String password = args[0];

        if (authManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Você já está registrado.", NamedTextColor.RED));
            return true;
        }

        authManager.register(player.getUniqueId(), password, IPUtils.getIP(player));
        Sentinel.getInstance().getLogger().info("\u001B[95mRegistro bem-sucedido para o jogador '"
                + player.getName() + "' no IP " + IPUtils.getIP(player) + "\u001B[0m");

        player.sendMessage(Component.text("Registrado com sucesso!", NamedTextColor.LIGHT_PURPLE));
        authManager.setLoggedIn(player.getUniqueId(), true);
        playerStateManager.restoreState(player);
        return true;
    }
}
