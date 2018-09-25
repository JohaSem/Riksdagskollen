package oscar.riksdagskollen.Util.Job;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.volley.VolleyError;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oscar.riksdagskollen.Activity.MotionActivity;
import oscar.riksdagskollen.R;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.Callback.PartyDocumentCallback;
import oscar.riksdagskollen.Util.JSONModel.PartyDocument;

/**
 * Created by gustavaaro on 2018-09-24.
 */

public class CheckRepliesJob extends Job {

    public static final String TAG = "job_alert_replies";
    public static final String REPLIES_CHANNEL = "replies_channel";
    public static final String DOK_ID = "doc_id";

    private CountDownLatch countDownLatch;

    @Override
    @NonNull
    protected Result onRunJob(Params params) {
        // Cancel job if no alerts are active
        if (!RiksdagskollenApp.getInstance().getAlertManager().hasAlerts()) cancel();

        countDownLatch = new CountDownLatch(1);

        for (String dokId : RiksdagskollenApp.getInstance().getAlertManager().getReplyAlerts()) {
            getDocument(dokId, countDownLatch);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }

        return Result.SUCCESS;
    }

    // Search for document trough documentid
    private void getDocument(String docid, final CountDownLatch countDownLatch) {
        RiksdagskollenApp.getInstance().getRiksdagenAPIManager().getDocument(docid, new PartyDocumentCallback() {
            @Override
            public void onDocumentsFetched(List<PartyDocument> documents) {
                checkForReplies(documents.get(0));
            }

            @Override
            public void onFail(VolleyError error) {
                countDownLatch.countDown();
            }
        });
    }

    // Check for reply document trough documentid

    private void checkForReplies(final PartyDocument document) {
        RiksdagskollenApp.getInstance().getRiksdagenAPIManager().searchForReply(document, new PartyDocumentCallback() {
            @Override
            public void onDocumentsFetched(List<PartyDocument> documents) {
                showNotification(documents.get(0));
                RiksdagskollenApp.getInstance().getAlertManager().setAlertEnabledForDoc(document, false);
                countDownLatch.countDown();
            }

            @Override
            public void onFail(VolleyError error) {
                countDownLatch.countDown();
            }
        });
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        countDownLatch.countDown();
    }

    public static void scheduleJob() {
        // Check once an hour
        new JobRequest.Builder(CheckRepliesJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(60), JobRequest.MIN_FLEX)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    private void showNotification(PartyDocument document) {

        Intent intent = new Intent(getContext(), MotionActivity.class);
        intent.putExtra("document", document);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notifikationer för svar på frågor";
            String description = "Visar notifikationer när en fråga som användaren bevakar har blivit besvarad.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(REPLIES_CHANNEL, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager1 = getContext().getSystemService(NotificationManager.class);
            notificationManager1.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext(), REPLIES_CHANNEL)
                .setSmallIcon(R.drawable.riksdagen_logo_yellow)
                .setContentTitle("Svar på fråga: " + document.getTitel())
                .setContentText("Klicka för att läsa svaret")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Klicka för att läsa svaret"))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        notificationManager.notify(Integer.valueOf(document.getBeteckning()), mBuilder.build());
    }
}