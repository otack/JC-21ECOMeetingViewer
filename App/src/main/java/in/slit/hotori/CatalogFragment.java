package in.slit.hotori;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.Toast;

import com.artifex.mupdf.MuPDFActivity;

public class CatalogFragment extends Fragment implements SearchView.OnQueryTextListener {
    private ListView mListView;
    private BookAdapter mAdapter;
    private Filter mFilter;

    private int mID;
    private String mFileTitle;
    private String mFilePath;

    int loginMode;

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
            getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(Const.LOADER_CURSOR, null, mCursorLoaderCallbacks);
        }
        mListView.setTextFilterEnabled(true);

        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String word = "%" + constraint.toString() + "%";
                Cursor cursor = getActivity().getContentResolver().query(
                        Book.CONTENT_URI,
                        null,
                        "`" + Book.KEY_NAME + "` LIKE ?",
                        new String[]{word},
                        Book.KEY_MODDATE + " DESC"
                );
                return cursor;
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
//            mFilter.filter(null);
//            mAdapter.runQueryOnBackgroundThread(null);
//            mListView.clearTextFilter();
//            Filter lFilter = mAdapter.getFilter();
            mFilter.filter("");
        } else {
//            mFilter.filter(queryText);
//            mAdapter.runQueryOnBackgroundThread(queryText);
//            mListView.setFilterText(queryText);
//            Filter lFilter = mAdapter.getFilter();
            mFilter.filter(queryText);
        }
        return true;
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
                    if (loginMode == Const.LOGIN_MODE_ONLINE) {
                        return new CursorLoader(getActivity(), Book.CONTENT_URI, null, null, null, null);
                    } else {
                        return new CursorLoader(getActivity(), Book.CONTENT_URI, null, Book.KEY_CACHED + " LIKE 'true'", null, null);
                    }
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
