package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipData;
import android.widget.Toast;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import java.util.Timer;
import java.util.TimerTask;

public class MyCountdown implements MyTimeKeeper {
    public long countdownTime = 0;
    public long baseTime = 0;
    public long pauseTime = 0;
    public boolean paused = false;
    public boolean active = false;
    
    private Activity context;
    private View mainContainer;
    private BigTextView mainView;
    private TextView fractionView;
    private SharedPreferences options;
    
    private Timer timer;
    private Handler updateHandler;
    
    private boolean quiet = false;

    public MyCountdown(Activity context, SharedPreferences options, BigTextView mainView,
            TextView fractionView, View mainContainer) {
        this.context = context;
        this.options = options;
        this.mainView = mainView;
        this.fractionView = fractionView;
        this.mainContainer = mainContainer;
        
        updateHandler = new Handler();
        
        restore();
    }
    
    public void setCountdownTime(long ms) {
        countdownTime = ms;
        active = false;
        paused = false;
        stopUpdating();
        save();
        updateViews();
    }
    
    public long getTime() {
        if (!active) {
            return countdownTime;
        }
        long passed = (paused ? pauseTime : SystemClock.elapsedRealtime()) - baseTime;
        long remaining = countdownTime - passed;
        if (remaining < 0) remaining = 0;
        return remaining;
    }
    
    public void firstButton(String action) {
        if (action.equals("Start") || action.equals("Continue")) {
            if (!active) {
                active = true;
                paused = false;
                baseTime = SystemClock.elapsedRealtime();
            } else if (paused) {
                paused = false;
                baseTime += SystemClock.elapsedRealtime() - pauseTime;
            }
            save();
            startUpdating();
        } else if (action.equals("Stop")) {
            if (active && !paused) {
                paused = true;
                pauseTime = SystemClock.elapsedRealtime();
                stopUpdating();
                save();
            }
        }
        updateViews();
    }
    
    private Ringtone alarmRingtone;

    @Override
    public void updateViews() {
        long t = getTime();
        
        if (active && !paused && t <= 0) {
            t = 0;
            active = false;
            paused = false;
            save();
            stopUpdating();
            ShowTime.vibrate(context, options.getBoolean(Options.PREF_VIBRATE_AFTER_COUNTDOWN, true) ? 1000 : 0);
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (notification == null) {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                alarmRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
                if (alarmRingtone != null) {
                    alarmRingtone.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (context instanceof Countdown) {
                ((Countdown)context).updateButtons();
            }
        }

        long tFraction = (t/100)%10;
        long tSecs = t/1000;
        long tMins = tSecs/60;
        long tHours = tMins/60;
        
        tSecs = tSecs % 60;
        tMins = tMins % 60;
        
        String mainText = "";
        if (tHours > 0) {
            mainText = String.format("%d:%02d:%02d", tHours, tMins, tSecs);
        } else {
            mainText = String.format("%02d:%02d", tMins, tSecs);
        }
        
        mainView.setText(mainText, Boolean.FALSE, Boolean.valueOf(!active || paused));
        if (fractionView != null) {
            fractionView.setText(String.format(".%d", tFraction));
        }
    }

    public void save() {
        SharedPreferences.Editor ed = options.edit();
        ed.putLong("countdown_time", countdownTime);
        ed.putLong("countdown_base", baseTime);
        ed.putLong("countdown_pause", pauseTime);
        ed.putBoolean("countdown_active", active);
        ed.putBoolean("countdown_paused", paused);
        ed.apply();
    }

    @Override
    public void restore() {
        countdownTime = options.getLong("countdown_time", 0);
        baseTime = options.getLong("countdown_base", 0);
        pauseTime = options.getLong("countdown_pause", 0);
        active = options.getBoolean("countdown_active", false);
        paused = options.getBoolean("countdown_paused", false);

        if (active && !paused) {
            startUpdating();
        } else {
            stopUpdating();
        }
        updateViews();
    }

    @Override
    public void stopUpdating() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void startUpdating() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateViews();
                        }
                    });
                }
            }, 0, 50);
        }
        if (options.getBoolean(Options.PREF_SCREEN_ON, true))
            context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void destroy() {
        stopUpdating();
    }

    @Override
    public void suspend() {
        stopUpdating();
    }

    @Override
    public void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("countdown time", mainView.text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Time copied", Toast.LENGTH_SHORT).show();
    }

    public boolean isAlarmPlaying() {
        return alarmRingtone != null && alarmRingtone.isPlaying();
    }

    public void stopAlarm() {
        if (alarmRingtone != null) {
            alarmRingtone.stop();
            alarmRingtone = null;
        }
    }
}