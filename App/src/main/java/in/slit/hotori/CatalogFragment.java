package in.slit.hotori;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.artifex.mupdf.MuPDFActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CatalogFragment extends Fragment implements SearchView.OnQueryTextListener {
    private ListView mListView;
    private BookAdapter mAdapter;
    private Filter mFilter;

    private int mID;
    private String mFileTitle;
    private String mFilePath;

    private AlertDialog mFilterDialog;

    private int mCurrentFilterStartYear;
    private int mCurrentFilterStartMonth;
    private int mCurrentFilterStartDay;
    private int mCurrentFilterEndYear;
    private int mCurrentFilterEndMonth;
    private int mCurrentFilterEndDay;

    int loginMode;

    private final String defOrder = "`" + Book.KEY_MODDATE + "` DESC";
    private final String offlineSelection = "`" + Book.KEY_CACHED + "` == 'true'";
    private final String searchSelection = "`" + Book.KEY_NAME + "` LIKE ?";
    private final String searchClassSelection = "`" + Book.KEY_CLASS_NAME + "` LIKE ?";

    CursorLoaderCallbacks mCursorLoaderCallbacks = new CursorLoaderCallbacks();
    BooleanLoaderCallbacks mBooleanLoaderCallbacks = new BooleanLoaderCallbacks();
    StringLoaderCallbacks mStringLoaderCallbacks = new StringLoaderCallbacks();

    public CatalogFragment(int mode) {
        loginMode = mode;
        Log.d("Login Mode: ", String.valueOf(loginMode));
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, null);
        mListView = (ListView) view.findViewById(R.id.listViewBookItem);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                onClickDocument(position);
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();

        getActivity().getSupportLoaderManager().initLoader(0, null, mCursorLoaderCallbacks);
        getActivity().getSupportLoaderManager().initLoader(0, null, mBooleanLoaderCallbacks);
        getActivity().getSupportLoaderManager().initLoader(0, null, mStringLoaderCallbacks);

        if (mAdapter == null) {
            mAdapter = new BookAdapter(getActivity(), null, false);
            if (loginMode == Const.LOGIN_MODE_ONLINE) {
                actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg_online)));
                StringBuilder url = new StringBuilder(Const.DEFAULT_CATALOG_URI)
                        .append("?token=")
                        .append(getAccessToken());
                Bundle args = new Bundle(1);
                args.putString(Const.BUNDLE_URI, new String(url));
                getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_RAW, args, mStringLoaderCallbacks);
            } else {
                actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg_offline)));
            }
        }
        getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);
        mListView.setTextFilterEnabled(true);

        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String word = "%" + constraint.toString() + "%";
                StringBuilder selection = new StringBuilder(searchSelection);
                if (loginMode == Const.LOGIN_MODE_OFFLINE) {
                    selection.append(" AND ").append(offlineSelection);
                }
                return getActivity().getContentResolver().query(
                        Book.CONTENT_URI,
                        null,
                        new String(selection),
                        new String[]{word},
                        defOrder
                );
            }
        });

        mFilter = mAdapter.getFilter();
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        getActivity().getSupportLoaderManager().destroyLoader(Const.LOADER_RAW);
//        getActivity().getSupportLoaderManager().destroyLoader(Const.LOADER_BINARY);
//        getActivity().getSupportLoaderManager().destroyLoader(Const.LOADER_STORE_CATALOG);
//        getActivity().getSupportLoaderManager().destroyLoader(Const.LOADER_CURSOR);
        mListView.setAdapter(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.catalog, menu);
        mListView.setTextFilterEnabled(true);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        ((SearchView) MenuItemCompat.getActionView(searchItem))
                .setOnQueryTextListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                createFilterDialog();
                return true;
            case R.id.action_sort:
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String queryText) {
        if (TextUtils.isEmpty(queryText)) {
            mFilter.filter(""); /* nullにすると入力フォームをクリアしても一覧が更新されなくなります */
        } else {
            mFilter.filter(queryText);
        }
        return true;
    }

    private void createFilterDialog() {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_catalog_filter, null);
        final RadioGroup radioGroupPeriod = (RadioGroup) dialogView.findViewById(R.id.radioGroupPeriod);
        final CheckBox checkBoxPeriodFrom = (CheckBox) dialogView.findViewById(R.id.checkBoxPeriodFrom);
        final CheckBox checkBoxPeriodTo = (CheckBox) dialogView.findViewById(R.id.checkBoxPeriodTo);
        final Button buttonPeriodFrom = (Button) dialogView.findViewById(R.id.buttonPeriodFrom);
        final Button buttonPeriodTo = (Button) dialogView.findViewById(R.id.buttonPeriodTo);
        final Spinner spinnerClass = (Spinner) dialogView.findViewById(R.id.spinnerClass);
        final EditText editTextSearch = (EditText) dialogView.findViewById(R.id.editTextSearch);
        final Spinner spinnerSearchBy = (Spinner) dialogView.findViewById(R.id.spinnerSearchBy);
        final RadioGroup radioGroupProtected = (RadioGroup) dialogView.findViewById(R.id.radioGroupProtected);
        final RadioButton radioButtonProtectNone = (RadioButton) dialogView.findViewById(R.id.radioButtonProtectNone);
        final RadioButton radioButtonProtected = (RadioButton) dialogView.findViewById(R.id.radioButtonProtected);
        final RadioButton radioButtonNoneProtected = (RadioButton) dialogView.findViewById(R.id.radioButtonNoneProtected);
        final RadioGroup radioGroupCached = (RadioGroup) dialogView.findViewById(R.id.radioGroupCached);
        final RadioButton radioButtonCachedNone = (RadioButton) dialogView.findViewById(R.id.radioButtonCachedNone);
        final RadioButton radioButtonCached = (RadioButton) dialogView.findViewById(R.id.radioButtonCached);
        final RadioButton radioButtonNoneCached = (RadioButton) dialogView.findViewById(R.id.radioButtonNoneCached);

        Calendar calendar = Calendar.getInstance();
        setButtonFilterStartPeriod(buttonPeriodFrom, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        setButtonFilterEndPeriod(buttonPeriodTo, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        checkBoxPeriodFrom.setEnabled(false);
        checkBoxPeriodTo.setEnabled(false);
        buttonPeriodFrom.setEnabled(false);
        buttonPeriodTo.setEnabled(false);
        checkBoxPeriodFrom.setChecked(true);
        checkBoxPeriodTo.setChecked(true);
        radioGroupPeriod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {
                if (resId == R.id.radioButtonPeriodBy) {
                    checkBoxPeriodFrom.setEnabled(true);
                    checkBoxPeriodTo.setEnabled(true);
                    buttonPeriodFrom.setEnabled(true);
                    buttonPeriodTo.setEnabled(true);
                } else {
                    checkBoxPeriodFrom.setEnabled(false);
                    checkBoxPeriodTo.setEnabled(false);
                    buttonPeriodFrom.setEnabled(false);
                    buttonPeriodTo.setEnabled(false);
                }
            }
        });
        buttonPeriodFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View button) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                setButtonFilterStartPeriod((Button) button, year, monthOfYear, dayOfMonth);
                            }
                        },
                        mCurrentFilterStartYear, mCurrentFilterStartMonth, mCurrentFilterStartDay);
                datePickerDialog.show();
            }
        });
        buttonPeriodTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View button) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                setButtonFilterEndPeriod((Button) button, year, monthOfYear, dayOfMonth);
                            }
                        },
                        mCurrentFilterEndYear, mCurrentFilterEndMonth, mCurrentFilterEndDay);
                datePickerDialog.show();
            }
        });

        ArrayAdapter<String> adapterClass = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        adapterClass.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterClass.add(getString(R.string.not_specified));
        Uri uri = Uri.parse(Book.CONTENT_URI.toString())
                .buildUpon()
                .appendQueryParameter("distinct", "true")
                .build();
        String selection = null;
        if (loginMode == Const.LOGIN_MODE_OFFLINE) {
            selection = offlineSelection;
        }
        Cursor cursor = getActivity().getContentResolver().query(
                uri,
                new String[]{Book.KEY_CLASS_NAME},
                selection,
                null,
                null
        );
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            adapterClass.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        spinnerClass.setAdapter(adapterClass);
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ArrayAdapter<String> adapterSearchBy = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        adapterSearchBy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterSearchBy.add(getString(R.string.contains));
        adapterSearchBy.add(getString(R.string.not_contains));
        adapterSearchBy.add(getString(R.string.start_with));
        adapterSearchBy.add(getString(R.string.end_with));
        spinnerSearchBy.setAdapter(adapterSearchBy);

        if (loginMode == Const.LOGIN_MODE_OFFLINE) {
            radioGroupCached.clearCheck();
            radioGroupCached.check(R.id.radioButtonCached);
            radioButtonCachedNone.setEnabled(false);
            radioButtonCached.setEnabled(false);
            radioButtonNoneCached.setEnabled(false);
        }

        mFilterDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_search)
                .setView(dialogView)
                .setPositiveButton(R.string.run, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        StringBuilder selectionBuilder = new StringBuilder();
                        if (!TextUtils.isEmpty(editTextSearch.getText())) {
                            String word = editTextSearch.getText().toString();
                            StringBuilder wordQueryBuilder = new StringBuilder();
                            switch (spinnerSearchBy.getSelectedItemPosition()) {
                                case 0:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '%")
                                            .append(word)
                                            .append("%'");
                                    break;
                                case 1:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" NOT LIKE '%")
                                            .append(word)
                                            .append("%'");
                                    break;
                                case 2:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '")
                                            .append(word)
                                            .append("%'");
                                    break;
                                case 3:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '%")
                                            .append(word)
                                            .append("'");
                                    break;
                                default:
                                    break;
                            }
                            selectionBuilder.append(new String(wordQueryBuilder));
                        }

                        if (radioGroupPeriod.getCheckedRadioButtonId() == R.id.radioButtonPeriodBy) {
                            if (checkBoxPeriodFrom.isChecked()) {
                                Date startDate;
                                String periodFrom = buttonPeriodFrom.getText().toString();
                                SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy'年'MM'月'dd'日'");
                                try {
                                    startDate = baseFormat.parse(periodFrom);
                                } catch (ParseException ex) {
                                    throw new RuntimeException("a bad date string.");
                                }
                                StringBuilder periodFromQueryBuilder = new StringBuilder(Book.KEY_MODDATE)
                                        .append(" >= ")
                                        .append(startDate.getTime());
                                queryAppendChecker(selectionBuilder).append(periodFromQueryBuilder);
                            }
                            if (checkBoxPeriodTo.isChecked()) {
                                Date endDate;
                                String periodTo = buttonPeriodTo.getText().toString();
                                SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy'年'MM'月'dd'日'");
                                try {
                                    endDate = baseFormat.parse(periodTo);
                                } catch (ParseException ex) {
                                    throw new RuntimeException("a bad date string.");
                                }
                                StringBuilder periodFromQueryBuilder = new StringBuilder(Book.KEY_MODDATE)
                                        .append(" < ")
                                        .append(endDate.getTime() + 86400000);
                                queryAppendChecker(selectionBuilder).append(periodFromQueryBuilder);
                            }
                        }

                        if (spinnerClass.getSelectedItemPosition() != 0) {
                            StringBuilder classQueryBuilder = new StringBuilder(Book.KEY_CLASS_NAME)
                                    .append(" == '")
                                    .append(spinnerClass.getSelectedItem().toString())
                                    .append("'");
                            queryAppendChecker(selectionBuilder).append(classQueryBuilder);
                        }

                        if (radioGroupProtected.getCheckedRadioButtonId() != R.id.radioButtonProtectNone) {
                            StringBuilder protectedQueryBuilder = new StringBuilder(Book.KEY_CONFIDENTIAL);
                            if (radioGroupProtected.getCheckedRadioButtonId() == R.id.radioButtonProtected) {
                                protectedQueryBuilder.append(" == 'true'");
                            } else {
                                protectedQueryBuilder.append(" == 'false'");
                            }
                            queryAppendChecker(selectionBuilder).append(protectedQueryBuilder);
                        }

                        if (radioGroupCached.getCheckedRadioButtonId() != R.id.radioButtonCachedNone &&
                                radioButtonCached.isEnabled()) {
                            StringBuilder cachedQueryBuilder = new StringBuilder(Book.KEY_CACHED);
                            if (radioGroupProtected.getCheckedRadioButtonId() == R.id.radioButtonCached) {
                                cachedQueryBuilder.append(" == 'true'");
                            } else {
                                cachedQueryBuilder.append(" == 'false'");
                            }
                            queryAppendChecker(selectionBuilder).append(cachedQueryBuilder);
                        }

                        Bundle args = new Bundle(1);
                        args.putString("selection", new String(selectionBuilder));
                        getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, args, mCursorLoaderCallbacks);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        mFilterDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mFilterDialog.show();
    }


    private StringBuilder queryAppendChecker(StringBuilder builder) {
        if (builder.length() != 0) {
            builder.append(" AND ");
        }
        return builder;
    }

    private void setButtonFilterStartPeriod(Button button, int year, int monthOfYear, int dayOfMonth) {
        mCurrentFilterStartYear = year;
        mCurrentFilterStartMonth = monthOfYear;
        mCurrentFilterStartDay = dayOfMonth;
        button.setText(
                String.valueOf(year) + getString(R.string.year) +
                        String.valueOf(monthOfYear + 1) + getString(R.string.month) +
                        String.valueOf(dayOfMonth) + getString(R.string.day)
        );
    }

    private void setButtonFilterEndPeriod(Button button, int year, int monthOfYear, int dayOfMonth) {
        mCurrentFilterEndYear = year;
        mCurrentFilterEndMonth = monthOfYear;
        mCurrentFilterEndDay = dayOfMonth;
        button.setText(
                String.valueOf(year) + getString(R.string.year) +
                        String.valueOf(monthOfYear + 1) + getString(R.string.month) +
                        String.valueOf(dayOfMonth) + getString(R.string.day)
        );
    }

    private void onClickDocument(int position) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        int serial = cursor.getInt(cursor.getColumnIndex(Book.KEY_SERIAL));
        String title = cursor.getString(cursor.getColumnIndex(Book.KEY_TITLE));
        String name = Const.getFileName(serial, title);
        mFilePath = Const.getFilePath(getActivity(), name);
        mFileTitle = cursor.getString(cursor.getColumnIndex(Book.KEY_NAME));
        mID = cursor.getInt(cursor.getColumnIndex(Book.KEY_ID));
        if (cursor.getString(cursor.getColumnIndex(Book.KEY_CACHED)).equals("true")) {
            Log.d(getClass().getSimpleName(), "Cache open");
            openPDF(mFilePath, mFileTitle);
        } else {
            Log.d(getClass().getSimpleName(), "Start download");
            int size = cursor.getInt(cursor.getColumnIndex(Book.KEY_SIZE));
            StringBuilder uri = new StringBuilder(Const.DEFAULT_DOWNLOAD_URI)
                    .append("?token=")
                    .append(getAccessToken())
                    .append("&serial=")
                    .append(String.valueOf(serial));
            Bundle args = new Bundle(3);
            args.putString(Const.BUNDLE_URI, new String(uri));
            args.putString(Const.BUNDLE_FILENAME, name);
            args.putInt(Const.BUNDLE_FILESIZE, size);
            getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_BINARY, args, mBooleanLoaderCallbacks);
        }
    }

    private void openPDF(String path, String title) {
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(getActivity(), MuPDFActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.putExtra("fileTitle", title);
        startActivity(intent);
    }

    private String getAccessToken() {
        String token = LoginUtils.loadAccessToken(getActivity());
        if (token.equals(Const.NULL)) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        }
        return token;
    }

    class CursorLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case Const.LOADER_CURSOR:
                    StringBuilder selectionBuilder = new StringBuilder();
                    String order = defOrder;
                    if (bundle != null) {
                        selectionBuilder.append(bundle.getString("selection"));
//                        order = bundle.getString("order");
                    }
                    if (loginMode == Const.LOGIN_MODE_OFFLINE) {
                        queryAppendChecker(selectionBuilder).append(offlineSelection);
                    }
                    return new CursorLoader(getActivity(), Book.CONTENT_URI, null, new String(selectionBuilder), null, order);
                default:
                    break;
            }
            return null;
        }
        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.swapCursor(cursor);
        }
        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mAdapter.swapCursor(null);
        }
    }

    class BooleanLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {
        @Override
        public Loader<Boolean> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case Const.LOADER_STORE_CATALOG:
                    return new StoreCatalogLoader(getActivity(), bundle.getString(Const.BUNDLE_RAW));
                case Const.LOADER_BINARY:
                    return new BinaryLoader(getActivity(), bundle);
                default:
                    break;
            }
            return null;
        }
        @Override
        public void onLoadFinished(Loader<Boolean> booleanLoader, Boolean b) {
            if (booleanLoader instanceof StoreCatalogLoader) {
                if (b) {
                    getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);
                }
            } else {
                if (b) {
                    ContentValues values = new ContentValues();
                    values.put(Book.KEY_CACHED, "true");
                    Uri bookUri = Uri.withAppendedPath(Book.CONTENT_URI, String.valueOf(mID));
                    getActivity().getContentResolver().update(bookUri, values, null, null);
                    openPDF(mFilePath, mFileTitle);
                } else {
                    Toast.makeText(getActivity(), "PDFのDL失敗",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
        @Override
        public void onLoaderReset(Loader<Boolean> booleanLoader) {
        }
    }

    class StringLoaderCallbacks implements LoaderManager.LoaderCallbacks<String> {
        @Override
        public Loader<String> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case Const.LOADER_RAW:
                    return new RawLoader(getActivity(), bundle.getString(Const.BUNDLE_URI));
                default:
                    break;
            }
            return null;
        }
        @Override
        public void onLoadFinished(Loader<String> stringLoader, String s) {
            if (!TextUtils.isEmpty(s)) {
                Bundle args = new Bundle(1);
                args.putString(Const.BUNDLE_RAW, s);
                getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_STORE_CATALOG, args, mBooleanLoaderCallbacks);
            } else {
                Toast.makeText(getActivity(), "カタログXMLのDL失敗",
                        Toast.LENGTH_LONG).show();
            }
        }
        @Override
        public void onLoaderReset(Loader<String> stringLoader) {
        }
    }
}
