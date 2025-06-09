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

public class LoginCommand implements CommandExecutor {

    private final AuthManager authManager;
    private final PlayerStateManager playerStateManager;

    public LoginCommand(AuthManager authManager, PlayerStateManager playerStateManager) {
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
            player.sendMessage(Component.text("Uso correto: /login <senha>", NamedTextColor.RED));
            return true;
        }

        String password = args[0];

        if (!authManager.isRegistered(player.getUniqueId())) {
            player.sendMessage(Component.text("Você não está registrado.", NamedTextColor.RED));
            return true;
        }

        if (authManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage(Component.text("Você já está logado.", NamedTextColor.RED));
            return true;
        }
        String playerName = player.getName();
        String ipAddress = IPUtils.getIP(player);

        boolean success = authManager.login(player.getUniqueId(), password, ipAddress);

        if (success) {
            Sentinel.getInstance().getLogger().info("\u001B[95mLogin bem-sucedido para o jogador '"
                    + player.getName() + "' no IP " + IPUtils.getIP(player) + "\u001B[0m");

            player.sendMessage(Component.text("Login bem-sucedido!", NamedTextColor.LIGHT_PURPLE));

            authManager.setLoggedIn(player.getUniqueId(), true);
            playerStateManager.restoreState(player);
        } else {
            player.sendMessage(Component.text("Senha incorreta.", NamedTextColor.RED));


            Sentinel.getInstance().getLogger().warning("Tentativa de login falha para o jogador '" + playerName + "' no IP " + ipAddress);
        }

        return true;
    }
}
