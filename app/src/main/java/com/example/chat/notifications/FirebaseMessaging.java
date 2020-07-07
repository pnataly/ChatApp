package com.example.chat.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.chat.ChatActivity;
import com.example.chat.model.User;
import com.example.chat.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

public class FirebaseMessaging extends FirebaseMessagingService {

    private static final String ADMIN_CHANNEL_ID = "admin_channel";
    private List<User> userPhoneList;
    private String image;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        String notificationType = remoteMessage.getData().get("notificationType");

        if(notificationType.equals(Util.MESSAGE_NOTIFICATION)){

            String sent = remoteMessage.getData().get("sent");

            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if(fUser != null && sent.equals(fUser.getUid())){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    sendNotification(remoteMessage);
                }
                else {
                    sendNormalNotification(remoteMessage);
                }
            }
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(NotificationManager notificationManager) {
        CharSequence channelName = "New Notification";
        String channelDescription = "Device to device post notification";

        NotificationChannel channel = new NotificationChannel(ADMIN_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(channelDescription);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        if(notificationManager != null){
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNormalNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String title = remoteMessage.getData().get("title");
        String icon = remoteMessage.getData().get("icon");
        String body = remoteMessage.getData().get("body");
        String phone = remoteMessage.getData().get("phone");

        getContactsList();

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        String name = Util.getUserName(userPhoneList, phone);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(user).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                   image = dataSnapshot.child("image").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("id", user);
        intent.putExtra("name", name);
        intent.putExtra("image", image);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, i, intent,  PendingIntent.FLAG_ONE_SHOT);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(uri)
                .setContentIntent(pIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String title = remoteMessage.getData().get("title");
        String icon = remoteMessage.getData().get("icon");
        String body = remoteMessage.getData().get("body");
        String phone = remoteMessage.getData().get("phone");

        getContactsList();

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        String name = Util.getUserName(userPhoneList, phone);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(user).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    image = dataSnapshot.child("image").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("id", user);
        intent.putExtra("name", name);
        intent.putExtra("image", image);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, i, intent,  PendingIntent.FLAG_ONE_SHOT);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notifications notifications = new Notifications(this);
        Notification.Builder builder = notifications.getNotifications(title, body, pIntent, uri, icon);

        notifications.getManager().notify(1, builder.build());
    }

    private void getContactsList(){
        userPhoneList = new ArrayList<>();

        String ISOPrefix = Util.getCountryISO();
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()){
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            String str = number;

            number = number.replace(" ", "");
            number = number.replace("-", "");
            number = number.replace("(", "");
            number = number.replace(")", "");

            if(!String.valueOf(number.charAt(0)).equals("+")){
                str = number.substring(1);
                str = ISOPrefix + str;
            }

            User user = new User(name, str);
            userPhoneList.add(user);
        }
    }


}
