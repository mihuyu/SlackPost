package apps.mihuyu.com.slackpost;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommonUtil {

    /**
     * twitterの簡易URLに変換する
     * @param param 変換するURL
     * @return 変換したURL
     */
    public static String convertTwitterURLPretty(String param) {
        String url;

        // urlのみに変換
        int startIndex = param.indexOf(CommonConst.HTTP);
        param = param.substring(startIndex);
        // URLのqueryを削除する
        param = param.replaceAll(CommonConst.URL_QUERY_ALL, "");
        // "/photo/1"を削除する
        url = param.replaceAll(CommonConst.TWITTER_URL_PHOTO, "");

        return url;
    }

    /**
     * slack method name.
     * @param url String
     * @return key
     */
    public static String geyKeyForURL(String url) {
        if (url == null || "".equals(url)) {
            return "";
        }
        String[] keys = url.split(CommonConst.URL_QUERY);
        String[] keys2 = keys[0].split(CommonConst.URL_SLASH);
        String[] keys3 = keys2[keys2.length - 1].split(CommonConst.URL_DOT);
        return keys3[0];
    }

    /**
     * InputStream -> String
     * @param is InputStream
     * @return String
     * @throws IOException IOException
     */
    public static String InputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader fis = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(fis);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    /**
     * ネットワークに接続していない状態かを確認する
     * @param context Context
     * @return 接続不可:true/接続可能:false
     */
    public static boolean isNetworkNoConnected(Context context) {
        boolean result = true;
        ConnectivityManager cm =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null) {
                result = !info.isConnected();
            }
        }

        return result;
    }
}
