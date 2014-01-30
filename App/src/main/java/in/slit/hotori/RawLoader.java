package in.slit.hotori;

import android.content.Context;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;

public class RawLoader extends AsyncLoader<String> {
    private String mArg;

    public RawLoader(Context context, String arg) {
        super(context);
        mArg = arg;
    }

    @Override
    public String loadInBackground() {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(mArg);
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                httpResponse.getEntity().writeTo(outputStream);
                return outputStream.toString();
            }
        } catch (Exception e) {
        }
        return null;
    }
}

