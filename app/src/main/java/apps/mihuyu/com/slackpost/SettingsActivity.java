package apps.mihuyu.com.slackpost;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new MainPreferenceFragment())
                    .commit();
        }

    }

    /**
     * セキュリティ対策
     * @param fragmentName fragmentのクラス名
     * @return 判定結果
     */
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return MainPreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class MainPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        Preference tokenPreference;
        Preference channelPreference;
        List<String> entityList = new ArrayList<>();
        List<String> entityValueList = new ArrayList<>();
        Map<String, String> channelsMap = new HashMap<>();

        private SlackRequest slackRequestChannels;
        private SlackRequest slackRequestGroups;

        public MainPreferenceFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            setRetainInstance(true);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();

            //ネットワークチェック
            if (CommonUtil.isNetworkNoConnected(getContext())) {
                Toast.makeText(getContext(), R.string.network_no_connected, Toast.LENGTH_SHORT).show();
                return;
            }

            PreferenceScreen screenPref = (PreferenceScreen)findPreference("preferenceScreen");
            PreferenceCategory categoryPref = (PreferenceCategory) screenPref.findPreference("preferenceCategory");

            // Preference設定
            tokenPreference = categoryPref.findPreference(CommonConst.KEY_TOKEN);
            channelPreference = categoryPref.findPreference(CommonConst.KEY_CHANNEL);

            // 初期化
            entityList.clear();
            entityValueList.clear();
            channelsMap.clear();

            // 非同期処理
            slackRequestChannels = new SlackRequest();
            slackRequestGroups = new SlackRequest();
            slackRequestChannels.setListener(createListener());
            slackRequestGroups.setListener(createListener());

            // 設定値取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            String token = sp.getString(CommonConst.KEY_TOKEN, "");

            // 無効化
            channelPreference.setEnabled(false);

            if (!"".equals(token)) {
                // summaryに設定
                loadPreference(tokenPreference.getSharedPreferences(), CommonConst.KEY_TOKEN);
                if (AsyncTask.Status.PENDING.equals(slackRequestChannels.getStatus()) &&
                        AsyncTask.Status.PENDING.equals(slackRequestGroups.getStatus())) {
                    // slack request
                    slackRequestChannels.execute(token, CommonConst.URL_CHANNELS_LIST);
                    slackRequestGroups.execute(token, CommonConst.URL_GROUPS_LIST);
                }
            }

            sp.registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            loadPreference(sharedPreferences, key);
        }

        private void loadPreference(SharedPreferences sp, String key) {
            String sharedPreferencesKey = sp.getString(key, "");
            if (CommonConst.KEY_CHANNEL.equals(key)) {
                channelPreference.setSummary(channelsMap.get(sharedPreferencesKey));
            }
            if (CommonConst.KEY_TOKEN.equals(key)) {
                tokenPreference.setSummary(sharedPreferencesKey);
                if (!"".equals(sharedPreferencesKey)) {
                    if (AsyncTask.Status.FINISHED.equals(slackRequestChannels.getStatus())) {
                        slackRequestChannels = new SlackRequest();
                        slackRequestChannels.setListener(createListener());
                    }
                    if (AsyncTask.Status.FINISHED.equals(slackRequestGroups.getStatus())) {
                        slackRequestGroups = new SlackRequest();
                        slackRequestGroups.setListener(createListener());
                    }
                    if (AsyncTask.Status.PENDING.equals(slackRequestChannels.getStatus()) &&
                            AsyncTask.Status.PENDING.equals(slackRequestGroups.getStatus())) {
                        // 初期化
                        entityList.clear();
                        entityValueList.clear();
                        channelsMap.clear();
                        // slack request
                        slackRequestChannels.execute(sharedPreferencesKey, CommonConst.URL_CHANNELS_LIST);
                        slackRequestGroups.execute(sharedPreferencesKey, CommonConst.URL_GROUPS_LIST);
                    }
                }
            }
        }

        private SlackRequest.Listener createListener() {
            return new SlackRequest.Listener() {
                @Override
                public void onSuccess(String result) {

                    if (result == null) {
                        // 有効化
                        channelPreference.setEnabled(true);

                    } else {
                        // 無効化
                        channelPreference.setEnabled(false);

                        int index = result.indexOf(CommonConst.R_ID);
                        if (index < 0) {
                            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();

                        } else {
                            result = result.replaceAll(CommonConst.R_ID, "");
                            int rid = Integer.parseInt(result);
                            Toast.makeText(getContext(), rid, Toast.LENGTH_SHORT).show();

                        }

                        // 初期化
                        entityList.clear();
                        entityValueList.clear();
                        channelsMap.clear();

                        // summaryに設定
                        channelPreference.setSummary("");
                        loadPreference(channelPreference.getSharedPreferences(), CommonConst.KEY_CHANNEL);

                    }

                }

                @Override
                public void setListMap(Map<String, String> listMap) {
                    if (listMap != null) {
                        for (String key :listMap.keySet()) {
                            entityList.add(listMap.get(key));
                            entityValueList.add(key);
                            channelsMap.put(key, listMap.get(key));
                        }

                        ListPreference listPreference = (ListPreference)channelPreference;
                        String[] entities = entityList.toArray( new String[ entityList.size() ] );
                        String[] entityValues = entityValueList.toArray( new String[ entityValueList.size() ] );

                        listPreference.setEntries( entities );
                        listPreference.setEntryValues( entityValues );

                        // summaryに設定
                        loadPreference(channelPreference.getSharedPreferences(), CommonConst.KEY_CHANNEL);

                    }
                }
            };
        }

    }
}
