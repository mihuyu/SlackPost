package apps.mihuyu.com.slackpost;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    /**
     * 共有するテキストを指定されたものだけに変換する
     * @param shareText 変換するテキスト
     * @param targetUrl 共有対象のURL
     * @return 変換したURL(複数あった場合は、「,」区切りで返す)
     */
    public static String convertURLPretty(String shareText, String targetUrl) {
        StringBuilder url = new StringBuilder("");

        // urlのみに変換
        Pattern p = Pattern.compile(targetUrl);
        Matcher m = p.matcher(shareText);
        while (m.find()) {
            if (m.groupCount() != 1 && url.length() != 0) {
                url.append(",");
            }
            url.append(m.group());
        }

        String replace = url.toString();
        // URLのqueryを削除する
        replace = replace.replaceAll(CommonConst.URL_QUERY_ALL, "");
        // "/photo/1"を削除する
        replace = replace.replaceAll(CommonConst.TWITTER_URL_PHOTO, "");
        // "/video/1"を削除する
        replace = replace.replaceAll(CommonConst.TWITTER_URL_VIDEO, "");

        //再設定
        url = new StringBuilder(replace);

        return url.toString();
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
