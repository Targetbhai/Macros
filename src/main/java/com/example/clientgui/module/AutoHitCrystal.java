package com.example.clientgui.module;

import com.example.clientgui.setting.BoolSetting;
import com.example.clientgui.setting.ModeSetting;
import com.example.clientgui.setting.NumSetting;
import com.example.clientgui.util.Util;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/** Left-click a block: obsidian on the clicked face (skipped on obsidian/bedrock), crystal on top, break it. */
public class AutoHitCrystal extends Module {
    private enum Step { OBI, CRYSTAL, BREAK }

    private final NumSetting delay = add(new NumSetting("Delay (ticks)", 1, 0, 10, 1));
    private final BoolSetting silent = add(new BoolSetting("Silent aim", true));
    private final ModeSetting active = add(new ModeSetting("Active with", List.of("Any item", "Sword only"), 0));

    private boolean running, placeObi;
    private Step step;
    private int wait, tries, prevSlot;
    private BlockPos clicked, obi;
    private Direction side;

    public AutoHitCrystal() { super("Auto Hit Crystal", "Combat"); }

    @Override public void init() { AttackBlockCallback.EVENT.register(this::onAttack); }
    @Override public void onDisable() { running = false; }

    private ActionResult onAttack(PlayerEntity pl, World w, Hand hand, BlockPos pos, Direction dir) {
        if (!enabled || running || !w.isClient() || hand != Hand.MAIN_HAND || !Util.ready() || mc.currentScreen != null) return ActionResult.PASS;
        if (active.get() == 1 && !(pl.getMainHandStack().getItem() instanceof SwordItem)) return ActionResult.PASS;

        BlockState st = w.getBlockState(pos);
        boolean solid = st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK);
        if (Util.hotbar(Items.END_CRYSTAL, 1) < 0) return ActionResult.PASS;
        if (!solid && Util.hotbar(Items.OBSIDIAN, 1) < 0) return ActionResult.PASS;

        clicked = pos; side = dir; placeObi = !solid;
        obi = solid ? pos : pos.offset(dir);
        prevSlot = Util.selected();
        step = placeObi ? Step.OBI : Step.CRYSTAL;
        wait = 0; tries = 0; running = true;
        return ActionResult.FAIL;
    }

    @Override public void onTick() {
        if (!running) return;
        if (wait > 0) { wait--; return; }
        switch (step) {
            case OBI -> {
                Util.select(Util.hotbar(Items.OBSIDIAN, 1));
                Util.useBlock(Hand.MAIN_HAND, clicked, side);
                step = Step.CRYSTAL; wait = delay.i();
            }
            case CRYSTAL -> {
                var s = mc.world.getBlockState(obi);
                if (!(s.isOf(Blocks.OBSIDIAN) || s.isOf(Blocks.BEDROCK)) || !mc.world.getBlockState(obi.up()).isReplaceable()) { finish("Can't place crystal"); return; }
                Util.select(Util.hotbar(Items.END_CRYSTAL, 1));
                Util.useBlock(Hand.MAIN_HAND, obi, Direction.UP);
                step = Step.BREAK; wait = delay.i();
            }
            case BREAK -> {
                List<EndCrystalEntity> list = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(obi.up()).expand(0.5), e -> true);
                if (list.isEmpty()) { if (++tries > 8) finish("Crystal not found"); else wait = 1; return; }
                EndCrystalEntity c = list.get(0);
                if (silent.get()) Util.silent(c.getPos().add(0, 0.5, 0), () -> Util.attack(c)); else Util.attack(c);
                finish(null);
            }
        }
    }

    private void finish(String err) {
        Util.select(prevSlot);
        if (err != null) Util.msg("Auto Hit Crystal: " + err);
        running = false;
    }
}
