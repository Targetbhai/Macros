package com.example.clientgui.module;

import com.example.clientgui.setting.BoolSetting;
import com.example.clientgui.setting.ModeSetting;
import com.example.clientgui.setting.NumSetting;
import com.example.clientgui.util.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;

import java.util.List;

/** Attacks whatever is in your crosshair. Swaps to a weapon for the hit, then swaps back. */
public class TriggerBot extends Module {
    private final NumSetting range = add(new NumSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final NumSetting minCooldown = add(new NumSetting("Min cooldown", 0.9, 0.0, 1.0, 0.05));
    private final ModeSetting swap = add(new ModeSetting("Swap to", List.of("Mace", "Sword", "Axe", "Held item"), 0));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile mobs", true));
    private final BoolSetting passive = add(new BoolSetting("Passive mobs", true));

    public TriggerBot() { super("Trigger Bot", "Combat"); }

    @Override public void onTick() {
        if (mc.currentScreen != null || Util.attackedTick == Util.now()) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
        Entity e = ehr.getEntity();
        if (!allowed(e)) return;
        if (ehr.getPos().distanceTo(mc.player.getEyePos()) > range.get()) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < minCooldown.get()) return;
        Util.swapHit(pick(), e);
    }

    private boolean allowed(Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof Monster) return hostile.get();
        return passive.get();
    }

    private int pick() {
        int mode = swap.get();
        if (mode == 3) return Util.selected();
        int mace = Util.hotbar(Items.MACE, 1);
        int sword = Util.hotbar(s -> s.getItem() instanceof SwordItem);
        int axe = Util.hotbar(s -> s.getItem() instanceof AxeItem);
        int[] order = switch (mode) {
            case 0 -> new int[]{mace, sword, axe};
            case 1 -> new int[]{sword, mace, axe};
            default -> new int[]{axe, sword, mace};
        };
        for (int s : order) if (s >= 0) return s;
        return Util.selected();
    }
}
