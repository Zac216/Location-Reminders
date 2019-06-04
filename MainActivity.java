package gocrew.locationreminders;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<Integer> selectedLines = new ArrayList<>();
    private boolean hideTrash = true;

    DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseHelper(this);
        listView = findViewById(R.id.listv);

        addExistingReminders();

        //Starts the location tracking service if it was not already running
        if (!isMyServiceRunning(RSSPullService.class)) {
            Intent intent = new Intent(this, RSSPullService.class);
            startService(intent);
        }





    } //when the app is opened, start service if not already started

    public void newReminder(View view) {
        Intent intent = new Intent(this, addReminder.class);
        intent.putExtra("clicked", (String) null);
        startActivityForResult(intent, 0);
    } //opens the addReminder class

    public void addExistingReminders() {
        hideTrash = true;
        invalidateOptionsMenu();
        selectedLines.clear();
        listView.setAdapter(null);

        try {
            Cursor res = myDb.getAllData();
            if (res.getCount() == 0) {
                return;
            }
            String title, place;
            Integer ID;
            List<Map<String, String>> data = new ArrayList<>();

            while (res.moveToNext()) {
                ID = res.getInt(0);
                title = res.getString(1);
                place = res.getString(2);



                Map<String, String> datum = new HashMap<>(3);
                datum.put("ID", ID.toString());
                datum.put("title", title);
                datum.put("description", place);
                data.add(datum);
            }

            final SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.list_item, new String[]{"title", "description", "ID"}, new int[]{R.id.text1, R.id.text2, R.id.text3});
            listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        editReminder(i);
                    }
                });
                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                        longClickItem(view, i);
                        return true;
                    }
                });
        } catch (Exception e) {
            Toast.makeText(this, "addExistingReminders: " + e.toString(), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, RSSPullService.class);
        stopService(intent);
        startService(intent);
    } //populates the list view with the reminders in the database

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        addExistingReminders();
    } //refreshes the list view with the new reminder after the add reminder class is closed

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    } //checks to see if the service is running

    public void editReminder(int clicked) {
        try {
            if (selectedLines.size() > 0) {
                longClickItem(listView.getChildAt(clicked), clicked);
            } else {
                Map<String, String> datum = (Map<String, String>)listView.getItemAtPosition(clicked);

                Intent intent = new Intent(this, addReminder.class);
                intent.putExtra("clicked", datum.get("ID"));
                startActivityForResult(intent, 0);
            }
        } catch (Exception e) {
            Toast.makeText(this, "editReminder: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //brings you to the addReminder page and populates it with the reminder you clicked' information

    public void longClickItem(View view, int pos) {
        try {
            Map<String, String> datum = (Map<String, String>)listView.getItemAtPosition(pos);
            int color = Color.TRANSPARENT;
            Drawable background = view.getBackground();
            if (background instanceof ColorDrawable)
                color = ((ColorDrawable) background).getColor();

            if (Color.parseColor("lightgray") != color) {
                view.setBackgroundColor(Color.parseColor("lightgray"));

                selectedLines.add(Integer.valueOf(datum.get("ID")));
                hideTrash = false;
                invalidateOptionsMenu();
            } else {
                view.setBackgroundColor(android.R.drawable.edit_text);
                selectedLines.remove(Integer.valueOf(datum.get("ID")));
                if (selectedLines.size() == 0) {
                    hideTrash = true;
                    invalidateOptionsMenu();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "longClick: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //when an item is long clicked, highlight it and show the trash option

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        if (hideTrash) {
            MenuItem a = menu.getItem(1);
            a.setVisible(false);
        } else {
            MenuItem a = menu.getItem(1);
            a.setVisible(true);
        }
        return true;
    } //called when the MenuItem bar is created or changed. Hides or shows the trash

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.trash) {
            removeItems();
        }
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return true;
    } //when an item on the menu bar (ex trashcan) is clicked

    public void removeItems() {
        try {
            for (Integer ID : selectedLines ) {
                myDb.deleteData(ID);
                removeNotifications(ID);
            }
            selectedLines.clear();
            hideTrash = true;
            invalidateOptionsMenu();
            addExistingReminders();
        } catch (Exception e) {
            Toast.makeText(this, "removeItems: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //removes the selected items from the list view and database

    @Override
    public void onBackPressed() {
        try {
            if (selectedLines.size() > 0) {
                View view;
                for (int i = 0; i < listView.getCount(); i++) {
                    view = listView.getChildAt(i);
                    view.setBackgroundColor(android.R.drawable.edit_text);
                }
                selectedLines.clear();
                hideTrash = true;
                invalidateOptionsMenu();
            } else {
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "onBackPressed: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //if some items are selected, deselect them, else, close the app

    public void removeNotifications(Integer ID) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);

        if (ID == -1)
            nMgr.cancelAll();
        else
            nMgr.cancel(ID);
    }
    @Override
    public void onResume() {
        super.onResume();

        removeNotifications(-1);
    }





}










