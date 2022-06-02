package me.cl.lingxi.module.mine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.List;

import me.cl.library.base.BaseActivity;
import me.cl.library.util.ToolbarUtil;
import me.cl.library.view.LoadingDialog;
import me.cl.library.view.MoeToast;
import me.cl.lingxi.R;
import me.cl.lingxi.common.config.Api;
import me.cl.lingxi.common.config.Constants;
import me.cl.lingxi.common.okhttp.OkUtil;
import me.cl.lingxi.common.okhttp.ResultCallback;
import me.cl.lingxi.common.result.Result;
import me.cl.lingxi.common.util.ContentUtil;
import me.cl.lingxi.common.util.ImageUtil;
import me.cl.lingxi.common.util.SPUtil;
import me.cl.lingxi.databinding.PersonalInfoActivityBinding;
import me.cl.lingxi.dialog.EditTextDialog;
import me.cl.lingxi.entity.UserInfo;
import me.cl.lingxi.viewmodel.UploadViewModel;
import me.cl.lingxi.viewmodel.UserViewModel;
import me.iwf.photopicker.PhotoPicker;
import me.iwf.photopicker.PhotoPreview;
import okhttp3.Call;

/**
 * 用户资料
 */
public class PersonalInfoActivity extends BaseActivity implements View.OnClickListener {

    private static final int PHOTO_REQUEST_CUT = 456;

    private PersonalInfoActivityBinding mBinding;
    private UploadViewModel mUploadViewModel;
    private UserViewModel mUserViewModel;

    private String mUserId;
    private String saveName;
    private String mImagePath;

    private LoadingDialog loadingProgress;

    // 用户更新的参数
    private String username;
    private String avatar;
    private Integer sex;
    private String qq;
    private String signature;

    // 是否为更新头像
    private boolean isUpdateAvatar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = PersonalInfoActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        init();
    }

    private void init() {
        mBinding.personImg.setOnClickListener(this);
        mBinding.personName.setOnClickListener(this);
        mBinding.userSignature.setOnClickListener(this);

        ToolbarUtil.init(mBinding.includeTb.toolbar, this)
                .setTitle(R.string.title_bar_personal_info)
                .setBack()
                .setTitleCenter(R.style.AppTheme_Toolbar_TextAppearance)
                .build();

        loadingProgress = new LoadingDialog(this, R.string.dialog_update_avatar);

        int x = (int) (Math.random() * 5) + 1;
        if (x == 1) {
            MoeToast.makeText(this, R.string.egg_who_is_there);
        }

        mUserId = SPUtil.build().getString(Constants.SP_USER_ID);
        saveName = SPUtil.build().getString(Constants.SP_USER_NAME);
        mBinding.personName.setText(saveName);

        initViewModel();
        mUserViewModel.doUserInfo();
    }

    private void initViewModel() {
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        mUploadViewModel = viewModelProvider.get(UploadViewModel.class);
        mUserViewModel = viewModelProvider.get(UserViewModel.class);
        mUploadViewModel.mPhoto.observe(this, photo -> {
            if (TextUtils.isEmpty(photo)) {
                showUserImageUpdateError();
            } else {
                avatar = photo;
                isUpdateAvatar = true;
                postUpdateUserInfo();
            }
        });
        mUserViewModel.mUserInfo.observe(this, userInfo -> {
            if (userInfo == null) {
                onBackPressed();
            } else {
                setUserInfo(userInfo);
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.person_img:
                PhotoPicker.builder()
                        .setPhotoCount(1)
                        .setShowCamera(true)
                        .setShowGif(false)
                        .setPreviewEnabled(false)
                        .start(PersonalInfoActivity.this, PhotoPicker.REQUEST_CODE);
                break;
            case R.id.user_signature:
                EditTextDialog editTextDialog = EditTextDialog.newInstance("修改个性签名", mBinding.userSignature.getText().toString(), 60);
                editTextDialog.show(getSupportFragmentManager(), "edit");
                editTextDialog.setPositiveListener(value -> {
                    if (!TextUtils.isEmpty(value)) {
                        isUpdateAvatar = false;
                        signature = value;
                        postUpdateUserInfo();
                    }
                });
                break;
            default:
                showToast("暂不支持修改");
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // 图片选择
        if (requestCode == PhotoPicker.REQUEST_CODE || requestCode == PhotoPreview.REQUEST_CODE) {
            List<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
            }
            if (photos != null) {
                String photo = photos.get(0);
                Uri uri = ImageUtil.getFileUri(this, new File(photo));
                String imagePath = ImageUtil.getImagePath();
                mImagePath = imagePath;
                int size = 240;
                Intent intent = ImageUtil.callSystemCrop(uri, imagePath, size);
                startActivityForResult(intent, PHOTO_REQUEST_CUT);
            }
        }

        // 图片裁剪
        if (requestCode == PHOTO_REQUEST_CUT) {
            ContentUtil.loadAvatar(mBinding.personImg, mImagePath);
            postUserImage();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 上传用户头像
     */
    private void postUserImage() {
        File file = new File(mImagePath);
        if (!file.exists()) {
            showUserImageUpdateError();
            return;
        }
        mUploadViewModel.uploadUserImage(file);
    }

    /**
     * 更新用户信息
     */
    private void postUpdateUserInfo() {
        OkUtil.post()
                .url(Api.updateUser)
                .addParam("id", mUserId)
                .addParam("username", username)
                .addParam("avatar", avatar)
                .addParam("signature", signature)
                .execute(new ResultCallback<Result<UserInfo>>() {

                    @Override
                    public void onSuccess(Result<UserInfo> response) {
                        if ("00000".equals(response.getCode())) {
                            showUserUpdateSuccess();
                            setUserInfo(response.getData());
                        } else {
                            showUserUpdateError();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        showUserUpdateError();
                    }
                });
    }

    /**
     * 设置用户信息
     */
    private void setUserInfo(UserInfo userInfo) {
        if (isUpdateAvatar) {
            isUpdateAvatar = false;
            File file = new File(mImagePath);
            if (file.exists()) {
                boolean delete = file.delete();
                Log.d(TAG, "setUserInfo: delete file " + delete);
            }
        }

        ContentUtil.loadUserAvatar(mBinding.personImg, userInfo.getAvatar());

        if (!TextUtils.isEmpty(userInfo.getUsername())) {
            mBinding.personName.setText(userInfo.getUsername());
        }

        if (!TextUtils.isEmpty(userInfo.getSignature())) {
            signature = userInfo.getSignature();
            mBinding.userSignature.setText(userInfo.getSignature());
        }

        cleanData();
        notifyUpdateUserInfo();
    }

    /**
     * 清除数据
     */
    private void cleanData() {
        avatar = null;
        username = null;
        sex = null;
        qq = null;
        signature = null;
    }

    /**
     * 通知更新用户信息
     */
    private void notifyUpdateUserInfo() {
        Intent intent = new Intent();
        intent.setPackage(getPackageName());
        intent.setAction(Constants.UPDATE_USER_INFO);
        sendBroadcast(intent);
    }

    /**
     * 提示头像修改失败
     */
    private void showUserImageUpdateError() {
        showToast("更新头像失败");
    }

    /**
     * 提示用户信息更新失败
     */
    private void showUserUpdateSuccess() {
        showToast("更新用户信息成功");
    }

    /**
     * 提示用户信息更新失败
     */
    private void showUserUpdateError() {
        showToast("更新用户信息失败");
    }
}
