package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.IPUtils;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
            sender.sendMessage(Messages.getComponent("server.only_players"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Messages.getComponent(player, "login.usage"));
            return true;
        }

        String password = args[0];
        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());

        if (!authManager.isRegistered(uuid)) {
            player.sendMessage(Messages.getComponent(player, "login.not_registered"));
            return true;
        }

        if (authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "login.already_logged_in"));
            return true;
        }
        String playerName = player.getName();
        String ipAddress = IPUtils.getIP(player);

        boolean success = authManager.login(uuid, password, ipAddress);

        if (success) {
            Sentinel.getInstance().getLogger().info(AnsiColor.LIGHT_PURPLE + Messages.get("server.login_success")
                    .replace("%player%", playerName)
                    .replace("%ip%", IPUtils.getIP(player)) + AnsiColor.RESET);

            player.sendMessage(Messages.getComponent(player, "login.success"));

            authManager.setLoggedIn(uuid, true);
            playerStateManager.restoreState(player);
        } else {
            player.sendMessage(Messages.getComponent(player, "login.wrong_password"));

            Sentinel.getInstance().getLogger().info(AnsiColor.LIGHT_PURPLE + Messages.get("server.login_failed")
                    .replace("%player%", playerName)
                    .replace("%ip%", IPUtils.getIP(player)) + AnsiColor.RESET);
        }

        return true;
    }
}
