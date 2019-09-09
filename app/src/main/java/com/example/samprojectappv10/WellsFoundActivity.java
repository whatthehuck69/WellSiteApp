package com.example.samprojectappv10;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
//excel imports jxl.jar from app/libs.jxl.jar
import jxl.*;
import jxl.read.biff.BiffException;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import java.io.*;
import java.util.List;
import java.util.Locale;

import static android.content.res.AssetManager.*;

public class WellsFoundActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private LocationListener listener;
    private LocationProvider provider;
    private double latitude;
    private double longitude;
    private TextView coordinatesText;
    private TextView well_idText;
    private Button resetLocationButton;
    private Button useWellButton;
    private Button findWellManuallyButton;
    private Button newWellButton;
    private static String EXCEL_FILE_LOCATION = "TrialData.xls";
    private static String UPDATE_FILE_LOCATION = "updates.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wells_found);

        resetLocationButton = findViewById(R.id.resetLocationButton);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationProvider provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        latitude = 0.0;
        longitude = 0.0;
        coordinatesText = findViewById(R.id.coordinatesText);
        well_idText = findViewById(R.id.well_idText);
        useWellButton = findViewById(R.id.useWellButton);
        findWellManuallyButton = findViewById(R.id.findWellManuallyButton);
        newWellButton = findViewById(R.id.newWellButton);

        double[] coordinates = getGPS();
        latitude = coordinates[0];
        longitude = coordinates[1];
        coordinatesText.setText("You have been found at \nLatitude: " + latitude + "\nLongitude: " + longitude);
        //this allows the location to be reset if the reset location button is clicked
        resetLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double[] coordinates = getGPS();
                latitude = coordinates[0];
                longitude = coordinates[1];
                coordinatesText = findViewById(R.id.coordinatesText);
                coordinatesText.setText("You have been found at \nLatitude: " + latitude + "\nLongitude: " + longitude);
            }
        });

        final int wellID = findNearestWell(latitude, longitude);
        /* if a nearest well cant be found then findNearestWell() will return -1; below is error handling
        so that the useWellButton cannot be used if wellID is a negative number.
         */
        if (wellID < 0) {
            useWellButton.setText("THERE WAS AN ERROR FINDING NEAREST WELL");
        } else {
            well_idText.setText("The Nearest Well Found Is: Well #" + wellID);
            useWellButton.setText("USE WELL #" + wellID);

            useWellButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), UpdateGivenWellData.class);
                    intent.putExtra("Well ID", wellID);
                    startActivity(intent);
                }
            });


        }
        newWellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newWellText = BuildNewWell();
                newWellButton.setText(newWellText);
            }
        });
    }


    //Method from http://www.androidsnippets.com/get-the-phones-last-known-location-using-locationmanager.html
    //This method finds the gps coordinates and changes latitude and longitude values.
    private double[] getGPS() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
        Location l = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        double[] gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }
        return gps;
    }

    /* method that uses jxl (JAVA's excel library) and the haversine formula to find the well closest
    to gps location and returns the well id.
     */
    private int findNearestWell(double lat1, double lon1) {
        String errorString = "";
        Workbook workbook = null;
        try {
            //BELOW WORKS TO GET WORKBOOK BUT THERE MIGHT BE A BETTER WAY!!!!
            AssetManager am = getAssets();
            InputStream is = am.open(EXCEL_FILE_LOCATION);
            workbook = Workbook.getWorkbook(is);

            //TRY THIS
            //Context ctx = getApplicationContext();
            //InputStream is = ctx.openFileInput(EXCEL_FILE_LOCATION);
            //workbook = Workbook.getWorkbook(is);

            Sheet sheet = workbook.getSheet(0);

            int latInd = getColumnIndex(workbook,"Y");
            int lonInd = getColumnIndex(workbook,"X");
            if(latInd == -1 || lonInd == -1){
                well_idText.setText("ERROR, could not find headers Y or X in the spreadsheet");
                return -1;
            }
            Cell[] latCell = sheet.getColumn(latInd);
            Cell[] lonCell = sheet.getColumn(lonInd);


            //Variables used for finding shortest distance
            double lat2 = 0;
            double lon2 = 0;
            double r = 6371e3; // metres
            double φ1 = 0;
            double φ2 = 0;
            double Δφ = 0;
            double Δλ = 0;
            double a = 0;
            double c = 0;
            double d = 0;
            double minDist = 1000000000;
            int rowIndex = 0;

            for (int i = 1; i<latCell.length; i++){
                //System.out.println(i);
                if(latCell[i].getContents()!= "" && lonCell[i].getContents()!=""){
					/* Uses Haversines formula to compare distances between to latitudes and longitudes
						from https://www.movable-type.co.uk/scripts/latlong.html
						*/
                    lat1 = latitude;
                    lon1 = longitude;
                    lat2 = Double.parseDouble(latCell[i].getContents());
                    lon2 = Double.parseDouble(lonCell[i].getContents());
                    φ1 = lat1*0.0174533; //From degrees to Radians
                    φ2 = lat2*0.0174533; //to Radians
                    Δφ = (lat2-lat1)*0.0174533; //to Radians
                    Δλ = (lon2-lon1)*0.0174533; //to Radians

                    a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                            Math.cos(φ1) * Math.cos(φ2) *
                                    Math.sin(Δλ/2) * Math.sin(Δλ/2);
                    c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

                    d = r * c;
                    if(d<minDist){
                        minDist = d;
                        rowIndex = i;
                    }
                }
            }

            String closestWellIDString = sheet.getCell(getColumnIndex(workbook,"Well ID#"),rowIndex).getContents();
            int closestWellID = Integer.parseInt(closestWellIDString);
            return closestWellID;

        } catch (IOException e) {
            e.printStackTrace();
            errorString = e.toString();
        } catch (BiffException e) {
            e.printStackTrace();
            errorString = e.toString();
        } catch (Exception e){
            errorString = e.toString();
        } finally {
            if (workbook != null) {
                workbook.close();
            }

        }
        well_idText.setText("There was an Error of kind: " + errorString);

        //Context ctx = getApplicationContext();
        //well_idText.setText("There was an Error of kind: " + errorString + "... you are currently in ..."+ctx);
        return -1;

    }

    /* Method designed to find the column in the excel sheet that includes the header,
    for example getColumnIndex(workbook, "Village") will return the column that includes all village
    values. Note that the header string MUST be contained in the first row of the spreadsheet
     */
    public int getColumnIndex(Workbook workbook, String header){
        Sheet sheet = workbook.getSheet(0);
        Cell[] headerCells = sheet.getRow(0);
        for(int i = 0; i<headerCells.length;i++){
            if(sheet.getCell(i,0).getContents().equalsIgnoreCase(header)){
                return i;
            }
        }
        return -1;
    }

    /* A method that adds a new well to the workbook.
     */

    public String BuildNewWell(){
        String errorString = "";
        Workbook workbook = null;
        try {
            AssetManager am = getAssets();
            InputStream is = am.open(EXCEL_FILE_LOCATION);
            workbook = Workbook.getWorkbook(is);

            Sheet sheet = workbook.getSheet(0);
            int wellIDindex = getColumnIndex(workbook, "Well ID#");
            Cell idCell = sheet.getCell(wellIDindex, 1); //getCell(column,row)

            //find new well id (find the highest well id and then use one higher for the new id)
            int index = 1;
            int maxID = 0;
            int currID = 0;
            int prevID = 0;
            while (idCell.getContents() != "") {
                currID = Integer.parseInt(idCell.getContents());
                if (currID > prevID) {
                    maxID = currID;
                }
                prevID = currID;
                index++;
                idCell = sheet.getCell(wellIDindex, index);
            }

            //write necessary updates to Updates.txt
            currID = maxID + 1;
            return "BUILD NEW WELL METHOD NOT COMPLETED";



        } catch (IOException e) {
            e.printStackTrace();
            errorString = e.toString();
        } catch (BiffException e) {
            e.printStackTrace();
            errorString = e.toString();
        } catch (Exception e){
            errorString = e.toString();
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        return "ERROR1" + errorString;
    }

}
