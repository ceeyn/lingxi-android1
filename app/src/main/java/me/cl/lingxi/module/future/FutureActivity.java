package me.cl.lingxi.module.future;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import me.cl.library.base.BaseActivity;
import me.cl.library.util.ToolbarUtil;
import me.cl.lingxi.R;
import me.cl.lingxi.common.config.Constants;
import me.cl.lingxi.common.util.SPUtil;
import me.cl.lingxi.common.util.Utils;
import me.cl.lingxi.databinding.FutureActivityBinding;
import me.cl.lingxi.dialog.FutureDialog;
import me.cl.lingxi.viewmodel.FutureViewModel;

/**
 * 写给未来
 */
public class FutureActivity extends BaseActivity {

    private FutureActivityBinding mBinding;
    private FutureViewModel mFutureViewModel;

    private String futureInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = FutureActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        futureInfo = SPUtil.build().getString(Constants.SP_FUTURE_INFO);
        if (!TextUtils.isEmpty(futureInfo)) {
            mBinding.futureInfo.setText(futureInfo);
            mBinding.futureInfo.setSelection(futureInfo.length());
        }
    }

    /**
     * 初始化
     */
    private void init() {
        ToolbarUtil.init(mBinding.includeTb.toolbar, this)
                .setTitle(R.string.title_bar_future)
                .setBack()
                .setTitleCenter(R.style.AppTheme_Toolbar_TextAppearance)
                .setMenu(R.menu.future_menu, new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_send) {
                            if (TextUtils.isEmpty(futureInfo)) {
                                showToast("没有写下任何给未来的话哟~");
                            } else {
                                showSendDialog();
                            }
                        }
                        return false;
                    }
                })
                .build();

        initListener();
        initViewModel();
    }

    private void initListener() {
        // 输入监听
        mBinding.futureInfo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                futureInfo = s.toString();
                // 实时保存
                SPUtil.build().putString(Constants.SP_FUTURE_INFO, futureInfo);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initViewModel() {
        mFutureViewModel = new ViewModelProvider(this).get(FutureViewModel.class);
        mFutureViewModel.mTipMessage.observe(this, this::showTip);
        mFutureViewModel.mSuccess.observe(this, success -> {
            showToast("信件进入时空隧道，等候传达");
            SPUtil.build().putString(Constants.SP_FUTURE_INFO, null);
            onBackPressed();
        });
    }

    /**
     * 发送配置Dialog
     */
    private void showSendDialog() {
        String tag = "sendFuture";
        // 展示dialog
        FutureDialog futureDialog = FutureDialog.newInstance();
        futureDialog.show(Utils.fragmentTransaction(getSupportFragmentManager(), tag), tag);
        futureDialog.setCancelable(false);
        futureDialog.setOnSendClickListener(this::postSaveFuture);
    }

    /**
     * 提交保存信息
     */
    private void postSaveFuture(int type, String mail, Integer startNum, Integer endNum) {
        mFutureViewModel.saveFuture(type, mail, futureInfo, startNum, endNum);
    }
}
