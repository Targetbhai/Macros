package com.example.clientgui.setting;

import com.google.gson.JsonObject;

public class NumSetting extends Setting {
    public double value;
    public final double min, max, step;
    public NumSetting(String name, double def, double min, double max, double step) {
        super(name); this.min = min; this.max = max; this.step = step; this.value = def;
    }
    public double get() { return value; }
    public int i() { return (int) Math.round(value); }
    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        v = Math.round((v - min) / step) * step + min;
        value = Math.max(min, Math.min(max, v));
    }
    public String display() { return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format(step < 0.1 ? "%.2f" : "%.1f", value); }
    @Override public void save(JsonObject o) { o.addProperty(name, value); }
    @Override public void load(JsonObject o) { if (o.has(name)) set(o.get(name).getAsDouble()); }
}
