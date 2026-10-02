package com.example.clientgui.module;

import com.example.clientgui.setting.Setting;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static final LinkedHashMap<String, int[]> panels = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GuiTheme theme;

    private ModuleManager() {}

    public static void init() {
        modules.add(new AutoSafeAnchor());
        modules.add(new AutoMace());
        modules.add(new TriggerBot());
        modules.add(new AutoHitCrystal());
        modules.add(new PearlCatch());
        theme = new GuiTheme();
        modules.add(theme);

        int i = 0;
        for (Module m : modules) {
            if (!panels.containsKey(m.category)) panels.put(m.category, new int[]{10 + i++ * 140, 10});
        }
        for (Module m : modules) m.init();
        load();
    }

    public static List<Module> all() { return modules; }
    public static Set<String> categories() { return panels.keySet(); }
    public static int[] panelPos(String cat) { return panels.get(cat); }

    public static List<Module> inCategory(String cat) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category.equals(cat)) out.add(m);
        return out;
    }

    public static int accent() { return Color.HSBtoRGB((float) theme.hue.get(), 0.65f, 1f) | 0xFF000000; }
    public static float opacity() { return (float) theme.opacity.get(); }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("clientgui.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject(), mods = new JsonObject(), pan = new JsonObject();
            for (Module m : modules) {
                JsonObject o = new JsonObject(), s = new JsonObject();
                o.addProperty("enabled", m.enabled);
                for (Setting st : m.settings) st.save(s);
                o.add("settings", s);
                mods.add(m.name, o);
            }
            for (var e : panels.entrySet()) {
                JsonArray a = new JsonArray();
                a.add(e.getValue()[0]); a.add(e.getValue()[1]);
                pan.add(e.getKey(), a);
            }
            root.add("modules", mods);
            root.add("panels", pan);
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception ignored) {}
    }

    private static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            JsonObject mods = root.getAsJsonObject("modules");
            for (Module m : modules) {
                if (mods == null || !mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (o.has("settings")) for (Setting st : m.settings) st.load(o.getAsJsonObject("settings"));
            }
            JsonObject pan = root.getAsJsonObject("panels");
            if (pan != null) for (var e : panels.entrySet()) {
                if (pan.has(e.getKey())) {
                    JsonArray a = pan.getAsJsonArray(e.getKey());
                    e.getValue()[0] = a.get(0).getAsInt();
                    e.getValue()[1] = a.get(1).getAsInt();
                }
            }
        } catch (Exception ignored) {}
    }
}
