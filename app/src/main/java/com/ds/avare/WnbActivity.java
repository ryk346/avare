/*
Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Ron Walker
 * A native android UI activity that deals with W&B
 */
public class WnbActivity extends Activity {

    class acWNB {
        String      mMake;      // Make
        String      mModel;     // Model
        String      mReg;       // Registration
        float       mGross;     // Gross/max weight
        float       mCGMin;     // Min CG location
        float       mCGMax;     // Max CG location
        armEntry[]  mAEList;    // Arm Entry List

        acWNB(String make, String model, String reg, float gross, float cgMin, float cgMax, armEntry[] aeList) {
            mMake   = make;
            mModel  = model;
            mReg    = reg;
            mGross  = gross;
            mCGMin  = cgMin;
            mCGMax  = cgMax;
            mAEList = aeList;
        }
    }

    class armEntry {
        int     mIdx;
        String  mDescription;
        float   mLocation;
        float   mWeight;

        armEntry(int idx, String description, float location, float weight) {
            mIdx = idx;
            mDescription = description;
            mLocation = location;
            mWeight = weight;
        }
        int x = R.id.idLocation0;
    }

    static final int[] idLocations = {R.id.idLocation0,
            R.id.idLocation1, R.id.idLocation2, R.id.idLocation3,
            R.id.idLocation4, R.id.idLocation5, R.id.idLocation6,
            R.id.idLocation7, R.id.idLocation8, R.id.idLocation9};

    static final int[] idNames = {R.id.idName0,
            R.id.idName1, R.id.idName2, R.id.idName3,
            R.id.idName4, R.id.idName5, R.id.idName6,
            R.id.idName7, R.id.idName8, R.id.idName9};

    static final int[] idWeights = {R.id.idWeight0,
            R.id.idWeight1, R.id.idWeight2, R.id.idWeight3,
            R.id.idWeight4, R.id.idWeight5, R.id.idWeight6,
            R.id.idWeight7, R.id.idWeight8, R.id.idWeight9};

    // A timer object to handle things when GPS goes away
    private Timer mTimer;

    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;
    
    /*
     * If page it loaded
     */
    private boolean mIsPageLoaded;

    private Context mContext;

    private View mView;

    private acWNB mACData;

    // To keep the GPS active when this tab is being interacted with
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity) this.getParent()).showMapTab();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Helper.setTheme(this);
        super.onCreate(savedInstanceState);
     
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContext = this;
        mService = null;
        mIsPageLoaded = false;

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.wnb, null);
        setContentView(mView);
        mACData = getRV10();

        populate();
        calcAndSetCG();

        Button buttonNew = mView.findViewById(R.id.idNew);
        buttonNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mACData = getDefault();
                populate();
                calcAndSetCG();
            }
        });

        Button buttonLoad = mView.findViewById(R.id.idLoad);
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] acProfiles = {"Vans RV7a N520TX", "Vans RV10 N820TX", "Grumman AA1A N706LG", "Diamond DA20 N183DA"};

                DecoratedAlertDialogBuilder dlgBldr = new DecoratedAlertDialogBuilder(WnbActivity.this);
                dlgBldr.setTitle(WnbActivity.this.getString(R.string.SelectACP));
                dlgBldr.setItems(acProfiles,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // _nNewSelection = which;
                                dialog.dismiss();
                            }
                        });

                // Cancel, nothing to do here, let the dialog self-destruct
                dlgBldr.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                // Create and show the dialog now
                AlertDialog dialog = dlgBldr.create();
                if (!isFinishing()) {
                    dialog.show();
                }
            }
        });
    }

    // Populate the display area with the values for the specified aircraft
    private void populate() {

        TextView makeAndModel = mView.findViewById(R.id.idMakeAndModel);
        makeAndModel.setText(mACData.mMake + " " + mACData.mModel + " " + mACData.mReg);

        TextView cgMin = mView.findViewById(R.id.idCGMin);
        cgMin.setText(Float.toString(mACData.mCGMin));

        TextView cgMax = mView.findViewById(R.id.idCGMax);
        cgMax.setText(Float.toString(mACData.mCGMax));

        TextView grossWT = mView.findViewById(R.id.idCGGrossWT);
        grossWT.setText(Float.toString(mACData.mGross));

        for(armEntry ae : mACData.mAEList) {
            TextView name     = mView.findViewById(idNames[ae.mIdx]);
            TextView location = mView.findViewById(idLocations[ae.mIdx]);
            TextView weight   = mView.findViewById(idWeights[ae.mIdx]);

            name.setText(ae.mDescription);
            if(!ae.mDescription.isEmpty()) {
                location.setText(Float.toString(ae.mLocation));
                weight.setText(Float.toString(ae.mWeight));
            } else {
                location.setText("");
                weight.setText("");
            }
        }
    }

    // Read all of the edit controls to calculate the CG and gross weight.
    // Populate the display fields with that value
    private void calcAndSetCG() {

        // Calculate the overall arm and gross weight
        float arm = 0;
        float WT  = 0;

        for(int idx = 0; idx < idNames.length; idx++) {
            TextView locationView = mView.findViewById(idLocations[idx]);
            TextView weightView = mView.findViewById(idWeights[idx]);

            if(weightView.getText().length() > 0 && locationView.getText().length() > 0) {
                float weight = Float.parseFloat(weightView.getText().toString());
                float location = Float.parseFloat(locationView.getText().toString());

                arm += weight * location;
                WT += weight;
            }
        }

        float cg = WT > 0 ? arm / WT : 0;

        boolean cgOK = true;

        TextView cgView = mView.findViewById(R.id.idCG);
        cgView.setText(Float.toString(cg));
        if(cg == 0) {
            cgView.setBackgroundColor(Color.WHITE);
        } else {
            if (cg <= mACData.mCGMax && cg >= mACData.mCGMin) {
                cgView.setBackgroundColor(Color.GREEN);
            } else {
                cgView.setBackgroundColor(Color.RED);
                cgOK = false;
            }
        }

        TextView weightView = mView.findViewById(R.id.idWeight);
        weightView.setText(Float.toString(WT));
        if(WT == 0) {
            weightView.setBackgroundColor(Color.WHITE);
        } else {
            if (WT <= mACData.mGross) {
                weightView.setBackgroundColor(Color.GREEN);
            } else {
                weightView.setBackgroundColor(Color.RED);
                cgOK = false;
            }
        }

        TextView statusView = mView.findViewById(R.id.idStatus);
        if(mACData.mGross == 0 || mACData.mCGMin == 0 || mACData.mCGMax == 0 || cg ==0 || WT == 0) {
            statusView.setText("");
            statusView.setBackgroundColor(Color.WHITE);
        } else {
            if (cgOK) {
                statusView.setText("OK !");
                statusView.setBackgroundColor(Color.GREEN);
            } else {
                statusView.setText("FAIL !");
                statusView.setBackgroundColor(Color.RED);
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    /**
     * 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.ServiceConnection#onServiceConnected(android.content
         * .ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            /*
             * We've bound to LocalService, cast the IBinder and get
             * LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
            /*
             * When both service and page loaded then proceed.
             * The wnb will be loaded either from here or from page load end event
             */
            mTimer = new Timer();
            TimerTask sim = new UpdateTask();
            mTimer.scheduleAtFixedRate(sim, 0, 1000);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.ServiceConnection#onServiceDisconnected(android.content
         * .ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        
        Helper.setOrientationAndOn(this);

        /*
         * Registering our receiver Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);

        // Cancel the timer if one is running
        if(mTimer != null) {
        	mTimer.cancel();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    /***
    * A background timer class to send off messages if we are in simulation mode
    * @author zkhan
    */
    private class UpdateTask extends TimerTask {
	    // Called whenever the timer fires.
	    public void run() {
	    	if(mService != null) {
	    	}
	    }
    }

    private acWNB getRV10() {
        armEntry ae0 = new armEntry (0, "Left Main", 124.4f, 581f);
        armEntry ae1 = new armEntry (1, "Right Main", 124.4f, 579f);
        armEntry ae2 = new armEntry (2, "Nose/Tail", 50.4f, 342f);
        armEntry ae3 = new armEntry (3, "Front Seat Pax", 114.58f, 180f);
        armEntry ae4 = new armEntry (4, "Rear Seat Pax", 151.26f, 0f);
        armEntry ae5 = new armEntry (5, "Fuel 1", 108.9f, 60f);
        armEntry ae6 = new armEntry (6, "Fuel 2", 0f, 0f);
        armEntry ae7 = new armEntry (7, "Baggage 1", 173.5f, 0f);
        armEntry ae8 = new armEntry (8, "Baggage 2", 0f, 0f);
        armEntry ae9 = new armEntry (9, "", 0f, 0f);

        armEntry[] armList = {ae0, ae1, ae2, ae3, ae4, ae5, ae6, ae7, ae8, ae9};


        return new acWNB("Vans", "RV10", "N820TX", 2700, 107.84f, 116.24f, armList);
    }

    private acWNB getDefault() {
        armEntry ae0 = new armEntry (0, "Left Main", 0f, 0f);
        armEntry ae1 = new armEntry (1, "Right Main", 0f, 0f);
        armEntry ae2 = new armEntry (2, "Nose/Tail", 0f, 0f);
        armEntry ae3 = new armEntry (3, "Front Seat Pax", 0f, 0f);
        armEntry ae4 = new armEntry (4, "Fuel 1", 0f, 0f);
        armEntry ae5 = new armEntry (5, "Baggage 1", 0f, 0f);
        armEntry ae6 = new armEntry (6, "", 0f, 0f);
        armEntry ae7 = new armEntry (7, "", 0f, 0f);
        armEntry ae8 = new armEntry (8, "", 0f, 0f);
        armEntry ae9 = new armEntry (9, "", 0f, 0f);

        armEntry[] armList = {ae0, ae1, ae2, ae3, ae4, ae5, ae6, ae7, ae8, ae9};


        return new acWNB("Default", "Aircraft", "N123AB", 0, 0f, 0f, armList);
    }
}
