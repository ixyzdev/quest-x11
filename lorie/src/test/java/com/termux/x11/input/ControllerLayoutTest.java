package com.termux.x11.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ControllerLayoutTest {
    @Test
    public void backButtonsSendBack() {
        assertEquals(ControllerLayout.BACK, ControllerLayout.input("B"));
        assertEquals(ControllerLayout.BACK, ControllerLayout.input("Y"));
    }

    @Test
    public void sticksSendScroll() {
        assertEquals(ControllerLayout.STICK, ControllerLayout.input("Left stick"));
        assertEquals(ControllerLayout.STICK, ControllerLayout.input("Right stick"));
        assertEquals("scroll:up", ControllerLayout.input("Left joystick up"));
        assertEquals("scroll:right", ControllerLayout.input("Right joystick right"));
    }

    @Test
    public void faceButtonsAndTriggersArriveAsClicks() {
        for (String button : new String[] { "A", "X", "Left trigger", "Right trigger" })
            assertEquals(button, ControllerLayout.TOUCH, ControllerLayout.input(button));
    }

    @Test
    public void systemButtonsAndPressesSendNothing() {
        for (String button : new String[] { "Menu", "Meta", "Left grip", "Right grip", "Left stick press" })
            assertEquals(button, ControllerLayout.NOTHING, ControllerLayout.input(button));
    }

    @Test
    public void onlyBackAndSticksAreMappable() {
        assertTrue(ControllerLayout.isMappable(ControllerLayout.BACK));
        assertTrue(ControllerLayout.isMappable(ControllerLayout.STICK));
        assertTrue(ControllerLayout.isMappable("scroll:down"));
        assertFalse(ControllerLayout.isMappable(ControllerLayout.TOUCH));
        assertFalse(ControllerLayout.isMappable(ControllerLayout.NOTHING));
    }

    @Test
    public void stickSelectionHighlightsBothSticks() {
        assertTrue(ControllerLayout.sends(ControllerLayout.STICK, "scroll:left"));
        assertTrue(ControllerLayout.sends("scroll:left", ControllerLayout.STICK));
        assertFalse(ControllerLayout.sends("scroll:up", "scroll:left"));
        assertFalse(ControllerLayout.sends(ControllerLayout.STICK, ControllerLayout.BACK));
        assertFalse(ControllerLayout.sends(ControllerLayout.BACK, null));
        assertTrue(ControllerLayout.sends(ControllerLayout.BACK, ControllerLayout.BACK));
    }

    @Test
    public void indexFollowsInputs() {
        assertEquals(0, ControllerLayout.index(ControllerLayout.BACK));
        assertEquals(4, ControllerLayout.index("scroll:right"));
        assertEquals(-1, ControllerLayout.index(ControllerLayout.STICK));
        assertEquals(-1, ControllerLayout.index(ControllerLayout.TOUCH));
    }
}
