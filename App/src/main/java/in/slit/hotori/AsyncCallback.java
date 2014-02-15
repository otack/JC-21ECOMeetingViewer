package in.slit.hotori;

public interface AsyncCallback {
    void onPreExecute();
    void onProgressUpdate(int progress);
    void onPostExecute(Object result);
    void onCancelled();
}