package in.slit.hotori;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<String> {

    private Button mSetUri;
    private EditText mId;
    private EditText mPass;
    private Button mLogin;
    private Button mLoginOffline;

    private View mDialogView;
    private EditText mUri;
    private Button mResetUri;

    private ProgressDialog mDialog;

    private String loginId = null;
    private String loginPass = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        getSupportLoaderManager().initLoader(0, null, this);

        mDialog = new ProgressDialog(this);

        mSetUri = (Button) findViewById(R.id.buttonLoginUri);
        mId = (EditText) findViewById(R.id.editTextId);
        mPass = (EditText) findViewById(R.id.editTextPassword);
        mLogin = (Button) findViewById(R.id.buttonLogin);
        mLoginOffline = (Button) findViewById(R.id.buttonOffline);

        mDialogView = getLayoutInflater().inflate(R.layout.dialog_login_uri, null);
        mUri = (EditText) mDialogView.findViewById(R.id.editTextUri);
        mResetUri = (Button) mDialogView.findViewById(R.id.buttonResetLoginUri);

        mResetUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUri.setText(LoginUtils.getBaseURI(getApplicationContext()));
                mUri.setSelection(mUri.getText().length());
            }
        });
        mSetUri.setText(LoginUtils.getBaseURI(this));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.login_uri)
                .setView(mDialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (!TextUtils.isEmpty(mUri.getText())) {
                            mSetUri.setText(mUri.getText());
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.uri_could_not_be_empty,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        mSetUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUri.setText(mSetUri.getText());
                mUri.setSelection(mUri.getText().length());
                dialog.show();
            }
        });
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginPressed();
            };
        });
        mLoginOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOfflineLoginPressed();
            }
        });

        mSetUri.setVisibility(View.INVISIBLE);
    }

    private Bundle formChecker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        String baseUri = null;
        String id = null;
        String pass = null;
        StringBuilder notInputValue = new StringBuilder();
        if (!TextUtils.isEmpty(mSetUri.getText())) {
            baseUri = mSetUri.getText().toString();
        } else {
            if (notInputValue.length() != 0) {
                notInputValue.append("・");
            }
            notInputValue.append(getString(R.string.login_uri));
        }
        if (!TextUtils.isEmpty(mId.getText())) {
            id = mId.getText().toString();
        } else {
            if (notInputValue.length() != 0) {
                notInputValue.append("・");
            }
            notInputValue.append(getString(R.string.id));
        }
        if (!TextUtils.isEmpty(mPass.getText())) {
            pass = mPass.getText().toString();
        } else {
            if (notInputValue.length() != 0) {
                notInputValue.append("・");
            }
            notInputValue.append(getString(R.string.password));
        }
        if (id != null && pass != null) {
            Bundle args = new Bundle(3);
            args.putString(Const.BUNDLE_ID, id);
            args.putString(Const.BUNDLE_PASS, pass);
            args.putString(Const.BUNDLE_URI, baseUri);
            return args;
        } else {
            notInputValue.append(getString(R.string.is_not_input));
            Toast.makeText(this, new String(notInputValue),
                    Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private void onLoginPressed() {
        Bundle forms = formChecker();
        if (forms != null) {
            StringBuilder uri = new StringBuilder(forms.getString(Const.BUNDLE_URI))
                    .append(Const.DEFAULT_LOGIN_URI_APPEND)
                    .append("?user=")
                    .append(loginId = forms.getString(Const.BUNDLE_ID))
                    .append("&pass=")
                    .append(loginPass = forms.getString(Const.BUNDLE_PASS));
            Bundle args = new Bundle(1);
            args.putString(Const.BUNDLE_URI, new String(uri));
            getSupportLoaderManager().restartLoader(Const.LOADER_RAW, args, this);
        }
    }

    private void onOfflineLoginPressed() {
        Bundle forms = formChecker();
        Bundle args = LoginUtils.loadLoginInfo(getApplicationContext());
        if (forms != null) {
            if (args == null) {
                Toast.makeText(this, R.string.please_login_once_before_use_offline_mode,
                        Toast.LENGTH_LONG).show();
            } else if (forms.getString(Const.BUNDLE_ID).equals(args.getString(Const.BUNDLE_ID)) &&
                    forms.getString(Const.BUNDLE_PASS).equals(args.getString(Const.BUNDLE_PASS))) {
                setResult(Const.LOGIN_MODE_OFFLINE);
                finish();
            } else {
                Toast.makeText(this, R.string.login_failure_please_check_id_pass,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onSuccessAuth(String token) {
        LoginUtils.storeLoginInfo(this, loginId, loginPass, token);
        setResult(Const.LOGIN_MODE_ONLINE);
        finish();
    }

    @Override
    public Loader<String> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case Const.LOADER_RAW:
                return new RawLoader(this, bundle.getString(Const.BUNDLE_URI));
            case Const.LOADER_LOGIN:
                return new LoginLoader(this, bundle.getString(Const.BUNDLE_RAW));
            default:
                break;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<String> stringLoader, String s) {
        if (stringLoader instanceof RawLoader) {
            if (TextUtils.isEmpty(s)) {
                Toast.makeText(this, R.string.login_failure_please_check_login_uri,
                        Toast.LENGTH_LONG).show();
            } else {
                Bundle args = new Bundle(1);
                args.putString(Const.BUNDLE_RAW, s);
                getSupportLoaderManager().restartLoader(Const.LOADER_LOGIN, args, this);
            }
        } else {
            if (TextUtils.isEmpty(s)) {
                Toast.makeText(this, R.string.login_failure_please_check_id_pass,
                        Toast.LENGTH_LONG).show();
            } else {
                onSuccessAuth(s);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<String> stringLoader) {
    }

    @Override
    protected void onStop() {
        super.onStop();
        mId.setText(null);
        mPass.setText(null);
        mSetUri.setText(LoginUtils.getBaseURI(this));
        mId.requestFocus();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}