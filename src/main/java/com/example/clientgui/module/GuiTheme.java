package com.example.clientgui.module;

import com.example.clientgui.setting.NumSetting;

/** Not a real module: just holds GUI look settings. */
public class GuiTheme extends Module {
    public final NumSetting hue = add(new NumSetting("Accent color", 0.55, 0, 1, 0.01));
    public final NumSetting opacity = add(new NumSetting("Opacity", 0.9, 0.3, 1, 0.05));

    public GuiTheme() { super("GUI Theme", "Client"); permanent = true; }
}
