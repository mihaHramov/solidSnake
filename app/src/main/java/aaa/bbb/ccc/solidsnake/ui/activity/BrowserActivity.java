package aaa.bbb.ccc.solidsnake.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.facebook.applinks.AppLinkData;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aaa.bbb.ccc.solidsnake.R;
import aaa.bbb.ccc.solidsnake.data.JavaScriptInterface;
import aaa.bbb.ccc.solidsnake.data.WebClient;
import aaa.bbb.ccc.solidsnake.data.api.RestApi;
import aaa.bbb.ccc.solidsnake.data.api.RestService;
import aaa.bbb.ccc.solidsnake.data.metricaSender.AppsFlyerMetricSender;
import aaa.bbb.ccc.solidsnake.data.metricaSender.FacebookMetricSender;
import aaa.bbb.ccc.solidsnake.data.metricaSender.IMetricEventSender;
import aaa.bbb.ccc.solidsnake.data.metricaSender.YandexMetricSender;
import aaa.bbb.ccc.solidsnake.model.ServerResult;
import aaa.bbb.ccc.solidsnake.model.UserEvent;
import aaa.bbb.ccc.solidsnake.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static aaa.bbb.ccc.solidsnake.ui.broadcastReceiver.InstallReferrerReceiver.REFERRER_DATA;

public class BrowserActivity extends AppCompatActivity {
    private List<IMetricEventSender> senders;
    private WebView webView;
    private Integer userId;
    private RestApi api;
    private Timer mTimer;


    private TimerTask metricSender = new TimerTask() {
        @Override
        public void run() {

//            Type listType = new TypeToken<List<UserEvent>>() {
//            }.getType();

//            List<UserEvent> events = new Gson().fromJson("[" +
//                    "{\"goal\":\"fd\"," +
//                    "\"offerid\":\"val offeridl\"," +
//                    "\"status\":\"val status\"," +
//                    "\"currency\":\"val currency\"," +
//                    "\"pid\":\"val pid\"," +
//                    "\"depsum\":3.3," +
//                    "\"sum\":2.3" + "}," +
//                    "{\"goal\":\"reg\"," +
//                    "\"offerid\":\"val offeridl\"," +
//                    "\"status\":\"status val\"," +
//                    "\"currency\":\"val currency\"," +
//                    "\"pid\":\"val pid\"," +
//                    "\"depsum\":3.3," +
//                    "\"sum\":2.3" + "}," +
//                    "{\"goal\":\"dep\"," +
//                    "\"offerid\":\"val offeridl\"," +
//                    "\"status\":\" val status\"," +
//                    "\"currency\":\"val currency\"," +
//                    "\"pid\":\" val pid\"," +
//                    "\"depsum\":3.3," +
//                    "\"sum\":2.3" +
//                    "}" +
//                    "]", listType);
            api.getUserEvent(userId).enqueue(new Callback<List<UserEvent>>() {
                @Override
                public void onResponse(@NonNull Call<List<UserEvent>> call,@NonNull Response<List<UserEvent>> response) {
                    if (response.isSuccessful()) {
                        List<UserEvent> events = response.body();
                        for (UserEvent event : events) {
                            for (IMetricEventSender sender : senders) {
                                sender.sendEvent(event);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<UserEvent>> call,@NonNull Throwable t) {

                }
            });


        }
    };

    private Callback<ServerResult> getServerResultCallback() {
        return new Callback<ServerResult>() {
            @Override
            public void onResponse(@NonNull Call<ServerResult> call,@NonNull Response<ServerResult> response) {
                if (response.isSuccessful()) {
                    String url = response.body().getResult();
                    userId = response.body().getId();
                    BrowserActivity.this.openWebPage(url);
                    BrowserActivity.this.sendUserSClick(url);
                    BrowserActivity.this.sendMetric();
                } else {

                }
            }

            @Override
            public void onFailure(Call<ServerResult> call, Throwable t) {
                BrowserActivity.this.onError(t);
            }
        };
    }

    private void sendMetric() {
        mTimer = new Timer();
        Log.d("mihaHramov", "sendMetric");
        mTimer.schedule(metricSender, 0, 1000 * 60);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        senders = Arrays.asList(
                new FacebookMetricSender(getApplicationContext()),
                new YandexMetricSender(),
                new AppsFlyerMetricSender(getApplicationContext()));

        showStatusBar(isPortrait());
        initWebView();
        api = RestService.getInstance(getString(R.string.base_url));
        AppLinkData.fetchDeferredAppLinkData(this, new AppLinkData.CompletionHandler() {
            @Override
            public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                Map<String, String> query = getFieldMap(appLinkData);
                Call<ServerResult> call = api.getSereverUrl(query);
                call.enqueue(getServerResultCallback());
            }
        });

    }

    private void onError(Throwable t) {
        //  setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private Map<String, String> getFieldMap(AppLinkData appLinkData) {
        Map<String, String> query = new Hashtable<>();
        String referrer;
        if (appLinkData != null && appLinkData.getTargetUri() != null) {
            referrer = appLinkData.getTargetUri().toString();
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            referrer = sp.getString(REFERRER_DATA, getString(R.string.base_ref));
        }
        Long time = Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis();
        query.put(Constants.ID_USER, "");
        query.put(Constants.APP_NAME, getPackageName());
        query.put(Constants.COUNTRY, getUserCountry());
        query.put(Constants.REFERRER, referrer);
        query.put(Constants.OS_VERSION, Build.VERSION.RELEASE);
        query.put(Constants.TIME_ZONE, time.toString());
        query.put(Constants.DEVICE_MODEL, android.os.Build.MODEL);
        return query;
    }

    private String getUserCountry() {
        return getResources().getConfiguration().locale.getCountry();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        showStatusBar(isPortrait());
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            mTimer.cancel();
            setResult(Activity.RESULT_CANCELED);
            super.onBackPressed();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView = findViewById(R.id.web_view);
        CookieSyncManager.createInstance(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebClient(CookieManager.getInstance(), findViewById(R.id.progress_bar)));
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JavaScriptInterface() {
            @JavascriptInterface
            @Override
            public void showHTML(String html) {
                String pattern = getString(R.string.find_email_reg);
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(html);
                while (m.find()) {
                }
            }
        }, "AndroidFunction");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // chromium, enable hardware acceleration
//            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//        } else {
//            // older android version, disable hardware acceleration
//            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        }
//        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
    }

    private Boolean isPortrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private void showStatusBar(Boolean showStatusBar) {
        if (showStatusBar) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void openWebPage(String url) {
        webView.loadUrl(url);

    }

    private void sendUserSClick(String url) {
        String sclick = Uri.parse(url).getQueryParameter(Constants.SCLICK);
        api.sendUserSClick(userId, sclick).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }
}