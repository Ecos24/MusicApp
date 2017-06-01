package divyang.musicapp_v6;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Icon;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.session.MediaButtonReceiver;

class MediaStyleHelper
{
    public static Notification.Builder from(Context context, MediaSession mediaSession, Class DActivity, int state)
    {
        MediaController controller = mediaSession.getController();
        MediaMetadata mediaMetadata = controller.getMetadata();
        MediaDescription description = mediaMetadata.getDescription();

        // Code For Notification Click Event to Open Activity.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(DActivity);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(new Intent(context, DActivity));

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_ONE_SHOT);// .FLAG_UPDATE_CURRENT);
        ////////////////////////////////////////////////////////

        Notification.Builder builder = new Notification.Builder(context);
        builder
                // Add the metadata for the currently playing track
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setCategory(Notification.CATEGORY_TRANSPORT)
                // Enable launching the player by clicking the notification.
                //.setContentIntent(controller.getSessionActivity())
                // Stop the service when the notification is swiped away
                .setDeleteIntent( MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackState.ACTION_STOP))
                // Make the transport controls visible on the lock screen.
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                // Add the pending Intent to Notification
                // To Open The Desired Activity on Click Event.
                .setContentIntent(resultPendingIntent);

        // Add an app icon and set its accent color Be careful about the color
        builder.setSmallIcon(R.drawable.ic_music);
        //builder.setColor(0x2ab52ea);
        //builder.setColor(Context.getColor(this, R.color.primaryDark));

        //PorterDuffColorFilter iconColorFilter = new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);

        // SETTING NOTIFICATION BUTTONS & ACTIONS.
        if( state == 1 ) //Playing Notification.
        {
            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            {
                // Add a buttons.
                builder.addAction(new Notification.Action( R.drawable.ic_previous, context.getString(R.string.previous),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)));
                builder.addAction(new Notification.Action(R.drawable.ic_pause, context.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_PAUSE)));
                builder.addAction(new Notification.Action( R.drawable.ic_next, context.getString(R.string.next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_NEXT)));
            }
            else
            {
                // Add a buttons.
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_previous) , context.getString(R.string.previous),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)).build());
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_pause), context.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_PAUSE)).build());
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_next), context.getString(R.string.next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_NEXT)).build());
            }

            // Take advantage of MediaStyle features
            builder.setStyle( new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(1,2) );

            //Set's the priority of the notification as maximum.
            builder.setPriority(Notification.PRIORITY_MAX);
        }
        else if( state == 0 ) //Paused Notification.
        {
            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            {
                // Add a buttons.
                builder.addAction(new Notification.Action( R.drawable.ic_previous, context.getString(R.string.previous),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)));
                builder.addAction(new Notification.Action(R.drawable.ic_tinted_action_button, context.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,PlaybackState.ACTION_PLAY)));
                builder.addAction(new Notification.Action( R.drawable.ic_next, context.getString(R.string.next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_NEXT)));
            }
            else
            {
                // Add a buttons.
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_previous) , context.getString(R.string.previous),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)).build());
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_play), context.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_PLAY)).build());
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context,
                        R.drawable.ic_next), context.getString(R.string.next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackState.ACTION_SKIP_TO_NEXT)).build());
            }

            // Take advantage of MediaStyle features
            builder.setStyle( new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(1) );

            // Try to make notification removable without rebuilding it.
            //builder.setOngoing(false);
        }

        return builder;
    }
}
