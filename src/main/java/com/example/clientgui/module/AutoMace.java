package com.example.clientgui.module;

import com.example.clientgui.setting.BoolSetting;
import com.example.clientgui.setting.NumSetting;
import com.example.clientgui.util.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

/**
 * While falling, when an enemy is in your crosshair within range:
 * breach mace (short fall) / density mace (long fall) / axe stun first if they block with a shield.
 */
public class AutoMace extends Module {
    private final NumSetting range = add(new NumSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final NumSetting densityFall = add(new NumSetting("Density from (blocks)", 8, 2, 30, 1));
    private final NumSetting minFall = add(new NumSetting("Min fall (blocks)", 1.5, 0.5, 5, 0.1));
    private final BoolSetting stun = add(new BoolSetting("Stun slam (axe vs shield)", true));
    private final NumSetting stunDelay = add(new NumSetting("Stun delay (ticks)", 1, 0, 5, 1));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));

    private int cool, pendingTicks, pendingSlot;
    private Entity pendingTarget;

    public AutoMace() { super("Auto Mace", "Combat"); }

    @Override public void onDisable() { pendingTarget = null; }

    @Override public void onTick() {
        if (pendingTarget != null) {
            if (pendingTicks > 0) { pendingTicks--; return; }
            Entity t = pendingTarget; pendingTarget = null;
            if (t.isAlive() && mc.player.distanceTo(t) <= range.get() + 1.5) Util.swapHit(pendingSlot, t);
            return;
        }
        if (cool > 0) { cool--; return; }
        if (mc.currentScreen != null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
        Entity e = ehr.getEntity();
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
        if (playersOnly.get() && !(e instanceof PlayerEntity)) return;
        if (ehr.getPos().distanceTo(mc.player.getEyePos()) > range.get()) return;

        ClientPlayerEntity p = mc.player;
        if (p.isOnGround() || p.fallDistance < minFall.get() || p.getVelocity().y >= 0) return;
        if (p.isTouchingWater() || p.isClimbing() || p.isFallFlying()) return;

        boolean dense = p.fallDistance >= densityFall.get();
        int mace = pickMace(dense);
        if (mace < 0) return;

        if (stun.get() && le.isBlocking()) {
            int axe = Util.hotbar(s -> s.getItem() instanceof AxeItem);
            if (axe >= 0) {
                Util.swapHit(axe, e);
                pendingTarget = e; pendingSlot = mace; pendingTicks = stunDelay.i();
                cool = 10;
                return;
            }
        }
        Util.swapHit(mace, e);
        cool = 10;
    }

    private int pickMace(boolean dense) {
        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            var s = mc.player.getInventory().getStack(i);
            if (!s.isOf(Items.MACE)) continue;
            if (fallback < 0) fallback = i;
            int lvl = Util.enchant(s, dense ? Enchantments.DENSITY : Enchantments.BREACH);
            if (lvl > 0) return i;
        }
        return fallback;
    }
}
