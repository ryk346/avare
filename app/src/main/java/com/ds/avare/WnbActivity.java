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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.flight.AircraftSpecs;
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

    private LinkedList<AircraftSpecs> mACData = new LinkedList<>();

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
     
        mContext = this;
        mPref = new Preferences(mContext);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mService = null;

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

        mView.findViewById(R.id.idCGMin).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idCGMax).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idGross).setOnFocusChangeListener(doRecalc);
        mView.findViewById(R.id.idEmpty).setOnFocusChangeListener(doRecalc);

        // Fetch all of the WnB default info that we know about
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_DEFAULT).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_C172R).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_PA23_250).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_PA28R_200B).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_VANS_RV7A).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_VANS_RV10).getJSON()));
        mACData.add(new AircraftSpecs(new WeightAndBalance(WeightAndBalance.WNB_GRUMMAN_AA1A).getJSON()));

        // Set how many default profiles we have
        mDefProfiles = mACData.size();

        // Add in all of the saved profiles
        mACData.addAll(WeightAndBalance.getWnbsFromStorageFormat(mPref.getWnbs()));

        // Populate the display with the default profile
        populate(mACData.getFirst());

        String defProf = mPref.getDefaultWNB(); // Get the most recent profile used
        for(AircraftSpecs acSpecs : mACData) {  // Search for it in our collection
            if(defProf.equals(acSpecs.getName())) {
                populate(acSpecs);              // When found, populate display with that data
                break;
            }
        }

        // Use this data to calc and set the CG info
        calcAndSetCG();

        // The display toggle button in the upper left will close/open the top calculation area
        // This is useful on small displays to be able to view the ARM stations without clutter.
        final ImageButton buttonToggle = mView.findViewById(R.id.idToggle);
        final TableLayout vCGnWeight = mView.findViewById(R.id.idCGnWeight);
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(vCGnWeight.getVisibility()) {
                    case View.VISIBLE:
                        vCGnWeight.setVisibility(View.GONE);
                        buttonToggle.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_up_float));
                        break;

                    default:
                        vCGnWeight.setVisibility(View.VISIBLE);
                        buttonToggle.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_down_float));
                        break;
                }
            }
        });

        // Load a saved profile into the display area.
        mView.findViewById(R.id.idLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int idx = 0;
                final String[] acProfiles = new String[mACData.size()];
                for(AircraftSpecs as : mACData){
                    acProfiles[idx++] = as.getName();
                }

                DecoratedAlertDialogBuilder dlgBldr = new DecoratedAlertDialogBuilder(WnbActivity.this);
                dlgBldr.setTitle(WnbActivity.this.getString(R.string.SelectACP));
                dlgBldr.setItems(acProfiles,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPref.setDefaultWNB(mACData.get(which).getName());
                                populate(mACData.get(which));
                                calcAndSetCG();
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

        // Display a color chart showing the W&B
        mView.findViewById(R.id.idGraph).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog graphDlg = new Dialog(mContext);
                graphDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
                graphDlg.setContentView(R.layout.graph_wnb);
                ((TextView)graphDlg.findViewById(R.id.idGraph)).
                        setText(extract().toJSon().toString());
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

                etdMake.setText (((TextView) mView.findViewById(R.id.idMake)). getText());
                etdModel.setText(((TextView) mView.findViewById(R.id.idModel)).getText());
                etdReg.setText  (((TextView) mView.findViewById(R.id.idReg))  .getText());

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
                            Toast.makeText(mContext, "Make/Model/Registration needs to be filled out", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new profile from the dialog contents
                        AircraftSpecs acSpecs = extract();

                        // Override with the new make/model/reg info
                        acSpecs.setMake(make);
                        acSpecs.setModel(model);
                        acSpecs.setReg(reg);

                        // Does this profile exist already ? If it does remove it
                        String name = acSpecs.getName();
                        for(AircraftSpecs as : mACData) {
                            if(name.equals(as.getName())) {
                                if(mACData.indexOf(as) < mDefProfiles) {
                                    Toast.makeText(mContext, "Cannot overwrite a default profile", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mACData.remove(as);
                            }
                        }

                        // Add this new one to the end of the list
                        mACData.add(acSpecs);

                        // Set the default profile to load to be this one
                        mPref.setDefaultWNB(acSpecs.getName());

                        // Save W&B profiles to storage, exclude the defaults
                        saveACSpecs();

                        // Set the make/model/reg values of the main window
                        populate(acSpecs);

                        // All done, close out
                        Toast.makeText(mContext, acSpecs.getName() + " saved.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(mContext, "Make/Model/Registration needs to be filled out", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new profile from the dialog contents
                        AircraftSpecs acSpecs = extract();

                        // Override with the new make/model/reg info
                        acSpecs.setMake(make);
                        acSpecs.setModel(model);
                        acSpecs.setReg(reg);

                        // Does this profile exist already ? If it does remove it
                        String name = acSpecs.getName();
                        for(AircraftSpecs as : mACData) {
                            if(name.equals(as.getName())) {
                                if(mACData.indexOf(as) < mDefProfiles) {
                                    Toast.makeText(mContext, "Cannot delete a default profile", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mACData.remove(as);
                                Toast.makeText(mContext, acSpecs.getName() + " deleted.", Toast.LENGTH_SHORT).show();

                                // Save W&B profiles to storage, exclude the defaults
                                saveACSpecs();

                                // Set the default to be the first entry in our collection
                                acSpecs = mACData.getFirst();
                                mPref.setDefaultWNB(acSpecs.getName());
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

    // Save all the created specs to storage
    private void saveACSpecs() {
        LinkedList<AircraftSpecs> saveList = new LinkedList<>();
        for(int idx = mDefProfiles; idx < mACData.size(); idx++){
            saveList.add(mACData.get(idx));
        }
        mPref.putWnbs(WeightAndBalance.putWnbsToStorageFormat(saveList));
    }

    // Populate the display area with the values for the specified aircraft
    private void populate(AircraftSpecs acData) {

        TextView make = mView.findViewById(R.id.idMake);
        make.setText(acData.getMake());

        TextView model = mView.findViewById(R.id.idModel);
        model.setText(acData.getModel());

        TextView reg = mView.findViewById(R.id.idReg);
        reg.setText(acData.getReg());

        TextView cgMin = mView.findViewById(R.id.idCGMin);
        cgMin.setText(Float.toString(acData.getCGMin()));

        TextView cgMax = mView.findViewById(R.id.idCGMax);
        cgMax.setText(Float.toString(acData.getCGMax()));

        TextView grossWT = mView.findViewById(R.id.idGross);
        grossWT.setText(Float.toString(acData.getGross()));

        TextView vEmpty = mView.findViewById(R.id.idEmpty);
        vEmpty.setText(Float.toString(acData.getEmpty()));

        TextView cgEnv = mView.findViewById(R.id.idCGEnv);
        cgEnv.setText(acData.getCGEnv());

        for(AircraftSpecs.ArmEntry ae : acData.getAEList()) {
            TextView name     = mView.findViewById(idNames[ae.idx()]);
            TextView location = mView.findViewById(idLocations[ae.idx()]);
            TextView weight   = mView.findViewById(idWeights[ae.idx()]);

            name.setText(ae.description());
            if(!ae.description().isEmpty()) {
                location.setText(Float.toString(ae.location()));
                weight.setText(Float.toString(ae.weight()));
            } else {
                location.setText("");
                weight.setText("");
            }
        }
    }

    // Extract all of the dialog values and build an explicit object
    private AircraftSpecs extract() {

        AircraftSpecs acData = new AircraftSpecs();

        TextView vMake = mView.findViewById(R.id.idMake);
        acData.setMake(vMake.getText().toString());

        TextView vModel = mView.findViewById(R.id.idModel);
        acData.setModel(vModel.getText().toString());

        TextView vReg = mView.findViewById(R.id.idReg);
        acData.setReg(vReg.getText().toString());

        TextView vCGMin = mView.findViewById(R.id.idCGMin);
        acData.setCGMin(Helper.parseFloat(vCGMin.getText().toString()));

        TextView vCGMax = mView.findViewById(R.id.idCGMax);
        acData.setCGMax(Helper.parseFloat(vCGMax.getText().toString()));

        TextView vEmpty = mView.findViewById(R.id.idEmpty);
        acData.setEmpty(Helper.parseFloat(vEmpty.getText().toString()));

        TextView vGross = mView.findViewById(R.id.idGross);
        acData.setGross(Helper.parseFloat(vGross.getText().toString()));

        TextView vCGEnv = mView.findViewById(R.id.idCGEnv);
        acData.setCGEnv(vCGEnv.getText().toString());

        TextView vWeight = mView.findViewById(R.id.idWeight);
        acData.setWeight(Helper.parseFloat(vWeight.getText().toString()));

        TextView vCG = mView.findViewById(R.id.idCG);
        acData.setCG(Helper.parseFloat(vCG.getText().toString()));

        TextView vMoment = mView.findViewById(R.id.idMoment);
        acData.setMoment(Helper.parseFloat(vMoment.getText().toString()));

        for(int idx = 0; idx < idNames.length; idx++) {
            TextView vNames     = mView.findViewById(idNames[idx]);
            TextView vLocations = mView.findViewById(idLocations[idx]);
            TextView vWeights   = mView.findViewById(idWeights[idx]);
            acData.addArm(vNames.getText().toString(),
                          Helper.parseFloat(vLocations.getText().toString()),
                          Helper.parseFloat(vWeights.getText().toString()));
        }
        return acData;
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

        TextView cgMin = mView.findViewById(R.id.idCGMin);
        float fCGMin = Float.parseFloat(cgMin.getText().toString());

        TextView cgMax = mView.findViewById(R.id.idCGMax);
        float fCGMax = Float.parseFloat(cgMax.getText().toString());

        TextView grossWT = mView.findViewById(R.id.idGross);
        float fCGGrossWT = Float.parseFloat(grossWT.getText().toString());

        float cg = WT > 0 ? arm / WT : 0;

        boolean cgOK = true;

        TextView vMoment = mView.findViewById(R.id.idMoment);
        vMoment.setText(Float.toString(arm));

        TextView cgView = mView.findViewById(R.id.idCG);
        cgView.setText(Float.toString(cg));
        if(cg == 0) {
            cgView.setBackgroundColor(Color.WHITE);
        } else {
            if (cg <= fCGMax && cg >= fCGMin) {
                cgView.setTextColor(Color.BLACK);
                cgView.setBackgroundColor(Color.GREEN);
            } else {
                cgView.setTextColor(Color.WHITE);
                cgView.setBackgroundColor(Color.RED);
                cgOK = false;
            }
        }

        TextView weightView = mView.findViewById(R.id.idWeight);
        weightView.setText(Float.toString(WT));
        if(WT == 0) {
            weightView.setBackgroundColor(Color.WHITE);
        } else {
            if (WT <= fCGGrossWT) {
                weightView.setTextColor(Color.BLACK);
                weightView.setBackgroundColor(Color.GREEN);
            } else {
                weightView.setTextColor(Color.WHITE);
                weightView.setBackgroundColor(Color.RED);
                cgOK = false;
            }
        }

        TextView statusView = mView.findViewById(R.id.idStatus);
        if(fCGGrossWT == 0 || fCGMin == 0 || fCGMax == 0 || cg ==0 || WT == 0) {
            statusView.setText("");
            statusView.setBackgroundColor(Color.WHITE);
            vMoment.setBackgroundColor(Color.WHITE);
        } else {
            if (cgOK) {
                statusView.setText(R.string.CGOK);
                statusView.setTextColor(Color.BLACK);
                statusView.setBackgroundColor(Color.GREEN);
                vMoment.setBackgroundColor(Color.GREEN);
            } else {
                statusView.setText(R.string.CGFail);
                statusView.setTextColor(Color.WHITE);
                statusView.setBackgroundColor(Color.RED);
                vMoment.setBackgroundColor(Color.RED);
            }
        }
    }

    // Defines callbacks for service binding, passed to bindService()

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