package com.termux.x11.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.view.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class ControllerButtonsTest {
    @Test
    public void keyCodesFollowAliases() {
        assertEquals(Arrays.asList(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_PAGE_UP),
                ControllerButtons.keyCodes("ctrl+pageup"));
        assertEquals(Arrays.asList(KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_F5),
                ControllerButtons.keyCodes("super + F5"));
        assertEquals(Arrays.asList(KeyEvent.KEYCODE_ESCAPE), ControllerButtons.keyCodes("esc"));
    }

    @Test
    public void unknownKeysAreReported() {
        assertNull(ControllerButtons.keyCodes("ctrl+foo"));
        assertEquals("foo", ControllerButtons.unknownKey("ctrl+foo"));
        assertNull(ControllerButtons.unknownKey("ctrl+shift+tab"));
    }
}
