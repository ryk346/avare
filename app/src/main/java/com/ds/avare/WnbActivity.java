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
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.flight.AircraftSpecsWB;
import com.ds.avare.flight.WeightAndBalance;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Ron Walker
 * A native android UI activity that deals with W&B
 */
public class WnbActivity extends Activity {

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
    
    private View mView;

    private Context mContext;

    private LinkedList<AircraftSpecsWB> mACData = new LinkedList<>();

    private Preferences mPref;

    private int mDefProfiles = 0;

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

        // Save our context and create a preference object
        mContext = this;
        mPref = new Preferences(mContext);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mService = null;

        // Inflate our view and set it as this tabs content
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.wnb, null);
        setContentView(mView);

        // Create a callback that will recalc all the values
        View.OnFocusChangeListener doRecalc = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    calcAndSetCG();
                } else {
                    ((EditText) v).setSelection(0, ((EditText) v).getText().length());
                }
            }
        };

        // When any of the WEIGHT fields is exited, do the recalc
        for(int idWeight : idWeights) {
            mView.findViewById(idWeight).setOnFocusChangeListener(doRecalc);
        }

        // When any of the LOCATION fields is exited, do the recalc
        for(int idLocation : idLocations) {
            mView.findViewById(idLocation).setOnFocusChangeListener(doRecalc);
        }

        // The other fields that could change the data
        mView.findViewById(R.id.idCGMin).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idCGMax).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idGross).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idEmpty).setOnFocusChangeListener(doRecalc);

        // Fetch all of the WnB default info that we know about
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_DEFAULT));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_C172R));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_PA23_250));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_PA28R_200B));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_VANS_RV7A));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_VANS_RV10));
        mACData.add(getAircraftSpecsWB(WeightAndBalance.WNB_GRUMMAN_AA1A));

        // Set how many default profiles we have
        mDefProfiles = mACData.size();

        // Add in all of the saved profiles
        mACData.addAll(WeightAndBalance.getWnbsFromStorageFormat(mPref.getWnbs()));

        // Populate the display with the WNB_DEFAULT profile
        populate(mACData.getFirst());

        String defProf = mPref.getRecentWNB(); // Get the most recent profile used
        for(AircraftSpecsWB acSpecs : mACData) {  // Search for it in our collection
            if(defProf.equals(acSpecs.getName())) {
                populate(acSpecs);              // When found, populate display with that data
                break;
            }
        }

        // Use this data to calc and set the CG info
        calcAndSetCG();

        // Load a saved profile into the display area.
        mView.findViewById(R.id.idLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int idx = 0;
                String[] acProfiles = new String[mACData.size()];
                for(AircraftSpecsWB as : mACData){
                    acProfiles[idx++] = as.getName();
                }

                DecoratedAlertDialogBuilder dlgBldr = new DecoratedAlertDialogBuilder(WnbActivity.this);
                dlgBldr.setTitle(WnbActivity.this.getString(R.string.SelectACP));
                dlgBldr.setItems(acProfiles,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AircraftSpecsWB acSpecs = mACData.get(which);
                                mPref.setRecentWNB(acSpecs.getName());
                                populate(acSpecs);
                                calcAndSetCG();
                                dialog.dismiss();
                            }
                        });

                // Create and show the dialog now
                AlertDialog dialog = dlgBldr.create();
                if (!isFinishing()) {
                    dialog.show();
                }
            }
        });

        // Display a color chart showing the W&B, with option to edit it
        mView.findViewById(R.id.idGraph).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog graphDlg = new Dialog(mContext);
                graphDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
                graphDlg.setContentView(R.layout.graph_wnb);
                setDialogFieldText(graphDlg, R.id.idGraph, extract().toJSon().toString());

                // This button brings up the custom weight/CG table
                graphDlg.findViewById(R.id.idSetCustomEnv).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Dialog dlgSetCustomEnv = new Dialog(mContext);
                        dlgSetCustomEnv.setTitle(R.string.EnvelopePoints);
                        dlgSetCustomEnv.setContentView(R.layout.custom_envelope);

                        // Seed the current station arm data into the dialog fields
                        int idx = 0;
                        TextView vCGEnv = mView.findViewById(R.id.idCGEnv);
                        String cgEnv = vCGEnv.getText().toString();
                        if(!cgEnv.isEmpty()) {
                            String[] armStations = cgEnv.split(" ");
                            for (String armStation : armStations) {
                                String[] as = armStation.split(",");
                                setDialogFieldText(dlgSetCustomEnv, idLocations[idx], as[0]);
                                setDialogFieldText(dlgSetCustomEnv, idWeights[idx], as[1]);
                                idx++;
                            }
                        }

                        // When the DONE button is pressed, copy all of the data from that dialog
                        // into our temp profile so that the graph gets re-drawn
                        dlgSetCustomEnv.findViewById(R.id.idDone).setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 StringBuilder cgEnv = new StringBuilder();
                                 for(int idx = 0; idx < idWeights.length; idx++) {
                                     EditText vWeight = dlgSetCustomEnv.findViewById(idWeights[idx]);
                                     EditText vLocation = dlgSetCustomEnv.findViewById(idLocations[idx]);
                                     if(vWeight.getText().length() > 0 && vLocation.getText().length() > 0) {
                                         cgEnv.append(" " + vLocation.getText() + "," + vWeight.getText());
                                     }
                                 }
                                 setViewFieldText(mView, R.id.idCGEnv, cgEnv.toString().trim());
                                 setDialogFieldText(graphDlg, R.id.idGraph, extract().toJSon().toString());
                                 dlgSetCustomEnv.dismiss();
                             }
                        });

                        // Show this dialog to the user
                        dlgSetCustomEnv.show();
                    }
                });

                // Time to show this dialog to the user
                graphDlg.show();
            }
        });

        // Build up what to do when the SAVE button is pressed
        mView.findViewById(R.id.idSaveDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Create a new dialog window
                final Dialog saveDlg = new Dialog(mContext);
                saveDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
                saveDlg.setContentView(R.layout.save_wnb);

                // Locate some controls of interest that are in the dialog
                final EditText etdMake  = saveDlg.findViewById(R.id.idMake);
                final EditText etdModel = saveDlg.findViewById(R.id.idModel);
                final EditText etdReg   = saveDlg.findViewById(R.id.idReg);

                // Seed the text in the save/delete dialog from the main view
                etdMake.setText (getViewFieldText(mView, R.id.idMake));
                etdModel.setText(getViewFieldText(mView, R.id.idModel));
                etdReg.setText  (getViewFieldText(mView, R.id.idReg));

                // Set what to do when the SAVE button is clicked.
                saveDlg.findViewById(R.id.idSave).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get the user inputted fields
                        String make  = etdMake.getText().toString();
                        String model = etdModel.getText().toString();
                        String reg   = etdReg.getText().toString();

                        // They must all be non-zero in length
                        if(make.length() == 0 || model.length() == 0 || reg.length() == 0) {
                            Toast.makeText(mContext, getString(R.string.errMakeModelReg), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new profile from the dialog contents
                        AircraftSpecsWB acSpecs = extract();

                        // Override with the new make/model/reg info
                        acSpecs.setMake(make);
                        acSpecs.setModel(model);
                        acSpecs.setReg(reg);

                        // Does this profile exist already ? If it does remove it
                        String name = acSpecs.getName();
                        for(AircraftSpecsWB as : mACData) {
                            if(name.equals(as.getName())) {
                                if(mACData.indexOf(as) < mDefProfiles) {
                                    Toast.makeText(mContext, getString(R.string.errNoDefaultOverwrite), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mACData.remove(as);
                            }
                        }

                        // Add this new one to the end of the list
                        mACData.add(acSpecs);

                        // Set the default profile to load to be this one
                        mPref.setRecentWNB(acSpecs.getName());

                        // Save W&B profiles to storage, exclude the defaults
                        saveACData();

                        // Set the make/model/reg values of the main window
                        populate(acSpecs);

                        // All done, close out
                        Toast.makeText(mContext, acSpecs.getName() + getString(R.string.Saved), Toast.LENGTH_SHORT).show();
                        saveDlg.dismiss();
                    }
                });

                // If DELETE is pressed, then remove this profile from our collection
                // both in memory and saved in storage
                saveDlg.findViewById(R.id.idDelete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get the user inputted fields
                        String make  = etdMake.getText().toString();
                        String model = etdModel.getText().toString();
                        String reg   = etdReg.getText().toString();

                        // They must all be non-zero in length
                        if(make.length() == 0 || model.length() == 0 || reg.length() == 0) {
                            Toast.makeText(mContext, getString(R.string.errMakeModelReg), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new profile from the dialog contents
                        AircraftSpecsWB acSpecs = extract();

                        // Override with the new make/model/reg info
                        acSpecs.setMake(make);
                        acSpecs.setModel(model);
                        acSpecs.setReg(reg);

                        // Does this profile exist already ? If it does remove it
                        String name = acSpecs.getName();
                        for(AircraftSpecsWB as : mACData) {
                            if(name.equals(as.getName())) {
                                if(mACData.indexOf(as) < mDefProfiles) {
                                    Toast.makeText(mContext, getString(R.string.errNoDeleteDefault), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mACData.remove(as);
                                Toast.makeText(mContext, acSpecs.getName() + getString(R.string.Deleted), Toast.LENGTH_SHORT).show();

                                // Save W&B profiles to storage, exclude the defaults
                                saveACData();

                                // Set the default to be the first entry in our collection
                                acSpecs = mACData.getFirst();
                                mPref.setRecentWNB(acSpecs.getName());
                                populate(acSpecs);
                            }
                        }
                        saveDlg.dismiss();
                    }
                });

                // Dialog is ready, display it
                saveDlg.show();
            }
        });

    }

    // Create and return an aircraftspecs object of the specified
    // type
    //
    private  AircraftSpecsWB getAircraftSpecsWB(int type) {
        return new AircraftSpecsWB(new WeightAndBalance(type).getJSON());
    }

    // Save all the created specs to storage
    //
    private void saveACData() {
        LinkedList<AircraftSpecsWB> saveList = new LinkedList<>();
        for(int idx = mDefProfiles; idx < mACData.size(); idx++){
            saveList.add(mACData.get(idx));
        }
        mPref.putWnbs(WeightAndBalance.putWnbsToStorageFormat(saveList));
    }

    // Populate the display area with the values for the specified aircraft
    //
    private void populate(AircraftSpecsWB acData) {

        setViewFieldText(mView, R.id.idMake,  acData.getMake());
        setViewFieldText(mView, R.id.idModel, acData.getModel());
        setViewFieldText(mView, R.id.idReg,   acData.getReg());
        setViewFieldFloat(mView, R.id.idCGMin, acData.getCGMin());
        setViewFieldFloat(mView, R.id.idCGMax, acData.getCGMax());
        setViewFieldFloat(mView, R.id.idGross, acData.getGross());
        setViewFieldFloat(mView, R.id.idEmpty, acData.getEmpty());
        setViewFieldText(mView, R.id.idCGEnv, acData.getCGEnv());

        for(AircraftSpecsWB.ArmEntry ae : acData.getAEList()) {
            TextView location = setViewFieldText(mView, idLocations[ae.idx()], "");
            TextView weight   = setViewFieldText(mView, idWeights[ae.idx()], "");

            setViewFieldText(mView, idNames[ae.idx()], ae.description());
            if(!ae.description().isEmpty()) {
                location.setText(Float.toString(ae.location()));
                weight.setText(Float.toString(ae.weight()));
            }
        }
    }

    // Extract all of the dialog values and build an explicit object
    //
    private AircraftSpecsWB extract() {

        // Allocate a new object to hold the data
        AircraftSpecsWB acData = new AircraftSpecsWB();

        // Fetch the primary info
        acData.setMake(getViewFieldText(mView, R.id.idMake));
        acData.setModel(getViewFieldText(mView, R.id.idModel));
        acData.setReg(getViewFieldText(mView, R.id.idReg));
        acData.setCGMin(getViewFieldFloat(mView, R.id.idCGMin));
        acData.setCGMax(getViewFieldFloat(mView, R.id.idCGMax));
        acData.setEmpty(getViewFieldFloat(mView, R.id.idEmpty));
        acData.setGross(getViewFieldFloat(mView, R.id.idGross));
        acData.setCGEnv(getViewFieldText(mView, R.id.idCGEnv));

        // Fetch each of the arm data points
        for(int idx = 0; idx < idNames.length; idx++) {
            acData.addArm(getViewFieldText(mView, idNames[idx]),
                    getViewFieldFloat(mView, idLocations[idx]),
                    getViewFieldFloat(mView, idWeights[idx]));
        }

        // These are calculated values. This is required because it is needed by the auto
        // graphing routines
        acData.setWeight(getViewFieldFloat(mView, R.id.idWeight));
        acData.setCG(getViewFieldFloat(mView, R.id.idCG));
        acData.setMoment(getViewFieldFloat(mView, R.id.idMoment));

        return acData;
    }

    // Read all of the edit controls to calculate the CG and gross weight.
    // Populate the display fields with that value
    //
    private void calcAndSetCG() {

        // Calculate the overall moment and gross weight
        float calcMoment = 0;
        float calcWT  = 0;

        // Add up all the weights we have in the station list
        // Calculate the arms at the stations, and add all those as well
        for(int idx = 0; idx < idNames.length; idx++) {
            calcMoment += getViewFieldFloat(mView, idWeights[idx]) *
                          getViewFieldFloat(mView, idLocations[idx]);
            calcWT += getViewFieldFloat(mView, idWeights[idx]);
        }

        // The forwardmost allowable CG location
        float fCGMin = getViewFieldFloat(mView, R.id.idCGMin);

        // The rearwardmost allowable CG location
        float fCGMax = getViewFieldFloat(mView, R.id.idCGMax);

        // Max/Gross weight allowed
        float fGross = getViewFieldFloat(mView, R.id.idGross);

        // Calculate the CG for this condition.
        float calcCG = calcWT > 0 ? calcMoment / calcWT : 0;

        // Set the calculated moment of the cg
        TextView vMoment = setViewFieldFloat(mView, R.id.idMoment, calcMoment);

        // Set the CG and adjust the color
        TextView vCG = setViewFieldFloat(mView, R.id.idCG, calcCG);
        boolean cgOK = setStatusColor(vCG, calcCG <= fCGMax && calcCG >= fCGMin);

        // Set the calculated weight and adjust the color
        TextView vWeight = setViewFieldFloat(mView, R.id.idWeight, calcWT);
        cgOK = setStatusColor(vWeight, (calcWT <= fGross && calcWT > 0) && cgOK);

        // Overall good/bad status setting
        TextView vStatus = mView.findViewById(R.id.idStatus);
        if(fGross == 0 || fCGMin == 0 || fCGMax == 0 || calcCG == 0 || calcWT == 0) {
            setUnknown(vStatus);
            setUnknown(vMoment);
            setUnknown(vWeight);
            setUnknown(vCG);
        } else {
            setStatusColor(vStatus, cgOK);
            setStatusColor(vMoment, cgOK);
            vStatus.setText(cgOK ? R.string.CGOK : R.string.CGFail);
        }
    }

    // A little helper method to set good/bad colors of a specific
    // view
    private boolean setStatusColor(TextView v, boolean bGoodBad) {
        v.setTextColor(bGoodBad ? Color.BLACK : Color.WHITE);
        v.setBackgroundColor(bGoodBad ? Color.GREEN : Color.RED);

        return bGoodBad;
    }

    // Set a control to "unknown" status
    //
    private void setUnknown(TextView v) {
        v.setText("");
        v.setBackgroundColor(Color.WHITE);
    }

    // Find a field, then set its value with the float indicated
    //
    private TextView setViewFieldFloat(View v, int id, float f) {
        TextView tv = v.findViewById(id);
        tv.setText(Float.toString(f));
        return tv;
    }

    // Find a field in the view and return as a float
    //
    private float getViewFieldFloat(View v, int id) {
        TextView tv = v.findViewById(id);
        return Helper.parseFloat(tv.getText().toString());
    }

    // Find a field in the view and return as a string
    //
    private String getViewFieldText(View v, int id) {
        TextView tv = v.findViewById(id);
        return tv.getText().toString();
    }

    // Find a field in the view, then set its value with the String indicated
    //
    private TextView setViewFieldText(View v, int id, String s) {
        TextView tv = v.findViewById(id);
        tv.setText(s);
        return tv;
    }

    // Find a field in the dialog, then set its value with the String indicated
    //
    private TextView setDialogFieldText(Dialog d, int id, String s) {
        TextView tv = d.findViewById(id);
        tv.setText(s);
        return tv;
    }

    // Defines callbacks for service binding, passed to bindService()
    //
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
}