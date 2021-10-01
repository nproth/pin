package de.nproth.pin;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.Toast;

import de.nproth.pin.pinboard.PinboardService;
import de.nproth.pin.util.Timespan;

/**
 * Only activity of this app. Allows the user to add or edit pins and set the snooze duration
 */
public class NoteActivity extends AppCompatActivity implements ServiceConnection {

    private EditText NoteInputField;
    private ImageButton SaveNoteButton;
    private Button SnoozeDurationButton;
    private RotaryControlView SnoozeDurationSlider;
    private ImageButton PinBackgroundButton;

    private Handler mAnimHandler;

    private Animation mEmptyPinAnimation;
    private Animation mPinAnimation;
    private Animation mRotaryAnimation;
    private Animation mRotarySetAnimation;
    private Animation mPopInAnimation;

    private int[] mDurations;
    private int[] mSteps;
    private int[] mZeroSnoozeDurations;
    private int mZeroSnoozeDur;

    private PinboardService.PinboardBinder mPinboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);

        setContentView(R.layout.activity_note);

        NoteInputField = findViewById(R.id.note_input_field);
        SaveNoteButton = findViewById(R.id.note_save_button);
        SnoozeDurationButton = findViewById(R.id.snooze_duration_button);
        SnoozeDurationSlider = findViewById(R.id.snooze_duration_slider);

        PinBackgroundButton = findViewById(R.id.note_pin_background);

        mAnimHandler = new Handler(Looper.getMainLooper());
        mPinAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_input_field_pin);

        mEmptyPinAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_input_field_empty);

        mRotaryAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_rotary_control);
        mRotarySetAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_rotary_set);

        mPopInAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_pop_in);


        mDurations = getResources().getIntArray(R.array.snooze_durations);
        mSteps = getResources().getIntArray(R.array.snooze_duration_steps);
        mZeroSnoozeDurations = getResources().getIntArray(R.array.zero_snooze_durations);

        prepareEditPin();

    }

    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        prepareEditPin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, PinboardService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mPinboard = (PinboardService.PinboardBinder) service;

        /*****************************
         Initialize UI components here
         *****************************/

        //pin note to notification drawer on enter action in textfield
        NoteInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    pinNote(0);
                    return true;
                }
                else
                    return false;
            }
        });

        //Wake up hidden pins on long click on SnoozeDurationutton
        SnoozeDurationButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ContentValues cv = new ContentValues();
                cv.put(NotesProvider.Notes.WAKE_UP, 0);
                int updated = getContentResolver().update(Uri.parse(NotesProvider.CONTENT_URI + "/notes"), cv, NotesProvider.Notes.WAKE_UP + " <> 0 AND " + NotesProvider.Notes.TEXT + " IS NOT NULL", null);
                //Show toast when notes woke up
                if(updated > 0)
                    Toast.makeText(NoteActivity.this, R.string.toast_show_all, Toast.LENGTH_SHORT).show();
                //update silently (without notification ping)
                mPinboard.update();
                return true;
            }
        });

        //Prepare Rotary Control
        SnoozeDurationSlider.setOnValueChangedListener(new RotaryControlView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int val, int min, int max, int stepVal) {}

            @Override
            public void onUserChangeBegin(int val) {}

            @Override
            public void onUserChange(int val) {
                // if zero, assume zero_snooze_duration
                int cur = val == 0 ? mZeroSnoozeDur : val * 1000;
                Timespan span = new Timespan(NoteActivity.this, cur);
                String txt = getResources().getString(R.string.ftext_button_snooze_duration, (int) span.inHours(), span.restMins());
                if (span.millis < 60000) txt = getResources().getString(R.string.ftext_button_zero_snooze, (int) span.inSecs());
                SnoozeDurationButton.setText(txt);
            }

            @Override
            public void onUserChangeEnd(int val) {
                // if zero, assume zero_snooze_duration
                mPinboard.setSnoozeDuration(val == 0 ? mZeroSnoozeDur : val * 1000);
            }
        });

        //And fix pins on long click on PinButton
        SaveNoteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                boolean fixPins = !mPinboard.getIsFixed();
                mPinboard.setIsFixed(fixPins);
                String toast;
                if(fixPins)
                    toast = NoteActivity.this.getString(R.string.toast_fix_pins);
                else
                    toast = NoteActivity.this.getString(R.string.toast_unfix_pins);
                Toast.makeText(NoteActivity.this, toast, Toast.LENGTH_SHORT).show();
                mPinboard.update();
                return true;
            }
        });

        loadSnoozeDuration();
        SnoozeDurationSlider.startAnimation(mRotaryAnimation);

        PinBackgroundButton.startAnimation(mPopInAnimation);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.w("NoteActivity", "PinboardService disconnected, shutting down activity...");
        NoteInputField.setOnEditorActionListener(null);
        SnoozeDurationButton.setOnLongClickListener(null);
        SnoozeDurationSlider.setOnValueChangedListener(null);
        SaveNoteButton.setOnLongClickListener(null);
        mPinboard = null;
    }


    private void loadSnoozeDuration() {
        long dur = mPinboard.getSnoozeDuration();

        int index = 0;

        while(index < mDurations.length - 1 && mDurations[index] < dur)
            index++;

        //if there is a zero snooze duration exactly matching the current duration, set up this instead
        for(int i = 0; i < mZeroSnoozeDurations.length; i++)
            if(mZeroSnoozeDurations[i] == dur) {
                dur = 0;
                index = i;//Assuming that mDurations and mZeroDurations have equal length
            }

        SnoozeDurationSlider.setMaxValue(mDurations[index] / 1000);
        SnoozeDurationSlider.setStepValue(mSteps[index] / 1000);
        SnoozeDurationSlider.setValue((int) (dur / 1000));//in seconds
        //setValue / getValue takes care of normalizing, aligning to steps, ...

        mZeroSnoozeDur = mZeroSnoozeDurations[index];

        Log.d("NoteActivity", "Loaded slider value " + SnoozeDurationSlider.getValue());
        // If zero, assume zero_snooze_duration
        long spanMillis = SnoozeDurationSlider.getValue() == 0 ? mZeroSnoozeDur : SnoozeDurationSlider.getValue() * 1000;
        Timespan span = new Timespan(this, spanMillis);

        Log.d("NoteActivity", "Loaded milliseconds " + span.millis);
        String txt = getResources().getString(R.string.ftext_button_snooze_duration, (int) span.inHours(), span.restMins());
        if (span.millis < 60000)
            txt =  getResources().getString(R.string.ftext_button_zero_snooze, (int) span.inSecs());
        SnoozeDurationButton.setText(txt);
        mPinboard.update();
    }

    public void onNoteSaveButtonClicked(View v) {
        if(mPinboard == null) {
            Log.e("NoteActivity", "PinButton clicked but PinboardService disconnected");
            return;
        }
        pinNote(0);
    }

    public void onNotePinBackgroundButtonClicked(View v) {
        if(mPinboard == null) {
            Log.e("NoteActivity", "PinButton clicked but PinboardService disconnected");
            return;
        }
        if(pinNote(mPinboard.getSnoozeDuration()))
            Toast.makeText(this, getResources().getString(R.string.toast_pin_background, new Timespan(this, mPinboard.getSnoozeDuration()).toString()), Toast.LENGTH_SHORT).show();
    }

    public void onParentLayoutClicked(View v) {
        finish();//exit activity when user clicks on empty space in activity
    }

    public void onSetSnoozeDurationButtonClicked(View v) {
        if(mPinboard == null) {
            Log.e("NoteActivity", "PinButton clicked but PinboardService disconnected");
            return;
        }

        int index = 0;
        long dur = SnoozeDurationSlider.getValue() * 1000;
        while(index < mDurations.length && mDurations[index] <= dur)
            index++;
        if(index == mDurations.length)
            index = 0;

        final int i = index;
        SnoozeDurationSlider.startAnimation(mRotarySetAnimation);
        mAnimHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPinboard.setSnoozeDuration(mDurations[i]);
                loadSnoozeDuration();
            }
        }, 250);
    }

    private boolean pinNote(long snooze) {
        String txt = NoteInputField.getText().toString();
        long time = System.currentTimeMillis();

        //Save note in Database via ContentProvider
        if(!TextUtils.isEmpty(txt)) {

            ContentValues cv = new ContentValues();
            cv.put(NotesProvider.Notes.TEXT, txt);
            cv.put(NotesProvider.Notes.MODIFIED, time);
            cv.put(NotesProvider.Notes.WAKE_UP, time + snooze);

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
                mPinboard.update();

            NoteInputField.startAnimation(mPinAnimation);
            SnoozeDurationSlider.startAnimation(mRotarySetAnimation);
            mAnimHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    NoteInputField.setText("");
                    NoteInputField.requestFocus();
                }
            }, 125);

            //Do not close but give user the opportunity to enter additional notes

            //But clear any data we previously operated upon
            getIntent().setData(null);
            return true;
        } else {
            NoteInputField.startAnimation(mEmptyPinAnimation);
            return false;
        }
    }

    /**
     * Checks whether any Uri data is contained in the activity's intent and prepares the textfield accordingly. To be called from onCreate and onNewIntent
     */
    private void prepareEditPin() {

        Uri uri = getIntent().getData();
        Log.d("NoteActivity", String.format("Activity called with uri: %s", uri == null? "null" : uri));
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
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
    }
}
