package me.cl.lingxi.module.webview;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import me.cl.library.base.BaseActivity;
import me.cl.library.util.ToolbarUtil;
import me.cl.lingxi.R;
import me.cl.lingxi.databinding.WebActivityBinding;
import me.cl.lingxi.view.webview.MoeChromeClient;
import me.cl.lingxi.view.webview.MoeWebClient;

/**
 * WebActivity
 * 设置独立的web进程，与主进程隔开
 * {@code <activity android:name=".webview.WebViewActivity" android:launchMode="singleTop" android:process=":remote" android:screenOrientation="unspecified" />}
 */
public class WebActivity extends BaseActivity {

    private static final String TAG = "WebActivity";

    private WebActivityBinding mBinding;

    private String mTittle;
    private String mUrl;

    private MoeChromeClient mChromeClient;

    public static void gotoWeb(Context context, String tittle, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("tittle",tittle);
        bundle.putString("url", url);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = WebActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        init();
    }

    private void init() {
        Intent intent = getIntent();
        Bundle bundle = null;
        if (intent != null) {
            bundle = intent.getExtras();
        }
        if (bundle != null) {
            if ("text/plain".equals(intent.getType())) {
                mTittle = bundle.getString(Intent.EXTRA_TITLE);
                mUrl = bundle.getString(Intent.EXTRA_TEXT);
                mUrl = TextUtils.isEmpty(mUrl) ? "http://47.100.245.128" : mUrl;
                mUrl = mUrl.replaceAll("\t", "");
                mUrl = mUrl.replaceAll("\n", "");
                int i = mUrl.indexOf("http");
                if (TextUtils.isEmpty(mTittle)) {
                    mTittle = mUrl.substring(0, i);
                } else {
                    mTittle = "分享内容";
                }
                mUrl = mUrl.substring(i);
            } else {
                mTittle = bundle.getString("tittle");
                mUrl = bundle.getString("url");
            }
        }

        ToolbarUtil.init(mBinding.includeTb.toolbar, this)
                .setTitle(mTittle)
                .setBack()
                .setTitleCenter(R.style.AppTheme_Toolbar_TextAppearance)
                .build();

        MoeWebClient webClient = new MoeWebClient();
        mChromeClient = new MoeChromeClient(mBinding.videoView, new MoeChromeClient.onChangedListener() {
            @Override
            public void onFullscreen(boolean fullscreen) {
                if (fullscreen) {
                    mBinding.webView.setVisibility(View.GONE);
                } else {
                    mBinding.webView.setVisibility(View.VISIBLE);
                }
                setFullscreen(fullscreen);
            }
        });
        mBinding.webView.setWebViewClient(webClient);
        mBinding.webView.setWebChromeClient(mChromeClient);

        mBinding.webView.loadUrl(mUrl);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, " 现在是横屏");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, " 现在是竖屏");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (inCustomView()) {
                    hideCustomView();
                } else {
                    if (mBinding.webView.canGoBack()) {
                        mBinding.webView.goBack();
                    } else {
                        clearWebView();
                        this.finish();
                    }
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * 判断是否是全屏
     */
    public boolean inCustomView() {
        return mChromeClient.getCustomView() != null;
    }

    /**
     * 全屏时按返加键执行退出全屏方法
     */
    public void hideCustomView() {
        mChromeClient.onHideCustomView();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        resumeWebView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        pauseWebView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        clearWebView();
        super.onDestroy();
        // 因为独立的web进程，与主进程隔开，在关闭WebActivity时销毁进程
        System.exit(0);
    }

    private void pauseWebView() {
        mBinding.webView.onPause();
        mBinding.webView.pauseTimers();
    }

    private void resumeWebView() {
        mBinding.webView.resumeTimers();
        mBinding.webView.onResume();
    }

    private void clearWebView() {
        mBinding.webView.clearHistory();
        mBinding.webView.clearCache(true);
        mBinding.webView.loadUrl("about:blank");
        mBinding.webView.pauseTimers();
    }

    @Override
    public void finish() {
        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        view.removeAllViews();
        super.finish();
    }
}
