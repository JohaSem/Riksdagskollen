package oscar.riksdagskollen.Activity;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import oscar.riksdagskollen.Manager.AnalyticsManager;
import oscar.riksdagskollen.Manager.SavedDocumentManager;
import oscar.riksdagskollen.R;
import oscar.riksdagskollen.RiksdagskollenApp;
import oscar.riksdagskollen.Util.RiksdagenCallback.StringRequestCallback;


/**
 * Created by oscar on 2018-06-07.
 *
 */

public class ProtocolReaderActivity extends AppCompatActivity {

    private String url;
    private String title;
    private String docID;
    private boolean isSaved = false;


    private SavedDocumentManager savedDocumentManager;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(RiksdagskollenApp.getInstance().getThemeManager().getCurrentTheme(true));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_reader);
        savedDocumentManager = SavedDocumentManager.getInstance();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(RiksdagskollenApp.getColorFromAttribute(R.attr.mainBackgroundColor, this));
        }


        url = getIntent().getStringExtra("url");
        title = getIntent().getStringExtra("title");
        docID = getIntent().getStringExtra("id");


        AnalyticsManager.getInstance().logMessage("Opened protocol: " + title);

        final ViewGroup loadingView = findViewById(R.id.loading_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(title);

        final WebView webView = findViewById(R.id.news_reader_webview);
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, final int newProgress) {
                if(newProgress == 100){
                    loadingView.setVisibility(View.GONE);
                }
            }
        });
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptEnabled(true);


        final RiksdagskollenApp app = RiksdagskollenApp.getInstance();
        app.getRequestManager().getDownloadString("http:" + url, new StringRequestCallback() {
            @Override
            public void onResponse(String response) {
                Document doc = Jsoup.parse(response);

                doc.head().append("<meta name=\"viewport\" content='width=device-width, initial-scale=1.0,text/html, charset='utf-8'>\n");
                doc.head().appendElement("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", app.getThemeManager().getCurrentTheme().getCss());
                doc.select("div>span.sidhuvud_publikation").remove();
                doc.select("div>span.sidhuvud_beteckning").remove();
                doc.select("div>span.MotionarLista").remove();
                doc.select("div.pconf>h1").remove();
                doc.select("div>hr.sidhuvud_linje").remove();
                doc.select("head>style").remove();
                doc.select("body>div>br").remove();
                doc.select("body>div>style").remove();
                doc.select("body>style").remove();


                //Clear default styling
                //String result  = doc.toString().replaceAll("class=\\\"[A-Öa-ö0-9]+\\\"","");
                String result = doc.toString().replaceAll("style=\"[A-Öa-ö-_:;\\s0-9.%'#:space:/]+\"","");
                result = result.replaceAll("&nbsp;","");
                webView.loadDataWithBaseURL("file:///android_asset/", result, "text/html", "UTF-8", null);
            }

            @Override
            public void onFail(VolleyError error) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.motion_activity_menu, menu);
        isSaved = savedDocumentManager.isSaved(docID);
        if (isSaved) menu.findItem(R.id.save_motion).setIcon(R.drawable.ic_star_filled);
        else menu.findItem(R.id.save_motion).setIcon(R.drawable.ic_star_border);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.open_in_web:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http:" + url));
                startActivity(i);
                break;
            case R.id.save_motion:
                if (isSaved) {
                    savedDocumentManager.unSave(docID);
                    item.setIcon(R.drawable.star_filled_to_border_animated);
                } else {
                    item.setIcon(R.drawable.star_border_to_filled_animated);
                    savedDocumentManager.save(docID);
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


    // For debug purposes in the future
    /*
    private void writeToFile(String data) {

            try {
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
                Toast.makeText(this, e.getMessage(),Toast.LENGTH_SHORT).show();
            }

    }*/


}
