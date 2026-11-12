package dev.war.sentinel.commands;

import dev.war.sentinel.managers.AuthService;
import dev.war.sentinel.utils.CommandMatcher;
import org.bukkit.entity.Player;

public class AuthCommands {

    private final AuthService authService;

    public AuthCommands(AuthService authService) {
        this.authService = authService;
    }

    public void dispatch(CommandMatcher.Type type, Player player, String[] args) {
        switch (type) {
            case LOGIN -> authService.login(player, args);
            case REGISTER -> authService.register(player, args);
            case CHANGEPASSWORD -> authService.changePassword(player, args);
            default -> {
            }
        }
    }
}
