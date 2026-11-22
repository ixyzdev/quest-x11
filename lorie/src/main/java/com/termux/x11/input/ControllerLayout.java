package com.termux.x11.input;

public final class ControllerLayout {
    public static final String BACK = "key:KEYCODE_BACK";
    public static final String STICK = "scroll";
    public static final String TOUCH = "touch";
    public static final String NOTHING = "nothing";
    public static final String[] INPUTS = { BACK, "scroll:up", "scroll:down", "scroll:left", "scroll:right" };

    private ControllerLayout() {}

    public static String input(String button) {
        if (button.endsWith(" press"))
            return NOTHING;
        if (button.contains(" joystick "))
            return STICK + ":" + button.substring(button.lastIndexOf(' ') + 1);
        switch (button) {
            case "Left stick": case "Right stick": return STICK;
            case "B": case "Y": return BACK;
            case "A": case "X": case "Right trigger": case "Left trigger": return TOUCH;
            default: return NOTHING;
        }
    }

    public static boolean isStick(String input) {
        return input.equals(STICK) || input.startsWith(STICK + ":");
    }

    public static boolean isMappable(String input) {
        return input.equals(BACK) || isStick(input);
    }

    public static boolean sends(String buttonInput, String selection) {
        if (selection == null)
            return false;
        if (buttonInput.equals(STICK) || selection.equals(STICK))
            return isStick(buttonInput) && isStick(selection);
        return buttonInput.equals(selection);
    }

    public static int index(String input) {
        for (int i = 0; i < INPUTS.length; i++)
            if (INPUTS[i].equals(input))
                return i;
        return -1;
    }
}
