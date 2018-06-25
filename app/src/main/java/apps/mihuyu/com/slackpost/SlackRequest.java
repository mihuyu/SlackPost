package apps.mihuyu.com.slackpost;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SlackRequest extends AsyncTask<String, Void, String> {

    /**
     * Query integration char.
     */
    private static final String AND = "&";

    /**
     * Query Start char
     */
    private static final String HATENA = "?";

    /**
     * リスナー
     */
    private Listener listener;

    Map<String, String> listMap = null;

    static Map<String, String> LIST_ICON;

    static {
        LIST_ICON = new HashMap<>();
        //"\uD83C\uDF0F"  = ちきゅう(🌏)
        LIST_ICON.put("channels", "\uD83C\uDF0F");
        //"\uD83D\uDD12" = かぎ(🔒)
        LIST_ICON.put("groups", "\uD83D\uDD12");
    }

    // 非同期処理
    @Override
    protected String doInBackground(String... params) {

        if (params == null) {
            return "Param Error.";
        }

        String result = "エラー";

        //search & post
        if (params.length == 3) {
            String token_val = params[0];
            String channel_id = params[1];
            String param = params[2];

            result = this.searchMessages(token_val, param);
            if (result == null) {
                result = this.chatPostMessage(token_val, channel_id, param);
            }
        }

        //channel & group
        if (params.length == 2) {
            String token_val = params[0];
            String url = params[1];
            result = getList(token_val, url);
        }

        return result;
    }

    private String geyKey(String url) {
        String[] keys = url.split("\\?");
        String[] keys2 = keys[0].split("\\/");
        String[] keys3 = keys2[keys2.length - 1].split("\\.");
        return keys3[0];
    }

    private String getList(String token_val, String base_url_list) {
        String result = null;

        //token
        String token = "token=";

        // channels.list or groups.list
        StringBuilder list = new StringBuilder();
        list.append(base_url_list);
        list.append(HATENA);
        list.append(token);
        list.append(token_val);

        HttpURLConnection con = null;

        try {
            // URL設定
            URL url = new URL(list.toString());

            // HttpURLConnection
            con = (HttpURLConnection) url.openConnection();

            // request POST
            con.setRequestMethod("GET");

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
                String list_res = InputStreamToString(is);
                Log.d("debug","search_res:" + list_res);

                JSONObject json_res = new JSONObject(list_res);
                Log.d("debug","ok:" + json_res.getString("ok"));

                if (Boolean.valueOf(json_res.getString("ok"))) {
                    String key = this.geyKey(base_url_list);
                    Log.d("debug","key:" + key);
                    JSONArray json_res_list = json_res.getJSONArray(key);
                    Log.d("debug", key + ".list:" + json_res_list);

                    // データなしの場合
                    if (json_res_list.length() != 0) {
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

            } catch (IOException ex) {
                // GETリクエストエラー
                ex.printStackTrace();
                result = "GETリクエストエラー";
            } catch (Exception ex) {
                ex.printStackTrace();
                result = "JSONパースエラー";
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

        return result;
    }

    private String searchMessages(String token_val, String param) {
        String result = null;

        // urlのみに変換
        int startIndex = param.indexOf("http");
        param = param.substring(startIndex);
        // URLのqueryを削除する
        param = param.replaceAll("\\?.+", "");
        // "/photo/1"を削除する
        param = param.replaceAll("\\/photo\\/1", "");

        //token
        String token = "token=";

        // 重複Postを避けるため、全体検索→検索結果0件の場合PostMessageを実施する。
        // search
        String base_url_messages = CommonConst.URL_SEARCH_MESSAGES;
        String query = "query=";
        StringBuilder search_url = new StringBuilder();
        search_url.append(base_url_messages);
        search_url.append(HATENA);
        search_url.append(token);
        search_url.append(token_val);
        search_url.append(AND);
        search_url.append(query);
        search_url.append(param);

        HttpURLConnection con = null;

        try {
            // URL設定
            URL url = new URL(search_url.toString());

            // HttpURLConnection
            con = (HttpURLConnection) url.openConnection();

            // request POST
            con.setRequestMethod("GET");

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
                String search_res = InputStreamToString(is);
                Log.d("debug","search_res:" + search_res);

                JSONObject json_res = new JSONObject(search_res);
                Log.d("debug","ok:" + json_res.getString("ok"));

                if (Boolean.valueOf(json_res.getString("ok"))) {
                    JSONObject json_res_message = new JSONObject(json_res.getString("messages"));
                    Log.d("debug", "messages.total:" + json_res_message.getInt("total"));

                    // 重複データなしの場合
                    if (json_res_message.getInt("total") == 0) {
                        // postMessageを実行するため、なにもしない。
                    } else {
                        result= CommonConst.R_ID + R.string.post_duplicate;
                    }
                } else {
                    result= CommonConst.R_ID + R.string.post_failed;
                }

            } catch (IOException ex) {
                // GETリクエストエラー
                ex.printStackTrace();
                result = "GETリクエストエラー";
            } catch (Exception ex) {
                ex.printStackTrace();
                result = "JSONパースエラー";
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

        return result;
    }

    private String chatPostMessage(String token_val, String channel_id, String param) {
        String result = null;

        // urlのみに変換
        int startIndex = param.indexOf("http");
        param = param.substring(startIndex);
        // URLのqueryを削除する
        param = param.replaceAll("\\?.+", "");
        // "/photo/1"を削除する
        param = param.replaceAll("\\/photo\\/1", "");

        //token
        String token = "token=";

        // post message
        String base_url_postMessage = CommonConst.URL_CHAT_POSTMESSAGE;
        String channel = "channel=";
        String as_user = "as_user=true";
        String text = "text=";
        StringBuilder post_url = new StringBuilder();
        post_url.append(base_url_postMessage);
        post_url.append(HATENA);
        post_url.append(token);
        post_url.append(token_val);
        post_url.append(AND);
        post_url.append(channel);
        post_url.append(channel_id);
        post_url.append(AND);
        post_url.append(as_user);
        post_url.append(AND);
        post_url.append(text);
        post_url.append(param);

        HttpURLConnection con = null;

        try {
            // URL設定
            URL url = new URL(post_url.toString());

            // HttpURLConnection
            con = (HttpURLConnection) url.openConnection();

            // request POST
            con.setRequestMethod("GET");

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
                String postmessage_res = InputStreamToString(is);
                Log.d("debug","postmessage_res:" + postmessage_res);
            } catch (IOException ex) {
                // GETリクエストエラー
                ex.printStackTrace();
                result = "GETリクエストエラー";
            } catch (Exception ex) {
                ex.printStackTrace();
                result = "JSONパースエラー";
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

        return result;
    }

    // InputStream -> String
    private String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
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

    void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
        void setListMap(Map<String, String> listMap);
    }
}
