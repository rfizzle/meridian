package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.anvil.AnvilDispatcher;
import com.rfizzle.meridian.anvil.AnvilResult;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * T-4.1.1 — RETURN hook on {@code AnvilMenu#createResult} that lets
 * {@link AnvilDispatcher} swap in custom outputs for combinations vanilla doesn't handle
 * (prismatic web curse-strip, iron-block anvil repair, the tome families).
 *
 * <p>Injected at every RETURN opcode so the dispatcher fires even when vanilla early-exits
 * for unrecognized item pairings. {@code createResult()} contains multiple early returns
 * (empty input, non-enchantable item, no valid repair/combine match); using RETURN instead
 * of TAIL ensures our handlers are consulted on every code path. When no handler claims the
 * pairing the mixin is still behaviorally transparent — vanilla's result is left untouched.
 *
 * <p>T-5.2.3 extends the hook with an {@code onTake} tail that reinstates the handler's
 * {@link AnvilResult#leftReplacement()} into slot 0 after vanilla has cleared it. Most
 * handlers leave {@code leftReplacement} empty — the source item is consumed — but the
 * Extraction Tome returns a damaged, unenchanted copy of the source so the player recovers
 * the item. We stash the most recent claimed result on the mixin instance so the take-path
 * can read it without re-running the dispatcher (which might yield a different result if the
 * slots have already been mutated by vanilla's take logic).
 *
 * <p>The class extends {@link ItemCombinerMenu} to give the mixin lexical access to the
 * parent's {@code inputSlots} / {@code resultSlots} / {@code player} protected fields; the stub
 * constructor only satisfies the Java compiler's super-call requirement — Mixin does not merge
 * mixin constructors into the target class.
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu {

    /**
     * The most recent claim returned by the dispatcher, or {@code null} if no handler fired
     * on the current slot state. Stashed at {@code createResult} RETURN and consumed at
     * {@code onTake} TAIL. Cleared whenever the dispatcher returns empty so a stale
     * {@code leftReplacement} from a prior slot state cannot leak into a later take.
     */
    @Unique
    @Nullable
    private AnvilResult meridian$pendingResult;

    /**
     * Guard flag set during {@code onTake} to suppress re-entrant dispatch. Vanilla's
     * {@code onTake} clears input slots via {@code inputSlots.setItem}, which synchronously
     * triggers {@code slotsChanged} → {@code createResult} → our RETURN hook. Without this
     * guard, that re-entrant call would null out {@link #meridian$pendingResult}
     * before the {@code onTake} TAIL hook can consume the {@code leftReplacement}.
     */
    @Unique
    private boolean meridian$takingResult;

    private AnvilMenuMixin(
            @Nullable MenuType<?> type, int containerId, Inventory inv, ContainerLevelAccess access) {
        super(type, containerId, inv, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void meridian$dispatch(CallbackInfo ci) {
        if (this.meridian$takingResult) return;

        ItemStack left = this.inputSlots.getItem(0);

        if (EnchantmentEffects.getEnchantmentLevel(left, EnchantmentEffects.CURSE_OF_SEALING) > 0) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.meridian$pendingResult = null;
            return;
        }

        AnvilMenu self = (AnvilMenu) (Object) this;
        AnvilMenuAccessor accessor = (AnvilMenuAccessor) (Object) this;
        ItemStack right = this.inputSlots.getItem(1);
        Player player = this.player;
        int currentCost = accessor.meridian$getCost().get();

        Optional<AnvilResult> result =
                AnvilDispatcher.handle(self, left, right, player, currentCost);
        if (result.isEmpty()) {
            this.meridian$pendingResult = null;
            return;
        }
        AnvilResult r = result.get();
        this.meridian$pendingResult = r;

        this.resultSlots.setItem(0, r.output());
        accessor.meridian$getCost().set(r.xpCost());
        accessor.meridian$setRepairItemCountCost(r.rightConsumed());
        self.broadcastChanges();
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void meridian$beginTake(Player player, ItemStack stack, CallbackInfo ci) {
        this.meridian$takingResult = true;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void meridian$restoreLeft(Player player, ItemStack stack, CallbackInfo ci) {
        this.meridian$takingResult = false;
        AnvilResult pending = this.meridian$pendingResult;
        this.meridian$pendingResult = null;
        if (pending == null) return;
        ItemStack replacement = pending.leftReplacement();
        if (replacement == null || replacement.isEmpty()) return;
        this.inputSlots.setItem(0, replacement.copy());
    }
}
