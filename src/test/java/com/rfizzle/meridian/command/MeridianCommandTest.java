package com.rfizzle.meridian.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeridianCommandTest {

    // ---- Dispatcher wiring ----

    @Test
    void rootLiteral_isStable() {
        assertEquals("meridian", MeridianCommand.ROOT);
    }

    @Test
    void register_addsRootLiteralToDispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MeridianCommand.register(dispatcher);

        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(MeridianCommand.ROOT);
        assertNotNull(node, "dispatcher should expose the meridian literal (visible to /help)");
        assertEquals(MeridianCommand.ROOT, node.getName());
    }

    @Test
    void register_addsReloadSubcommandUnderRoot() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MeridianCommand.register(dispatcher);

        CommandNode<CommandSourceStack> reloadNode = dispatcher.getRoot()
                .getChild(MeridianCommand.ROOT)
                .getChild("reload");
        assertNotNull(reloadNode, "reload subcommand should be registered under the root literal");
        assertNotNull(reloadNode.getCommand(), "reload should have an executor");
    }

    // ---- Permission predicate ----

    @Test
    void reload_requiresPerm2_rejectsPerm0() {
        Predicate<CommandSourceStack> requires = getRequirement("reload");

        assertFalse(requires.test(stubSource(0)), "perm 0 must not satisfy requires predicate");
        assertFalse(requires.test(stubSource(1)), "perm 1 must not satisfy requires predicate");
    }

    @Test
    void reload_requiresPerm2_acceptsPerm2AndAbove() {
        Predicate<CommandSourceStack> requires = getRequirement("reload");

        assertTrue(requires.test(stubSource(2)), "perm 2 should satisfy requires predicate");
        assertTrue(requires.test(stubSource(4)), "perm 4 should satisfy requires predicate");
    }

    // ---- Reload core ----

    @Test
    void reload_atPerm2_succeeds() {
        AtomicInteger reloaded = new AtomicInteger();
        AtomicReference<Boolean> sinkSuccess = new AtomicReference<>();
        AtomicReference<Supplier<Component>> sinkMessage = new AtomicReference<>();

        int result = MeridianCommand.runReload(
                reloaded::incrementAndGet,
                (ok, msg) -> {
                    sinkSuccess.set(ok);
                    sinkMessage.set(msg);
                });

        assertEquals(Command.SINGLE_SUCCESS, result, "successful reload should return SINGLE_SUCCESS");
        assertEquals(1, reloaded.get(), "reload action should be invoked exactly once");
        assertEquals(Boolean.TRUE, sinkSuccess.get(), "sink should report success");
        assertTranslationKey(sinkMessage.get().get(), MeridianCommand.RELOAD_OK_KEY);
    }

    @Test
    void reload_atPerm0_fails() {
        Predicate<CommandSourceStack> requires = getRequirement("reload");

        assertFalse(requires.test(stubSource(0)));
    }

    @Test
    void reload_failedReload_returnsZeroAndSendsFailure() {
        AtomicReference<Boolean> sinkSuccess = new AtomicReference<>();
        AtomicReference<Supplier<Component>> sinkMessage = new AtomicReference<>();

        int result = MeridianCommand.runReload(
                () -> { throw new RuntimeException("boom"); },
                (ok, msg) -> {
                    sinkSuccess.set(ok);
                    sinkMessage.set(msg);
                });

        assertEquals(0, result, "failed reload should return 0");
        assertEquals(Boolean.FALSE, sinkSuccess.get(), "sink should report failure");
        assertTranslationKey(sinkMessage.get().get(), MeridianCommand.RELOAD_ERROR_KEY);
    }

    // ---- helpers ----

    private static CommandNode<CommandSourceStack> rootOf(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(MeridianCommand.ROOT);
        assertNotNull(root);
        return root;
    }

    private static Predicate<CommandSourceStack> getRequirement(String subcommand) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        MeridianCommand.register(dispatcher);
        CommandNode<CommandSourceStack> node = rootOf(dispatcher).getChild(subcommand);
        assertNotNull(node, subcommand + " subcommand should be registered");
        return node.getRequirement();
    }

    private static CommandSourceStack stubSource(int permissionLevel) {
        return new CommandSourceStack(
                null, null, null, null,
                permissionLevel, "stub",
                Component.literal("stub"), null, null) {
            @Override
            public boolean hasPermission(int level) {
                return permissionLevel >= level;
            }
        };
    }

    private static void assertTranslationKey(Component component, String expectedKey) {
        assertTrue(component.getContents() instanceof TranslatableContents,
                "expected translatable component, got " + component.getContents().getClass());
        TranslatableContents contents = (TranslatableContents) component.getContents();
        assertEquals(expectedKey, contents.getKey());
    }
}
