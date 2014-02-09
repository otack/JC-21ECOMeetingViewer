package in.slit.hotori;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getSupportActionBar().setDisplayShowHomeEnabled(false);

        if (savedInstanceState == null) {
            SharedPreferences preferences = getSharedPreferences(Const.PREF_NAME, Context.MODE_PRIVATE);
            int loginMode = preferences.getInt(Const.PREF_DEFAULT_LOGIN_MODE, 0);
            if (LoginUtils.hasAccessToken(this) && loginMode != 0) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, new CatalogFragment(loginMode))
                        .commit();
            } else {
                startActivityForResult(new Intent(this, LoginActivity.class), Const.LOGIN_MODE);
            }

//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new CatalogFragment(Const.LOGIN_MODE_OFFLINE))
//                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.LOGIN_MODE) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new CatalogFragment(resultCode))
                    .commit();
        }
    }
}