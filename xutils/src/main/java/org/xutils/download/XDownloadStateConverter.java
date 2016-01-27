package org.xutils.download;

import android.database.Cursor;

import org.xutils.db.converter.ColumnConverter;
import org.xutils.db.sqlite.ColumnDbType;

/**
 * Created by Administrator on 2016/1/9.
 */
class XDownloadStateConverter implements ColumnConverter<XDownloadState> {

    @Override
    public XDownloadState getFieldValue(Cursor cursor, int index) {
        int dbValue = cursor.getInt(index);
        return XDownloadState.valueOf(dbValue);
    }

    @Override
    public Object fieldValue2DbValue(XDownloadState fieldValue) {
        return fieldValue.value();
    }

    @Override
    public ColumnDbType getColumnDbType() {
        return ColumnDbType.INTEGER;
    }
}
