package com.termux.x11.input;

import static org.junit.Assert.assertEquals;

import android.content.res.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ControllerActionsTest {
    private final Resources resources = RuntimeEnvironment.getApplication().getResources();

    @Test
    public void presetsUseTheirLabels() {
        assertEquals("Right click", ControllerActions.label(resources, "click:right"));
        assertEquals("Switch window", ControllerActions.label(resources, "keys:alt+tab"));
        assertEquals("Quest keyboard", ControllerActions.label(resources, "app:toggle soft keyboard"));
    }

    @Test
    public void ignoreDoesNothing() {
        assertEquals("Do nothing", ControllerActions.label(resources, ControllerActions.IGNORE));
    }

    @Test
    public void ownCombinationsAreSpelledOut() {
        assertEquals("Ctrl + Shift + T", ControllerActions.label(resources, "keys:ctrl+shift+t"));
        assertEquals("Ctrl + Page Up", ControllerActions.combination("ctrl+pageup"));
        assertEquals("Up arrow", ControllerActions.combination("up"));
        assertEquals("Super + F5", ControllerActions.combination("win + f5"));
    }

    @Test
    public void unknownActionsShowTheirId() {
        assertEquals("app:something new", ControllerActions.label(resources, "app:something new"));
    }

    @Test
    public void normalizeLowercasesAndTrims() {
        assertEquals("ctrl+shift+t", ControllerActions.normalize(" Ctrl + Shift+T "));
    }
}
