package com.tianfangIMS.im.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.request.BaseRequest;
import com.squareup.picasso.Picasso;
import com.tianfangIMS.im.ConstantValue;
import com.tianfangIMS.im.R;
import com.tianfangIMS.im.activity.InfoActivity;
import com.tianfangIMS.im.activity.Settings_Activity;
import com.tianfangIMS.im.activity.UserInfo_Activity;
import com.tianfangIMS.im.adapter.DeparmentLevelAdatper;
import com.tianfangIMS.im.adapter.InfoAdapter;
import com.tianfangIMS.im.bean.LoginBean;
import com.tianfangIMS.im.bean.TreeInfo;
import com.tianfangIMS.im.bean.ViewMode;
import com.tianfangIMS.im.dialog.LoadDialog;
import com.tianfangIMS.im.utils.CommonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by LianMengYu on 2017/1/4.
 */

public class Mine_Fragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private RelativeLayout rl_me_use, rl_mine_settings;
    private TextView tv_me_username, tv_mine_company, tv_mine_department, tv_position;
    private ImageView iv_sex;
    private Context mContext;
    private ImageView iv_me_icon_photo;
    private RelativeLayout rl_mine_company;

    Gson mGson;

    ArrayList<Integer> childCount;
    ArrayList<TreeInfo> mTreeInfos, clickHistory, tmpList;

    HashMap<Integer, TreeInfo> map, tmpMap;
    HashMap<Integer, HashMap<Integer, TreeInfo>> maps;
    private boolean flag = true;
    InfoAdapter mAdapter;
    int workerCount;

    int currentLevel;

    Intent mIntent;
    private ListView mine_department_List;
    HashMap<Integer, Boolean> prepare;
    DeparmentLevelAdatper adatper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.me_fragment, container, false);
        initView(view);
        GetData();
        return view;
    }

    private void initView(View view) {
        rl_me_use = (RelativeLayout) view.findViewById(R.id.rl_me_use);
        rl_mine_settings = (RelativeLayout) view.findViewById(R.id.rl_mine_settings);
        tv_me_username = (TextView) view.findViewById(R.id.tv_me_username);
        iv_sex = (ImageView) view.findViewById(R.id.iv_sex);
        tv_mine_company = (TextView) view.findViewById(R.id.tv_mine_company);
//        tv_mine_department = (TextView) view.findViewById(R.id.tv_mine_department);
//        tv_position = (TextView) view.findViewById(R.id.tv_position);
        iv_me_icon_photo = (ImageView) view.findViewById(R.id.iv_setting_photo);
        rl_mine_company = (RelativeLayout) view.findViewById(R.id.rl_mine_company);
        mine_department_List = (ListView) view.findViewById(R.id.mine_department_List);
        rl_mine_company.setOnClickListener(this);
        rl_mine_settings.setOnClickListener(this);
        rl_me_use.setOnClickListener(this);
        mine_department_List.setOnItemClickListener(this);
        Gson gson = new Gson();
        LoginBean loginBean = gson.fromJson(CommonUtil.getUserInfo(getActivity()), LoginBean.class);
        Picasso.with(getActivity())
                .load(ConstantValue.ImageFile + loginBean.getText().getLogo())
                .resize(500, 500)
                .into(iv_me_icon_photo);
        tv_me_username.setText(loginBean.getText().getFullname());
        tv_mine_company.setText(" ");
        if (loginBean.getText().getSex().equals(1)) {
            iv_sex.setImageResource(R.mipmap.me_sexicon_nan);
        } else {
            iv_sex.setImageResource(R.mipmap.me_sexicon_nv);
        }
//        tv_mine_department.setText(loginBean.getText().getIntro());
//        tv_position.setText(loginBean.getText().getWorkno());
    }


    private void GetData() {
        OkGo.post(ConstantValue.DEPARTMENTPERSON)
                .tag(this)
                .connTimeOut(10000)
                .readTimeOut(10000)
                .writeTimeOut(10000)
                .execute(new StringCallback() {
                    @Override
                    public void onBefore(BaseRequest request) {
                        super.onBefore(request);
                        LoadDialog.show(getActivity());
                    }

                    @Override
                    public void onSuccess(String s, Call call, Response response) {
                        LoadDialog.dismiss(getActivity());
                        mGson = new Gson();
                        mTreeInfos = mGson.fromJson(s, new TypeToken<List<TreeInfo>>() {
                        }.getType());
//                        getMap();
                        //根据PID进行平行节点排序 如果PID相同 则根据自身ID进行前后排序
                        Collections.sort(mTreeInfos, new Comparator<TreeInfo>() {
                            @Override
                            public int compare(TreeInfo o1, TreeInfo o2) {
                                if (o1.getPid() < o2.getPid()) {
                                    return -1;
                                } else if (o1.getPid() > o2.getPid()) {
                                    return 1;
                                }
                                return o1.getId() < o2.getId() ? -1 : 1;
                            }
                        });
                        currentLevel = mTreeInfos.get(0).getPid();
                        tv_mine_company.setText(mTreeInfos.get(0).getName());
                        maps = new HashMap<Integer, HashMap<Integer, TreeInfo>>();
                        //规定最小PID为0 保证与最小PID不相同
                        int pid = -1;
                        tmpMap = new HashMap<Integer, TreeInfo>();
                        Gson gson = new Gson();
                        LoginBean loginBean = gson.fromJson(CommonUtil.getUserInfo(getActivity()), LoginBean.class);
                        int ids = Integer.parseInt(loginBean.getText().getId().toString());
                        for (TreeInfo treeInfo : mTreeInfos) {
                            tmpMap.put(treeInfo.getId(), treeInfo);
                            //如果当前pid等于之前的pid 说明该平行节点组已被创建 直接将其放入当前平行节点组内即可
                            if (pid == treeInfo.getPid()) {
                                map.put(treeInfo.getId(), treeInfo);
                            } else {
                                if (map != null) {
                                    //当前平行节点已结束 填入父Map 自身置空
                                    maps.put(pid, map);
                                    map = null;
                                }
                                //如果不同 则说明进入了新的平行节点组
                                pid = treeInfo.getPid();
                                if (map == null) {
                                    map = new HashMap<Integer, TreeInfo>();
                                    map.put(treeInfo.getId(), treeInfo);
                                }
                            }
                        }
                        SetListData(ids);
                        //最后的一组平行节点组进行嵌入
                        maps.put(pid, map);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clickHistory = new ArrayList<TreeInfo>();
                                mTreeInfos = new ArrayList<>();
                                childCount = new ArrayList<Integer>();
                                mAdapter = new InfoAdapter(mContext, mTreeInfos, childCount, ViewMode.NORMAL, prepare);
//                                lv_addGroup_company.setAdapter(mAdapter);
                                Log.e("哈哈", "TreeInfo：" + mTreeInfos);
                                transfer();
                            }
                        });
                    }
                });
    }

    private void SetListData(int ids) {
        tmpList = new ArrayList<>();
        Log.e("hahaha", "ids:" + ids);
        Log.e("怎么可能:", "----:" + ids);
        getParent(ids);
        Collections.sort(tmpList, new Comparator<TreeInfo>() {
            @Override
            public int compare(TreeInfo o1, TreeInfo o2) {
                return o1.getId() < o2.getId() ? -1 : 1;
            }
        });
        for (TreeInfo info : tmpList) {
            Log.i("aaaa", "info.getId() : " + info.getId() + " , info.getName() : " + info.getName());
        }
        adatper = new DeparmentLevelAdatper(getActivity(), tmpList);
        mine_department_List.setAdapter(adatper);
        adatper.notifyDataSetChanged();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private void getParent(int id) {
        if (tmpMap.containsKey(id)) {
            Log.e("怎么可能", "---tmpMap:" + tmpMap);
            TreeInfo mInfo = tmpMap.get(id);
            Log.e("怎么可能", "---TreeInfo:" + mInfo.getPid() + "----Flag:" + mInfo.getFlag());
            if(mInfo.getPid() > 0 ){
                if(mInfo.getFlag() != 1){
                    tmpList.add(mInfo);
                }
                getParent(mInfo.getPid());
            }
        }
    }

    private void transfer() {
        //清除适配数据集合
        mTreeInfos.clear();
        childCount.clear();
        //得到下一级部门的数据集合
        map = maps.get(currentLevel);
        //如果没有子部门 直接进行提示
        if (map == null) {
            return;
        }
        //将Map数据集合转换为List
        for (TreeInfo treeInfo : map.values()) {
            mTreeInfos.add(treeInfo);
        }
        //根据部门编号 进行排序
        Collections.sort(mTreeInfos, new Comparator<TreeInfo>() {
            @Override
            public int compare(TreeInfo o1, TreeInfo o2) {
                return o1.getId() < o2.getId() ? -1 : 1;
            }
        });
        //显示Item后的子部门人数
        for (TreeInfo treeInfo : mTreeInfos) {
            workerCount = 0;
            //员工类型
            if (treeInfo.getFlag() == 1) {
                childCount.add(workerCount);
            } else {
                //部门类型
                calcSum(maps.get(treeInfo.getId()));
                childCount.add(workerCount);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 迭代计算传入部门的总人数
     *
     * @param tmp
     */
    private void calcSum(Map<Integer, TreeInfo> tmp) {
        if (tmp != null) {
            for (TreeInfo treeInfo : tmp.values()) {
                workerCount++;
                if (treeInfo.getFlag() == 1) {
                    continue;
                }
                Map<Integer, TreeInfo> child = maps.get(treeInfo.getId());
                if (child != null) {
                    calcSum(child);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_me_use:
                startActivity(new Intent(getActivity(), UserInfo_Activity.class));
                break;
            case R.id.rl_mine_settings:
                startActivity(new Intent(getActivity(), Settings_Activity.class));
                break;
            case R.id.rl_mine_company:
                mIntent = new Intent(getActivity(), InfoActivity.class);
                mIntent.putExtra("maps", maps);
                mIntent.putExtra("viewMode", ViewMode.NORMAL);
                mIntent.putExtra("currentLevel", mTreeInfos.get(0).getId());
                mIntent.putExtra("parentLevel", mTreeInfos.get(0).getPid());
                startActivity(mIntent);
                break;
        }
    }
}
