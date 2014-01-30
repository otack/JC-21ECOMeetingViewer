package in.slit.hotori;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class AsyncLoader<D> extends AsyncTaskLoader<D> {

    private D mData = null;

    public AsyncLoader(Context context) {
        super(context);
    }

    /**
     * {@link #startLoading()}が呼び出された時に呼び出される。
     * データが有る場合は結果を送り、ない場合は新規のデータをロードする。
     *
     * @UiThread
     */
    @Override
    protected void onStartLoading(){
        // すでにデータが有る場合は結果を送る
        if(mData != null){
            deliverResult(mData);
        }
        // loader停止中にコンテンツが変わった、またはデータがない
        if(takeContentChanged() || mData == null){
            // #startLoading()とは異なり、以前のデータを無視して新規のデータをロードする
            forceLoad();
        }
    }

    /**
     * 非同期処理を行い、その結果を返す。
     *
     * @Background
     */
    @Override
    public abstract D loadInBackground();

    /**
     * {@link #stopLoading()}が呼び出された時に呼び出される。ロードをキャンセルする。
     *
     * @UiThread
     */
    @Override
    protected void onStopLoading(){
        // 可能な場合、ロードをキャンセルする
        cancelLoad();
    }

    /**
     * {@link #reset()}が呼び出された時に呼び出される。ロードをキャンセルし、データを破棄する。
     *
     * @UiThread
     */
    @Override
    protected void onReset(){
        super.onReset();
        stopLoading();
        mData = null;
    }

    /**
     * onLoadCompleteリスナーに結果を送る。
     * メインスレッドから呼び出す。
     *
     * @param 結果
     */
    @Override
    public void deliverResult(D data){
        if(isReset()){
            // Loader停止中に呼び出された
            return;
        }
        mData = data;
        super.deliverResult(data);
    }

}