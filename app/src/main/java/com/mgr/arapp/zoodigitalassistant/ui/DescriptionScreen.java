package com.mgr.arapp.zoodigitalassistant.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mgr.arapp.zoodigitalassistant.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Marcin on 14.07.2018.
 */

public class DescriptionScreen extends Activity {

    private static final String LOGTAG = "DescriptionScreen";

    private WebView mDescriptionText;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description_screen);


        String webText =("description.html");
        mDescriptionText = (WebView) findViewById(R.id.desc_html_text);
        DescriptionWebViewClient descriptionWebViewClient = new DescriptionWebViewClient();
        mDescriptionText.setWebViewClient(descriptionWebViewClient);

        String descText = "";

        try
        {
            InputStream is = getAssets().open(webText);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null){
                descText += line;
            }

        } catch (IOException e)
        {
            Log.e(LOGTAG, "Description html loading failed");
        }

        mDescriptionText.loadData(descText, "text/html", "UTF-8");


    }

    private class DescriptionWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }
}
