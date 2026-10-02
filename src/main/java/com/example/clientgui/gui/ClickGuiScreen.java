package com.example.clientgui.gui;

import com.example.clientgui.module.Module;
import com.example.clientgui.module.ModuleManager;
import com.example.clientgui.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Simple, clean click GUI. Left-click module = toggle. Right-click = show settings. Drag headers to move. */
public class ClickGuiScreen extends Screen {
    private static final int W = 130, HEAD = 18, ROW = 16;

    private record Hit(int x, int y, int w, int h, String cat, Module mod, Setting set, int kind) {}

    private final List<Hit> hits = new ArrayList<>();
    private String dragCat;
    private int offX, offY;
    private Hit dragSlider;

    public ClickGuiScreen() { super(Text.literal("Click GUI")); }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { ModuleManager.save(); }

    private static int col(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x66000000);
        hits.clear();
        int accent = ModuleManager.accent();
        float op = ModuleManager.opacity();

        for (String cat : ModuleManager.categories()) {
            int[] pos = ModuleManager.panelPos(cat);
            int x = pos[0], y = pos[1];

            c.fill(x, y, x + W, y + HEAD, col(0x111111, op));
            c.fill(x, y + HEAD - 1, x + W, y + HEAD, accent);
            c.drawText(textRenderer, cat, x + 6, y + 5, 0xFFFFFFFF, false);
            hits.add(new Hit(x, y, W, HEAD, cat, null, null, 0));
            int cy = y + HEAD;

            for (Module m : ModuleManager.inCategory(cat)) {
                boolean hov = in(mx, my, x, cy, W, ROW);
                int bg = m.enabled && !m.permanent ? col(accent, op * 0.55f) : col(hov ? 0x2A2A2A : 0x1B1B1B, op);
                c.fill(x, cy, x + W, cy + ROW, bg);
                c.drawText(textRenderer, m.name, x + 6, cy + 4, m.enabled || m.permanent ? 0xFFFFFFFF : 0xFFAAAAAA, false);
                if (!m.settings.isEmpty()) c.drawText(textRenderer, m.expanded ? "-" : "+", x + W - 10, cy + 4, 0xFF888888, false);
                hits.add(new Hit(x, cy, W, ROW, cat, m, null, 1));
                cy += ROW;

                if (m.expanded) {
                    for (Setting s : m.settings) {
                        int h = s instanceof NumSetting ? 20 : 14;
                        c.fill(x, cy, x + W, cy + h, col(0x0E0E0E, op));
                        if (s instanceof BoolSetting b) {
                            c.drawText(textRenderer, s.name, x + 10, cy + 3, 0xFFCCCCCC, false);
                            c.fill(x + W - 16, cy + 3, x + W - 8, cy + 11, b.get() ? accent : 0xFF444444);
                        } else if (s instanceof ModeSetting md) {
                            c.drawText(textRenderer, s.name + ": " + md.current(), x + 10, cy + 3, 0xFFCCCCCC, false);
                        } else if (s instanceof NumSetting n) {
                            c.drawText(textRenderer, s.name + ": " + n.display(), x + 10, cy + 2, 0xFFCCCCCC, false);
                            int bw = W - 16;
                            c.fill(x + 8, cy + 14, x + 8 + bw, cy + 17, 0xFF333333);
                            int fw = (int) (bw * (n.get() - n.min) / (n.max - n.min));
                            c.fill(x + 8, cy + 14, x + 8 + fw, cy + 17, accent);
                        }
                        hits.add(new Hit(x, cy, W, h, cat, m, s, 2));
                        cy += h;
                    }
                }
            }
        }
        c.drawText(textRenderer, "Left-click: toggle  |  Right-click: settings  |  Drag headers  |  M / Esc: close", 8, height - 14, 0xFF999999, false);
    }

    private void setSlider(NumSetting n, Hit h, double mx) {
        double t = Math.max(0, Math.min(1, (mx - (h.x + 8)) / (double) (W - 16)));
        n.set(n.min + t * (n.max - n.min));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            switch (h.kind) {
                case 0 -> {
                    if (button == 0) { dragCat = h.cat; int[] p = ModuleManager.panelPos(h.cat); offX = (int) mx - p[0]; offY = (int) my - p[1]; }
                }
                case 1 -> {
                    if (button == 0 && !h.mod.permanent) h.mod.toggle();
                    else if (button == 1 || h.mod.permanent) h.mod.expanded = !h.mod.expanded;
                }
                case 2 -> {
                    if (h.set instanceof BoolSetting b) { if (button == 0) b.toggle(); }
                    else if (h.set instanceof ModeSetting m) m.cycle(button == 1 ? -1 : 1);
                    else if (h.set instanceof NumSetting n && button == 0) { dragSlider = h; setSlider(n, h, mx); }
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragCat != null) {
            int[] p = ModuleManager.panelPos(dragCat);
            p[0] = (int) mx - offX; p[1] = (int) my - offY;
            return true;
        }
        if (dragSlider != null && dragSlider.set instanceof NumSetting n) { setSlider(n, dragSlider, mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragCat = null; dragSlider = null;
        ModuleManager.save();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_M) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
