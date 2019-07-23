package aaa.bbb.ccc.solidsnake.data;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebClient extends WebViewClient {
    private CookieManager mManager;
    private View mProgress;

    public WebClient(CookieManager manager, View progress) {
        this.mManager = manager;
        this.mProgress = progress;
    }

    public WebClient(CookieManager manager) {
        this.mManager = manager;
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        view.loadUrl(request.getUrl().toString());
        return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        this.hideProgress(false);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mManager.setAcceptCookie(true);
        this.hideProgress(true);
        view.loadUrl("javascript:AndroidFunction.showHTML(document.getElementsByTagName('html')[0].innerHTML)");
    }

    private void hideProgress(boolean b) {
        if (mProgress == null) return;
        mProgress.setVisibility(b ? View.GONE : View.VISIBLE);
    }
}
