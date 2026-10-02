package com.example.clientgui.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public final class Util {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    /** World tick of the last attack made by any module (prevents double hits in one tick). */
    public static long attackedTick = -1;

    private Util() {}

    public static boolean ready() {
        return mc.player != null && mc.world != null && mc.interactionManager != null && mc.getNetworkHandler() != null;
    }

    public static long now() { return mc.world == null ? 0 : mc.world.getTime(); }

    public static int selected() { return mc.player.getInventory().selectedSlot; }

    public static int hotbar(Predicate<ItemStack> p) {
        for (int i = 0; i < 9; i++) if (p.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    public static int hotbar(Item item, int minCount) {
        return hotbar(s -> s.isOf(item) && s.getCount() >= minCount);
    }

    public static void select(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void useBlock(Hand hand, BlockPos pos, Direction side) {
        Vec3d hitPos = Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(hitPos, side, pos, false));
        mc.player.swingHand(hand);
    }

    public static void useItem(Hand hand) {
        mc.interactionManager.interactItem(mc.player, hand);
        mc.player.swingHand(hand);
    }

    public static void attack(Entity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackedTick = now();
    }

    /** Swap to slot, attack, swap back (all in the same tick). */
    public static void swapHit(int slot, Entity e) {
        int prev = selected();
        if (slot != prev) select(slot);
        attack(e);
        if (slot != prev) select(prev);
    }

    public static int enchant(ItemStack s, RegistryKey<Enchantment> key) {
        var reg = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        return reg.getEntry(key).map(e -> EnchantmentHelper.getLevel(e, s)).orElse(0);
    }

    /** Runs an action while the server sees us looking at the target; camera is restored in the same tick. */
    public static void silent(Vec3d target, Runnable action) {
        ClientPlayerEntity p = mc.player;
        Vec3d eye = p.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float oy = p.getYaw(), op = p.getPitch();
        p.setYaw(yaw);
        p.setPitch(pitch);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, p.isOnGround()));
        action.run();
        p.setYaw(oy);
        p.setPitch(op);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(oy, op, p.isOnGround()));
    }

    public static void msg(String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(s), true);
    }
}
