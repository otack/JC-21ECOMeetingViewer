package in.slit.hotori;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncCore extends AsyncTask<String, Integer, Boolean> {

    private AsyncCallback mAsyncCallback = null;;
    private Context mContext;

    public AsyncCore(Context context, AsyncCallback asyncCallback) {
        mContext = context;
        mAsyncCallback = asyncCallback;
    }

    @Override
    protected Boolean doInBackground(String... value) {
        return false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.mAsyncCallback.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        this.mAsyncCallback.onProgressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        this.mAsyncCallback.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        this.cancel(true);
        this.mAsyncCallback.onCancelled();
    }

    Context getContext() {
        return mContext;
    }
}
