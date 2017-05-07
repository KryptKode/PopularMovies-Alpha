package com.kryptkode.cyberman.popularmovies;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kryptkode.cyberman.popularmovies.adapters.MovieAdapter;
import com.kryptkode.cyberman.popularmovies.data_models.Movie;
import com.kryptkode.cyberman.popularmovies.utilities.JsonGet;
import com.kryptkode.cyberman.popularmovies.utilities.NetworkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

//TODO: Remove Commented Code

public class MainActivity extends AppCompatActivity {

   //Saved instance state variables
    private  Bundle moviesState;
    private ConnectivityChangeReceiver receiver;
    private boolean receiverRegistered;
    private static final String MOVIES_STATE = "MOVIES_STATE";

  //Widgets
    private TextView errorTextView;
    private ImageView poster_movie;
    private ProgressBar progressBar;

    private String moviesUrl; // holds the movieDB URL

    private int pageNumber = 1; //sets the page number in the request
    boolean dataLoaded = false; //checks if the data has loaded in order to load more pages

    private MovieAdapter movieAdapter; // instance of the movie adapter
    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ArrayList<Movie> moviesStack = new ArrayList<>();

    private Toast internetNotConnectedToast; //Displays toast when connection goes offline


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //reference to the widgets
        progressBar=  (ProgressBar) findViewById(R.id.loading_indicator);
        errorTextView = (TextView) findViewById(R.id.error_text_view);
        poster_movie = (ImageView) findViewById(R.id.movie_poster);
        errorTextView.setText(R.string.error_message);

        //initialize the URl to the default URL
        moviesUrl = NetworkUtil.buildPopularMoviesURL().toString();

        //reference to the recycler view
        recyclerView = (RecyclerView) findViewById(R.id.rv_movies);
        gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addOnScrollListener(loadMoreData); //listener for endless scrolling

        movieAdapter = new MovieAdapter(this, moviesStack);
        recyclerView.setAdapter(movieAdapter);
        recyclerView.addOnScrollListener(loadMoreData);

        //TODO: add Broadcast receiver for network state change
    //        receiver = new ConnectivityChangeReceiver();

        // Receiver for Network connectivity
        registerReceiver(new ConnectivityChangeReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        }

//TODO: Restore Data with SavedInstance State
   /* @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(MOVIES_STATE, moviesStack);
        Log.e("Save","onSaved: " + String.valueOf(moviesStack));
    }*/

//method to register receiver
    private void registerReceivers() {
        if(!receiverRegistered){
            registerReceiver(receiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            receiverRegistered = true;
        }
    }

    //checks for network connectivity, receiver class
    public class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetworkUtil.isOnline(getApplicationContext())) {
                getData();
                Toast.makeText(getApplicationContext(), R.string.is_connected_text, Toast.LENGTH_SHORT).show();
                showRecyclerView();
            } else {
                Toast.makeText(getApplicationContext(), R.string.is_not_connected_text, Toast.LENGTH_SHORT).show();
                if (!dataLoaded)
                    displayError();
            }
        }
    }

    //Endless Scrolling listener implementation
    RecyclerView.OnScrollListener loadMoreData = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
           super.onScrollStateChanged(recyclerView, newState);
            if (gridLayoutManager.findLastCompletelyVisibleItemPosition() == moviesStack.size() - 1) {
               if (dataLoaded) {
                   pageNumber++;
                   dataLoaded = false;


                   if (NetworkUtil.isOnline(getApplicationContext())) {
                       getData();
                   } else {
                       if (internetNotConnectedToast != null) {
                           internetNotConnectedToast.cancel();
                       }

                       internetNotConnectedToast = Toast.makeText(MainActivity.this, R.string.is_not_connected_text, Toast.LENGTH_SHORT);
                       internetNotConnectedToast.show();

                   }
               }
            }
        }
    };


    //reference to the menu item
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu, menu);
            return true;
        }

        //caalled when each item in the menu is selected
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int itemSelected = item.getItemId();
            switch (itemSelected) {
                case R.id.sort_by_popularity:
                    Toast.makeText(MainActivity.this, R.string.sort_by_polularity_toast, Toast.LENGTH_SHORT).show();
                    //TODO: Add Code to set the parameter to SORT by POPULARITY
                    moviesUrl = NetworkUtil.buildPopularMoviesURL("popularity").toString(); //sets the URl to the right one for popularity
                    if (NetworkUtil.isOnline(getApplicationContext())){ //if there is internet connection
                        resetView();
                        getData();
                    }
                    else{
                        Toast.makeText(MainActivity.this, R.string.is_not_connected_text,
                                Toast.LENGTH_SHORT).show();

                    }
                    return true;

                case R.id.sort_by_rating:
                    Toast.makeText(MainActivity.this, R.string.sort_by_top_rated_toast, Toast.LENGTH_SHORT).show();
                    //TODO: Add Code to set the parameter to SORT by RATINGS
                    moviesUrl = NetworkUtil.buildPopularMoviesURL("rating").toString(); //sets the URl to the right one for rating
                    if (NetworkUtil.isOnline(getApplicationContext())){
                        resetView();
                        getData();
                    }
                    else{
                        Toast.makeText(MainActivity.this, R.string.is_not_connected_text,
                                Toast.LENGTH_SHORT).show();

                    }

                    return true;
            }
            return super.onOptionsItemSelected(item);
        }

    //resets the recycler view, called when menu option is selected
    private void resetView() {
            moviesStack.clear();
            pageNumber = 1;
            movieAdapter.notifyDataSetChanged();
        }

    //method to show th erecycler view, called if internet connection goes offline initially and then online subsequently
        public void showRecyclerView() {
            errorTextView.setVisibility(INVISIBLE);
            recyclerView.setVisibility(VISIBLE);
        }
    //displays the error message ehrn the internet connection is offline
        private void displayError() {
            errorTextView.setVisibility(VISIBLE);
            recyclerView.setVisibility(INVISIBLE);
        }

        //method to parse the JSON data
        private void loadMovieData(String jsonData) {
            String poster;
            String title;
            String releaseDate;
            String overview;
            double popularity;
            double vote_average;
            int id;
            if (jsonData != null) {
                try {
                    JSONObject movie = new JSONObject(jsonData);
                    JSONArray results = movie.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject currentJson = results.getJSONObject(i);
                        poster = currentJson.getString("poster_path");
                        title = currentJson.getString("original_title");
                        overview = currentJson.getString("overview");
                        releaseDate = currentJson.getString("release_date");
                        popularity = currentJson.getDouble("popularity");
                        vote_average = currentJson.getDouble("vote_average");
                        id = currentJson.getInt("id");
                        Movie movieObject = new Movie(title, poster, overview, releaseDate, vote_average, popularity, id);
                        moviesStack.add(movieObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


//method to get the JSON data
        public void getData(){
            if (NetworkUtil.isOnline(getApplicationContext())){
                new GetDataTask().execute();
            }
            else{
                Toast.makeText(MainActivity.this, R.string.is_not_connected_text, Toast.LENGTH_SHORT).show();
                displayError();
            }
        }


// thread for connecting to the internet and getting the JSOn data
            public class GetDataTask extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(VISIBLE);
            }

            @Override
            protected String doInBackground(Void... params) {
                String str = null;
                JSONObject jsonObject = JsonGet.getDataFromWeb(moviesUrl, pageNumber);
                if(jsonObject != null)
                    str = jsonObject.toString();
                return str;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loadMovieData(s);
                progressBar.setVisibility(INVISIBLE);
                showRecyclerView();
                dataLoaded = true;
                movieAdapter.notifyDataSetChanged();
            }
        }
}