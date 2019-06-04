package gocrew.locationreminders;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.IOException;
import java.util.Date;


public class addReminder extends AppCompatActivity {

    private EditText titleBox;
    private TextView locationBox;
    private Place selectedPlace;
    private boolean editReminder = false;
    private int loadedPrimaryKey;
    DatabaseHelper myDb;
    Spinner radiusBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);
        radiusBox = findViewById(R.id.radiusBox);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.radius_sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        radiusBox.setAdapter(adapter);

        myDb = new DatabaseHelper(this);
        titleBox = findViewById(R.id.titleBox);
        locationBox = findViewById(R.id.locationBox);

        if (getIntent().getExtras().get("clicked") != null) {
            try {
                editReminder = true;
                TextView textView = findViewById(R.id.textView);
                textView.setText(R.string.edit_reminder);

                Cursor res = myDb.getData((Integer.valueOf((String)getIntent().getExtras().get("clicked"))));
                res.moveToFirst();
                loadedPrimaryKey = res.getInt(0);
                titleBox.setText(res.getString(1));

                radiusBox.setSelection(res.getInt(6));

                if (res.getString(3).substring(0, res.getString(2).length()).equals(res.getString(2)))
                    locationBox.setText(res.getString(3));
                else
                    locationBox.setText(String.format("%s\n%s", res.getString(2), res.getString(3)));
            } catch (Exception e) {
                Toast.makeText(this, "addReminder-OnCreate" + e.toString(), Toast.LENGTH_LONG).show();
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);
        }
     } //assign text boxes to variables, populate fields if edit reminder, check for access fine location permissions

     public void checkFilledOut(final View view) throws IOException {
         boolean stopFlag = false;
         if (titleBox.getText().toString().trim().length() < 4) {
             titleBox.setError("The title must be at least 4 characters");
             stopFlag = true;
         }
         if (locationBox.getText().toString().equals("Select a location")) {
             locationBox.setError("A location is required.");
             stopFlag = true;
         }
         if (stopFlag) {
             return;
         }
             addReminderFunction();
             Intent intent = new Intent();
             setResult(RESULT_OK, intent);
             finish();
     } //check that title and location are not null

    private void addReminderFunction() {
        try {
            Integer now =(int) new Date().getTime() - 1800001; //time {30 minutes (and 1 msec)} ago so that it can still trigger notifications
            String title, name, address, latLng;
            Integer radius;

            title = titleBox.getText().toString().trim();
            radius = radiusBox.getSelectedItemPosition();



            if (selectedPlace == null) {
                Cursor res = myDb.getData(loadedPrimaryKey);
                res.moveToFirst();
                name = res.getString(2);
                address = res.getString(3);
                latLng = res.getString(4);
            }
            else {
                name = selectedPlace.getName().toString();
                address = selectedPlace.getAddress().toString();
                latLng = selectedPlace.getLatLng().toString();
            }

            if (name.contains("\u00b0"))
                name = address;
            if (address.equals(""))
                address = latLng;

            if (loadedPrimaryKey == 0)
                myDb.insertData(title, name, address, latLng, now, radius);
            else
                myDb.updateData(loadedPrimaryKey, title, name, address, latLng, now, radius);
            /*
            if (loadedPrimaryKey == 0) { //new reminder is being created
                boolean isInserted;
                if (selectedPlace.getName().toString().contains("\u00b0"))  //if user clicks "select this location and the location does not have a name, it will use the address instead of coordinates
                    isInserted = myDb.insertData(titleBox.getText().toString().trim(), selectedPlace.getAddress().toString(), selectedPlace.getAddress().toString(), selectedPlace.getLatLng().toString(), now);
                else
                    isInserted = myDb.insertData(titleBox.getText().toString().trim(), selectedPlace.getName().toString(), selectedPlace.getAddress().toString(), selectedPlace.getLatLng().toString(), now);

                if (!isInserted)
                    Toast.makeText(this, "DATA insertion failed", Toast.LENGTH_LONG).show();
            }
            else { //editing an existing reminder
                if (selectedPlace == null) {
                    Cursor res = myDb.getData(loadedPrimaryKey);
                    res.moveToFirst();
                    myDb.updateData(loadedPrimaryKey, titleBox.getText().toString().trim(), res.getString(2), res.getString(3), res.getString(4), now);
                }
                else {
                        if (selectedPlace.getName().toString().contains("\u00b0"))
                            myDb.updateData(loadedPrimaryKey, titleBox.getText().toString().trim(), selectedPlace.getAddress().toString(), selectedPlace.getAddress().toString(), selectedPlace.getLatLng().toString(), now);
                        else
                            myDb.updateData(loadedPrimaryKey, titleBox.getText().toString().trim(), selectedPlace.getName().toString(), selectedPlace.getAddress().toString(), selectedPlace.getLatLng().toString(), now);
                }
            }
            */
        } catch (Exception e) {
            Toast.makeText(this, "addReminderFunction: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }//writes new reminder to file, writes edited reminder to file

    public void openMapAPI(View view) {
        try {
            int PLACE_PICKER_REQUEST = 1;
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

            if (editReminder) {
                double lat, lng;
                if (selectedPlace != null) {
                    LatLng startLocationPiece = selectedPlace.getLatLng();
                    lat = startLocationPiece.latitude;
                    lng = startLocationPiece.longitude;
                }
                else {
                Cursor res = myDb.getData(loadedPrimaryKey);
                res.moveToFirst();
                String latOrLng = res.getString(4);

                lat = Double.parseDouble(latOrLng.substring(latOrLng.indexOf("(") + 1, latOrLng.indexOf(",")));
                lng = Double.parseDouble(latOrLng.substring(latOrLng.indexOf(",") + 1, latOrLng.indexOf(")")));
                }

                LatLng startLocationPiece1 = new LatLng(lat - .0019,lng - .0019);
                LatLng startLocationPiece2 = new LatLng(lat + .0019,lng + .0019);
                LatLngBounds startLocation = new LatLngBounds(startLocationPiece1, startLocationPiece2);
                builder.setLatLngBounds(startLocation);
            }





            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        }
        catch (Exception e) {
            Toast.makeText(this, "openMapAPI: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //opens the google place picker API

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                selectedPlace = PlacePicker.getPlace(data, this);
                if (selectedPlace.getAddress().toString().equals("")) {
                    locationBox.setText(selectedPlace.getLatLng().toString());
                }
                if (selectedPlace.getAddress().toString().substring(0, selectedPlace.getName().toString().length()).equals(selectedPlace.getName().toString())) {
                    locationBox.setText(selectedPlace.getAddress());
                }
                else {
                    if (selectedPlace.getName().toString().contains("\u00b0"))
                        locationBox.setText(selectedPlace.getAddress());
                    else
                        locationBox.setText(String.format("%s\n%s", selectedPlace.getName().toString(), selectedPlace.getAddress().toString()));
                }
                locationBox.setError(null);
            }
        }
    } //populates location box with the location selected and is saved in selectedPlace

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        menu.getItem(0).setVisible(false);

        if (!editReminder)
            menu.getItem(1).setVisible(false);
        return true;
    } //called when the MenuItem bar is created or changed

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.trash) {
            try {
                myDb.deleteData(loadedPrimaryKey);

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "onOptionsItemSelected: " + e.toString(), Toast.LENGTH_LONG).show();
            }
        }
        return true;
    } //when an item on the menu bar is clicked
    @Override
    public void onBackPressed() {
        try {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Save Reminder?")
                    .setMessage("Do you want to save your changes?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                checkFilledOut(null);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        }
        catch (Exception e) {
            Toast.makeText(this, "onBackPressed: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //create popup for user to decide to save or discard changes
}
