package com.termux.x11.input;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.termux.x11.LoriePreferences;
import com.termux.x11.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ControllerButtons {
    public static final String[] ACTIONS = {
            "keys:", "click:left", "click:middle", "click:right",
            "app:toggle soft keyboard", "app:toggle additional key bar", "app:open preferences", "app:toggle fullscreen",
            "ignore",
    };

    private static final String TAG = "QuestX11Input";
    private static final String PREFERENCES = "controllerButtons";
    private static final String BUTTON_INPUTS = "controllerButtonInputs";
    private static final String RECORDING = "recording";
    private static final int LOG_SIZE = 200;
    private static final long SCROLL_GAP_MS = 250;
    private static final Map<String, String> KEY_ALIASES = new HashMap<>() {{
        put("ctrl", "CTRL_LEFT");
        put("control", "CTRL_LEFT");
        put("alt", "ALT_LEFT");
        put("shift", "SHIFT_LEFT");
        put("super", "META_LEFT");
        put("win", "META_LEFT");
        put("meta", "META_LEFT");
        put("esc", "ESCAPE");
        put("return", "ENTER");
        put("backspace", "DEL");
        put("delete", "FORWARD_DEL");
        put("home", "MOVE_HOME");
        put("end", "MOVE_END");
        put("pageup", "PAGE_UP");
        put("pagedown", "PAGE_DOWN");
        put("up", "DPAD_UP");
        put("down", "DPAD_DOWN");
        put("left", "DPAD_LEFT");
        put("right", "DPAD_RIGHT");
    }};

    private static final ArrayDeque<String> log = new ArrayDeque<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Runnable logListener;

    private final MainActivity activity;
    private final Map<String, Long> lastScroll = new HashMap<>();

    public ControllerButtons(MainActivity activity) {
        this.activity = activity;
    }

    public boolean onKey(KeyEvent event) {
        if (isRecording(activity))
            record(describe(event));

        String action = preferences(activity).getString(inputOf(event), null);
        if (action == null || !activity.getLorieView().connected())
            return false;

        if (event.getRepeatCount() == 0 && event.getAction() != KeyEvent.ACTION_MULTIPLE)
            perform(action, event.getAction() == KeyEvent.ACTION_DOWN);
        return true;
    }

    public boolean onMotion(MotionEvent event) {
        int action = event.getActionMasked();
        if (isRecording(activity) && action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_HOVER_MOVE)
            record(describe(event));

        String input = inputOf(event);
        String mapped = input == null ? null : preferences(activity).getString(input, null);
        if (mapped == null || !activity.getLorieView().connected())
            return false;

        long now = SystemClock.uptimeMillis();
        Long previous = lastScroll.put(input, now);
        if (previous == null || now - previous > SCROLL_GAP_MS) {
            perform(mapped, true);
            perform(mapped, false);
        }
        return true;
    }

    private void perform(String action, boolean down) {
        if (action.startsWith("keys:")) {
            List<Integer> keys = keyCodes(action.substring(5));
            if (keys == null)
                return;
            if (!down)
                Collections.reverse(keys);
            for (int key : keys)
                activity.getLorieView().sendKeyEvent(0, key, down);
        } else if (action.startsWith("click:")) {
            int button = action.endsWith("left") ? InputStub.BUTTON_LEFT : action.endsWith("middle") ? InputStub.BUTTON_MIDDLE : InputStub.BUTTON_RIGHT;
            activity.getLorieView().sendMouseEvent(-1, -1, button, down, true);
        } else if (down) {
            switch (action) {
                case "app:toggle soft keyboard": activity.toggleKeyboardVisibility(); break;
                case "app:toggle additional key bar": activity.toggleExtraKeys(); break;
                case "app:open preferences": activity.startActivity(new Intent(activity, LoriePreferences.class) {{ setAction(Intent.ACTION_MAIN); }}); break;
                case "app:toggle fullscreen": activity.prefs.fullscreen.put(!activity.prefs.fullscreen.get()); break;
            }
        }
    }

    public static List<Integer> keyCodes(String combination) {
        List<Integer> keys = new ArrayList<>();
        for (String name : combination.trim().split("\\s*\\+\\s*")) {
            String upper = KEY_ALIASES.getOrDefault(name.toLowerCase(Locale.ROOT), name.toUpperCase(Locale.ROOT));
            int key = KeyEvent.keyCodeFromString("KEYCODE_" + upper);
            if (key == KeyEvent.KEYCODE_UNKNOWN)
                return null;
            keys.add(key);
        }
        return keys;
    }

    public static String inputOf(KeyEvent event) {
        return "key:" + KeyEvent.keyCodeToString(event.getKeyCode());
    }

    public static String inputOf(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_SCROLL)
            return null;
        float vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        float horizontal = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
        if (vertical == 0 && horizontal == 0)
            return null;
        if (Math.abs(vertical) >= Math.abs(horizontal))
            return vertical > 0 ? "scroll:up" : "scroll:down";
        return horizontal > 0 ? "scroll:right" : "scroll:left";
    }

    public static String describe(KeyEvent event) {
        return String.format(Locale.ROOT, "key %s %s scan=%d repeat=%d source=%s device=%s",
                KeyEvent.keyCodeToString(event.getKeyCode()), keyAction(event.getAction()), event.getScanCode(),
                event.getRepeatCount(), sources(event.getSource()), device(event.getDeviceId()));
    }

    public static String describe(MotionEvent event) {
        return String.format(Locale.ROOT, "motion %s vscroll=%.2f hscroll=%.2f buttons=%d tool=%d source=%s device=%s",
                MotionEvent.actionToString(event.getActionMasked()), event.getAxisValue(MotionEvent.AXIS_VSCROLL),
                event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getButtonState(),
                event.getToolType(event.getActionIndex()), sources(event.getSource()), device(event.getDeviceId()));
    }

    private static String keyAction(int action) {
        return action == KeyEvent.ACTION_DOWN ? "down" : action == KeyEvent.ACTION_UP ? "up" : "multiple";
    }

    private static String device(int id) {
        InputDevice device = InputDevice.getDevice(id);
        return id + (device == null ? "" : " '" + device.getName() + "'");
    }

    private static String sources(int source) {
        StringBuilder names = new StringBuilder();
        int[] flags = { InputDevice.SOURCE_KEYBOARD, InputDevice.SOURCE_DPAD, InputDevice.SOURCE_GAMEPAD,
                InputDevice.SOURCE_TOUCHSCREEN, InputDevice.SOURCE_MOUSE, InputDevice.SOURCE_STYLUS,
                InputDevice.SOURCE_MOUSE_RELATIVE, InputDevice.SOURCE_TOUCHPAD, InputDevice.SOURCE_JOYSTICK,
                InputDevice.SOURCE_ROTARY_ENCODER };
        String[] labels = { "keyboard", "dpad", "gamepad", "touchscreen", "mouse", "stylus",
                "mouse_relative", "touchpad", "joystick", "rotary" };
        for (int i = 0; i < flags.length; i++)
            if ((source & flags[i]) == flags[i])
                names.append(names.length() == 0 ? "" : "|").append(labels[i]);
        return names.length() == 0 ? String.format("0x%x", source) : names.toString();
    }

    public static synchronized void record(String line) {
        Log.i(TAG, line);
        log.addFirst(new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(new Date()) + " " + line);
        while (log.size() > LOG_SIZE)
            log.removeLast();
        if (logListener != null)
            mainHandler.post(logListener);
    }

    public static synchronized String logText() {
        return TextUtils.join("\n", log);
    }

    public static synchronized void clearLog() {
        log.clear();
    }

    public static synchronized void setLogListener(Runnable listener) {
        logListener = listener;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public static boolean isRecording(Context context) {
        return preferences(context).getBoolean(RECORDING, false);
    }

    public static void setRecording(Context context, boolean recording) {
        preferences(context).edit().putBoolean(RECORDING, recording).apply();
    }

    public static String action(Context context, String input) {
        return preferences(context).getString(input, null);
    }

    public static String buttonInput(Context context, String button) {
        return context.getSharedPreferences(BUTTON_INPUTS, Context.MODE_PRIVATE).getString(button, null);
    }

    public static void setButtonInput(Context context, String button, String input) {
        context.getSharedPreferences(BUTTON_INPUTS, Context.MODE_PRIVATE).edit().putString(button, input).apply();
    }

    public static void map(Context context, String input, String action) {
        preferences(context).edit().putString(input, action).apply();
    }

    public static void unmap(Context context, String input) {
        preferences(context).edit().remove(input).apply();
    }
}
