package com.example.clientgui.module;

import com.example.clientgui.setting.BoolSetting;
import com.example.clientgui.setting.NumSetting;
import com.example.clientgui.util.Util;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Right-click with a sword: throws a pearl where you look, then (optionally) a wind charge
 * at your pearl in flight, then swaps back to the sword.
 */
public class PearlCatch extends Module {
    private final BoolSetting wind = add(new BoolSetting("Wind charge follow-up", true));
    private final NumSetting windDelay = add(new NumSetting("Wind delay (ticks)", 2, 0, 10, 1));
    private final BoolSetting needSword = add(new BoolSetting("Require sword", true));
    private final BoolSetting airOnly = add(new BoolSetting("Only when aiming at air", false));
    private final BoolSetting sneakBypass = add(new BoolSetting("Sneak bypass", true));

    private boolean pressed;
    private int stage, wait, prev;

    public PearlCatch() { super("Pearl Catch", "Combat"); }

    @Override public void init() { UseItemCallback.EVENT.register(this::onUse); }

    @Override public void onDisable() { stage = 0; pressed = false; }

    private TypedActionResult<ItemStack> onUse(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!enabled || stage != 0 || !world.isClient() || hand != Hand.MAIN_HAND || !Util.ready()) return TypedActionResult.pass(stack);
        if (mc.currentScreen != null) return TypedActionResult.pass(stack);
        if (needSword.get() && !(stack.getItem() instanceof SwordItem)) return TypedActionResult.pass(stack);
        if (sneakBypass.get() && player.isSneaking()) return TypedActionResult.pass(stack);
        if (airOnly.get() && mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS) return TypedActionResult.pass(stack);
        if (Util.hotbar(Items.ENDER_PEARL, 1) < 0) { Util.msg("Pearl Catch: no ender pearl in hotbar"); return TypedActionResult.pass(stack); }
        pressed = true;
        return TypedActionResult.fail(stack);
    }

    @Override public void onTick() {
        if (stage == 0) {
            if (!pressed) return;
            pressed = false;
            int pearl = Util.hotbar(Items.ENDER_PEARL, 1);
            if (pearl < 0) return;
            prev = Util.selected();
            Util.select(pearl);
            Util.useItem(Hand.MAIN_HAND);
            if (wind.get() && Util.hotbar(Items.WIND_CHARGE, 1) >= 0) { stage = 1; wait = windDelay.i(); }
            else Util.select(prev);
            return;
        }
        if (wait > 0) { wait--; return; }
        int wc = Util.hotbar(Items.WIND_CHARGE, 1);
        EnderPearlEntity pearl = findPearl();
        if (pearl != null && wc >= 0) {
            Util.select(wc);
            Util.silent(pearl.getPos(), () -> Util.useItem(Hand.MAIN_HAND));
        }
        Util.select(prev);
        stage = 0;
    }

    private EnderPearlEntity findPearl() {
        EnderPearlEntity best = null;
        double bd = 400;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EnderPearlEntity pe && pe.getOwner() == mc.player) {
                double d = mc.player.squaredDistanceTo(pe);
                if (d < bd) { bd = d; best = pe; }
            }
        }
        return best;
    }
}
