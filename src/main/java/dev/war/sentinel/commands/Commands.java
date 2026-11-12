package dev.war.sentinel.commands;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.regex.Pattern;

public class Commands {
    public static void register(Sentinel instance, AuthService authService, AuthCommands authCommands) {
        instance.getCommand("register").setExecutor(new RegisterCommand(authCommands));
        instance.getCommand("login").setExecutor(new LoginCommand(authCommands));
        instance.getCommand("logout").setExecutor(new LogoutCommand(authService));
        instance.getCommand("unregister").setExecutor(new UnregisterCommand(authService));
        instance.getCommand("changepassword").setExecutor(new ChangePasswordCommand(authCommands));
        instance.getCommand("sentinel").setExecutor(new SentinelCommand(instance));
    }

    public static Filter installCommandLoggingFilter(Pattern pattern) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        Filter filter = new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                if (event == null || event.getMessage() == null) {
                    return Result.NEUTRAL;
                }
                String message = event.getMessage().getFormattedMessage();

                if (message != null && pattern.matcher(message).matches()) {
                    return Result.DENY;
                }

                return Result.NEUTRAL;
            }
        };

        LoggerConfig rootLoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootLoggerConfig.addFilter(filter);
        context.updateLoggers();
        return filter;
    }

    public static void removeCommandLoggingFilter(Filter filter) {
        if (filter == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        LoggerConfig rootLoggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootLoggerConfig.removeFilter(filter);
        context.updateLoggers();
    }
}
