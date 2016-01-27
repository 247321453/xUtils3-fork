package org.xutils.download;

import org.xutils.common.Callback;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.DbException;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by wyouflf on 15/11/10.
 */
/*package*/ class XDownloadCallback implements
        Callback.CommonCallback<File>,
        Callback.ProgressCallback<File>,
        Callback.Cancelable {

    private XDownloadInfo mXDownloadInfo;
    private WeakReference<XDownloadListener> viewHolderRef;
    private XDownloadManager downloadManager;
    private boolean cancelled = false;
    private Cancelable cancelable;

    public XDownloadCallback(XDownloadListener viewHolder) {
        this.switchViewHolder(viewHolder);
    }

    public boolean switchViewHolder(XDownloadListener viewHolder) {
        if (viewHolder == null) return false;

        synchronized (XDownloadCallback.class) {
            if (mXDownloadInfo != null) {
                if (this.isStopped()) {
                    return false;
                }
            }
            this.mXDownloadInfo = viewHolder.getXDownloadInfo();
            this.viewHolderRef = new WeakReference<XDownloadListener>(viewHolder);
        }
        return true;
    }

    public void setDownloadManager(XDownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void setCancelable(Cancelable cancelable) {
        this.cancelable = cancelable;
    }

    private XDownloadListener getViewHolder() {
        if (viewHolderRef == null) return null;
        XDownloadListener viewHolder = viewHolderRef.get();
        if (viewHolder != null) {
            XDownloadInfo downloadInfo = viewHolder.getXDownloadInfo();
            if (this.mXDownloadInfo != null && this.mXDownloadInfo.equals(downloadInfo)) {
                return viewHolder;
            }
        }
        return null;
    }

    @Override
    public void onWaiting() {
        try {
            mXDownloadInfo.setState(XDownloadState.WAITING);
            downloadManager.updateDownloadInfo(mXDownloadInfo);
        } catch (DbException ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        XDownloadListener viewHolder = this.getViewHolder();
        if (viewHolder != null) {
            viewHolder.onWaiting();
        }
    }

    @Override
    public void onStarted() {
        try {
            mXDownloadInfo.setState(XDownloadState.STARTED);
            downloadManager.updateDownloadInfo(mXDownloadInfo);
        } catch (DbException ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        XDownloadListener viewHolder = this.getViewHolder();
        if (viewHolder != null) {
            viewHolder.onStarted();
        }
    }

    @Override
    public void onLoading(long total, long current, boolean isDownloading) {
        if (isDownloading) {
            try {
                mXDownloadInfo.setState(XDownloadState.STARTED);
                mXDownloadInfo.setFileLength(total);
                mXDownloadInfo.setProgress((int) (current * 100 / total));
                downloadManager.updateDownloadInfo(mXDownloadInfo);
            } catch (DbException ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
            XDownloadListener viewHolder = this.getViewHolder();
            if (viewHolder != null) {
                viewHolder.onLoading(total, current);
            }
        }
    }

    @Override
    public void onSuccess(File result) {
        synchronized (XDownloadCallback.class) {
           // Log.d("KK",result.getAbsolutePath());
            try {
                mXDownloadInfo.setState(XDownloadState.FINISHED);
                downloadManager.updateDownloadInfo(mXDownloadInfo);
            } catch (DbException ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
            XDownloadListener viewHolder = this.getViewHolder();
            if (viewHolder != null) {
                viewHolder.onSuccess(result);
            }
        }
    }

    @Override
    public void onError(Throwable ex, boolean isOnCallback) {
        synchronized (XDownloadCallback.class) {
            try {
                mXDownloadInfo.setState(XDownloadState.ERROR);
                downloadManager.updateDownloadInfo(mXDownloadInfo);
            } catch (DbException e) {
                LogUtil.e(e.getMessage(), e);
            }
            XDownloadListener viewHolder = this.getViewHolder();
            if (viewHolder != null) {
                viewHolder.onError(ex, isOnCallback);
            }
        }
    }

    @Override
    public void onCancelled(CancelledException cex) {
        synchronized (XDownloadCallback.class) {
            try {
                mXDownloadInfo.setState(XDownloadState.STOPPED);
                downloadManager.updateDownloadInfo(mXDownloadInfo);
            } catch (DbException ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
            XDownloadListener viewHolder = this.getViewHolder();
            if (viewHolder != null) {
                viewHolder.onCancelled(cex);
            }
        }
    }

    @Override
    public void onFinished() {
        cancelled = false;
    }

    private boolean isStopped() {
        XDownloadState state = mXDownloadInfo.getState();
        return isCancelled() || state.value() > XDownloadState.STARTED.value();
    }

    @Override
    public void cancel() {
        cancelled = true;
        if (cancelable != null) {
            cancelable.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
