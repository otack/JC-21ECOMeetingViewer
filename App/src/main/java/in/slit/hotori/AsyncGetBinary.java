package in.slit.hotori;

import android.content.Context;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncGetBinary extends AsyncCore {
    private Bundle mArgs;

    public AsyncGetBinary(Context context, AsyncCallback asyncCallback, Bundle args) {
        super(context, asyncCallback);
        mArgs = args;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String urlValue = mArgs.getString(Const.BUNDLE_URI);
        String name = mArgs.getString(Const.BUNDLE_FILENAME);
        String tempName = name + Const.TEMPFILE_NAME_APPEND;
        int size = mArgs.getInt(Const.BUNDLE_FILESIZE);

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                return "Server returned HTTP " + connection.getResponseCode()
//                        + " " + connection.getResponseMessage();
                return false;
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
//            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            output = getContext().openFileOutput(tempName, Context.MODE_PRIVATE);

            byte data[] = new byte[size];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    getContext().deleteFile(tempName);
                    return false;
                }
                total += count;
                // publishing the progress....
//                if (fileLength > 0) // only if total length is known
                publishProgress((int) (total / 1024));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
        File from = new File(getContext().getFilesDir(), tempName);
        File to = new File(getContext().getFilesDir(), name);
        from.renameTo(to);
        return true;
    }
}