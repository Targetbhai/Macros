package com.example.clientgui.module;

import com.example.clientgui.setting.BoolSetting;
import com.example.clientgui.setting.NumSetting;
import com.example.clientgui.util.Util;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Right-click a block with a sword: anchor -> charge -> glowstone shield -> totem explode -> back to sword. */
public class AutoSafeAnchor extends Module {
    private enum Step { PLACE_ANCHOR, CHARGE, SHIELD, EXPLODE, RESTORE }

    private final NumSetting delay = add(new NumSetting("Step delay (ticks)", 1, 0, 5, 1));
    private final BoolSetting shield = add(new BoolSetting("Glowstone shield", true));
    private final BoolSetting needSword = add(new BoolSetting("Require sword", true));
    private final BoolSetting sneakBypass = add(new BoolSetting("Sneak bypass", true));

    private boolean running;
    private Step step;
    private int wait, swordSlot, anchorSlot, glowSlot, totemSlot;
    private boolean totemOff;
    private BlockHitResult clickHit;
    private BlockPos anchorPos;

    public AutoSafeAnchor() { super("Auto Safe Anchor", "Combat"); }

    @Override public void init() { UseBlockCallback.EVENT.register(this::onUse); }

    @Override public void onDisable() { running = false; }

    private ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!enabled || running || !world.isClient() || hand != Hand.MAIN_HAND || !Util.ready()) return ActionResult.PASS;
        if (sneakBypass.get() && player.isSneaking()) return ActionResult.PASS;
        if (needSword.get() && !(player.getStackInHand(hand).getItem() instanceof SwordItem)) return ActionResult.PASS;

        swordSlot = Util.selected();
        anchorSlot = Util.hotbar(Items.RESPAWN_ANCHOR, 1);
        glowSlot = Util.hotbar(Items.GLOWSTONE, shield.get() ? 2 : 1);
        totemSlot = Util.hotbar(Items.TOTEM_OF_UNDYING, 1);
        totemOff = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        StringBuilder missing = new StringBuilder();
        if (anchorSlot < 0) missing.append("Anchor ");
        if (glowSlot < 0) missing.append("Glowstone ");
        if (totemSlot < 0 && !totemOff) missing.append("Totem ");
        if (missing.length() > 0) { Util.msg("Auto Safe Anchor missing: " + missing.toString().trim()); return ActionResult.PASS; }

        BlockPos target = hit.getBlockPos().offset(hit.getSide());
        if (!world.getBlockState(target).isReplaceable()) return ActionResult.PASS;
        if (player.getEyePos().squaredDistanceTo(hit.getPos()) > 20.25) return ActionResult.PASS;

        clickHit = hit; anchorPos = target; step = Step.PLACE_ANCHOR; wait = 0; running = true;
        return ActionResult.FAIL;
    }

    @Override public void onTick() {
        if (!running) return;
        if (wait > 0) { wait--; return; }
        switch (step) {
            case PLACE_ANCHOR -> {
                Util.select(anchorSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, clickHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                next(Step.CHARGE);
            }
            case CHARGE -> {
                if (!mc.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) { abort("Anchor wasn't placed"); return; }
                Util.select(glowSlot);
                Util.useBlock(Hand.MAIN_HAND, anchorPos, faceToPlayer());
                next(shield.get() ? Step.SHIELD : Step.EXPLODE);
            }
            case SHIELD -> { placeShield(); next(Step.EXPLODE); }
            case EXPLODE -> {
                Hand h;
                if (totemSlot >= 0) { Util.select(totemSlot); h = Hand.MAIN_HAND; } else h = Hand.OFF_HAND;
                Util.useBlock(h, anchorPos, faceToPlayer());
                next(Step.RESTORE);
            }
            case RESTORE -> { Util.select(swordSlot); running = false; }
        }
    }

    private void placeShield() {
        Direction toward = Direction.getFacing(mc.player.getX() - (anchorPos.getX() + 0.5), 0, mc.player.getZ() - (anchorPos.getZ() + 0.5));
        BlockPos spot = anchorPos.offset(toward);
        if (!mc.world.getBlockState(spot).isReplaceable()) return;
        Util.select(glowSlot);
        for (Direction d : Direction.values()) {
            BlockPos support = spot.offset(d);
            if (support.equals(anchorPos)) continue;
            if (mc.world.getBlockState(support).isSolidBlock(mc.world, support)) { Util.useBlock(Hand.MAIN_HAND, support, d.getOpposite()); return; }
        }
    }

    private Direction faceToPlayer() {
        return Direction.getFacing(mc.player.getX() - (anchorPos.getX() + 0.5), mc.player.getEyeY() - (anchorPos.getY() + 0.5), mc.player.getZ() - (anchorPos.getZ() + 0.5));
    }

    private void next(Step s) { step = s; wait = delay.i(); }
    private void abort(String m) { Util.select(swordSlot); Util.msg("Auto Safe Anchor: " + m); running = false; }
}
