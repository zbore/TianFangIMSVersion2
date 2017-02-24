package com.tianfangIMS.im.fragment;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.BitmapCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.request.BaseRequest;
import com.tianfangIMS.im.ConstantValue;
import com.tianfangIMS.im.R;
import com.tianfangIMS.im.bean.OneGroupBean;
import com.tianfangIMS.im.dialog.LoadDialog;

import net.qiujuer.genius.blur.StackBlur;

import java.util.List;
import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imkit.userInfoCache.RongUserInfoManager;
import io.rong.imkit.utilities.PermissionCheckUtil;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;
import io.rong.ptt.JoinSessionCallback;
import io.rong.ptt.PTTClient;
import io.rong.ptt.PTTSession;
import io.rong.ptt.PTTSessionStateListener;
import io.rong.ptt.RequestToSpeakCallback;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by LianMengYu on 2017/2/9.
 * 对讲fragment
 */

public class IntercomFragment extends BaseFragment implements View.OnClickListener, PTTSessionStateListener {
    public static IntercomFragment Instance = null;
    private List<String> participants;
    private PTTClient pttClient;
    public static IntercomFragment getInstance() {
        if (Instance == null) {
            Instance = new IntercomFragment();
        }
        return Instance;
    }

    ImageView main_call_blur;
    ImageView main_call_header;
    private Conversation.ConversationType mConversationType;
    ImageView main_call_free, main_call_flash, main_call_talk;
    private String userid;
    private UserInfo userInfo;
    private TextView intercom_name;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.intercom_layout, container, false);
        Intent intent = getActivity().getIntent();
        main_call_blur = (ImageView) view.findViewById(R.id.main_call_blur);
        main_call_header = (ImageView) view.findViewById(R.id.main_call_header);
        main_call_free = (ImageView) view.findViewById(R.id.main_call_free);
        main_call_flash = (ImageView) view.findViewById(R.id.main_call_flash);
        main_call_talk = (ImageView) view.findViewById(R.id.main_call_talk);
        intercom_name = (TextView) view.findViewById(R.id.intercom_name);
        setListener();
//        main_call_blur.setImageBitmap(blur(getBitmapFromUri(userInfo.getPortraitUri()), 25f));
        mConversationType = Conversation.ConversationType.valueOf(intent.getData()
                .getLastPathSegment().toUpperCase(Locale.getDefault()));

        userid = intent.getData().getQueryParameter("targetId");
        //获取userinfo
        if (mConversationType == Conversation.ConversationType.PRIVATE) {
            userInfo = RongUserInfoManager.getInstance().getUserInfo(userid);
            if (userInfo != null) {
                intercom_name.setText(userInfo.getName());
                Log.e("intercom", "确实是否执行：" + userInfo.getName());
                getBitmap(userInfo.getPortraitUri().toString());
            }
        }
        if (mConversationType == Conversation.ConversationType.GROUP) {
            userid = intent.getData().getQueryParameter("targetId");
            GetGroupUserInfo();
            Log.e("intercom", "群组id" + userid);
        }
        if (!PermissionCheckUtil.checkPermissions(getActivity(), new String[]{android.Manifest.permission.RECORD_AUDIO})) {
            PermissionCheckUtil.requestPermissions(getInstance(), new String[]{Manifest.permission.RECORD_AUDIO});
        }
        PTTClient.setPTTServerBaseUrl("http://35.164.107.27:8080/rce/restapi/ptt");
        pttClient = PTTClient.getInstance();
        pttClient.init(getActivity());
        Log.e("debugPTT","打印一个数据类型："+mConversationType+"打印userid："+userid);
//        PTTClient pttKitManager = PTTClient.getInstance();
        pttClient.joinSession(mConversationType, userid, new JoinSessionCallback() {
            @Override
            public void onSuccess(List<String> list) {
                Log.e("OnSuccess", "测试对讲是否连接成功");
                PTTSession pttSession = pttClient.getCurrentPttSession();
                participants = pttSession.getParticipantIds();
                pttClient.setPttSessionStateListener(getInstance());
            }

            @Override
            public void onError(String s) {
                Log.e("OnSuccess", "对讲链接失败:"+s);
            }
        });
        setListener();
        main_call_talk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return requestToSpeak(v, event);
            }
        });
        return view;
    }

    private void GetGroupUserInfo() {
        OkGo.post(ConstantValue.GETONEGROUPINFO)
                .tag(this)
                .connTimeOut(10000)
                .readTimeOut(10000)
                .writeTimeOut(10000)
                .params("groupid", userid)
                .execute(new StringCallback() {
                    @Override
                    public void onBefore(BaseRequest request) {
                        super.onBefore(request);
                    }

                    @Override
                    public void onSuccess(String s, Call call, Response response) {
                        if (!TextUtils.isEmpty(s) && !s.equals("{}")) {
                            Gson gson = new Gson();
                            OneGroupBean oneGroupBean = gson.fromJson(s, OneGroupBean.class);
                            intercom_name.setText(oneGroupBean.getText().getName());
                            getBitmap(ConstantValue.ImageFile + oneGroupBean.getText().getLogo());
                            Log.e("intercom", "群组成员都有什么：" + oneGroupBean.getText().getName());
                        } else {
                            Log.e("intercom", "没有获取数据：" + s);
                        }
                    }

                    @Override
                    public void onError(Call call, Response response, Exception e) {
                        super.onError(call, response, e);
                        Log.e("intercom", "返回数据错误" + call);
                    }
                });

    }

    private void getBitmap(String path) {
        OkGo.post(path)
                .tag(this)
                .execute(new BitmapCallback() {
//                    @Override
//                    public void onBefore(BaseRequest request) {
//                        super.onBefore(request);
//                        LoadDialog.show(getActivity());
//                    }

                    @Override
                    public void onSuccess(Bitmap bitmap, Call call, Response response) {
                        LoadDialog.dismiss(getActivity());
                        if (bitmap != null) {
                            Bitmap newBitmap = StackBlur.blur(bitmap, (int) 20, false);
                            main_call_blur.setImageBitmap(newBitmap);
                            main_call_header.setImageBitmap(bitmap);
                        }
                    }
                });
    }

    private void setListener() {
        main_call_free.setOnClickListener(this);
        main_call_flash.setOnClickListener(this);
        main_call_talk.setOnClickListener(this);
    }

    @Nullable
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            Log.e("[Android]", e.getMessage());
            Log.e("[Android]", "目录为：" + uri);
            e.printStackTrace();
            return null;
        }
    }

    //请求说话，抢麦
    boolean requestToSpeak(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            main_call_talk.setImageResource(R.mipmap.talk_voice_green_connect);
            pttClient.requestToSpeak(new RequestToSpeakCallback() {

                //抢麦成功
                @Override
                public void onReadyToSpeak(long maxDurationMillis) {
//                    updateMicHolder(RongIMClient.getInstance().getCurrentUserId());

                }

                //抢麦失败
                @Override
                public void onFail(String msg) {
                    RLog.e("onFail", "start speak error " + msg);
                }


                //说话超时，通过服务器设定时长，如果超过自动停止说话
                @Override
                public void onSpeakTimeOut() {
                    Toast.makeText(getActivity(), "speak time out", Toast.LENGTH_SHORT).show();
//                    updateMicHolder("");
                }
            });
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            main_call_talk.setImageResource(R.drawable.talk_voice_normal);
//            micHolderTextView.setText(getString(io.rong.ptt.kit.R.string.rce_ptt_hold_to_request_mic));
//            micHolderImageView.setImageResource(io.rong.ptt.kit.R.drawable.rc_default_portrait);
            pttClient.stopSpeak();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_call_free:
                Toast.makeText(getActivity(), "点击了免提", Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_call_flash:
                Toast.makeText(getActivity(), "点击了Flash", Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_call_talk:
                Toast.makeText(getActivity(), "点击了对讲", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onMicHolderChanged(PTTSession pttSession, String s) {

    }

    @Override
    public void onParticipantChanged(PTTSession pttSession, List<String> list) {

    }

    @Override
    public void onNetworkError(String s) {

    }
}
