package org.shadowice.flocke.andotp.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.shadowice.flocke.andotp.Activities.DashboardActivity;
import org.shadowice.flocke.andotp.R;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    private Context mContext = MyFirebaseMessagingService.this;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.e(" FirebaseService ", " getData " + remoteMessage.getData());

     /*   if (remoteMessage.getData().size() > 0) {
            createNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("message"));
        }*/
        createNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("message"));

    }

    public void createNotification(String title, String message) {

        /*Creates an explicit intent for an Activity in your app*/

        Intent resultIntent = new Intent(mContext, DashboardActivity.class);
//        resultIntent.putExtra("title", title);
        resultIntent.putExtra("title", "notification");
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bitmap icon2 = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);
        } else {
            mBuilder = new NotificationCompat.Builder(mContext);
        }

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.bigText(message);

        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setLargeIcon(icon2)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setSound(defaultSoundUri)
                .setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(defaultSoundUri, audioAttributes);
            notificationChannel.setVibrationPattern(new long[]{500, 500, 500});
            assert mNotificationManager != null;
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);

        }
        assert mNotificationManager != null;
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }
}