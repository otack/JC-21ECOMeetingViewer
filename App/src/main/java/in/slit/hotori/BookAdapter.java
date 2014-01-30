package in.slit.hotori;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends CursorAdapter {

    private LayoutInflater mInflater;

    private List<Cursor> mListCursors;
    private List<Cursor> mOriginalCursors;

    class ViewHolder {
        TextView name;
        TextView cached;
    }

    public BookAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        String name = cursor.getString(cursor
                .getColumnIndexOrThrow(Book.KEY_NAME));
        String cached = cursor.getString(cursor
                .getColumnIndexOrThrow(Book.KEY_CACHED));
        holder.name.setText(name);
        holder.cached.setText(cached);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.adapter_item, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.name = (TextView) view.findViewById(R.id.textViewItem);
        holder.cached = (TextView) view.findViewById(R.id.textViewCached);
        view.setTag(holder);
        return view;
    }


    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {
            CharSequence mConstraint;

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mListCursors = (List<Cursor>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Cursor> FilteredArrayNames = new ArrayList<>();
                if (mOriginalCursors == null) {
                    mOriginalCursors = new ArrayList<>(mListCursors);
                }
                if (constraint == null || constraint.length() == 0) {
                    results.count = mOriginalCursors.size();
                    results.values = mOriginalCursors;
                } else {
                    mConstraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < mOriginalCursors.size(); i++) {
                        Log.e("-----------", mOriginalCursors.get(i).getString(mOriginalCursors.get(i).getColumnIndex(Book.KEY_NAME)));
                        if (isFilterPassed(String.valueOf(mOriginalCursors.get(i).getString(mOriginalCursors.get(i).getColumnIndex(Book.KEY_NAME))))) {
                            FilteredArrayNames.add(mOriginalCursors.get(i));
//                        } else if (isFilterPassed(mOriginalCursors.get(i).name)) {
//                            FilteredArrayNames.add(mOriginalCursors.get(i));
//                        } else if (isFilterPassed(String.valueOf(mOriginalCursors.get(i).price))) {
//                            FilteredArrayNames.add(mOriginalCursors.get(i));
                        }
                    }
                    results.count = FilteredArrayNames.size();
                    results.values = FilteredArrayNames;
                }
                return results;
            }

            private boolean isFilterPassed(String s) {
                if (s.toLowerCase().contains(mConstraint.toString())) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        return filter;
    }
}

//        String date = cursor.getString(cursor.getColumnIndex(Book.KEY_MODDATE));
//        StringBuilder builder = new StringBuilder(date).delete(23, 29);
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'H:m:s.S");
//        try {
//            item.moddate = format.parse(new String(builder));
//        } catch (ParseException ex) {
//            throw new RuntimeException("a bad date string.");
//        }
//        item.size = cursor.getInt(cursor.getColumnIndex(Book.KEY_SIZE));
//        item.vlt = cursor.getInt(cursor.getColumnIndex(Book.KEY_VOLATILE)) == 1;
//        item.confidential = cursor.getInt(cursor.getColumnIndex(Book.KEY_CONFIDENTIAL)) == 1;
//        item.className = cursor.getString(cursor.getColumnIndex(Book.KEY_CLASS_NAME));
//        item.classId = cursor.getInt(cursor.getColumnIndex(Book.KEY_CLASS_ID));
//        item.classCode = cursor.getString(cursor.getColumnIndex(Book.KEY_CLASS_CODE));
//        item.subject = cursor.getInt(cursor.getColumnIndex(Book.KEY_SUBJECT)) == 1;
//        item.cached = cursor.getInt(cursor.getColumnIndex(Book.KEY_CACHED)) == 1;