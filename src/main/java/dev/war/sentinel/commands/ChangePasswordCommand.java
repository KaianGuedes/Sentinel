package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.IPUtils;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChangePasswordCommand implements CommandExecutor {

    private final AuthManager authManager;

    public ChangePasswordCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
        sender.sendMessage(Messages.getComponent("server.only_players"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Messages.getComponent(player, "changepassword.usage"));
            return true;
        }

        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());

        if (!authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "changepassword.not_logged_in"));
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        if (!authManager.validatePassword(uuid, oldPassword)) {
            player.sendMessage(Messages.getComponent("changepassword.wrong_old_password"));
            return true;
        }

        authManager.changePassword(uuid, newPassword);
        Sentinel.getInstance().getLogger().info(AnsiColor.LIGHT_PURPLE + Messages.get("server.password_changed")
                .replace("%player%", player.getName())
                .replace("%ip%", IPUtils.getIP(player)) + AnsiColor.RESET);

        player.sendMessage(Messages.getComponent("changepassword.success"));
        return true;
    }
}
