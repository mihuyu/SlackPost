package apps.mihuyu.com.slackpost;

import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import apps.mihuyu.com.slackpost.common.CommonConst;
import apps.mihuyu.com.slackpost.common.CommonUtil;

public class SettingsActivity extends PreferenceActivity {

    private static MainPreferenceFragment mainPreferenceFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mainPreferenceFragment = new MainPreferenceFragment();
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, mainPreferenceFragment)
                    .commit();
        }

    }

    // メニューをActivity上に設置する
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 参照するリソースは上でリソースファイルに付けた名前と同じもの
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // メニューが選択されたときの処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:

                // 非同期処理実行可
                mainPreferenceFragment.syncChannelsFlg = true;

                // 呼び出し
                String token = mainPreferenceFragment.getSharedPreferencesValue(CommonConst.KEY_TOKEN);
                mainPreferenceFragment.doExecute(token);

                return true;

            default:
                return super.onOptionsItemSelected(item);
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
        Preference targetUrlPreference;
        List<String> entityList = new ArrayList<>();
        List<String> entityValueList = new ArrayList<>();
        LinkedHashMap<String, String> channelsMap = new LinkedHashMap<>();

        boolean syncChannelsFlg = false;

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

            PreferenceScreen screenPref = (PreferenceScreen)findPreference("preferenceScreen");
            PreferenceCategory categoryPref = (PreferenceCategory) screenPref.findPreference("preferenceCategory");

            // Preference設定
            tokenPreference = categoryPref.findPreference(CommonConst.KEY_TOKEN);
            channelPreference = categoryPref.findPreference(CommonConst.KEY_CHANNEL);
            targetUrlPreference = categoryPref.findPreference(CommonConst.KEY_TARGET_URL);

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
            String targetUrl = sp.getString(CommonConst.KEY_TARGET_URL, "");

            if (!"".equals(token)) {
                // summaryに設定
                loadPreference(tokenPreference.getSharedPreferences(), CommonConst.KEY_TOKEN);
            }

            Map<String, ?> map = getActivity().getSharedPreferences(CommonConst.KEY_CHANNEL_LIST, Context.MODE_PRIVATE).getAll();
            if (map != null && map.size() > 0) {
                try {
                    String obj = (String) map.get(CommonConst.KEY_CHANNEL_LIST);
                    JSONObject json = new JSONObject(obj);
                    Iterator<String> iterator = json.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        entityList.add((String)json.get(key));
                        entityValueList.add(key);
                        channelsMap.put(key, (String)json.get(key));
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                if (!channelsMap.isEmpty()) {
                    String[] entities = entityList.toArray(new String[entityList.size()]);
                    String[] entityValues = entityValueList.toArray(new String[entityValueList.size()]);
                    ListPreference listPreference = (ListPreference) channelPreference;
                    listPreference.setEntries(entities);
                    listPreference.setEntryValues(entityValues);

                    // summaryに設定
                    loadPreference(channelPreference.getSharedPreferences(), CommonConst.KEY_CHANNEL);

                    // 有効化
                    channelPreference.setEnabled(true);
                } else {
                    // 無効化
                    channelPreference.setEnabled(false);
                }
            }

            // summaryに設定
            targetUrlPreference.setSummary(targetUrl);
            loadPreference(targetUrlPreference.getSharedPreferences(), CommonConst.KEY_TARGET_URL);

            sp.registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        public String getSharedPreferencesValue(String key) {
            // 設定値取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            return sp.getString(key, "");
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            if (slackRequestChannels != null &&
                    AsyncTask.Status.RUNNING.equals(slackRequestChannels.getStatus())) {
                slackRequestChannels.cancel(true);
            }
            if (slackRequestChannels != null &&
                    AsyncTask.Status.RUNNING.equals(slackRequestChannels.getStatus())) {
                slackRequestChannels.cancel(true);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (CommonConst.KEY_TOKEN.equals(key)) {
                syncChannelsFlg = true;
            }
            loadPreference(sharedPreferences, key);
        }

        private void loadPreference(SharedPreferences sp, String key) {
            String sharedPreferencesValue = sp.getString(key, "");
            if (CommonConst.KEY_TARGET_URL.equals(key)) {
                targetUrlPreference.setSummary(sharedPreferencesValue);
            }
            if (CommonConst.KEY_CHANNEL.equals(key)) {
                channelPreference.setSummary(channelsMap.get(sharedPreferencesValue));
            }
            if (CommonConst.KEY_TOKEN.equals(key)) {
                tokenPreference.setSummary(sharedPreferencesValue);
                if (syncChannelsFlg && !"".equals(sharedPreferencesValue)) {
                    // 呼び出し
                    this.doExecute(sharedPreferencesValue);
                }

                syncChannelsFlg = false;
            }
        }

        protected void doExecute(String sharedPreferencesValue) {
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

                //ネットワークチェック
                if (CommonUtil.isNetworkNoConnected(getContext())) {
                    Toast.makeText(getContext(), R.string.network_no_connected, Toast.LENGTH_SHORT).show();
                    syncChannelsFlg = false;
                    return;
                }

                // 初期化
                entityList.clear();
                entityValueList.clear();
                channelsMap.clear();

                // slack request
                slackRequestChannels.execute(sharedPreferencesValue, CommonConst.URL_CHANNELS_LIST);
                slackRequestGroups.execute(sharedPreferencesValue, CommonConst.URL_GROUPS_LIST);
            }

        }

        private SlackRequest.Listener createListener() {
            return new SlackRequest.Listener() {
                @Override
                public void onSuccess(String result) {

                    if (result == null) {
                        // 有効化
                        channelPreference.setEnabled(true);
                        Toast.makeText(getContext(), R.string.channel_sync_complete, Toast.LENGTH_SHORT).show();

                    } else {
                        if (!channelsMap.isEmpty()) {
                            // 有効化
                            channelPreference.setEnabled(true);
                            return;
                        }
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

                        SharedPreferences sp = getActivity().getSharedPreferences(CommonConst.KEY_CHANNEL_LIST, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        JSONObject json = (JSONObject) JSONObject.wrap(channelsMap);
                        editor.putString(CommonConst.KEY_CHANNEL_LIST, json.toString());
                        editor.apply();

                    }
                }
            };
        }
    }
}
