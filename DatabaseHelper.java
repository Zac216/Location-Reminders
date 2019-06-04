package gocrew.locationreminders;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "reminders.db";
    private static final String TABLE_NAME = "reminders_table";
    // COL_0 = "ID";
    // COL_1 = "title";
    // COL_2 = "place";
    // COL_3 = "address";
    // COL_4 = "latLng";
    // COL_5 = "userNotified";
    // COL_6 = "radius"



    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    } //default constructor (do not delete)

    @Override
    public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, place TEXT, address TEXT, latLng TEXT, userNotified INTEGER, radius INTEGER)");
    } //creates the table to hold and access the data using the columns listed above: PART 1

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    } //creates the table to hold and access the data using the columns listed above: PART 2

    boolean insertData(String title, String place, String address, String latLng, Integer userNotified, Integer radius) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", title);
        contentValues.put("place", place);
        contentValues.put("address", address);
        contentValues.put("latLng", latLng);
        contentValues.put("userNotified", userNotified);
        contentValues.put("radius", radius);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    } //inserts new data to the table. Returns true if successful

    Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY ID", null);
    } //get all data from the table and return it as a cursor

    Cursor getData(Integer ID) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE ID='" + ID + "'", null);
    } //get a row from the table by primary key

    void deleteData(Integer ID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE ID='" + ID + "'");
    } //deletes a row from the table by primary key

    void updateData(Integer ID, String title, String place, String address, String latLng, Integer userNotified, Integer radius) {
        SQLiteDatabase db = this.getWritableDatabase();

        String strFiler = "ID=" + ID;
        ContentValues args = new ContentValues();
        args.put("title", title);
        args.put("place", place);
        args.put("address", address);
        args.put("latLng", latLng);
        args.put("userNotified", userNotified);
        args.put("radius", radius);
        db.update(TABLE_NAME, args, strFiler, null);
    } //edit a row by primary key

}
