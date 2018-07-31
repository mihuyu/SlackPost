package apps.mihuyu.com.slackpost;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Map;

import apps.mihuyu.com.slackpost.common.CommonConst;
import apps.mihuyu.com.slackpost.common.CommonUtil;
import apps.mihuyu.com.slackpost.common.FireMissilesDialogFragment;


public class ChooseTransparentActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    FireMissilesDialogFragment dialogFragment;
    private String shareText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_activity);

        Button button = findViewById(R.id.button);

        // ボタンタップでAlertを表示させる
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentManager = getSupportFragmentManager();

                // DialogFragment を継承したAlertDialogFragmentのインスタンス
                dialogFragment = FireMissilesDialogFragment.newInstance(null);
                dialogFragment.show(fragmentManager, "");
            }
        });

        //ネットワークチェック
        if (CommonUtil.isNetworkNoConnected(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), R.string.network_no_connected, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        // param設定
        Intent intent = getIntent();
        shareText = null;

        // Figure out what to do based on the intent type
        if (CommonConst.HTTP_MIME_TYPE_TEXT.equals(intent.getType())) {
            ClipData clip = intent.getClipData();
            if (clip != null) {
                CharSequence[] contentText = new CharSequence[clip.getItemCount()];
                for (int i = 0; i < clip.getItemCount(); i++) {
                    contentText[i] = clip.getItemAt(i).getText();
                }
                shareText = contentText[0].toString();
            }
        }

        // ボタンクリックしたてい。
        button.performClick();
    }

    public void onPost(String channel) {

        // 非同期処理
        SlackRequest slackRequest = new SlackRequest();
        slackRequest.setListener(createListener());

        // 設定値取得
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String token = sp.getString(CommonConst.KEY_TOKEN, null);
//        String channel = sp.getString(CommonConst.KEY_CHANNEL, null);
        String targetUrl = sp.getString(CommonConst.KEY_TARGET_URL, null);

        if (token!= null && channel != null && targetUrl != null) {
            // slack request
            slackRequest.execute(token, channel, shareText, targetUrl);
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_post, Toast.LENGTH_SHORT).show();
        }


    }

    private SlackRequest.Listener createListener() {
        return new SlackRequest.Listener() {
            @Override
            public void onSuccess(String result) {

                if (result == null) {
                    Toast.makeText(getApplicationContext(), R.string.post_success, Toast.LENGTH_SHORT).show();

                } else {
                    int index = result.indexOf(CommonConst.R_ID);
                    if (index < 0) {
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();

                    } else {
                        result = result.replaceAll(CommonConst.R_ID, "");
                        int rid = Integer.parseInt(result);
                        Toast.makeText(getApplicationContext(), rid, Toast.LENGTH_SHORT).show();

                    }
                }

                finishAndRemoveTask();
            }

            @Override
            public void setListMap(Map<String, String> listMap) {

            }
        };
    }

    public void finishActivity() {
        finishAndRemoveTask();
    }
}
