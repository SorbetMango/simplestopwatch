package omegacentauri.mobi.simplestopwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Countdown extends ShowTime {
    private Button startButton;
    private Button setTimeButton;
    protected MyCountdown chrono;
    protected static final int TEXT_BUTTONS[] = {
            R.id.start,
            R.id.set_time
    };
    protected static final int IMAGE_BUTTONS[][] = {
            {R.id.settings, R.drawable.settings},
            {R.id.menu, R.drawable.menu}
    };

    @Override
    public boolean noTouch() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colorThemeOptionName = Options.PREF_STOPWATCH_COLOR; // use stopwatch color

        setContentView(R.layout.activity_countdown);
        bigDigits = (BigTextView)findViewById(R.id.chrono);
        controlBar = (LinearLayout)findViewById(R.id.controlBar);
        mainContainer = findViewById(R.id.main_view);
        textButtons = TEXT_BUTTONS;
        imageButtons = IMAGE_BUTTONS;
        
        startButton = (Button)findViewById(R.id.start);
        setTimeButton = (Button)findViewById(R.id.set_time);

        setupChrono();
        setInsetListener(findViewById(R.id.main_countdown));
    }

    protected void setupChrono() {
        chrono = new MyCountdown(this, options, bigDigits, (TextView)findViewById(R.id.fraction),
                mainContainer);
        timeKeeper = chrono;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    public void updateButtons() {
        if (chrono == null) return;
        if (chrono.active) {
            startButton.setText(chrono.paused ? "Continue" : "Stop");
        } else {
            startButton.setText("Start");
        }
    }

    public void onButtonStart(View v) {
        if (chrono != null && chrono.isAlarmPlaying()) {
            chrono.stopAlarm();
            return;
        }
        if (chrono.active && !chrono.paused) {
            chrono.firstButton("Stop");
        } else if (chrono.active && chrono.paused) {
            chrono.firstButton("Continue");
        } else {
            if (chrono.countdownTime <= 0) {
                Toast.makeText(this, "Set a time first", Toast.LENGTH_SHORT).show();
            } else {
                chrono.firstButton("Start");
            }
        }
        updateButtons();
    }
    
    public void onButtonSet(View v) {
        if (chrono != null && chrono.isAlarmPlaying()) {
            chrono.stopAlarm();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Countdown Time (minutes)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(chrono.countdownTime / 60000.0));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    double mins = Double.parseDouble(input.getText().toString());
                    long secs = (long) (mins * 60);
                    chrono.setCountdownTime(secs * 1000);
                    updateButtons();
                } catch (NumberFormatException e) {
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void setFullScreen() {
        super.setFullScreen();

        boolean fs = options.getString(Options.PREF_TAP_ACTION, "fullscreen").equals("fullscreen") && options.getBoolean(Options.PREF_FULLSCREEN, false);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (! fs) {
            lp.addRule(RelativeLayout.ABOVE, R.id.controlBar);
        }
        else {
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        mainContainer.setLayoutParams(lp);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
            onButtonMenu(null);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.copy_time).setVisible(false);
        menu.findItem(R.id.copy_laps).setVisible(false);
        menu.findItem(R.id.clear_laps).setVisible(false);
        menu.findItem(R.id.pace).setVisible(false);
        menu.findItem(R.id.unlock_mode).setVisible(false);
        menu.findItem(R.id.lock_mode).setVisible(false);
        menu.findItem(R.id.countdown).setVisible(false);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.copy_time).setVisible(false);
        menu.findItem(R.id.copy_laps).setVisible(false);
        menu.findItem(R.id.clear_laps).setVisible(false);
        menu.findItem(R.id.pace).setVisible(false);
        menu.findItem(R.id.unlock_mode).setVisible(false);
        menu.findItem(R.id.lock_mode).setVisible(false);
        menu.findItem(R.id.countdown).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        debug("options menu "+item.getItemId());
        int itemId = item.getItemId();
        if (itemId == R.id.stopwatch) {
            switchActivity(StopWatch.class, NONE);
            return true;
        } else if (itemId == R.id.clock) {
            switchActivity(Clock.class, NONE);
            return true;
        } else if (itemId == R.id.clock_with_seconds) {
            switchActivity(ClockWithSeconds.class, NONE);
            return true;
        } else if (itemId == R.id.fullscreen) {
            toggleFullscreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (chrono != null && chrono.isAlarmPlaying()) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                chrono.stopAlarm();
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}
