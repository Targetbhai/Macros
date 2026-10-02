package com.example.clientgui;

import com.example.clientgui.gui.ClickGuiScreen;
import com.example.clientgui.module.Module;
import com.example.clientgui.module.ModuleManager;
import com.example.clientgui.util.Util;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientGuiMod implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.clientgui.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.clientgui"));
        ModuleManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ClickGuiScreen());
            }
            for (Module m : ModuleManager.all()) {
                m.always();
                if (m.enabled && !m.permanent && Util.ready()) m.onTick();
            }
        });
    }
}
