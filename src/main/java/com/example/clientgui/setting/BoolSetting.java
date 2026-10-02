package com.example.clientgui.setting;

import com.google.gson.JsonObject;

public class BoolSetting extends Setting {
    public boolean value;
    public BoolSetting(String name, boolean def) { super(name); this.value = def; }
    public boolean get() { return value; }
    public void toggle() { value = !value; }
    @Override public void save(JsonObject o) { o.addProperty(name, value); }
    @Override public void load(JsonObject o) { if (o.has(name)) value = o.get(name).getAsBoolean(); }
}
