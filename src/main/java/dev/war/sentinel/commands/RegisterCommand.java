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
            sender.sendMessage(Messages.getComponent("server.only_players"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Messages.getComponent(player, "register.usage"));
            return true;
        }

        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());

        String password = args[0];

        if (authManager.isRegistered(uuid)) {
            player.sendMessage(Messages.getComponent(player, "register.already_registered"));
            return true;
        }

        authManager.register(uuid, password, IPUtils.getIP(player));
        Sentinel.getInstance().getLogger().info(AnsiColor.LIGHT_PURPLE + Messages.get("server.register_success")
                .replace("%player%", player.getName())
                .replace("%ip%", IPUtils.getIP(player)) + AnsiColor.RESET);

        player.sendMessage(Messages.getComponent(player, "register.success"));
        authManager.setLoggedIn(uuid, true);
        playerStateManager.restoreState(player);
        return true;
    }
}
