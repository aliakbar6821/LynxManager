package com.lynx.manager;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesProvider;

public class LynxSearchIndexablesProvider extends SearchIndexablesProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"xmlResId", "intentAction", "intentTargetPackage", "intentTargetClass"});
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"title", "summaryOn", "summaryOff", "entries", "keywords", "screenTitle", "className", "iconResId", "intentAction", "intentTargetPackage", "intentTargetClass", "key", "userId"});
        return cursor;
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key"});
        return cursor;
    }
}
