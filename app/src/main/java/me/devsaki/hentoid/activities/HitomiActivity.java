package me.devsaki.hentoid.activities;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.parsers.HitomiParser;
import me.devsaki.hentoid.util.ConstsPrefs;
import me.devsaki.hentoid.util.Helper;
import me.devsaki.hentoid.views.ObservableWebView;
import timber.log.Timber;

import static me.devsaki.hentoid.util.Helper.TYPE;
import static me.devsaki.hentoid.util.Helper.executeAsyncTask;
import static me.devsaki.hentoid.util.Helper.getWebResourceResponseFromAsset;
import static me.devsaki.hentoid.util.Helper.getWebViewInitialZoomPrefs;
import static me.devsaki.hentoid.util.Helper.getWebViewOverviewPrefs;

/**
 * Created by Shiro on 1/20/2016.
 * Implements Hitomi.la source
 */
public class HitomiActivity extends BaseWebActivity {

    @Override
    void setSite(Site site) {
        super.setSite(Site.HITOMI);
    }

    @Override
    void setWebView(ObservableWebView webView) {
        HitomiWebViewClient client = new HitomiWebViewClient();
        client.restrictTo("hitomi.la");

        webView.setWebViewClient(client);

        boolean bWebViewOverview = getWebViewOverviewPrefs();
        int webViewInitialZoom = getWebViewInitialZoomPrefs();

        if (bWebViewOverview) {
            webView.getSettings().setLoadWithOverviewMode(false);
            webView.setInitialScale(webViewInitialZoom);
            Timber.d("WebView Initial Scale: %s%", webViewInitialZoom);
        } else {
            webView.setInitialScale(ConstsPrefs.PREF_WEBVIEW_INITIAL_ZOOM_DEFAULT);
            webView.getSettings().setLoadWithOverviewMode(true);
        }

        super.setWebView(webView);
    }

    @Override
    void backgroundRequest(String extra) {
        Timber.d(extra);
        Helper.toast("Processing...");
        executeAsyncTask(new HtmlLoader(), extra);
    }

    private class HitomiWebViewClient extends CustomWebViewClient {
        final ByteArrayInputStream nothing = new ByteArrayInputStream("".getBytes());

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (url.contains("//hitomi.la/galleries/")) {
                executeAsyncTask(new HtmlLoader(), url);
            }
        }

        @SuppressWarnings("deprecation") // From API 21 we should use another overload
        @Override
        public WebResourceResponse shouldInterceptRequest(@NonNull WebView view,
                                                          @NonNull String url) {
            if (url.contains("hitomi.js")) {
                return getWebResourceResponseFromAsset(getSite(), "hitomi.js", TYPE.JS);
            } else if (url.contains("hitomi-horizontal.js") || url.contains("hitomi-vertical.js")) {
                return new WebResourceResponse("text/plain", "utf-8", nothing);
            } else {
                return super.shouldInterceptRequest(view, url);
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(@NonNull WebView view,
                                                          @NonNull WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.contains("hitomi.js")) {
                return getWebResourceResponseFromAsset(getSite(), "hitomi.js", TYPE.JS);
            } else if (url.contains("hitomi-horizontal.js") || url.contains("hitomi-vertical.js")) {
                return new WebResourceResponse("text/plain", "utf-8", nothing);
            } else {
                return super.shouldInterceptRequest(view, request);
            }
        }
    }

    private class HtmlLoader extends AsyncTask<String, Integer, Content> {
        @Override
        protected Content doInBackground(String... params) {
            String url = params[0];
            try {
                processContent(HitomiParser.parseContent(url));
            } catch (IOException e) {
                Timber.e(e, "Error parsing content.");
            }

            return null;
        }
    }
}
