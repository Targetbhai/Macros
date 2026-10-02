package com.example.clientgui.setting;

import com.google.gson.JsonObject;
import java.util.List;

public class ModeSetting extends Setting {
    public final List<String> modes;
    public int index;
    public ModeSetting(String name, List<String> modes, int def) { super(name); this.modes = modes; this.index = def; }
    public int get() { return index; }
    public String current() { return modes.get(index); }
    public void cycle(int d) { index = (index + d + modes.size()) % modes.size(); }
    @Override public void save(JsonObject o) { o.addProperty(name, current()); }
    @Override public void load(JsonObject o) {
        if (o.has(name)) { int i = modes.indexOf(o.get(name).getAsString()); if (i >= 0) index = i; }
    }
}
