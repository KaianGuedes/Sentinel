package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.utils.IPUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {

    private final AuthManager authManager;

    public ChangePasswordCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Comando apenas para jogadores.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Uso correto: /changepassword <senha antiga> <senha nova>", NamedTextColor.RED));
            return true;
        }

        if (!authManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage(Component.text("Você precisa estar logado para mudar a senha.", NamedTextColor.RED));
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        if (!authManager.validatePassword(player.getUniqueId(), oldPassword)) {
            player.sendMessage(Component.text("Senha antiga incorreta.", NamedTextColor.RED));
            return true;
        }

        authManager.changePassword(player.getUniqueId(), newPassword);
        Sentinel.getInstance().getLogger().info("\u001B[95mSenha do jogador '"
                + player.getName() + "' foi alterada pelo IP " + IPUtils.getIP(player) + "\u001B[0m");

        player.sendMessage(Component.text("Senha alterada com sucesso!", NamedTextColor.LIGHT_PURPLE));
        return true;
    }
}
