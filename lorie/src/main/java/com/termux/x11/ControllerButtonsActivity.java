package com.termux.x11;

import android.graphics.Typeface;
import android.os.Bundle;
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

import java.util.Map;

public class ControllerButtonsActivity extends AppCompatActivity {
    private TextView capturedText;
    private LinearLayout mappingList;
    private TextView logText;
    private String captured;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.lorie_pref_controllerButtons);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        root.addView(text(R.string.controller_buttons_intro));
        capturedText = text(R.string.controller_buttons_nothing_captured);
        capturedText.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(capturedText);
        root.addView(button(R.string.controller_buttons_assign, v -> chooseAction()));

        SwitchCompat recording = new SwitchCompat(this);
        recording.setText(R.string.controller_buttons_record);
        recording.setChecked(ControllerButtons.isRecording(this));
        recording.setOnCheckedChangeListener((v, checked) -> ControllerButtons.setRecording(this, checked));
        root.addView(recording);

        root.addView(heading(R.string.controller_buttons_mappings));
        mappingList = new LinearLayout(this);
        mappingList.setOrientation(LinearLayout.VERTICAL);
        root.addView(mappingList);

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
        showMappings();
        showLog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ControllerButtons.setLogListener(null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ControllerButtons.record(ControllerButtons.describe(event));
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
            capture(ControllerButtons.inputOf(event));
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        String input = ControllerButtons.inputOf(event);
        if (input == null)
            return super.dispatchGenericMotionEvent(event);

        ControllerButtons.record(ControllerButtons.describe(event));
        capture(input);
        return true;
    }

    private void capture(String input) {
        captured = input;
        capturedText.setText(getString(R.string.controller_buttons_captured, input));
    }

    private void chooseAction() {
        if (captured == null) {
            Toast.makeText(this, R.string.controller_buttons_nothing_captured, Toast.LENGTH_SHORT).show();
            return;
        }

        String input = captured;
        String[] labels = ControllerButtons.ACTIONS.clone();
        labels[0] = getString(R.string.controller_buttons_keys);
        new AlertDialog.Builder(this)
                .setTitle(input)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0)
                        chooseKeys(input);
                    else
                        save(input, ControllerButtons.ACTIONS[which]);
                })
                .show();
    }

    private void chooseKeys(String input) {
        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setHint("ctrl+c");
        new AlertDialog.Builder(this)
                .setTitle(input)
                .setMessage(R.string.controller_buttons_keys_help)
                .setView(field)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String combination = field.getText().toString().trim();
                    if (combination.isEmpty() || ControllerButtons.keyCodes(combination) == null)
                        Toast.makeText(this, getString(R.string.controller_buttons_unknown_keys, combination), Toast.LENGTH_LONG).show();
                    else
                        save(input, "keys:" + combination);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void save(String input, String action) {
        ControllerButtons.map(this, input, action);
        showMappings();
    }

    private void showMappings() {
        mappingList.removeAllViews();
        Map<String, String> mappings = ControllerButtons.mappings(this);
        if (mappings.isEmpty())
            mappingList.addView(text(R.string.controller_buttons_no_mappings));

        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setText(getString(R.string.controller_buttons_mapping, mapping.getKey(), mapping.getValue()));
            row.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle(mapping.getKey())
                    .setMessage(R.string.controller_buttons_remove)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        ControllerButtons.unmap(this, mapping.getKey());
                        showMappings();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show());
            mappingList.addView(row);
        }
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
