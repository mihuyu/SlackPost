package apps.mihuyu.com.slackpost;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import apps.mihuyu.com.slackpost.common.CommonConst;
import apps.mihuyu.com.slackpost.common.CommonUtil;

public class SlackRequest extends AsyncTask<String, Void, String> {

    /**
     * リスナー
     */
    private Listener listener;

    private Map<String, String> listMap = null;

    private static Map<String, String> LIST_ICON;

    static {
        LIST_ICON = new HashMap<>();
        //"\uD83C\uDF0F"  = ちきゅう(🌏)
        LIST_ICON.put(CommonConst.RESPONSE_KEY_CHANNELS,"\uD83C\uDF0F");
        //"\uD83D\uDD12" = かぎ(🔒)
        LIST_ICON.put(CommonConst.RESPONSE_KEY_GROUPS, "\uD83D\uDD12");
    }
    
    private static final String ERROR_GET_REQUEST = "GETリクエストエラー";

    private static final String ERROR_JSON_PARSE = "JSONパースエラー";

    // 非同期処理
    @Override
    protected String doInBackground(String... params) {

        if (params == null) {
            return "Param Error.";
        }

        String result = "エラー";

        //search & post
        if (params.length == 4) {

            // channels.list or groups.list
            String tokenValue = params[0];
            String channelId = params[1];
            String shareText = params[2];
            String targetUrl = params[3];

            // url変換
            String convertUrl = CommonUtil.convertURLPretty(shareText, targetUrl);

            String[] convertUrls = convertUrl.split(",");
            for (String url : convertUrls) {
                // 重複Postを避けるため、全体検索→検索結果0件の場合PostMessageを実施する。
                // search
                StringBuilder searchUrl = new StringBuilder();
                searchUrl.append(CommonConst.URL_SEARCH_MESSAGES);
                searchUrl.append(CommonConst.QUESTION);
                searchUrl.append(CommonConst.QUERY_TOKEN);
                searchUrl.append(tokenValue);
                searchUrl.append(CommonConst.AND);
                searchUrl.append(CommonConst.QUERY_SEARCH_QUERY);
                searchUrl.append(url);

                Log.d("debug", "searchUrl:" + searchUrl.toString());

                String jsonRes = this.doSlackAPI(searchUrl.toString());
                result = this.searchMessages(jsonRes, channelId);
                if (result == null) {
                    // post message
                    StringBuilder postUrl = new StringBuilder();
                    postUrl.append(CommonConst.URL_CHAT_POST_MESSAGE);
                    postUrl.append(CommonConst.QUESTION);
                    postUrl.append(CommonConst.QUERY_TOKEN);
                    postUrl.append(tokenValue);
                    postUrl.append(CommonConst.AND);
                    postUrl.append(CommonConst.QUERY_CHANNEL);
                    postUrl.append(channelId);
                    postUrl.append(CommonConst.AND);
                    postUrl.append(CommonConst.QUERY_AS_USER);
                    postUrl.append(CommonConst.AND);
                    postUrl.append(CommonConst.QUERY_TEXT);
                    postUrl.append(url);

                    Log.d("debug", "postUrl:" + postUrl.toString());

                    this.doSlackAPI(postUrl.toString());
                }
            }
        }

        //channel & group
        if (params.length == 2) {
            // channels.list or groups.list
            String tokenValue = params[0];
            String apiMethod = params[1];

            StringBuilder listUrl = new StringBuilder();
            listUrl.append(apiMethod);
            listUrl.append(CommonConst.QUESTION);
            listUrl.append(CommonConst.QUERY_TOKEN);
            listUrl.append(tokenValue);

            Log.d("debug","listUrl:" + listUrl.toString());

            String jsonRes = this.doSlackAPI(listUrl.toString());
            result = getList(jsonRes, apiMethod);
        }

        return result;
    }

    /**
     * 引数に指定されたURLを実行する。
     * @param urlString URL
     * @return 実行結果(json形式)
     */
    private String doSlackAPI(String urlString) {
        String jsonRes = null;

        HttpURLConnection con = null;

        try {
            // URL設定
            URL url = new URL(urlString);

            // HttpURLConnection
            con = (HttpURLConnection) url.openConnection();

            // request POST
            con.setRequestMethod(CommonConst.HTTP_GET);

            // no Redirects
            con.setInstanceFollowRedirects(false);

            // 時間制限
            con.setReadTimeout(10000);
            con.setConnectTimeout(20000);

            // 接続
            con.connect();

            // GETデータ送信処理
            InputStream is = null;

            try {
                is = con.getInputStream();
                jsonRes = CommonUtil.InputStreamToString(is);
                Log.d("debug","json_res:" + jsonRes);

            } catch (IOException ex) {
                // GETリクエストエラー
                ex.printStackTrace();
                jsonRes = ERROR_GET_REQUEST;
            } catch (Exception ex) {
                ex.printStackTrace();
                jsonRes = ERROR_JSON_PARSE;
            } finally {
                if (is != null) {
                    is.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return jsonRes;
    }

    private String getList(String jsonRes, String apiMethod) {
        String result = null;

        try {
            JSONObject json_res = new JSONObject(jsonRes);
            Log.d("debug","ok:" + json_res.getString("ok"));

            if (Boolean.valueOf(json_res.getString("ok"))) {
                String key = CommonUtil.geyKeyForURL(apiMethod);
                Log.d("debug","key:" + key);
                JSONArray json_res_list = json_res.getJSONArray(key);
                Log.d("debug", key + ".list:" + json_res_list);

                // データなしの場合
                if (json_res_list != null && json_res_list.length() != 0) {
                    // データを詰める
                    listMap = new HashMap<>();
                    for (int i = 0; i < json_res_list.length(); i++) {
                        JSONObject res = (JSONObject) json_res_list.get(i);
                        listMap.put(res.getString("id"),
                                LIST_ICON.get(key) + " " + res.getString("name"));
                    }
                } else {
                    result= CommonConst.R_ID + R.string.data_not_found;
                }
            } else {
                result= CommonConst.R_ID + R.string.post_failed;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            result = ERROR_JSON_PARSE;
        }


        return result;
    }

    private String searchMessages(String jsonRes, String channel_id) {
        String result = null;

        try {

            JSONObject json_res = new JSONObject(jsonRes);
            Log.d("debug","ok:" + json_res.getString("ok"));

            if (Boolean.valueOf(json_res.getString("ok"))) {
                JSONObject json_res_message = new JSONObject(json_res.getString("messages"));
                Log.d("debug", "messages.total:" + json_res_message.getInt("total"));

                // 重複データなしの場合
                // total == 0の場合は、postMessageを実行するため、なにもしない。
                if (json_res_message.getInt("total") != 0) {
                    JSONArray matches = json_res_message.getJSONArray("matches");
                    if (matches != null && matches.length() != 0) {
                        for (int i = 0; i < matches.length(); i++) {
                            JSONObject match = (JSONObject) matches.get(i);
                            if (match != null) {
                                JSONObject channel = (JSONObject) match.get("channel");
                                if (channel != null &&
                                        channel_id.equals(channel.getString("id"))) {
                                    // 同じChannelで重複している。
                                    result= CommonConst.R_ID + R.string.post_duplicate;
                                }
                            }
                        }
                    }
                }
            } else {
                result= CommonConst.R_ID + R.string.post_failed;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            result = ERROR_JSON_PARSE;
        }

        return result;
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (listener != null) {
            listener.setListMap(listMap);
            listener.onSuccess(result);
        }
    }

    void setListener(SlackRequest.Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
        void setListMap(Map<String, String> listMap);
    }
}
