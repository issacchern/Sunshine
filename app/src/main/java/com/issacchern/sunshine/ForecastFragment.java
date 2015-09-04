package com.issacchern.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.issacchern.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int FORECAST_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    private ForecastAdapter mForecastAdapter;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;
    private static final String SELECTED_KEY = "selected_position";

    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }



    public ForecastFragment() {
    }

    //where the fragment is created, and happens before onCreateView()
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            Toast.makeText(getActivity(), "Refreshed!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        //String[] data = {
        //        "Mon 6/23 - Sunny - 31/17",
        //        "Tue 6/24 - Foggy - 21/8",
        //        "Wed 6/25 - Cloudy - 22/17",
        //        "Thurs 6/26 - Rainy - 18/11",
        //        "Fri 6/27 - Foggy - 21/10",
        //        "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
        //        "Sun 6/29 - Sunny - 20/7"
        //};
        //List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
    //    mForecastAdapter =
    //            new ArrayAdapter<String>(
    //                   getActivity(), // The current context (this activity)
    //                    R.layout.list_item_forecast, // The name of the layout ID.
    //                    R.id.list_item_forecast_textview, // The ID of the textview to populate.
    //                    new ArrayList<String>());

    //    String locationSetting = Utility.getPreferredLocation(getActivity());

    //    String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
    //    Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
    //            locationSetting, System.currentTimeMillis());
    //    Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri, null, null,
    //            null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getActivity(),null,0);



        /**
        reference our UI layout resource, call fragment_main
         When our activity runs, it creates this placeholder (forecastFragment.java) fragment
         which then inflates the XML layout resource, convert everything in the XML file to
         a hierarchy of view objects in memory
         **/
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        // Get a reference to the ListView, and attach this adapter to it (fragment_main.xml).
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if(cursor != null){
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                //    Intent intent = new Intent(getActivity(),DetailActivity.class).
                //            setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                //                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                //            ));
                //    startActivity(intent);
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            locationSetting, cursor.getLong(COL_WEATHER_DATE)
                    ));
                }
                mPosition = position;

            }
        });

        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)){
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

    //    listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
    //        @Override
    //        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    //            String forecast = mForecastAdapter.getItem(position);
    //            //Toast.makeText(getActivity(),forecast,Toast.LENGTH_SHORT).show();
    //            Intent intent = new Intent(getActivity(), DetailActivity.class)
    //                    .putExtra(Intent.EXTRA_TEXT,forecast);
    //            startActivity(intent);
                
    //        }
    //   });

        mForecastAdapter.setmUseTodayLayout(mUseTodayLayout);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void updateWeather(){
    //    FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity(), mForecastAdapter);
    //    //SharedPreferences is interface for accessing and modifying preference data
    //    //returned by getSharedPreferences(String,int)
    //    //PreferenceManager.getDafaultSharedPreferences

    //    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //abstract string can be assessed by interface?
    //    String location = prefs.getString(getString(R.string.pref_location_key),
    //           getString(R.string.pref_location_default));

        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);  //execute the WeatherTask with location passing in
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        updateWeather();
//    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION){
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mForecastAdapter != null){
            mForecastAdapter.setmUseTodayLayout(mUseTodayLayout);
        }
    }
}
