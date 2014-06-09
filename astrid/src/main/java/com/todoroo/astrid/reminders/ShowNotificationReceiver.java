package com.todoroo.astrid.reminders;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.telephony.TelephonyManager;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;

/**
 * Receives requests to show an Astrid notification if they were not intercepted and handled
 * by the in-app reminders in AstridActivity.
 *
 * @author Sam
 */
public class ShowNotificationReceiver extends InjectingBroadcastReceiver {

    private static ExecutorService singleThreadVoicePool = Executors.newSingleThreadExecutor();
    private static long lastNotificationSound = 0L;

    @Inject NotificationManager notificationManager;
    @Inject Preferences preferences;
    @Inject VoiceOutputAssistant voiceOutputAssistant;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int notificationId = intent.getIntExtra(Notifications.EXTRAS_NOTIF_ID, 0);
        Intent customIntent = intent.getParcelableExtra(Notifications.EXTRAS_CUSTOM_INTENT);
        int type = intent.getIntExtra(Notifications.EXTRAS_TYPE, 0);
        String title = intent.getStringExtra(Notifications.EXTRAS_TITLE);
        String text = intent.getStringExtra(Notifications.EXTRAS_TEXT);
        int ringTimes = intent.getIntExtra(Notifications.EXTRAS_RING_TIMES, 1);
        showNotification(context, notificationId, customIntent, type, title, text, ringTimes);
    }

    /**
     * @return true if notification should sound
     */
    private static boolean checkLastNotificationSound() {
        long now = DateUtilities.now();
        if (now - lastNotificationSound > 10000) {
            lastNotificationSound = now;
            return true;
        }
        return false;
    }

    /**
     * @return whether we're in quiet hours
     */
    static boolean isQuietHours(Preferences preferences) {
        boolean quietHoursEnabled = preferences.getBoolean(R.string.p_rmd_enable_quiet, false);
        if (quietHoursEnabled) {
            long quietHoursStart = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietStart)).getMillis();
            long quietHoursEnd = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietEnd)).getMillis();
            long now = currentTimeMillis();
            if (quietHoursStart <= quietHoursEnd) {
                if (now >= quietHoursStart && now < quietHoursEnd) {
                    return true;
                }
            } else { // wrap across 24/hour boundary
                if (now >= quietHoursStart || now < quietHoursEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Shows an Astrid notification. Pulls in ring tone and quiet hour settings
     * from preferences. You can make it say anything you like.
     *
     * @param ringTimes number of times to ring (-1 = nonstop)
     */
    private void showNotification(Context context, int notificationId, Intent intent, int type, String title,
                                  String text, int ringTimes) {
        // don't ring multiple times if random reminder
        if (type == ReminderService.TYPE_RANDOM) {
            ringTimes = 1;
        }

        // quiet hours? unless alarm clock
        boolean quietHours = (type == ReminderService.TYPE_ALARM || type == ReminderService.TYPE_DUE) ? false : isQuietHours(preferences);

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // create notification object
        final Notification notification = new Notification(
                R.drawable.notif_astrid, text, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                title,
                text,
                pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (preferences.getBoolean(R.string.p_rmd_persistent, true)) {
            notification.flags |= Notification.FLAG_NO_CLEAR |
                    Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        } else {
            notification.defaults = Notification.DEFAULT_LIGHTS;
        }

        AudioManager audioManager = (AudioManager) context.getSystemService(
                Context.AUDIO_SERVICE);

        // detect call state
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int callState = tm.getCallState();

        boolean voiceReminder = preferences.getBoolean(R.string.p_voiceRemindersEnabled, false);

        // if multi-ring is activated and the setting p_rmd_maxvolume allows it, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        boolean maxOutVolumeForMultipleRingReminders = preferences.getBoolean(R.string.p_rmd_maxvolume, true);
        // remember it to set it to the old value after the alarm
        int previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if (ringTimes != 1 && (type != ReminderService.TYPE_RANDOM)) {
            notification.audioStreamType = AudioManager.STREAM_ALARM;
            if (maxOutVolumeForMultipleRingReminders) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }

            // insistent rings until notification is disabled
            if (ringTimes < 0) {
                notification.flags |= Notification.FLAG_INSISTENT;
                voiceReminder = false;
            }

        } else {
            notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
        }

        boolean soundIntervalOk = checkLastNotificationSound();

        // quiet hours = no sound
        if (quietHours || callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.sound = null;
            voiceReminder = false;
        } else {
            String notificationPreference = preferences.getStringValue(R.string.p_rmd_ringtone);
            if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) {
                notification.sound = null;
                voiceReminder = false;
            } else if (notificationPreference != null) {
                if (notificationPreference.length() > 0 && soundIntervalOk) {
                    notification.sound = Uri.parse(notificationPreference);
                } else {
                    notification.sound = null;
                }
            } else if (soundIntervalOk) {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        // quiet hours && ! due date or snooze = no vibrate
        if (quietHours && !(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_SNOOZE)) {
            notification.vibrate = null;
        } else if (callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.vibrate = null;
        } else {
            if (preferences.getBoolean(R.string.p_rmd_vibrate, true)
                    && audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION) && soundIntervalOk) {
                notification.vibrate = new long[]{0, 1000, 500, 1000, 500, 1000};
            } else {
                notification.vibrate = null;
            }
        }

        singleThreadVoicePool.submit(new NotificationRunnable(ringTimes, notificationId, notification, voiceReminder,
                maxOutVolumeForMultipleRingReminders, audioManager, previousAlarmVolume, text,
                notificationManager, voiceOutputAssistant));
    }

    private static class NotificationRunnable implements Runnable {
        private final int ringTimes;
        private final int notificationId;
        private final Notification notification;
        private final boolean voiceReminder;
        private final boolean maxOutVolumeForMultipleRingReminders;
        private final AudioManager audioManager;
        private final int previousAlarmVolume;
        private final String text;
        private NotificationManager notificationManager;
        private final VoiceOutputAssistant voiceOutputAssistant;

        public NotificationRunnable(int ringTimes, int notificationId, Notification notification,
                                    boolean voiceReminder, boolean maxOutVolume,
                                    AudioManager audioManager, int previousAlarmVolume,
                                    String text, NotificationManager notificationManager,
                                    VoiceOutputAssistant voiceOutputAssistant) {
            this.ringTimes = ringTimes;
            this.notificationId = notificationId;
            this.notification = notification;
            this.voiceReminder = voiceReminder;
            this.maxOutVolumeForMultipleRingReminders = maxOutVolume;
            this.audioManager = audioManager;
            this.previousAlarmVolume = previousAlarmVolume;
            this.text = text;
            this.notificationManager = notificationManager;
            this.voiceOutputAssistant = voiceOutputAssistant;
        }

        @Override
        public void run() {
            for (int i = 0; i < Math.max(ringTimes, 1); i++) {
                notificationManager.notify(notificationId, notification);
                AndroidUtilities.sleepDeep(500);
            }
            Flags.set(Flags.REFRESH); // Forces a reload when app launches
            if (voiceReminder || maxOutVolumeForMultipleRingReminders) {
                AndroidUtilities.sleepDeep(2000);
                for (int i = 0; i < 50; i++) {
                    AndroidUtilities.sleepDeep(500);
                    if (audioManager.getMode() != AudioManager.MODE_RINGTONE) {
                        break;
                    }
                }
                try {
                    // first reset the Alarm-volume to the value before it was eventually maxed out
                    if (maxOutVolumeForMultipleRingReminders) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
                    }
                    if (voiceReminder) {
                        voiceOutputAssistant.speak(text);
                    }
                } catch (VerifyError e) {
                    // unavailable
                }
            }
        }
    }
}
