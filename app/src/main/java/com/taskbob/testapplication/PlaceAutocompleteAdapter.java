package com.taskbob.testapplication;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by Neha on 11/08/15.
 */
public class PlaceAutocompleteAdapter extends ArrayAdapter<PlaceAutocompleteAdapter.PlaceAutocomplete> implements Filterable{

    private ArrayList<PlaceAutocomplete> resultList;
    private GoogleApiClient googleApiClient;
    private LatLngBounds bounds;
    private AutocompleteFilter placeFilter;
    private static final String TAG = "tag";

    public PlaceAutocompleteAdapter(Context _context, int _resource, GoogleApiClient _googleApiClient,
                                    LatLngBounds _bounds, AutocompleteFilter _filter) {
        super(_context, _resource);
        googleApiClient = _googleApiClient;
        bounds = _bounds;
        placeFilter = _filter;
    }

    public void setBounds(LatLngBounds _bounds){
        bounds = _bounds;
    }

    @Override
    public int getCount(){
        return resultList.size();
    }

    @Override
    public PlaceAutocomplete getItem(int position){
        return resultList.get(position);
    }

    private ArrayList<PlaceAutocomplete> getAutocomplete(CharSequence constraint){

        if(googleApiClient.isConnected()) {
            Log.i(TAG, "Starting autocomplete query for: " + constraint);


            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi.getAutocompletePredictions(googleApiClient, constraint.toString(),
                            bounds, placeFilter);

            AutocompletePredictionBuffer autocompletePredictions = results.await(60, TimeUnit.SECONDS);

            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                Toast.makeText(getContext(), "Error contacting API: " + status.toString(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error getting autocomplete prediction API call: " + status.toString());
                autocompletePredictions.release();
                return null;
            }

            Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount() + " predictions.");
            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            ArrayList resultList = new ArrayList<PlaceAutocomplete>(autocompletePredictions.getCount());

            while (iterator.hasNext()) {
                AutocompletePrediction prediction = iterator.next();
                resultList.add(new PlaceAutocomplete(prediction.getPlaceId(), prediction.getDescription()));
            }

            autocompletePredictions.release();

            return resultList;
        }

        Log.e(TAG, "Google API client is not connected for autocomplete query.");
        return null;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                // Skip the autocomplete query if no constraints are given.
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    resultList = getAutocomplete(constraint);
                    if (resultList != null) {
                        // The API successfully returned results.
                        results.values = resultList;
                        results.count = resultList.size();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged();
                } else {
                    // The API did not return any results, invalidate the data set.
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    class PlaceAutocomplete {

        public CharSequence placeId;
        public CharSequence description;

        PlaceAutocomplete(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description.toString();
        }
    }
}
