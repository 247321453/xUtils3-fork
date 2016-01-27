package org.xutils.download;

import android.os.Parcel;
import android.os.Parcelable;

import org.xutils.common.AESUtils;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * Author: wyouflf
 * Date: 13-11-10
 * Time: 下午8:11
 */
@Table(name = XDownloadInfo.TABLE_NAME)
public class XDownloadInfo implements Parcelable {

    public XDownloadInfo() {
        autoResume = true;
        autoRename = false;
    }

    public static final String TABLE_NAME = "download";
    public static final int TABLE_VERSION = 1;

    public static final String COL_ID = "id";
    /** 唯一id */
    @Column(name = COL_ID, isId = true)
    private long id;

    public static final String COL_STATE = "state";
    @Column(name = COL_STATE)
    private XDownloadState state = XDownloadState.STOPPED;

    public static final String COL_URL = "url";
    @Column(name = COL_URL)
    private String url;

    public static final String COL_FILESACEPATH = "fileSavePath";
    @Column(name = COL_FILESACEPATH)
    private String fileSavePath;

    public static final String COL_PROGRESS = "progress";
    /** 百分比 进度 */
    @Column(name = COL_PROGRESS)
    private int progress;

    public static final String COL_FILELENGTH = "fileLength";
    @Column(name = COL_FILELENGTH)
    private long fileLength;

    public static final String COL_AUTORESUME = "autoResume";
    /** 断点续传 */
    @Column(name = COL_AUTORESUME)
    private boolean autoResume;

    public static final String COL_AUTORENAME = "autoRename";
    /** 改为服务器发送的名字 */
    @Column(name = COL_AUTORENAME)
    private boolean autoRename;

    //  public static final String COL_PRIORITY = "priority";

    /** 优先级 */
//    @Column(name = COL_PRIORITY)
//    private Priority priority = Priority.DEFAULT;
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public XDownloadState getState() {
        return state;
    }

    public void setState(XDownloadState state) {
        this.state = state;
    }

    public String getDownloadUrl() {
        return AESUtils.decrypt(XDownloadManager.KEY, url);
    }

    public void setDownloadUrl(String url) {
        this.url = AESUtils.encrypt(XDownloadManager.KEY, url);
    }

    public String getRealSavePath() {
        return AESUtils.decrypt(XDownloadManager.KEY, fileSavePath);
    }

    public void setRealSavePath(String fileSavePath) {
        this.fileSavePath = AESUtils.encrypt(XDownloadManager.KEY, fileSavePath);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public boolean isAutoResume() {
        return autoResume;
    }

    public void setAutoResume(boolean autoResume) {
        this.autoResume = autoResume;
    }

    public boolean isAutoRename() {
        return autoRename;
    }

    public void setAutoRename(boolean autoRename) {
        this.autoRename = autoRename;
    }

//    public Priority getPriority() {
//        return priority;
//    }
//
//    public void setPriority(Priority priority) {
//        this.priority = priority;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XDownloadInfo)) return false;

        XDownloadInfo that = (XDownloadInfo) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "XDownloadInfo{" +
                "autoRename=" + autoRename +
                ", id=" + id +
                ", state=" + state +
                ", url='" + getDownloadUrl() + '\'' +
                ", fileSavePath='" + getRealSavePath() + '\'' +
                ", progress=" + progress +
                ", fileLength=" + fileLength +
                ", autoResume=" + autoResume +
                //       ", priority=" + priority +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeInt(this.state == null ? -1 : this.state.ordinal());
        dest.writeString(this.url);
        dest.writeString(this.fileSavePath);
        dest.writeInt(this.progress);
        dest.writeLong(this.fileLength);
        dest.writeByte(autoResume ? (byte) 1 : (byte) 0);
        dest.writeByte(autoRename ? (byte) 1 : (byte) 0);
        //   dest.writeInt(this.priority == null ? -1 : this.priority.ordinal());
    }

    protected XDownloadInfo(Parcel in) {
        this.id = in.readLong();
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? null : XDownloadState.values()[tmpState];
        this.url = in.readString();
        this.fileSavePath = in.readString();
        this.progress = in.readInt();
        this.fileLength = in.readLong();
        this.autoResume = in.readByte() != 0;
        this.autoRename = in.readByte() != 0;
        int tmpPriority = in.readInt();
        //   this.priority = tmpPriority == -1 ? null : Priority.values()[tmpPriority];
    }

    public static final Creator<XDownloadInfo> CREATOR = new Creator<XDownloadInfo>() {
        public XDownloadInfo createFromParcel(Parcel source) {
            return new XDownloadInfo(source);
        }

        public XDownloadInfo[] newArray(int size) {
            return new XDownloadInfo[size];
        }
    };
}
