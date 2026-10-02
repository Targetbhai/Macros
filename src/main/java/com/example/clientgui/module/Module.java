package com.example.clientgui.module;

import com.example.clientgui.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name, category;
    public boolean enabled, expanded, permanent;
    public final List<Setting> settings = new ArrayList<>();
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected Module(String name, String category) { this.name = name; this.category = category; }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    /** Called once at startup (register events / keybinds here). */
    public void init() {}
    /** Called every tick, even when disabled. */
    public void always() {}
    /** Called every tick while enabled and in a world. */
    public void onTick() {}
    public void onDisable() {}

    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean v) {
        if (enabled == v) return;
        enabled = v;
        if (!v) onDisable();
    }
}
