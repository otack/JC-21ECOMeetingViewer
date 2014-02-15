package in.slit.hotori;

import android.content.Context;
import android.os.AsyncTask;

public class AsyncCore extends AsyncTask<String, Integer, Object> {

    private AsyncCallback _asyncCallback = null;;
    private Context _context;

    public AsyncCore(Context context, AsyncCallback asyncCallback) {
        _context = context;
        _asyncCallback = asyncCallback;
    }

    @Override
    protected Object doInBackground(String... value) {
        return new Object();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this._asyncCallback.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        this._asyncCallback.onProgressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        this._asyncCallback.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        this._asyncCallback.onCancelled();
    }

    public Context getContext() {
        return _context;
    }
}
