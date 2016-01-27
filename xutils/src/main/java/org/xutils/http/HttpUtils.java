package org.xutils.http;

import org.xutils.common.util.KeyValue;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by Administrator on 2015/12/17.
 */
public class HttpUtils {
    private static int MAX_THREADS = 5;
    private static int MAX_TIMEOUT = 60 * 1000;
    private final int mTimeout;
    private final Executor executor;
    private static HttpUtils sHttpUtils;

    private HttpUtils() {
        this(MAX_THREADS, MAX_TIMEOUT);
    }

    private HttpUtils(int max, int timeout) {
        executor = new PriorityExecutor(max, true);
        mTimeout = timeout;
    }

    public static boolean init(int max, int timeout) {
        MAX_THREADS = Math.max(1, max);
        MAX_TIMEOUT = Math.max(15 * 1000, timeout);
        return sHttpUtils == null;
    }

    public synchronized static HttpUtils getInstance() {
        if (sHttpUtils == null) {
            sHttpUtils = new HttpUtils();
        }
        return sHttpUtils;
    }

    private RequestParams makeRequestParams(String url, List<KeyValue> data, boolean isGet, int timeout) {
        final RequestParams params = new RequestParams(url);
        params.setMethod(isGet ? HttpMethod.GET : HttpMethod.POST);
        params.setConnectTimeout(timeout);
        if (data != null) {
            for (KeyValue kv : data) {
                if (isGet) {
                    params.addQueryStringParameter(kv.key, "" + kv.value);
                } else {
                    params.addBodyParameter(kv.key, "" + kv.value);
                }
            }
        }
        return params;
    }

    public static byte[] getSync(String url, List<KeyValue> data) {
        return getInstance().get(url, data);
    }

    public static byte[] postSync(String url, List<KeyValue> data) {
        return getInstance().post(url, data);
    }

    public static byte[] get(String url, Map<String, String> data, int timeout) {
        List<KeyValue> list = new ArrayList<KeyValue>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            list.add(new KeyValue(e.getKey(), e.getValue()));
        }
        return getInstance().get(url, list, timeout);
    }

    public static byte[] post(String url, Map<String, String> data, int timeout) {
        List<KeyValue> list = new ArrayList<KeyValue>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            list.add(new KeyValue(e.getKey(), e.getValue()));
        }
        return getInstance().post(url, list, timeout);
    }

    private byte[] post(String url, List<KeyValue> data) {
        return post(url, data, mTimeout);
    }

    private byte[] post(String url, List<KeyValue> data, int timeout) {
        RequestParams params = makeRequestParams(url, data, false, timeout);
        byte[] res = null;
        try {
            res = x.http().postSync(params, byte[].class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return res;
    }

    private byte[] get(String url, List<KeyValue> data) {
        return get(url, data, mTimeout);
    }

    private byte[] get(String url, List<KeyValue> data, int timeout) {
        RequestParams params = makeRequestParams(url, data, true, timeout);
        byte[] res = null;
        try {
            res = x.http().getSync(params, byte[].class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return res;
    }
}
