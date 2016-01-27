package org.xutils.download;

import android.text.TextUtils;
import android.view.View;

import org.xutils.DbManager;
import org.xutils.common.AESUtils;
import org.xutils.common.Callback;
import org.xutils.common.task.Priority;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.db.converter.ColumnConverterFactory;
import org.xutils.ex.DbException;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Author: wyouflf
 * 下载管理器
 * Date: 13-11-10
 * Time: 下午8:10
 */
public final class XDownloadManager {

    private static XDownloadManager instance;
    private static final int MAX_DOWNLOAD_THREAD = 2; // 有效的值范围[1, 3], 设置为3时, 可能阻塞图片加载.
    private static int TIMEOUT = 60 * 1000;
    private static String DB_DIR;
    private static String DB_NAME;
    static String KEY = "mykey";
    private final DbManager db;
    private final Executor executor = new PriorityExecutor(MAX_DOWNLOAD_THREAD, true);
    private final List<XDownloadInfo> mXDownloadInfoList = new ArrayList<XDownloadInfo>();
    private final ConcurrentHashMap<XDownloadInfo, XDownloadCallback>
            callbackMap = new ConcurrentHashMap<XDownloadInfo, XDownloadCallback>(4);

    static {
        // 注册DownloadState在数据库中的值类型映射
        ColumnConverterFactory.registerColumnConverter(XDownloadState.class, new XDownloadStateConverter());
        //   ColumnConverterFactory.registerColumnConverter(Priority.class, new PriorityConverter());
    }

    public static void init(int timeout, String name, String dir, String key) {
        DB_DIR = dir;
        DB_NAME = name;
        TIMEOUT = Math.max(timeout, 15 * 1000);
        if (!TextUtils.isEmpty(key)) {
            KEY = key;
        }
        LogUtil.d(DB_DIR + "/" + DB_NAME + ",timeout=" + TIMEOUT);
    }

    private XDownloadManager() {
        DbManager.DaoConfig daoConfig = new DbManager.DaoConfig();
        daoConfig.setDbVersion(XDownloadInfo.TABLE_VERSION);
        if (!TextUtils.isEmpty(DB_DIR)) {
            daoConfig.setDbDir(new File(DB_DIR));
        }
        if (!TextUtils.isEmpty(DB_NAME)) {
            daoConfig.setDbName(DB_NAME);
        } else {
            daoConfig.setDbName("xutils_" + XDownloadInfo.TABLE_NAME + ".db");
        }
        db = x.getDb(daoConfig);
        loadList();
    }

    /***
     * 重新加载下载列表
     */
    private void loadList() {
        mXDownloadInfoList.clear();
        try {
            List<XDownloadInfo> infoList = db.selector(XDownloadInfo.class).findAll();
            if (infoList != null) {
                for (XDownloadInfo info : infoList) {
                    if (info.getState().value() < XDownloadState.FINISHED.value()) {
                        info.setState(XDownloadState.STOPPED);
                    }
                    mXDownloadInfoList.add(info);
                }
            }
        } catch (DbException ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    public synchronized static XDownloadManager getInstance() {
        if (instance == null) {
            synchronized (XDownloadManager.class) {
                if (instance == null) {
                    instance = new XDownloadManager();
                }
            }
        }
        return instance;
    }

    public void updateDownloadInfo(XDownloadInfo downloadInfo) throws DbException {
        db.update(downloadInfo);
    }

    public int getDownloadListCount() {
        return mXDownloadInfoList.size();
    }

    public XDownloadInfo getDownloadInfo(int index) {
        return mXDownloadInfoList.get(index);
    }

    /***
     * 获取下载信息
     *
     * @param url
     * @param fileSavePath
     * @return
     * @throws DbException
     */
    public XDownloadInfo getDownloadInfo(String url, String fileSavePath) throws DbException {
        return getDownloadInfo(url, fileSavePath, true, false);
    }

    /***
     * 获取下载信息
     *
     * @param url
     * @param savePath
     * @param autoResume 断点续传
     * @param autoRename 改为服务器发送的名字
     * @return
     * @throws DbException
     */
    public XDownloadInfo getDownloadInfo(String url, String savePath,
                                         boolean autoResume, boolean autoRename) throws DbException {
        String fileSavePath = new File(savePath).getAbsolutePath();
        XDownloadInfo downloadInfo = db.selector(XDownloadInfo.class)
                .where(XDownloadInfo.COL_FILESACEPATH,
                        "=",
                        AESUtils.encrypt(XDownloadManager.KEY, fileSavePath))
                .findFirst();
        String tmp = savePath + ".tmp";
        if (downloadInfo == null) {
            //如果没有记录则删除tmp文件
            try {
                File tmpf = new File(tmp);
                if(IOUtil.deleteFileOrDir(tmpf)){
                    LogUtil.d("del old " + tmpf);
                }
            } catch (Exception e) {
            }
            downloadInfo = new XDownloadInfo();
            downloadInfo.setDownloadUrl(url);
            downloadInfo.setAutoRename(autoRename);
            downloadInfo.setAutoResume(autoResume);
            downloadInfo.setRealSavePath(fileSavePath);
            mXDownloadInfoList.add(downloadInfo);
            db.saveBindingId(downloadInfo);
        }else{
            try {
                File tmpf = new File(tmp);
                if(!tmpf.exists()){
                    downloadInfo.setProgress(0);
					LogUtil.d("reset pos");
                }				
            } catch (Exception e) {
            }
            updateDownloadInfo(downloadInfo);
        }
        return downloadInfo;
    }

    /***
     * 开始下载
     *
     * @param url
     * @param savePath
     * @param viewHolder
     * @throws DbException
     */
    public void startDownload(String url, String savePath,
                              XDownloadListener viewHolder) throws DbException {
        startDownload(url, savePath, true, false, viewHolder);
    }

    /***
     * 开始下载
     *
     * @param url
     * @param savePath
     * @param autoResume
     * @param autoRename
     * @param listener
     * @throws DbException
     */
    public void startDownload(String url, String savePath,
                              boolean autoResume, boolean autoRename,
                              XDownloadListener listener) throws DbException {
        XDownloadInfo downloadInfo = getDownloadInfo(url, savePath, autoResume, autoRename);
        if (downloadInfo == null) {
            throw new DbException("not find info");
        }
        if (listener == null) {
            listener = new DefaultXDownloadListener(null, downloadInfo);
        } else {
            listener.update(downloadInfo);
        }
        XDownloadCallback callback = callbackMap.get(downloadInfo);
        if (callback != null) {
            if (callback.switchViewHolder(listener)) {
                return;
            } else {
                callback.cancel();
            }
        }
        resumeDownload(downloadInfo, listener);
    }

    /***
     * 恢复下载
     *
     * @param downloadInfo
     * @param listener     下载监听
     */
    public void resumeDownload(XDownloadInfo downloadInfo, XDownloadListener listener) {
        // start downloading
        stopDownload(downloadInfo);
        if (listener == null) {
            listener = new DefaultXDownloadListener(null, downloadInfo);
        }
        XDownloadCallback callback = new XDownloadCallback(listener);
        callback.setDownloadManager(this);
        callback.switchViewHolder(listener);
        RequestParams params = new RequestParams(downloadInfo.getDownloadUrl());
        params.setAutoResume(downloadInfo.isAutoResume());
        params.setAutoRename(downloadInfo.isAutoRename());
        params.setSaveFilePath(downloadInfo.getRealSavePath());
        params.setPriority(Priority.DEFAULT);
//        params.setPriority(downloadInfo.getPriority());
//        int v = downloadInfo.getPriority().ordinal();
//        if (v < Priority.DEFAULT.ordinal()) {
//            //低优先级
//            params.setExecutor(executorLow);
//        } else if (v > Priority.DEFAULT.ordinal()) {
//            //高优先级
//            params.setExecutor(executorHigh);
//        } else {
//            //一般
//
//        }
        params.setExecutor(executor);
//        params.setCancelFast(true);
        params.setConnectTimeout(TIMEOUT);
        Callback.Cancelable cancelable = x.http().get(params, callback);
        callback.setCancelable(cancelable);
        callbackMap.put(downloadInfo, callback);
    }

    public void stopDownload(int index) {
        XDownloadInfo downloadInfo = mXDownloadInfoList.get(index);
        stopDownload(downloadInfo);
    }

    public void stopDownload(XDownloadInfo downloadInfo) {
        Callback.Cancelable cancelable = callbackMap.get(downloadInfo);
        if (cancelable != null) {
            cancelable.cancel();
        }
    }

    public void stopAllDownload() {
        for (XDownloadInfo downloadInfo : mXDownloadInfoList) {
            Callback.Cancelable cancelable = callbackMap.get(downloadInfo);
            if (cancelable != null) {
                cancelable.cancel();
            }
        }
    }

    public void removeDownload(int index) throws DbException {
        XDownloadInfo downloadInfo = getDownloadInfo(index);
        removeDownload(downloadInfo);
    }

    public void removeDownload(XDownloadInfo downloadInfo) throws DbException {
        db.delete(downloadInfo);
        stopDownload(downloadInfo);
        mXDownloadInfoList.remove(downloadInfo);
    }

    /** 默认 */
    public static class DefaultXDownloadListener extends XDownloadListener {

        public DefaultXDownloadListener(View view, XDownloadInfo downloadInfo) {
            super(view, downloadInfo);
        }

        @Override
        public void onWaiting() {

        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onLoading(long total, long current) {

        }

        @Override
        public void onSuccess(File result) {
            //通知栏?
        }

        @Override
        public void onError(Throwable ex, boolean isOnCallback) {
        }

        @Override
        public void onCancelled(Callback.CancelledException cex) {
        }
    }
}

//class PriorityConverter implements ColumnConverter<Priority> {
//
//    @Override
//    public Priority getFieldValue(Cursor cursor, int index) {
//        int dbValue = cursor.getInt(index);
//        if (dbValue >= 0 && dbValue < Priority.values().length) {
//            return Priority.values()[dbValue];
//        }
//        return Priority.DEFAULT;
//    }
//
//    @Override
//    public Object fieldValue2DbValue(Priority fieldValue) {
//        return fieldValue.ordinal();
//    }
//
//    @Override
//    public ColumnDbType getColumnDbType() {
//        return ColumnDbType.INTEGER;
//    }
//}