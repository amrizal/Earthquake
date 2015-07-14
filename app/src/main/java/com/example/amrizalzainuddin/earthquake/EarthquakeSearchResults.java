package com.example.amrizalzainuddin.earthquake;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

/**
 * Created by amrizal.zainuddin on 14/7/2015.
 */
public class EarthquakeSearchResults extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //create a new adapter and bind it to the list view
        adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, null,
                new String[]{EarthquakeProvider.KEY_SUMMARY},
                new int[]{android.R.id.text1}, 0);
        setListAdapter(adapter);

        //initiate the cursor loader
        getLoaderManager().initLoader(0, null, this);

        //get the launch intent
        parseIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent(getIntent());
    }

    private static String QUERY_EXTRA_KEY = "QUERY_EXTRA_KEY";

    private void parseIntent(Intent intent) {
        //if the activity was started to service a search request,
        //extract the search query
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String searchQuery = intent.getStringExtra(SearchManager.QUERY);

            //Perform the search, passing in the search query as an argument
            //to the cursor loader
            Bundle args = new Bundle();
            args.putString(QUERY_EXTRA_KEY, searchQuery);

            //restart the cursor loader to execute the new query
            getLoaderManager().restartLoader(0, args, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = "0";

        if(args != null){
            //extract the search query from the arguments
            query = args.getString(QUERY_EXTRA_KEY);
        }

        //Construct the new query in the form of a cursor loader
        String[] projection = {EarthquakeProvider.KEY_ID, EarthquakeProvider.KEY_SUMMARY};
        String where = EarthquakeProvider.KEY_SUMMARY
                        + " LIKE \"%" + query + "%\"";
        String[] whereArgs = null;
        String sortOrder = EarthquakeProvider.KEY_SUMMARY + " COLLATE LOCALIZED ASC";

        //create the new cursor loader
        return new CursorLoader(this, EarthquakeProvider.CONTENT_URI,
                projection, where, whereArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //replace the result cursor displayed by the cursor adapter wtith
        //the new result set
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //remove the existing result cursor from the list adapter
        adapter.swapCursor(null);
    }
}
