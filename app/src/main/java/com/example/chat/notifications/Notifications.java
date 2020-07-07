package com.example.chat.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.chat.R;

public class Notifications extends ContextWrapper {

    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "channel_id";

    public Notifications(Context base) {
        super(base);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            createChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name)
                , NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(notificationChannel);
    }

    public NotificationManager getManager(){
        if(notificationManager == null){
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotifications(String title, String body, PendingIntent pIntent, Uri uri, String icon){

        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentIntent(pIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(uri)
                .setAutoCancel(true)
                .setSmallIcon(Integer.parseInt(icon));
    }

}

