package in.slit.hotori;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.FileOutputStream;
import java.io.InputStream;

public class BinaryLoader extends AsyncLoader<Boolean> {
    private Bundle mArgs;

    public BinaryLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(mArgs.getString(Const.BUNDLE_URI));
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();
                BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                FileOutputStream stream = getContext()
                        .openFileOutput(mArgs.getString(Const.BUNDLE_FILENAME), Context.MODE_PRIVATE);
                InputStream is = bufHttpEntity.getContent();
                byte[] data = new byte[mArgs.getInt(Const.BUNDLE_FILESIZE)];
                int size;
                while ((size = is.read(data)) > 0) {
                    stream.write(data, 0, size);
                }
                stream.flush();
                stream.close();
                is.close();
                return true;
            }
        } catch (Exception e) {
        }
        return null;
    }
}