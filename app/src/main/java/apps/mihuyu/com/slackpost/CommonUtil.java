package apps.mihuyu.com.slackpost;

public class CommonUtil {

    /**
     * twitterの簡易URLに変換する
     * @param param 変換するURL
     * @return 変換したURL
     */
    public static String convertTwitterURLPretty(String param) {
        String url;

        // urlのみに変換
        int startIndex = param.indexOf("http");
        param = param.substring(startIndex);
        // URLのqueryを削除する
        param = param.replaceAll("\\?.+", "");
        // "/photo/1"を削除する
        url = param.replaceAll("\\/photo\\/1", "");

        return url;
    }

    public static String geyKeyForURL(String url) {
        if (url == null || "".equals(url)) {
            return "";
        }
        String[] keys = url.split("\\?");
        String[] keys2 = keys[0].split("\\/");
        String[] keys3 = keys2[keys2.length - 1].split("\\.");
        return keys3[0];
    }
}
