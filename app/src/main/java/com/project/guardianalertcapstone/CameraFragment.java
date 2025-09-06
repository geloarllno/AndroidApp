package com.project.guardianalertcapstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.app.Activity;

public class CameraFragment extends Fragment {

    private WebView cameraWebView;
    private ProgressBar progressBar;
    private ImageButton fullscreenButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraWebView = view.findViewById(R.id.camera_webview);
        progressBar = view.findViewById(R.id.stream_progress_bar);
        fullscreenButton = view.findViewById(R.id.fullscreen_button);

        WebSettings webSettings = cameraWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // okay for internal camera stream
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        cameraWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        final String esp32CamUrl = "http://172.20.10.6"; // replace with your dynamic IP if needed
        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0' /></head>" +
                "<body style='margin:0;padding:0;overflow:hidden;'>" +
                "<img src='" + esp32CamUrl + "' style='width:100%;height:auto;' /></body></html>";

        cameraWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        fullscreenButton.setOnClickListener(v -> toggleFullscreen());

        return view;
    }

    private void toggleFullscreen() {
        Activity activity = getActivity();
        if (activity != null) {
            Window window = activity.getWindow();
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
