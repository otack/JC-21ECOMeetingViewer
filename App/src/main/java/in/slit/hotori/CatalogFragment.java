package in.slit.hotori;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
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
    private ActionBar mActionBar;
    private ListView mListView;
    private BookAdapter mAdapter;
    private Filter mFilter;

    private int mID;
    private String mFileTitle;
    private String mFilePath;

    int mLoginMode;

    private static AlertDialog mFilterDialog;
    private static ProgressDialog mProgressDialogCatalogLoading;

    private View mDialogView;
    private RadioGroup mRadioGroupPeriod;
    private RadioButton mRadioButtonPeriodNone;
    private RadioButton mRadioButtonPeriodBy;
    private CheckBox mCheckBoxPeriodFrom;
    private CheckBox mCheckBoxPeriodTo;
    private Button mButtonPeriodFrom;
    private Button mButtonPeriodTo;
    private Spinner mSpinnerClass;
    private EditText mEditTextSearch;
    private Spinner mSpinnerSearchBy;
    private RadioGroup mRadioGroupProtected;
    private RadioButton mRadioButtonProtectNone;
    private RadioButton mRadioButtonProtected;
    private RadioButton mRadioButtonNoneProtected;
    private RadioGroup mRadioGroupCached;
    private RadioButton mRadioButtonCachedNone;
    private RadioButton mRadioButtonCached;
    private RadioButton mRadioButtonNoneCached;

    private int mCurrentFilterStartYear;
    private int mCurrentFilterStartMonth;
    private int mCurrentFilterStartDay;
    private int mCurrentFilterEndYear;
    private int mCurrentFilterEndMonth;
    private int mCurrentFilterEndDay;

    private boolean filterSavedPeriodEnabled = false;
    private String filterSavedPeriodFrom = "";
    private boolean filterSavedPeriodFromEnabled = true;
    private String filterSavedPeriodTo = "";
    private boolean filterSavedPeriodToEnabled = true;
    private String filterSavedSearch = "";
    private int filterSavedSearchBy = 0;
    private String filterSavedClass = "";
    private int filterSavedProtected = 0;
    private int filterSavedCached = 0;

    private final String defOrder = "`" + Book.KEY_MODDATE + "` DESC";
    private final String offlineSelection = "`" + Book.KEY_CACHED + "` == 'true'";
    private final String searchSelection = "`" + Book.KEY_NAME + "` LIKE ?";

    CursorLoaderCallbacks mCursorLoaderCallbacks = new CursorLoaderCallbacks();
    BooleanLoaderCallbacks mBooleanLoaderCallbacks = new BooleanLoaderCallbacks();
    StringLoaderCallbacks mStringLoaderCallbacks = new StringLoaderCallbacks();

    AsyncGetBinary mAsyncGetBinary;

    public CatalogFragment(int i) {
        mLoginMode = i;
        Log.d("Login Mode: ", String.valueOf(mLoginMode));
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

        createProgresDialog();
        createFilterDialog();

        mActionBar = ((MainActivity) getActivity()).getSupportActionBar();
        drawActionBarBackground();

        if (mAdapter == null) {
            mAdapter = new BookAdapter(getActivity(), null, false);

            getActivity().getSupportLoaderManager().initLoader(0, null, mCursorLoaderCallbacks);
            getActivity().getSupportLoaderManager().initLoader(0, null, mBooleanLoaderCallbacks);
            getActivity().getSupportLoaderManager().initLoader(0, null, mStringLoaderCallbacks);

            if (mLoginMode == Const.LOGIN_MODE_ONLINE) {
                StringBuilder url = new StringBuilder(Const.DEFAULT_CATALOG_URI)
                        .append("?token=")
                        .append(getAccessToken());
                Bundle args = new Bundle(1);
                args.putString(Const.BUNDLE_URI, new String(url));
                mProgressDialogCatalogLoading.show();
                getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_RAW, args, mStringLoaderCallbacks);
            }
        }
        getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);

        mListView.setTextFilterEnabled(true);
        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String word = "%" + constraint.toString() + "%";
                StringBuilder selection = new StringBuilder(searchSelection);
                if (mLoginMode == Const.LOGIN_MODE_OFFLINE) {
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
                onRestoreFilterState();
                mFilterDialog.show();
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

    private void drawActionBarBackground() {
        if (mLoginMode == Const.LOGIN_MODE_ONLINE) {
            mActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg_online)));
        } else {
            mActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg_offline)));
        }
    }

    private void createProgresDialog() {
        mProgressDialogCatalogLoading = new ProgressDialog(getActivity());
        mProgressDialogCatalogLoading.setMessage(getString(R.string.loading_catalog));
        mProgressDialogCatalogLoading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    private void createFilterDialog() {
        mDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_catalog_filter, null);
        mRadioGroupPeriod = (RadioGroup) mDialogView.findViewById(R.id.radioGroupPeriod);
        mRadioButtonPeriodNone = (RadioButton) mDialogView.findViewById(R.id.radioButtonPeriodNone);
        mRadioButtonPeriodBy = (RadioButton) mDialogView.findViewById(R.id.radioButtonPeriodBy);
        mCheckBoxPeriodFrom = (CheckBox) mDialogView.findViewById(R.id.checkBoxPeriodFrom);
        mCheckBoxPeriodTo = (CheckBox) mDialogView.findViewById(R.id.checkBoxPeriodTo);
        mButtonPeriodFrom = (Button) mDialogView.findViewById(R.id.buttonPeriodFrom);
        mButtonPeriodTo = (Button) mDialogView.findViewById(R.id.buttonPeriodTo);
        mSpinnerClass = (Spinner) mDialogView.findViewById(R.id.spinnerClass);
        mEditTextSearch = (EditText) mDialogView.findViewById(R.id.editTextSearch);
        mSpinnerSearchBy = (Spinner) mDialogView.findViewById(R.id.spinnerSearchBy);
        mRadioGroupProtected = (RadioGroup) mDialogView.findViewById(R.id.radioGroupProtected);
        mRadioButtonProtectNone = (RadioButton) mDialogView.findViewById(R.id.radioButtonProtectNone);
        mRadioButtonProtected = (RadioButton) mDialogView.findViewById(R.id.radioButtonProtected);
        mRadioButtonNoneProtected = (RadioButton) mDialogView.findViewById(R.id.radioButtonNoneProtected);
        mRadioGroupCached = (RadioGroup) mDialogView.findViewById(R.id.radioGroupCached);
        mRadioButtonCachedNone = (RadioButton) mDialogView.findViewById(R.id.radioButtonCachedNone);
        mRadioButtonCached = (RadioButton) mDialogView.findViewById(R.id.radioButtonCached);
        mRadioButtonNoneCached = (RadioButton) mDialogView.findViewById(R.id.radioButtonNoneCached);

        Calendar calendar = Calendar.getInstance();
        setButtonFilterStartPeriod(mButtonPeriodFrom, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        setButtonFilterEndPeriod(mButtonPeriodTo, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        mCheckBoxPeriodFrom.setEnabled(false);
        mCheckBoxPeriodTo.setEnabled(false);
        mButtonPeriodFrom.setEnabled(false);
        mButtonPeriodTo.setEnabled(false);
        mCheckBoxPeriodFrom.setChecked(true);
        mCheckBoxPeriodTo.setChecked(true);
        mRadioGroupPeriod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int resId) {
                if (resId == R.id.radioButtonPeriodBy) {
                    mCheckBoxPeriodFrom.setEnabled(true);
                    mCheckBoxPeriodTo.setEnabled(true);
                    mButtonPeriodFrom.setEnabled(true);
                    mButtonPeriodTo.setEnabled(true);
                } else {
                    mCheckBoxPeriodFrom.setEnabled(false);
                    mCheckBoxPeriodTo.setEnabled(false);
                    mButtonPeriodFrom.setEnabled(false);
                    mButtonPeriodTo.setEnabled(false);
                }
            }
        });
        mButtonPeriodFrom.setOnClickListener(new View.OnClickListener() {
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
        mButtonPeriodTo.setOnClickListener(new View.OnClickListener() {
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
        if (mLoginMode == Const.LOGIN_MODE_OFFLINE) {
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
        mSpinnerClass.setAdapter(adapterClass);

        ArrayAdapter<String> adapterSearchBy = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        adapterSearchBy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterSearchBy.add(getString(R.string.contains));
        adapterSearchBy.add(getString(R.string.not_contains));
        adapterSearchBy.add(getString(R.string.start_with));
        adapterSearchBy.add(getString(R.string.end_with));
        mSpinnerSearchBy.setAdapter(adapterSearchBy);

        if (mLoginMode == Const.LOGIN_MODE_OFFLINE) {
            mRadioGroupCached.clearCheck();
            mRadioGroupCached.check(R.id.radioButtonCached);
            mRadioButtonCachedNone.setEnabled(false);
            mRadioButtonCached.setEnabled(false);
            mRadioButtonNoneCached.setEnabled(false);
        }

        mFilterDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_search)
                .setView(mDialogView)
                .setPositiveButton(R.string.run, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        StringBuilder selectionBuilder = new StringBuilder();
                        if (!TextUtils.isEmpty(mEditTextSearch.getText())) {
                            String word = mEditTextSearch.getText().toString();
                            filterSavedSearch = word;
                            StringBuilder wordQueryBuilder = new StringBuilder();
                            switch (mSpinnerSearchBy.getSelectedItemPosition()) {
                                case 0:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '%")
                                            .append(word)
                                            .append("%'");
                                    filterSavedSearchBy = 0;
                                    break;
                                case 1:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" NOT LIKE '%")
                                            .append(word)
                                            .append("%'");
                                    filterSavedSearchBy = 1;
                                    break;
                                case 2:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '")
                                            .append(word)
                                            .append("%'");
                                    filterSavedSearchBy = 2;
                                    break;
                                case 3:
                                    wordQueryBuilder.append(Book.KEY_NAME)
                                            .append(" LIKE '%")
                                            .append(word)
                                            .append("'");
                                    filterSavedSearchBy = 3;
                                    break;
                                default:
                                    break;
                            }
                            selectionBuilder.append(new String(wordQueryBuilder));
                        } else {
                            filterSavedSearch = "";
                        }

                        String periodFrom = mButtonPeriodFrom.getText().toString();
                        String periodTo = mButtonPeriodTo.getText().toString();
                        if (mRadioGroupPeriod.getCheckedRadioButtonId() == R.id.radioButtonPeriodBy) {
                            if (mCheckBoxPeriodFrom.isChecked()) {
                                Date startDate;
                                SimpleDateFormat baseFormat = new SimpleDateFormat(Const.DEFAULT_DATE_FORMAT);
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
                            if (mCheckBoxPeriodTo.isChecked()) {
                                Date endDate;
                                SimpleDateFormat baseFormat = new SimpleDateFormat(Const.DEFAULT_DATE_FORMAT);
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
                            filterSavedPeriodEnabled = true;
                        } else {
                            filterSavedPeriodEnabled = false;
                        }
                        if (mCheckBoxPeriodFrom.isChecked()) {
                            filterSavedPeriodFromEnabled = true;
                        } else {
                            filterSavedPeriodFromEnabled = false;
                        }
                        if (mCheckBoxPeriodTo.isChecked()) {
                            filterSavedPeriodToEnabled = true;
                        } else {
                            filterSavedPeriodToEnabled = false;
                        }
                        filterSavedPeriodFrom = periodFrom;
                        filterSavedPeriodTo = periodTo;

                        String itemClass;
                        if (mSpinnerClass.getSelectedItemPosition() != 0) {
                            itemClass = mSpinnerClass.getSelectedItem().toString();
                            StringBuilder classQueryBuilder = new StringBuilder(Book.KEY_CLASS_NAME)
                                    .append(" == '")
                                    .append(itemClass)
                                    .append("'");
                            queryAppendChecker(selectionBuilder).append(classQueryBuilder);
                        } else {
                            itemClass = mSpinnerClass.getItemAtPosition(0).toString();
                        }
                        filterSavedClass = itemClass;

                        if (mRadioGroupProtected.getCheckedRadioButtonId() != R.id.radioButtonProtectNone) {
                            StringBuilder protectedQueryBuilder = new StringBuilder(Book.KEY_CONFIDENTIAL);
                            if (mRadioGroupProtected.getCheckedRadioButtonId() == R.id.radioButtonProtected) {
                                protectedQueryBuilder.append(" == 'true'");
                                filterSavedProtected = 1;
                            } else {
                                protectedQueryBuilder.append(" == 'false'");
                                filterSavedProtected = 2;
                            }
                            queryAppendChecker(selectionBuilder).append(protectedQueryBuilder);
                        } else {
                            filterSavedProtected = 0;
                        }

                        if (mRadioGroupCached.getCheckedRadioButtonId() != R.id.radioButtonCachedNone) {
                            StringBuilder cachedQueryBuilder = new StringBuilder(Book.KEY_CACHED);
                            if (mRadioGroupCached.getCheckedRadioButtonId() == R.id.radioButtonCached) {
                                cachedQueryBuilder.append(" == 'true'");
                                filterSavedCached = 1;
                            } else {
                                cachedQueryBuilder.append(" == 'false'");
                                filterSavedCached = 2;
                            }
                            queryAppendChecker(selectionBuilder).append(cachedQueryBuilder);
                        } else {
                            filterSavedCached = 0;
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
    }

    private void onRestoreFilterState() {
        mRadioGroupPeriod.clearCheck();
        if (filterSavedPeriodEnabled) {
            mRadioButtonPeriodBy.setChecked(true);
        } else {
            mRadioButtonPeriodNone.setChecked(true);
        }
        mCheckBoxPeriodFrom.setChecked(filterSavedPeriodFromEnabled);
        mCheckBoxPeriodTo.setChecked(filterSavedPeriodToEnabled);
        if (filterSavedPeriodFrom.length() != 0) mButtonPeriodFrom.setText(filterSavedPeriodFrom);
        if (filterSavedPeriodTo.length() != 0) mButtonPeriodTo.setText(filterSavedPeriodTo);

        int position = 0;
        for (int i = 0; i < mSpinnerClass.getCount(); i++) {
            if (filterSavedClass.equals(mSpinnerClass.getItemAtPosition(i).toString())) {
                position = i;
                break;
            }
        }
        mSpinnerClass.setSelection(position);

        if (filterSavedSearch.length() != 0) mEditTextSearch.setText(filterSavedSearch);
        mSpinnerSearchBy.setSelection(filterSavedSearchBy);

        mRadioGroupProtected.clearCheck();
        switch (filterSavedProtected) {
            case 0: mRadioButtonProtectNone.setChecked(true); break;
            case 1: mRadioButtonProtected.setChecked(true); break;
            case 2: mRadioButtonNoneProtected.setChecked(true); break;
            default: break;
        }

        if (mLoginMode != Const.LOGIN_MODE_OFFLINE) {
            mRadioGroupCached.clearCheck();
            switch (filterSavedCached) {
                case 0: mRadioButtonCachedNone.setChecked(true); break;
                case 1: mRadioButtonCached.setChecked(true); break;
                case 2: mRadioButtonNoneCached.setChecked(true); break;
                default: break;
            }
        }
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
            final int size = cursor.getInt(cursor.getColumnIndex(Book.KEY_SIZE));
            StringBuilder uri = new StringBuilder(Const.DEFAULT_DOWNLOAD_URI)
                    .append("?token=")
                    .append(getAccessToken())
                    .append("&serial=")
                    .append(String.valueOf(serial));
            Bundle args = new Bundle(3);
            args.putString(Const.BUNDLE_URI, new String(uri));
            args.putString(Const.BUNDLE_FILENAME, name);
            args.putInt(Const.BUNDLE_FILESIZE, size);

            mAsyncGetBinary = new AsyncGetBinary(getActivity(), new AsyncCallback() {
                PowerManager.WakeLock wakeLock;
                ProgressDialog progressDialog;
                @Override
                public void onPreExecute() {
                    PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            getClass().getName());
                    wakeLock.acquire();
                    progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(getString(R.string.downloading_book));
                    progressDialog.setMax(size / 1024);
                    progressDialog.setProgressNumberFormat("%1d / %2d KB");
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(true);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.stop), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mAsyncGetBinary.cancel(true);
                        }
                    });
                    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            mAsyncGetBinary.cancel(true);
                        }
                    });
                    progressDialog.show();
                }
                @Override
                public void onProgressUpdate(int progress) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setProgress(progress);
                }
                @Override
                public void onPostExecute(boolean hasCompleted) {
                    wakeLock.release();
                    progressDialog.dismiss();
                    if (hasCompleted) {
                        ContentValues values = new ContentValues();
                        values.put(Book.KEY_CACHED, "true");
                        Uri bookUri = Uri.withAppendedPath(Book.CONTENT_URI, String.valueOf(mID));
                        getActivity().getContentResolver().update(bookUri, values, null, null);
                        openPDF(mFilePath, mFileTitle);
                    } else {
                        Toast.makeText(getActivity(), R.string.download_failure_please_try_again_later,
                                Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onCancelled() {
                    Log.d(getTag(), "PDF file download canceled.");
                    wakeLock.release();
                    progressDialog.dismiss();
                }
            }, args);
            mAsyncGetBinary.execute(null);
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
                    if (mLoginMode == Const.LOGIN_MODE_OFFLINE) {
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
        public void onLoadFinished(Loader<Boolean> booleanLoader, Boolean isCompleted) {
            if (booleanLoader instanceof StoreCatalogLoader) {
                if (isCompleted) {
                    mProgressDialogCatalogLoading.dismiss();
                    getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);
                } else {
                    Log.d(getTag(), "Catalog XML download failed.");
                }
            } else {
                if (isCompleted) {
                    ContentValues values = new ContentValues();
                    values.put(Book.KEY_CACHED, "true");
                    Uri bookUri = Uri.withAppendedPath(Book.CONTENT_URI, String.valueOf(mID));
                    getActivity().getContentResolver().update(bookUri, values, null, null);
                    openPDF(mFilePath, mFileTitle);
                } else {
                    Log.d(getTag(), "PDF file download failed.");
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
        public void onLoadFinished(Loader<String> stringLoader, String data) {
            if (!TextUtils.isEmpty(data)) {
                Bundle args = new Bundle(1);
                args.putString(Const.BUNDLE_RAW, data);
                getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_STORE_CATALOG, args, mBooleanLoaderCallbacks);
            } else {
                Log.d(getTag(), "Catalog XML store failed.");
            }
        }
        @Override
        public void onLoaderReset(Loader<String> stringLoader) {
        }
    }
}
