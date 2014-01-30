package in.slit.hotori;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LoginUtils {

    public static void storeLoginInfo(Context context, String id, String pass, String accessToken) {
        SharedPreferences preferences = context.getSharedPreferences(Const.PREF_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Const.PREF_ID, id);
        editor.putString(Const.PREF_PASS, pass);
        editor.putString(Const.PREF_TOKEN, accessToken);
        editor.commit();
    }

    public static void storeAccessToken(Context context, String accessToken) {
        SharedPreferences preferences = context.getSharedPreferences(Const.PREF_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Const.PREF_TOKEN, accessToken);
        editor.commit();
    }

    public static Bundle loadLoginInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Const.PREF_NAME,
                Context.MODE_PRIVATE);
        String id = preferences.getString(Const.PREF_ID, null);
        String pass = preferences.getString(Const.PREF_PASS, null);
        String token = preferences.getString(Const.PREF_TOKEN, null);
        if (id != null && pass != null && token != null) {
            Bundle args = new Bundle();
            args.putString("id", id);
            args.putString("pass", pass);
            args.putString("token", token);
            return args;
        } else {
            return null;
        }
    }

    public static String loadAccessToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Const.PREF_NAME,
                Context.MODE_PRIVATE);
        return preferences.getString(Const.PREF_TOKEN, Const.NULL);
    }

    public static boolean hasAccessToken(Context context) {
        return !loadAccessToken(context).equals(Const.NULL);
    }

    public static String getBaseURI(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Const.PREF_NAME,
                Context.MODE_PRIVATE);
        return preferences.getString(Const.PREF_LOGIN_URI, Const.BASE_URI);
    }
}