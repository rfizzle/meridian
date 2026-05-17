package com.rfizzle.meridian.command;

import com.rfizzle.meridian.Meridian;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class MeridianCommand {
    public static final String ROOT = "meridian";

    static final String RELOAD_OK_KEY = "command.meridian.reload.ok";
    static final String RELOAD_ERROR_KEY = "command.meridian.reload.error";

    private MeridianCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(ROOT)
                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(MeridianCommand::runReload))
        );
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        return runReload(() -> Meridian.reloadConfig(src.getServer()), (success, message) -> {
            if (success) src.sendSuccess(message, true);
            else src.sendFailure(message.get());
        });
    }

    /**
     * Pure reload core — runs {@code reloader} and routes the translated
     * success/failure message through {@code sink}. Split out so tests can
     * drive it without a live {@link CommandSourceStack}: the command layer
     * only translates the outcome into {@code sendSuccess}/{@code sendFailure}.
     */
    static int runReload(Runnable reloader, MessageSink sink) {
        try {
            reloader.run();
            sink.send(true, () -> Component.translatable(RELOAD_OK_KEY));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Meridian.LOGGER.error("Config reload failed via command", e);
            sink.send(false, () -> Component.translatable(RELOAD_ERROR_KEY));
            return 0;
        }
    }

    @FunctionalInterface
    interface MessageSink {
        void send(boolean success, Supplier<Component> message);
    }
}
