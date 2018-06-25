package apps.mihuyu.com.slackpost;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.Map;


public class TransparentActivity extends Activity {

    private SlackRequest slackRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transparent_activity);
        getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);

        // 非同期処理
        slackRequest = new SlackRequest();
        slackRequest.setListener(createListener());

        // param設定
        Intent intent = getIntent();
        String param = null;

        // Figure out what to do based on the intent type
        if (intent.getType().equals("text/plain")) {
            ClipData clip = intent.getClipData();
            CharSequence[] contentText = new CharSequence[clip.getItemCount()];
            for (int i = 0; i < clip.getItemCount(); i++) {
                contentText[i] = clip.getItemAt(i).getText();
            }
            param = contentText[0].toString();
        }

        // 設定値取得
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String token = sp.getString(CommonConst.KEY_TOKEN, null);
        String channel = sp.getString(CommonConst.KEY_CHANNEL, null);

        if (token!= null && channel != null) {
            // slack request
            slackRequest.execute(token, channel, param);
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
}
