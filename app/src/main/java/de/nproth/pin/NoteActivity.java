package de.nproth.pin;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import de.nproth.pin.util.LifecycleWatcher;
import de.nproth.pin.util.Timespan;

/**
 * Only activity of this app. Allows the user to add or edit pins and set the snooze duration
 */
public class NoteActivity extends AppCompatActivity {

    public static final String PREFERENCE_FIRST_RUN = "first_run";
    public static final String PREFERENCE_WARN_USER = "warn_user";

    public static final String PREFERENCE_SNOOZE_DURATION = "snooze_duration";
    public static final long DEFAULT_SNOOZE_DURATION = 30 * 60 * 1000; //30min in millis
        //10 * 1000;//10s //T.ODO reset
    private EditText NoteInputField;
    private ImageButton SaveNoteButton;
    private Button SnoozeDurationButton;
    private ImageButton AlertButton;

    private Handler mAnimHandler;

    private Animation mEmptyPinAnimation;
    private Animation mPinAnimation;

    private Animation mPopInAnim, mPopOutAnim;

    private int rowId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);

        setContentView(R.layout.activity_note);

        NoteInputField = findViewById(R.id.note_input_field);
        SaveNoteButton = findViewById(R.id.note_save_button);
        SnoozeDurationButton = findViewById(R.id.snooze_duration_button);
        AlertButton = findViewById(R.id.alert_button);

        mAnimHandler = new Handler(Looper.getMainLooper());
        mPinAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_input_field_pin);

        mEmptyPinAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_input_field_empty);

        mPopInAnim = AnimationUtils.loadAnimation(this, R.anim.anim_pop_in);
        mPopOutAnim = AnimationUtils.loadAnimation(this, R.anim.anim_pop_out);

        mPopInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                AlertButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mPopOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlertButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        NoteInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    pinNote();
                    return true;
                }
                else
                    return false;
            }
        });

        populateUI();

        //update snooze duration button text
        Timespan span = new Timespan(this, PreferenceManager.getDefaultSharedPreferences(this).getLong(PREFERENCE_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION));
        String txt = getResources().getString(R.string.ftext_button_snooze_duration, (int) span.inHours(), span.restMins());
        SnoozeDurationButton.setText(txt);

        Pinboard.get(this).updateAll();

    }

    @Override
    public void onNewIntent(Intent i) {
        setIntent(i);
        populateUI();
    }

    public void populateUI() {

        //Check whether supplied data uri (if any) is valid
        Uri uri = getIntent().getData();
        Log.d("NoteActivity", String.format("Activity called with uri: ", uri == null? "null" : uri));
        if(uri != null) {
            if (TextUtils.isEmpty(uri.getLastPathSegment()) || !TextUtils.isDigitsOnly(uri.getLastPathSegment())) {
                Log.e("NoteActivity", String.format("Invalid data uri '%s' supplied", uri));
                finish();
            }
            //And populate UI with supplied data
            Cursor c = getContentResolver().query(uri, new String[] { NotesProvider.Notes.TEXT }, null, null, null);
            if(c == null || c.getCount() != 1) {
                Log.e("NoteActivity", String.format("Could not query text for uri '%s'", uri));
            } else {
                c.moveToFirst();
                NoteInputField.setText(c.getString(0));
            }

            if(c != null)
                c.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAlertButton();
    }

    public void onAlertClicked(View v) {
        AlertButton.startAnimation(mPopOutAnim);
        //Show alert dialog
        new AlertDialog.Builder(this, R.style.AppAlertDialogStyle)
                .setTitle(R.string.title_battery_optimization)
                .setMessage(R.string.warn_battery_optimization)
                .setPositiveButton(R.string.dialog_acknowledged, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Nothing really
                updateAlertButton();
            }
        }).setNegativeButton(R.string.dialog_never_again, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                PreferenceManager.getDefaultSharedPreferences(NoteActivity.this).edit().putBoolean(PREFERENCE_WARN_USER, false).apply();
                updateAlertButton();
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                updateAlertButton();
            }
        }).show();
    }

    public void updateAlertButton() {
        Log.d("NoteActivity", (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_FIRST_RUN, true)? "" : "Not ") + "First run");
        Log.d("NoteActivity", "Should " + (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_WARN_USER, true)? "" : "not ") + "warn user");
        Log.d("NoteActivity", "Boot completed intent was " + (LifecycleWatcher.hasBootReceived()? "" : "not ") + "received");

        //Warn user if app was not started on boot and this is not it's first run
        if((!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_FIRST_RUN, true))
                && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_WARN_USER, true) && !LifecycleWatcher.hasBootReceived()) {
            //so we have been running before and were not notified on boot -> prompt user
            AlertButton.startAnimation(mPopInAnim);
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREFERENCE_FIRST_RUN, false).apply();
            LifecycleWatcher.informBootReceived();//Don't display any warnings until next (first) reboot
            if(AlertButton.getVisibility() != View.GONE)
                AlertButton.startAnimation(mPopOutAnim);
        }
    }

    public void onNoteSaveButtonClicked(View v) {
        pinNote();
    }

    public void onParentLayoutClicked(View v) {
        finish();//exit activity when user clicks on empty space in activity
    }

    public void onSetSnoozeDurationButtonClicked(View v) {
        int[] durations = getResources().getIntArray(R.array.snooze_durations);

        long cur = PreferenceManager.getDefaultSharedPreferences(this).getLong(PREFERENCE_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);

        int index = -1;

        for(int i = 0; i < durations.length; i++) {
            if (cur == durations[i]) {
                index = i;
                break;
            }
        }

        if(index >= 0 && index < durations.length - 1)
            cur = durations[index + 1];
        else
            cur = durations[0];

        PreferenceManager.getDefaultSharedPreferences(this).edit().putLong(PREFERENCE_SNOOZE_DURATION, cur).apply();

        Timespan span = new Timespan(this, cur);
        String txt = getResources().getString(R.string.ftext_button_snooze_duration, (int) span.inHours(), span.restMins());
        SnoozeDurationButton.setText(txt);

        //notify service about the changed snooze duration
        Pinboard.get(this).updateVisible();
    }

    private void pinNote() {
        String txt = NoteInputField.getText().toString();
        long time = System.currentTimeMillis();

        //Save note in Database via ContentProvider
        //Testing
        if(BuildConfig.DEBUG && "#alert".equals(txt)) {
            LifecycleWatcher.reset();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREFERENCE_FIRST_RUN, false).putBoolean(PREFERENCE_WARN_USER, true).apply();
            finish();
        } else if(BuildConfig.DEBUG && "#no-alert".equals(txt)) {
            LifecycleWatcher.informBootReceived();
            finish();
        }
        //End Testing
        else if(!TextUtils.isEmpty(txt)) {

            ContentValues cv = new ContentValues();
            cv.put(NotesProvider.Notes.TEXT, txt);
            cv.put(NotesProvider.Notes.MODIFIED, time);

            Uri uri = getIntent().getData();

            //We're about to pin a new note
            if(uri == null) {
                //Created is used to show a timestamp in the notification area
                cv.put(NotesProvider.Notes.CREATED, time);
                uri = getContentResolver().insert(Uri.parse(NotesProvider.CONTENT_URI + "/notes"), cv);
                Log.d("NoteActivity", String.format("Inserted note item with uri '%s'", uri));
            } else {
                //We're about to update a existing note
                getContentResolver().update(uri, cv, null, null);
                Log.d("NoteActivity", String.format("Updated note item with uri '%s'", uri));
            }

            if(uri != null)
                Pinboard.get(this).updateChanged();

            NoteInputField.startAnimation(mPinAnimation);
            mAnimHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    NoteInputField.setText("");
                    NoteInputField.requestFocus();
                }
            }, 125);
        } else
            NoteInputField.startAnimation(mEmptyPinAnimation);

        //Do not close but give user the opportunity to enter additional notes

        //But clear any data we previously operated upon
        getIntent().setData(null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
    }
}
