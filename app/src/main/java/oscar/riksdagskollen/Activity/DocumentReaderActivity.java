package oscar.riksdagskollen.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

import oscar.riksdagskollen.DebateView.DebateActivity;
import oscar.riksdagskollen.Manager.AlertManager;
import oscar.riksdagskollen.Manager.AnalyticsManager;
import oscar.riksdagskollen.Manager.SavedDocumentManager;
import oscar.riksdagskollen.R;
import oscar.riksdagskollen.RepresentativeList.data.Representative;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.Helper.CustomTabs;
import oscar.riksdagskollen.Util.JSONModel.PartyDocument;
import oscar.riksdagskollen.Util.RiksdagenCallback.OnDocumentHtmlViewLoadedCallback;
import oscar.riksdagskollen.Util.RiksdagenCallback.PartyDocumentCallback;
import oscar.riksdagskollen.Util.RiksdagenCallback.RepresentativeCallback;
import oscar.riksdagskollen.Util.View.DocumentHtmlView;

import static oscar.riksdagskollen.DebateView.DebateViewPresenter.DEBATE_INITIATOR_ID;
import static oscar.riksdagskollen.DebateView.DebateViewPresenter.INITIATING_DOCUMENT;
import static oscar.riksdagskollen.DebateView.DebateViewPresenter.SHOW_INITIATING_DOCUMENT;

/**
 * Created by gustavaaro on 2018-03-29.
 */

public class DocumentReaderActivity extends AppCompatActivity {

    private PartyDocument document;
    private ViewGroup loadingView;
    private Context context;
    private LinearLayout portaitContainer;
    private MenuItem notificationItem;
    private boolean isSaved = false;
    private boolean isNotificationsEnabled = false;
    private SavedDocumentManager savedDocumentManager;
    RiksdagskollenApp app;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(RiksdagskollenApp.getInstance().getThemeManager().getCurrentTheme(true));
        setContentView(R.layout.activity_motion);
        app = RiksdagskollenApp.getInstance();
        savedDocumentManager = SavedDocumentManager.getInstance();
        document = getIntent().getParcelableExtra("document");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(RiksdagskollenApp.getColorFromAttribute(R.attr.mainBackgroundColor, this));
        }
        AnalyticsManager.getInstance().logMessage("Document opened: " + document.toString());

        String intresentId = getIntent().getStringExtra("intressent");

        TextView titleTV = findViewById(R.id.act_doc_reader_title);
        TextView authorTV = findViewById(R.id.act_doc_reader_author);
        loadingView = findViewById(R.id.loading_view);
        context = this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(document.getDokumentnamn());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(!document.isMotion()){
            titleTV.setVisibility(View.GONE);
            authorTV.setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.GONE);
        } else {
            titleTV.setText(document.getTitel());
            authorTV.setText(document.getUndertitel());
        }
        DocumentHtmlView documentHtmlView = findViewById(R.id.webview);
        documentHtmlView.setDocument(document);
        documentHtmlView.setLoadedCallack(new OnDocumentHtmlViewLoadedCallback() {
            @Override
            public void onDocumentLoaded() {
                loadingView.setVisibility(View.GONE);
            }
        });
        //setUpWebView();
        portaitContainer = findViewById(R.id.act_doc_reader_portrait_container);


        if (document.getDokintressent() == null && intresentId != null) addSenderView(intresentId);
        else if (document.getDokintressent() != null) showSenders();
        else {
            findViewById(R.id.act_doc_reader_label_author).setVisibility(View.GONE);
        }

        if (document.getDoktyp() != null && document.getDoktyp().equals("ip") && document.getDebatt() != null && document.getDebattdag() != null) {

            Button replyButton = findViewById(R.id.reply_button);
            replyButton.setVisibility(View.VISIBLE);
            replyButton.setText("Denna interpellation har behandlats i en debatt i riksdagen: Läs debatt");
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, DebateActivity.class);
                    intent.putExtra(SHOW_INITIATING_DOCUMENT, false);
                    intent.putExtra(INITIATING_DOCUMENT, document);
                    if (document.getSenders().size() > 0)
                        intent.putExtra(DEBATE_INITIATOR_ID, document.getSenders().get(0));
                    startActivity(intent);
                }
            });

        }

        //ONLINE STUFF
        //setupRateFunctionality();

    }

    private void searchForReplyOrQuestion() {
        //Tries to find a response to the document if the doc has type = "fr"
        if (document.getTyp() != null) {
            if (document.getTyp().equals("fr")) {
                app.getRiksdagenAPIManager().searchForReply(document, new PartyDocumentCallback() {
                    @Override
                    public void onDocumentsFetched(List<PartyDocument> documents) {
                        final PartyDocument reply = documents.get(0);
                        Button replyButton = findViewById(R.id.reply_button);
                        replyButton.setVisibility(View.VISIBLE);
                        replyButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(context, DocumentReaderActivity.class);
                                intent.putExtra("document", (reply));
                                startActivity(intent);
                            }
                        });
                        app.getAlertManager().setAlertEnabledForDoc(document, false);
                    }

                    @Override
                    public void onFail(VolleyError error) {
                        notificationItem.setVisible(true);
                        findViewById(R.id.notification_tip).setVisibility(View.VISIBLE);
                    }
                });
            } else if (document.getTyp().equals("frs")) {
                app.getRiksdagenAPIManager().searchForQuestion(document, new PartyDocumentCallback() {
                    @Override
                    public void onDocumentsFetched(List<PartyDocument> documents) {
                        final PartyDocument reply = documents.get(0);
                        Button replyButton = findViewById(R.id.reply_button);
                        replyButton.setText("Läs fråga");
                        replyButton.setVisibility(View.VISIBLE);
                        replyButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(context, DocumentReaderActivity.class);
                                intent.putExtra("document", (reply));
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onFail(VolleyError error) {
                    }
                });
            }
        }
    }

    private void showSenders() {
        for (String sender : document.getSenders()) {
            addSenderView(sender);
        }
    }


    private void addSenderView(String iid) {
        final View portraitView = LayoutInflater.from(this).inflate(R.layout.intressent_layout, null);
        final NetworkImageView portrait = portraitView.findViewById(R.id.intressent_portait);
        portrait.setDefaultImageResId(R.drawable.ic_person);
        final TextView nameTv = portraitView.findViewById(R.id.intressent_name);

        final AppCompatActivity activity = this;
        RiksdagskollenApp.getInstance().getRiksdagenAPIManager().getRepresentative(iid, new RepresentativeCallback() {
            @Override
            public void onPersonFetched(final Representative representative) {
                portrait.setImageUrl(representative.getBild_url_192(), RiksdagskollenApp.getInstance().getRequestManager().getmImageLoader());
                portraitView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent repDetailsIntent = new Intent(activity, RepresentativeDetailActivity.class);
                        repDetailsIntent.putExtra("representative", representative);
                        startActivity(repDetailsIntent);
                    }
                });
                nameTv.setText(representative.getTilltalsnamn() + " " + representative.getEfternamn() + " (" + representative.getParti() + ")");

            }

            @Override
            public void onFail(VolleyError error) {

            }
        });
        portaitContainer.addView(portraitView);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        searchForReplyOrQuestion();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.motion_activity_menu,menu);

        notificationItem = menu.findItem(R.id.notification_menu_item);
        isNotificationsEnabled = AlertManager.getInstance().isAlertEnabledForDoc(document);
        if (isNotificationsEnabled) {
            notificationItem.setIcon(R.drawable.ic_notification_enabled);
        }

        isSaved = savedDocumentManager.isSaved(document.getId());
        if (isSaved) menu.findItem(R.id.save_motion).setIcon(R.drawable.ic_star_filled);
        else menu.findItem(R.id.save_motion).setIcon(R.drawable.ic_star_border);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                finish();
                break;
            case R.id.open_in_web:
                CustomTabs.openTab(this, Uri.parse("http:" + document.getDokument_url_html()).toString());
                break;
            case R.id.notification_menu_item:
                isNotificationsEnabled = RiksdagskollenApp.getInstance().getAlertManager().toggleEnabledForDoc(document);
                if (isNotificationsEnabled) {
                    Toast.makeText(getApplicationContext(), "Du kommer nu få en notis när ett svar publiceras på denna fråga", Toast.LENGTH_LONG).show();
                    item.setIcon(R.drawable.notifications_border_to_filled_animated);
                } else {
                    item.setIcon(R.drawable.notifications_filled_to_border_animated);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((Animatable) item.getIcon()).start();
                    }
                });
                break;
            case R.id.save_motion:
                if (isSaved) {
                    savedDocumentManager.unSave(document.getId());
                    item.setIcon(R.drawable.star_filled_to_border_animated);
                } else {
                    item.setIcon(R.drawable.star_border_to_filled_animated);
                    savedDocumentManager.save(document.getId());
                }
                isSaved = !isSaved;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((Animatable) item.getIcon()).start();
                    }
                });
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class CustomWebViewClient extends WebViewClient{

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Kunde inte hämta motionen");
            builder.setMessage("Motionen kunde inte hämtas. Vill du öppna i webbläsaren istället?");
            builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http:"+document.getDokument_url_html()));
                    startActivity(i);
                }
            });
            builder.setNegativeButton("Nej", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((DocumentReaderActivity) context).finish();
                }
            });
            builder.show();
        }
    }

    //For html debugging purposes
    /*
    private void writeToFile(String data) {

        try {
            System.out.println("Trying to write to SD Card");
            File myFile = new File(Environment.getExternalStorageDirectory() + "/config.html");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter =new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();

            Toast.makeText(this,"Done writing SD 'mysdfile.txt'", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {
            System.out.println("Failed to write " + e.getMessage());
            Toast.makeText(this, e.getMessage(),Toast.LENGTH_SHORT).show();
        }

    }*/

    /*private void setupRateFunctionality() {
        RateDocumentView rateView = findViewById(R.id.activity_motion_givelikeview);
        final ApprovalBarView approvalBar = findViewById(R.id.activity_motion_likebar);
        if (document.getDoktyp().equals("mot")) {
            rateView.setLikeListener(new RateDocumentView.ApprovalListener() {
                @Override
                public void onApprove() {
                    approvalBar.like();
                    //FUTURE API POST?
                }

                @Override
                public void onDisapprove() {
                    approvalBar.dislike();
                    //FUTURE API POST?
                }
            });
        } else {
            approvalBar.setVisibility(View.GONE);
            rateView.setVisibility(View.GONE);
        }
    }*/

}
