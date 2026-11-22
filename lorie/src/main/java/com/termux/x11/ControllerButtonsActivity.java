package com.termux.x11;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.termux.x11.input.ControllerButtons;

import java.util.Arrays;

public class ControllerButtonsActivity extends AppCompatActivity {
    private static final String[] BUTTONS = {
            "A", "B", "X", "Y", "Menu",
            "Right trigger", "Left trigger", "Right grip", "Left grip",
            "Right joystick up", "Right joystick down", "Right joystick left", "Right joystick right", "Right joystick press",
            "Left joystick up", "Left joystick down", "Left joystick left", "Left joystick right", "Left joystick press",
    };
    private static final String TOUCH = "touch";
    private static final String NOTHING = "nothing";
    private static final long WAIT_MS = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable nothingArrived = () -> captured(NOTHING);
    private LinearLayout buttonList;
    private TextView logText;
    private String waitingFor;
    private boolean swallowingTouch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.lorie_pref_controllerButtons);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        root.addView(text(R.string.controller_buttons_intro));
        buttonList = new LinearLayout(this);
        buttonList.setOrientation(LinearLayout.VERTICAL);
        root.addView(buttonList);

        SwitchCompat recording = new SwitchCompat(this);
        recording.setText(R.string.controller_buttons_record);
        recording.setChecked(ControllerButtons.isRecording(this));
        recording.setOnCheckedChangeListener((v, checked) -> ControllerButtons.setRecording(this, checked));
        root.addView(recording);

        root.addView(heading(R.string.controller_buttons_log));
        root.addView(button(R.string.controller_buttons_clear_log, v -> { ControllerButtons.clearLog(); showLog(); }));
        logText = new TextView(this);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextIsSelectable(true);
        root.addView(logText);

        root.addView(button(R.string.controller_buttons_close, v -> finish()));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ControllerButtons.setLogListener(this::showLog);
        showButtons();
        showLog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ControllerButtons.setLogListener(null);
        stopWaiting();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ControllerButtons.record(ControllerButtons.describe(event));
        if (waitingFor == null)
            return super.dispatchKeyEvent(event);

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
            captured(ControllerButtons.inputOf(event));
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        String input = ControllerButtons.inputOf(event);
        if (input != null)
            ControllerButtons.record(ControllerButtons.describe(event));
        if (input == null || waitingFor == null)
            return super.dispatchGenericMotionEvent(event);

        captured(input);
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (swallowingTouch) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                swallowingTouch = false;
            return true;
        }
        if (waitingFor == null || action != MotionEvent.ACTION_DOWN)
            return super.dispatchTouchEvent(event);

        ControllerButtons.record(ControllerButtons.describe(event));
        swallowingTouch = true;
        captured(TOUCH);
        return true;
    }

    private void waitFor(String button) {
        stopWaiting();
        waitingFor = button;
        handler.postDelayed(nothingArrived, WAIT_MS);
        showButtons();
    }

    private void stopWaiting() {
        handler.removeCallbacks(nothingArrived);
        waitingFor = null;
    }

    private void captured(String input) {
        String button = waitingFor;
        stopWaiting();
        ControllerButtons.setButtonInput(this, button, input);
        showButtons();
        if (!TOUCH.equals(input) && !NOTHING.equals(input))
            chooseAction(button, input);
    }

    private void chooseAction(String button, String input) {
        String[] labels = Arrays.copyOf(ControllerButtons.ACTIONS, ControllerButtons.ACTIONS.length + 1);
        labels[0] = getString(R.string.controller_buttons_keys);
        labels[labels.length - 1] = getString(R.string.controller_buttons_no_action);
        new AlertDialog.Builder(this)
                .setTitle(button)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0)
                        chooseKeys(button, input);
                    else if (which == labels.length - 1)
                        ControllerButtons.unmap(this, input);
                    else
                        ControllerButtons.map(this, input, ControllerButtons.ACTIONS[which]);
                    showButtons();
                })
                .show();
    }

    private void chooseKeys(String button, String input) {
        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setHint("ctrl+c");
        new AlertDialog.Builder(this)
                .setTitle(button)
                .setMessage(R.string.controller_buttons_keys_help)
                .setView(field)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String combination = field.getText().toString().trim();
                    if (combination.isEmpty() || ControllerButtons.keyCodes(combination) == null)
                        Toast.makeText(this, getString(R.string.controller_buttons_unknown_keys, combination), Toast.LENGTH_LONG).show();
                    else
                        ControllerButtons.map(this, input, "keys:" + combination);
                    showButtons();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showButtons() {
        buttonList.removeAllViews();
        for (String button : BUTTONS) {
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setText(describe(button));
            row.setOnClickListener(v -> waitFor(button));
            buttonList.addView(row);
        }
    }

    private String describe(String button) {
        if (button.equals(waitingFor))
            return getString(R.string.controller_buttons_waiting, button);

        String input = ControllerButtons.buttonInput(this, button);
        if (input == null)
            return getString(R.string.controller_buttons_not_captured, button);
        if (TOUCH.equals(input))
            return getString(R.string.controller_buttons_touch, button);
        if (NOTHING.equals(input))
            return getString(R.string.controller_buttons_nothing, button);

        String action = ControllerButtons.action(this, input);
        return getString(R.string.controller_buttons_input, button, input, action == null ? getString(R.string.controller_buttons_no_action) : action);
    }

    private void showLog() {
        logText.setText(ControllerButtons.logText());
    }

    private TextView text(int text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setPadding(0, 8, 0, 8);
        return view;
    }

    private TextView heading(int text) {
        TextView view = text(text);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 32, 0, 8);
        return view;
    }

    private Button button(int text, android.view.View.OnClickListener listener) {
        Button view = new Button(this);
        view.setText(text);
        view.setOnClickListener(listener);
        return view;
    }
}
