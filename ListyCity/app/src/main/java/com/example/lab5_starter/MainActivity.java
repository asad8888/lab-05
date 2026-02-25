package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;



    private Button deleteCityButton;
    private boolean deleteMode = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);


        deleteCityButton = findViewById(R.id.buttonDeleteCity);

        deleteCityButton.setOnClickListener(v -> {
            deleteMode = !deleteMode;
            deleteCityButton.setText(deleteMode ? "Delete Mode: ON" : "Delete Mode: OFF");
        });

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // addDummyData();

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            cityArrayList.clear();

            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
            }

            cityArrayAdapter.notifyDataSetChanged();
        });


        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) return;

            if (deleteMode) {
                citiesRef.document(city.getName())
                        .delete()
                        .addOnFailureListener(e -> Log.e("Firestore", "Delete failed", e));
            } else {
                CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
                cityDialogFragment.show(getSupportFragmentManager(),"City Details");
            }
        });

    }


    @Override
    public void updateCity(City city, String title, String year) {
        // Save old doc id BEFORE modifying the object
        String oldName = city.getName();

        // Apply changes locally (optional; Firestore listener will refresh anyway)
        city.setName(title);
        city.setProvince(year);

        // If the name changed, doc id changed => delete old document then create new one
        if (!oldName.equals(title)) {
            citiesRef.document(oldName)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        DocumentReference newDoc = citiesRef.document(title);
                        newDoc.set(city)
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Failed to write updated city", e));
                    })
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Failed to delete old city doc", e));
        } else {
            // Name unchanged => overwrite same document with updated fields
            citiesRef.document(oldName)
                    .set(city)
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Failed to update city", e));
        }

        // You can remove this later; snapshot listener should handle UI updates
        cityArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

}