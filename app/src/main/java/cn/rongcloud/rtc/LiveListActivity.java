package cn.rongcloud.rtc;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.rongcloud.rtc.base.RongRTCBaseActivity;
import cn.rongcloud.rtc.callback.JoinLiveUICallBack;
import cn.rongcloud.rtc.callback.RongRTCResultUICallBack;
import cn.rongcloud.rtc.engine.report.StatusBean;
import cn.rongcloud.rtc.engine.report.StatusReport;
import cn.rongcloud.rtc.engine.view.RongRTCVideoView;
import cn.rongcloud.rtc.events.RongRTCStatusReportListener;
import cn.rongcloud.rtc.room.RongRTCRoomConfig;
import cn.rongcloud.rtc.stream.remote.RongRTCLiveAVInputStream;
import cn.rongcloud.rtc.utils.FinLog;

/**
 * Created by Huichao.Li on 2019/8/29.
 */

public class LiveListActivity extends RongRTCBaseActivity implements RongRTCStatusReportListener {

    private Button queryButton, publishButton, unpublishButton;
    ListView liveListView;
    LiveListAdapter liveListAdapter;
    private List<LiveModel> liveModelList = new ArrayList<>();
    private RelativeLayout liveVideoLayout;
    private RelativeLayout liveVideoContainer;
    private Button liveVideoClose;
    private static final String TAG = LiveListActivity.class.getSimpleName();
    RongRTCVideoView videoView = null;
    private String liveUrl = "";
    private AppRTCAudioManager audioManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_list);
        initViews();
        LiveDataOperator.getInstance().query(null);
        initAudioManager();
    }

    private void initViews() {
        liveVideoLayout = (RelativeLayout) findViewById(R.id.live_video_layout);
        liveVideoContainer = (RelativeLayout) findViewById(R.id.live_video_container);
        liveVideoClose = (Button) findViewById(R.id.live_video_close);
        queryButton = (Button) findViewById(R.id.live_button_query);
        publishButton = (Button) findViewById(R.id.live_button_publish);
        unpublishButton = (Button) findViewById(R.id.live_button_unpublish);
        liveListView = (ListView) findViewById(R.id.live_list);
        liveListAdapter = new LiveListAdapter(this, liveModelList);
        liveListView.setAdapter(liveListAdapter);

        queryLiveData();

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(LiveDataOperator.ROOM_ID, "001");
                    jsonObject.put(LiveDataOperator.ROOM_NAME, "test001 room");
                    jsonObject.put(LiveDataOperator.LIVE_URL, "https://www.test.com");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LiveDataOperator.getInstance().publish(jsonObject.toString(), null);
            }
        });
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryLiveData();
            }
        });

        unpublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("roomId", "001");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LiveDataOperator.getInstance().unpublish(jsonObject.toString(), null);
            }
        });

        liveListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(), liveModelList.get(i).userName, Toast.LENGTH_SHORT).show();
                liveUrl = liveModelList.get(i).liveUrl;
                joinLive(liveUrl);
            }
        });

        liveVideoClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quitLive();
            }
        });
    }

    private void initAudioManager() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
    }

        private void queryLiveData() {
        LiveDataOperator.getInstance().query(new LiveDataOperator.OnResultCallBack() {
            @Override
            public void onSuccess(String result) {
                parseQueryResult(result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        liveListAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailed(String error) {

            }
        });
    }

    private void joinLive(String liveUrl) {
        liveVideoLayout.setVisibility(View.VISIBLE);
        CenterManager.getInstance().subscribeLiveAVStream(liveUrl, RongRTCRoomConfig.LiveType.AUDIO_VIDEO,
                new JoinLiveUICallBack() {
                    @Override
                    public void onUiVideoStreamReceived(RongRTCLiveAVInputStream stream) {
                        showToast("开始观看视频直播: " + stream.getUserId());
                        videoView = RongRTCEngine.getInstance().createVideoView(LiveListActivity.this);
                        stream.setRongRTCVideoView(videoView);
                        liveVideoContainer.addView(videoView, -1, -1);
                        onRegisterStatusReportListener();
                    }

                    @Override
                    public void onUiAudioStreamReceived(RongRTCLiveAVInputStream stream) {
                      showToast("收到音频流");
                      TextView view = new TextView(getApplicationContext());
                      view.setText("收到音频");
                      liveVideoContainer.addView(view);
                    }

                    @Override
                    public void onUiSuccess() {
                        showToast("订阅直播成功");
                    }

                    @Override
                    public void onUiFailed(RTCErrorCode code) {
                        showToast("打开直播失败！" + code);
                    }
                });
    }

    private void onRegisterStatusReportListener() {
        CenterManager.getInstance().registerStatusReportListener(this);
    }

    private void quitLive() {
        if (videoView != null) {
            videoView.release();
            videoView = null;
        }
        liveVideoContainer.removeAllViews();
        liveVideoLayout.setVisibility(View.GONE);
        CenterManager.getInstance().unsubscribeLiveAVStream(liveUrl, new RongRTCResultUICallBack() {
            @Override
            public void onUiSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("退出观看成功");
                    }
                });
            }

            @Override
            public void onUiFailed(final RTCErrorCode errorCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("退出观看失败: " + errorCode);
                        FinLog.d(TAG, "quit live errorCode: " + errorCode);
                    }
                });
            }
        });
    }

    private void parseQueryResult(String result) {
        try {
            liveModelList.clear();
            JSONObject jsonObject = new JSONObject(result);
            JSONArray array = jsonObject.getJSONArray("roomList");
            for (int i = 0; i < array.length(); i++) {
                JSONObject roomObject = array.getJSONObject(i);
                LiveModel liveModel = new LiveModel();
                liveModel.liveUrl = roomObject.getString(LiveDataOperator.LIVE_URL);
                liveModel.userName = roomObject.getString(LiveDataOperator.ROOM_NAME);
                liveModel.roomId = roomObject.getString(LiveDataOperator.ROOM_ID);
                liveModelList.add(liveModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioReceivedLevel(HashMap<String, String> audioLevel) {

    }

    @Override
    public void onAudioInputLevel(String audioLevel) {

    }

    @Override
    public void onConnectionStats(StatusReport statusReport) {
        for (Map.Entry<String, StatusBean> entry : statusReport.statusVideoRcvs.entrySet()) {
            StatusBean value = entry.getValue();
            Log.d(TAG, "onConnectionStats: "+ value.frameWidth+" x "+value.frameHeight);
        }
    }

    public class LiveListAdapter extends BaseAdapter {

        private Context context;
        private List<LiveModel> liveModelList;

        public LiveListAdapter(Context context, List<LiveModel> liveModelList) {
            this.context = context;
            this.liveModelList = liveModelList;
        }

        @Override
        public int getCount() {
            return liveModelList.size();
        }

        @Override
        public Object getItem(int i) {
            return liveModelList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = LayoutInflater.from(context).inflate(R.layout.activity_live_list_item, null);
            LiveModel liveModel = (LiveModel) getItem(i);
            TextView roomIdView = view.findViewById(R.id.live_roomid);
            roomIdView.setText(liveModel.roomId);
            TextView nameView = view.findViewById(R.id.live_name);
            nameView.setText(liveModel.userName);
            TextView liveUrlView = view.findViewById(R.id.live_url);
            liveUrlView.setText(liveModel.liveUrl);
            return view;
        }
    }

    public class LiveModel {
        public String roomId;
        public String userName;
        public String liveUrl;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        CenterManager.getInstance().registerStatusReportListener(this);
    }
}
