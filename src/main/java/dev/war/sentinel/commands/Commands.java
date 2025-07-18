package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.PlayerStateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

public class Commands {
    public static void register(Sentinel instance, AuthManager authManager, PlayerStateManager playerStateManager) {
        instance.getCommand("register").setExecutor(new RegisterCommand(authManager, playerStateManager));
        instance.getCommand("login").setExecutor(new LoginCommand(authManager, playerStateManager));
        instance.getCommand("unregister").setExecutor(new UnregisterCommand(authManager));
        instance.getCommand("changepassword").setExecutor(new ChangePasswordCommand(authManager));
        instance.getCommand("sentinel").setExecutor(new SentinelCommand(instance));
    }

    public static void setupCommandLoggingFilter() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        Filter filter = new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                String message = event.getMessage().getFormattedMessage();

                if (message.matches(".*issued server command: /(login|register|changepassword|l|r)(\\s.*|$)")) {
                    return Result.DENY;
                }

                return Result.NEUTRAL;
            }
        };

        LoggerConfig rootLoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootLoggerConfig.addFilter(filter);
        context.updateLoggers();
    }
}
