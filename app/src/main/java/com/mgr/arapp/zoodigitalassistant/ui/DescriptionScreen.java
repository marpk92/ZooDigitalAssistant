package com.mgr.arapp.zoodigitalassistant.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.mgr.arapp.zoodigitalassistant.R;
import com.mgr.arapp.zoodigitalassistant.ar.DigitalAssistantActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Marcin on 14.07.2018.
 */

public class DescriptionScreen extends AppCompatActivity {

    private static final String LOGTAG = "DescriptionScreen";

    private WebView mDescriptionText;
    private Button mButtonStart;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description_screen);


        String webText =("description.html");
        mDescriptionText = (WebView) findViewById(R.id.desc_html_text);
        DescriptionWebViewClient descriptionWebViewClient = new DescriptionWebViewClient();
        mDescriptionText.setWebViewClient(descriptionWebViewClient);

        mButtonStart = (Button) findViewById(R.id.button_start_digital_assistant);

        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               openDigitalAssistantScreen(v);
            }
        });

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

    private void openDigitalAssistantScreen(View view){
        if (ContextCompat.checkSelfPermission(DescriptionScreen.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DescriptionScreen.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            Intent intent = new Intent(this, DigitalAssistantActivity.class);
            startActivity(intent);
        }
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
