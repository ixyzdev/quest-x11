package com.termux.x11;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;

import com.termux.x11.input.ControllerActions;
import com.termux.x11.input.ControllerButtons;
import com.termux.x11.input.ControllerLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ControllerButtonsActivity extends AppCompatActivity {
    private static final long WAIT_MS = 10000;
    private static final long TICK_MS = 100;
    private static final float MINIATURE = 130f / 300f;
    private static final int[] DRAWN_BUTTONS = {
            R.id.controller_left_stick, R.id.controller_y, R.id.controller_x, R.id.controller_menu,
            R.id.controller_right_stick, R.id.controller_b, R.id.controller_a, R.id.controller_meta,
    };
    private static final int[] BADGES = {
            R.id.controller_left_stick_badge, R.id.controller_y_badge, 0, 0,
            R.id.controller_right_stick_badge, R.id.controller_b_badge, 0, 0,
    };

    private enum Screen { OVERVIEW, PICKER, TEST, DIAGNOSTICS }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private final List<View> cards = new ArrayList<>();
    private final List<View> keyCaps = new ArrayList<>();
    private final Set<String> modifiers = new HashSet<>();
    private Screen screen;
    private String selection = ControllerLayout.BACK;
    private CharSequence reason;
    private String key;
    private long deadline;
    private boolean waiting;
    private boolean swallowingTouch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showOverview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ControllerButtons.setLogListener(this::showLog);
        if (screen == Screen.TEST && findViewById(R.id.controller_buttons_waiting).getVisibility() == View.VISIBLE)
            startTest();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ControllerButtons.setLogListener(null);
        stopWaiting();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (screen == Screen.OVERVIEW)
            finish();
        else
            showOverview();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ControllerButtons.record(ControllerButtons.describe(event));
        if (!waiting)
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
        if (input == null || !waiting)
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
        if (!waiting || action != MotionEvent.ACTION_DOWN
                || touches(R.id.controller_buttons_close, event) || touches(R.id.controller_buttons_cancel, event))
            return super.dispatchTouchEvent(event);

        ControllerButtons.record(ControllerButtons.describe(event));
        swallowingTouch = true;
        captured(ControllerLayout.TOUCH);
        return true;
    }

    private void showOverview() {
        stopWaiting();
        screen = Screen.OVERVIEW;
        setContentView(R.layout.controller_buttons_overview);
        onClick(R.id.controller_buttons_close, v -> finish());
        onClick(R.id.controller_buttons_immersive, v -> comingSoon());
        onClick(R.id.controller_buttons_presets, v -> comingSoon());
        onClick(R.id.controller_buttons_test, v -> showTest());
        onClick(R.id.controller_buttons_diagnostics, v -> showDiagnostics());
        onClick(R.id.controller_buttons_reset, v -> confirmReset());
        for (int id : DRAWN_BUTTONS)
            onClick(id, this::pressButton);

        icon(R.id.controller_buttons_presets, R.drawable.ic_controller_presets, 22);
        icon(R.id.controller_buttons_test, R.drawable.ic_controller_test, 22);
        icon(R.id.controller_buttons_diagnostics, R.drawable.ic_controller_diagnostics, 22);
        for (int id : new int[] { R.id.controller_buttons_left_trigger, R.id.controller_buttons_left_grip,
                R.id.controller_buttons_right_trigger, R.id.controller_buttons_right_grip })
            icon(id, R.drawable.ic_controller_lock, 14);
        icon(findViewById(R.id.controller_buttons_legend_shared), R.drawable.ic_controller_link, 18,
                ColorStateList.valueOf(getColor(R.color.controller_buttons_accent_text)));

        setText(R.id.controller_buttons_diagnostics, getString(ControllerButtons.isRecording(this)
                ? R.string.controller_buttons_diagnostics_on : R.string.controller_buttons_diagnostics_off));
        setText(R.id.controller_buttons_input_count, getString(R.string.controller_buttons_input_count, ControllerLayout.INPUTS.length));
        paintOverview();
    }

    private void paintOverview() {
        ImageView infoIcon = findViewById(R.id.controller_buttons_info_icon);
        infoIcon.setImageResource(reason == null ? R.drawable.ic_controller_link : R.drawable.ic_controller_lock);
        infoIcon.setImageTintList(ColorStateList.valueOf(getColor(reason == null
                ? R.color.controller_buttons_accent_text : R.color.controller_buttons_text_2)));
        setText(R.id.controller_buttons_info, reason == null ? selectionText() : reason);
        paintDrawing(findViewById(R.id.controller_buttons_left_drawing));
        paintDrawing(findViewById(R.id.controller_buttons_right_drawing));
        paintRows();
    }

    private CharSequence selectionText() {
        if (selection.equals(ControllerLayout.STICK))
            return html(getString(R.string.controller_buttons_selected_sticks));
        if (ControllerLayout.isStick(selection))
            return html(getString(R.string.controller_buttons_selected_stick, inputName(selection)));
        return html(getString(R.string.controller_buttons_selected_back));
    }

    private void pressButton(View view) {
        String button = (String) view.getTag();
        String input = ControllerLayout.input(button);
        reason = null;
        if (input.equals(ControllerLayout.BACK)) {
            selection = input;
            showPicker();
            return;
        }
        if (input.equals(ControllerLayout.STICK))
            selection = input;
        else if (input.equals(ControllerLayout.TOUCH))
            reason = html(getString(R.string.controller_buttons_reason_click, button));
        else
            reason = html(getString(button.equals("Menu") ? R.string.controller_buttons_reason_menu : R.string.controller_buttons_reason_meta));
        paintOverview();
    }

    private void paintDrawing(View drawing) {
        for (int i = 0; i < DRAWN_BUTTONS.length; i++) {
            View button = drawing.findViewById(DRAWN_BUTTONS[i]);
            if (button == null)
                continue;
            String input = ControllerLayout.input((String) button.getTag());
            boolean shared = reason == null && ControllerLayout.sends(input, selection);
            button.setSelected(shared);
            if (BADGES[i] != 0)
                drawing.findViewById(BADGES[i]).setVisibility(shared ? View.VISIBLE : View.INVISIBLE);
            if (ControllerLayout.isMappable(input))
                paintMappable(button, isMapped(input));
        }
    }

    private void paintMappable(View button, boolean mapped) {
        button.setBackgroundResource(mapped ? R.drawable.controller_button_mapped : R.drawable.controller_button_default);
        if (button instanceof TextView)
            ((TextView) button).setTextColor(getColor(mapped ? R.color.controller_buttons_on_accent : R.color.controller_buttons_text));
        else
            ((ImageView) button).setImageResource(mapped ? R.drawable.controller_stick_mapped : R.drawable.controller_stick);
    }

    private boolean isMapped(String input) {
        if (!input.equals(ControllerLayout.STICK))
            return ControllerButtons.action(this, input) != null;
        for (String each : ControllerLayout.INPUTS)
            if (ControllerLayout.isStick(each) && ControllerButtons.action(this, each) != null)
                return true;
        return false;
    }

    private void paintRows() {
        LinearLayout rows = findViewById(R.id.controller_buttons_rows);
        rows.removeAllViews();
        String[] names = getResources().getStringArray(R.array.controller_buttons_input_names);
        String[] defaults = getResources().getStringArray(R.array.controller_buttons_input_defaults);
        for (int i = 0; i < ControllerLayout.INPUTS.length; i++) {
            String input = ControllerLayout.INPUTS[i];
            View row = getLayoutInflater().inflate(R.layout.controller_buttons_row, rows, false);
            ((TextView) row.findViewById(R.id.controller_buttons_row_name)).setText(names[i]);
            ((TextView) row.findViewById(R.id.controller_buttons_row_sources)).setText(input.equals(ControllerLayout.BACK)
                    ? R.string.controller_buttons_sent_back : R.string.controller_buttons_sent_stick);
            TextView value = row.findViewById(R.id.controller_buttons_row_action);
            String action = ControllerButtons.action(this, input);
            if (action == null)
                value.setText(getString(R.string.controller_buttons_default_value, defaults[i]));
            else if (action.equals(ControllerActions.IGNORE)) {
                value.setText(R.string.controller_buttons_ignored);
                icon(value, R.drawable.ic_controller_ignored, 16, value.getTextColors());
            } else {
                value.setText(ControllerActions.label(getResources(), action));
                value.setTextColor(getColor(R.color.controller_buttons_accent_text));
                value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }
            row.setSelected(reason == null && ControllerLayout.sends(input, selection));
            row.setOnClickListener(v -> {
                selection = input;
                reason = null;
                showPicker();
            });
            rows.addView(row);
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.controller_buttons_reset_title)
                .setMessage(R.string.controller_buttons_reset_message)
                .setPositiveButton(R.string.controller_buttons_reset_confirm, (dialog, which) -> {
                    ControllerButtons.unmapAll(this);
                    paintOverview();
                })
                .setNegativeButton(R.string.controller_buttons_cancel, null)
                .show();
    }

    private void showPicker() {
        screen = Screen.PICKER;
        setContentView(R.layout.controller_buttons_picker);
        boolean back = selection.equals(ControllerLayout.BACK);
        onClick(R.id.controller_buttons_close, v -> showOverview());
        onClick(R.id.controller_buttons_done, v -> showOverview());
        onClick(R.id.controller_buttons_use_default, v -> choose(null));
        onClick(R.id.controller_buttons_letter, v -> comingSoon());
        onClick(R.id.controller_buttons_function_keys, v -> comingSoon());
        onClick(R.id.controller_buttons_type_it, v -> toggleTyping());
        onClick(R.id.controller_buttons_use_combination, v -> choose(ControllerActions.KEYS + ControllerActions.normalize(combination())));

        setText(R.id.controller_buttons_title, inputName(selection));
        setText(R.id.controller_buttons_subtitle, getString(back ? R.string.controller_buttons_sent_by_back : R.string.controller_buttons_sent_by_stick));
        setText(R.id.controller_buttons_will_do, getString(back ? R.string.controller_buttons_will_do_back : R.string.controller_buttons_will_do_stick));
        setText(R.id.controller_buttons_note, getString(back ? R.string.controller_buttons_note_back : R.string.controller_buttons_note_stick));
        setText(R.id.controller_buttons_fires, getString(back ? R.string.controller_buttons_fires_back : R.string.controller_buttons_fires_stick));
        TextView error = findViewById(R.id.controller_buttons_typed_error);
        icon(error, R.drawable.ic_controller_error, 18, error.getTextColors());
        miniature(findViewById(R.id.controller_buttons_left_drawing));
        miniature(findViewById(R.id.controller_buttons_right_drawing));

        buildGroups(back);
        buildCombination();
        ((EditText) findViewById(R.id.controller_buttons_typed_keys)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) { paintCombination(); }
        });
        paintPicker();
    }

    private void miniature(View drawing) {
        drawing.setPivotX(0);
        drawing.setPivotY(0);
        drawing.setScaleX(MINIATURE);
        drawing.setScaleY(MINIATURE);
        drawing.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        disable(drawing);
    }

    private static void disable(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        if (view instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
                disable(((ViewGroup) view).getChildAt(i));
    }

    private void buildGroups(boolean back) {
        Resources resources = getResources();
        LinearLayout groups = findViewById(R.id.controller_buttons_groups);
        int index = ControllerLayout.index(selection);
        cards.clear();
        String ignored = back ? getString(R.string.controller_buttons_ignored_back)
                : getString(R.string.controller_buttons_ignored_stick, inputName(selection));
        group(groups, getString(R.string.controller_buttons_group_basics), Arrays.asList(
                card(groups, null, getString(R.string.controller_buttons_default), resources.getStringArray(R.array.controller_buttons_input_default_subs)[index]),
                card(groups, ControllerActions.IGNORE, getString(R.string.controller_buttons_do_nothing), ignored)));

        String[] actions = resources.getStringArray(R.array.controller_buttons_actions);
        String[] titles = resources.getStringArray(R.array.controller_buttons_action_groups);
        String[] labels = resources.getStringArray(R.array.controller_buttons_action_labels);
        String[] subs = resources.getStringArray(R.array.controller_buttons_action_subs);
        List<View> group = new ArrayList<>();
        for (int i = 0; i < actions.length; i++) {
            group.add(card(groups, actions[i], labels[i], subs[i]));
            if (i == actions.length - 1 || !titles[i + 1].equals(titles[i])) {
                group(groups, titles[i], group);
                group = new ArrayList<>();
            }
        }
    }

    private View card(ViewGroup parent, String action, String label, String sub) {
        View card = getLayoutInflater().inflate(R.layout.controller_buttons_card, parent, false);
        ((TextView) card.findViewById(R.id.controller_buttons_card_label)).setText(label);
        ((TextView) card.findViewById(R.id.controller_buttons_card_sub)).setText(sub);
        card.setTag(action);
        card.setOnClickListener(v -> choose(action));
        cards.add(card);
        return card;
    }

    private void group(LinearLayout container, String title, List<View> cells) {
        TextView heading = new TextView(this, null, 0, R.style.ControllerButtons_Text_Group);
        heading.setText(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = container.getChildCount() == 0 ? 0 : dp(28);
        container.addView(heading, params);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        container.addView(grid, params);
        grid(grid, cells, 3, 16);
    }

    private void grid(LinearLayout container, List<View> cells, int columns, int gap) {
        LinearLayout row = null;
        int count = (cells.size() + columns - 1) / columns * columns;
        for (int i = 0; i < count; i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = i == 0 ? 0 : dp(gap);
                container.addView(row, params);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            params.setMarginStart(i % columns == 0 ? 0 : dp(gap));
            row.addView(i < cells.size() ? cells.get(i) : new Space(this), params);
        }
    }

    private void buildCombination() {
        modifiers.clear();
        key = null;
        keyCaps.clear();
        LinearLayout modifierRow = findViewById(R.id.controller_buttons_modifiers);
        for (String modifier : getResources().getStringArray(R.array.controller_buttons_modifiers)) {
            TextView toggle = keyCap(modifier, ControllerActions.combination(modifier));
            toggle.setMinWidth(dp(96));
            toggle.setPadding(dp(18), 0, dp(18), 0);
            toggle.setTextSize(17);
            toggle.setOnClickListener(v -> {
                if (!modifiers.remove(modifier))
                    modifiers.add(modifier);
                paintCombination();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(60));
            params.setMarginEnd(dp(16));
            modifierRow.addView(toggle, params);
        }

        String[] keys = getResources().getStringArray(R.array.controller_buttons_keys);
        String[] caps = getResources().getStringArray(R.array.controller_buttons_key_caps);
        List<View> cells = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            String id = keys[i];
            TextView cap = keyCap(id, caps[i]);
            cap.setMinHeight(dp(56));
            cap.setContentDescription(ControllerActions.combination(id));
            cap.setOnClickListener(v -> {
                key = id.equals(key) ? null : id;
                paintCombination();
            });
            cells.add(cap);
        }
        grid(findViewById(R.id.controller_buttons_keys), cells, 7, 12);
    }

    private TextView keyCap(String id, String label) {
        TextView cap = new TextView(this, null, 0, R.style.ControllerButtons_Key);
        cap.setText(label);
        cap.setTag(id);
        keyCaps.add(cap);
        return cap;
    }

    private void toggleTyping() {
        View typed = findViewById(R.id.controller_buttons_typed);
        boolean typing = typed.getVisibility() != View.VISIBLE;
        typed.setVisibility(typing ? View.VISIBLE : View.GONE);
        if (typing)
            findViewById(R.id.controller_buttons_typed_keys).requestFocus();
        paintCombination();
    }

    private boolean isTyping() {
        return findViewById(R.id.controller_buttons_typed).getVisibility() == View.VISIBLE;
    }

    private String combination() {
        if (isTyping())
            return ((EditText) findViewById(R.id.controller_buttons_typed_keys)).getText().toString().trim();
        if (key == null)
            return "";
        List<String> names = new ArrayList<>();
        for (String modifier : getResources().getStringArray(R.array.controller_buttons_modifiers))
            if (modifiers.contains(modifier))
                names.add(modifier);
        names.add(key);
        return TextUtils.join("+", names);
    }

    private void paintCombination() {
        for (View cap : keyCaps)
            cap.setSelected(modifiers.contains(cap.getTag()) || cap.getTag().equals(key));

        String keys = combination();
        String unknown = isTyping() && !keys.isEmpty() ? ControllerButtons.unknownKey(keys) : null;
        findViewById(R.id.controller_buttons_typed_keys).setBackgroundResource(unknown == null
                ? R.drawable.controller_buttons_field : R.drawable.controller_buttons_field_error);
        TextView error = findViewById(R.id.controller_buttons_typed_error);
        error.setVisibility(unknown == null ? View.GONE : View.VISIBLE);
        if (unknown != null)
            error.setText(getString(R.string.controller_buttons_unknown_key, unknown));

        boolean valid = !keys.isEmpty() && unknown == null;
        setText(R.id.controller_buttons_combination, valid ? ControllerActions.combination(keys) : getString(R.string.controller_buttons_pick_key));
        findViewById(R.id.controller_buttons_use_combination).setEnabled(valid);
    }

    private void choose(String action) {
        if (action == null)
            ControllerButtons.unmap(this, selection);
        else
            ControllerButtons.map(this, selection, action);
        paintPicker();
    }

    private void paintPicker() {
        String action = ControllerButtons.action(this, selection);
        setText(R.id.controller_buttons_summary, actionLabel(selection, true));
        for (View card : cards)
            card.setSelected(Objects.equals(card.getTag(), action));
        paintDrawing(findViewById(R.id.controller_buttons_left_drawing));
        paintDrawing(findViewById(R.id.controller_buttons_right_drawing));
        paintCombination();
    }

    private void showTest() {
        screen = Screen.TEST;
        setContentView(R.layout.controller_buttons_test);
        onClick(R.id.controller_buttons_close, v -> showOverview());
        onClick(R.id.controller_buttons_cancel, v -> showOverview());
        onClick(R.id.controller_buttons_back_to_buttons, v -> showOverview());
        onClick(R.id.controller_buttons_try_again, v -> startTest());
        onClick(R.id.controller_buttons_test_another, v -> startTest());
        startTest();
    }

    private void startTest() {
        stage(R.id.controller_buttons_waiting);
        handler.removeCallbacks(tick);
        waiting = true;
        deadline = SystemClock.uptimeMillis() + WAIT_MS;
        tick();
    }

    private void tick() {
        long left = deadline - SystemClock.uptimeMillis();
        if (left <= 0) {
            captured(ControllerLayout.NOTHING);
            return;
        }
        ((ProgressBar) findViewById(R.id.controller_buttons_countdown)).setProgress((int) left);
        setText(R.id.controller_buttons_seconds, String.valueOf((left + 999) / 1000));
        handler.postDelayed(tick, TICK_MS);
    }

    private void stopWaiting() {
        handler.removeCallbacks(tick);
        waiting = false;
    }

    private void stage(int id) {
        for (int each : new int[] { R.id.controller_buttons_waiting, R.id.controller_buttons_received, R.id.controller_buttons_nothing })
            findViewById(each).setVisibility(each == id ? View.VISIBLE : View.GONE);
    }

    private void captured(String input) {
        stopWaiting();
        if (input.equals(ControllerLayout.NOTHING)) {
            stage(R.id.controller_buttons_nothing);
            return;
        }
        stage(R.id.controller_buttons_received);
        boolean mappable = ControllerLayout.index(input) >= 0;
        String name;
        CharSequence message;
        if (input.equals(ControllerLayout.BACK)) {
            name = inputName(input);
            message = html(getString(R.string.controller_buttons_received_back, Html.escapeHtml(actionLabel(input, false))));
        } else if (mappable) {
            name = inputName(input);
            message = html(getString(R.string.controller_buttons_received_stick, name, Html.escapeHtml(actionLabel(input, false))));
        } else if (input.equals(ControllerLayout.TOUCH)) {
            name = getString(R.string.controller_buttons_click);
            message = getString(R.string.controller_buttons_received_click);
        } else {
            name = getString(R.string.controller_buttons_other_input);
            message = getString(R.string.controller_buttons_received_other);
        }
        setText(R.id.controller_buttons_received_title, getString(R.string.controller_buttons_received, name));
        setText(R.id.controller_buttons_received_message, message);
        View change = findViewById(R.id.controller_buttons_change_action);
        change.setVisibility(mappable ? View.VISIBLE : View.GONE);
        change.setOnClickListener(v -> {
            selection = input;
            reason = null;
            showPicker();
        });
    }

    private boolean touches(int id, MotionEvent event) {
        View view = findViewById(id);
        if (view == null || !view.isShown())
            return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX() - location[0];
        float y = event.getRawY() - location[1];
        return x >= 0 && y >= 0 && x < view.getWidth() && y < view.getHeight();
    }

    private void showDiagnostics() {
        screen = Screen.DIAGNOSTICS;
        setContentView(R.layout.controller_buttons_diagnostics);
        onClick(R.id.controller_buttons_close, v -> showOverview());
        onClick(R.id.controller_buttons_clear_log, v -> {
            ControllerButtons.clearLog();
            showLog();
        });
        SwitchCompat record = findViewById(R.id.controller_buttons_record);
        record.setChecked(ControllerButtons.isRecording(this));
        record.setOnCheckedChangeListener((view, checked) -> ControllerButtons.setRecording(this, checked));
        showLog();
    }

    private void showLog() {
        TextView log = findViewById(R.id.controller_buttons_log);
        if (log != null)
            log.setText(ControllerButtons.logText());
    }

    private String inputName(String input) {
        return getResources().getStringArray(R.array.controller_buttons_input_names)[ControllerLayout.index(input)];
    }

    private String actionLabel(String input, boolean summary) {
        String action = ControllerButtons.action(this, input);
        if (action != null)
            return ControllerActions.label(getResources(), action);
        String fallback = getResources().getStringArray(R.array.controller_buttons_input_defaults)[ControllerLayout.index(input)];
        return summary ? getString(R.string.controller_buttons_default_value, fallback) : fallback;
    }

    private void comingSoon() {
        Toast.makeText(this, R.string.controller_buttons_coming_soon, Toast.LENGTH_SHORT).show();
    }

    private void onClick(int id, View.OnClickListener listener) {
        findViewById(id).setOnClickListener(listener);
    }

    private void setText(int id, CharSequence text) {
        ((TextView) findViewById(id)).setText(text);
    }

    private void icon(int id, int drawable, int size) {
        TextView view = findViewById(id);
        icon(view, drawable, size, view.getTextColors());
    }

    private void icon(TextView view, int drawable, int size, ColorStateList tint) {
        Drawable icon = Objects.requireNonNull(AppCompatResources.getDrawable(this, drawable)).mutate();
        icon.setBounds(0, 0, dp(size), dp(size));
        icon.setTintList(tint);
        view.setCompoundDrawablesRelative(icon, null, null, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static CharSequence html(String text) {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
    }
}
