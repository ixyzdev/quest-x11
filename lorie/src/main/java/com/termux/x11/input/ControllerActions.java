package com.termux.x11.input;

import android.content.res.Resources;
import android.text.TextUtils;

import com.termux.x11.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ControllerActions {
    public static final String IGNORE = "ignore";
    public static final String KEYS = "keys:";
    private static final Map<String, String> KEY_LABELS = new HashMap<>() {{
        put("ctrl", "Ctrl");
        put("control", "Ctrl");
        put("alt", "Alt");
        put("shift", "Shift");
        put("super", "Super");
        put("win", "Super");
        put("meta", "Super");
        put("esc", "Esc");
        put("escape", "Esc");
        put("return", "Enter");
        put("enter", "Enter");
        put("backspace", "Backspace");
        put("delete", "Delete");
        put("pageup", "Page Up");
        put("pagedown", "Page Down");
        put("up", "Up arrow");
        put("down", "Down arrow");
        put("left", "Left arrow");
        put("right", "Right arrow");
    }};

    private ControllerActions() {}

    public static String label(Resources resources, String action) {
        if (action.equals(IGNORE))
            return resources.getString(R.string.controller_buttons_do_nothing);
        int index = Arrays.asList(resources.getStringArray(R.array.controller_buttons_actions)).indexOf(action);
        if (index >= 0)
            return resources.getStringArray(R.array.controller_buttons_action_labels)[index];
        if (action.startsWith(KEYS))
            return combination(action.substring(KEYS.length()));
        return action;
    }

    public static String combination(String keys) {
        List<String> labels = new ArrayList<>();
        for (String name : ControllerButtons.keyNames(keys))
            labels.add(keyLabel(name));
        return TextUtils.join(" + ", labels);
    }

    public static String normalize(String keys) {
        return TextUtils.join("+", ControllerButtons.keyNames(keys.toLowerCase(Locale.ROOT)));
    }

    private static String keyLabel(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        String label = KEY_LABELS.get(lower);
        if (label != null)
            return label;
        return lower.isEmpty() ? lower : lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }
}
