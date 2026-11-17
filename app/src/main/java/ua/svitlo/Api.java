package ua.svitlo;

import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** HTTP-клиент к dtek-api (простой текст по http, без зависимостей). */
final class Api {
    static final String BASE = "http://uae1.alwaysdata.net/";

    static {
        // на Android до 2.2 keep-alive в HttpURLConnection ломает чтение
        if (Build.VERSION.SDK_INT < 8) System.setProperty("http.keepAlive", "false");
    }

    static final class Result {
        int code;
        String body;
        boolean error;
    }

    private static String url(String r, String g, String s) {
        return BASE + r + "/" + g + "/" + s + "/";
    }

    /** Список групп региона: 200 – "группа<TAB>подгруппы через запятую" по строке, 404 – данных нет. */
    static Result list(String r) {
        return req(BASE + r + "/", "GET");
    }

    static Result fetch(String r, String g, String s) {
        return req(url(r, g, s), "GET");
    }

    /** 200 – группа есть, 404 – нет, -1 – ошибка сети/сервера. HEAD экономит трафик при переборе. */
    static int probe(String r, String g, String s) {
        Result x = req(url(r, g, s), "HEAD");
        if (x.code == 405 || x.code == 501) x = req(url(r, g, s), "GET");
        return x.error ? -1 : x.code;
    }

    private static Result req(String url, String method) {
        Result res = new Result();
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestMethod(method);
            c.setRequestProperty("Accept", "text/plain");
            int code = c.getResponseCode();
            res.code = code;
            if (code == 200) {
                if ("GET".equals(method)) res.body = read(c.getInputStream());
            } else if (code != 404) {
                res.error = true;
            }
        } catch (Exception e) {
            res.error = true;
        } finally {
            if (c != null) c.disconnect();
        }
        return res;
    }

    private static String read(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
                if (total > 262144) break;
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
