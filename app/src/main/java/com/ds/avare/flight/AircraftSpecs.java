/*
Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.flight;

import org.json.JSONObject;

import java.util.LinkedList;

/**
 * Object to encapsulate the properties of an aircraft
 * @author Ron Walker
 *
 */

public class AircraftSpecs {
    private String      mMake;      // Make
    private String      mModel;     // Model
    private String      mReg;       // Registration
    private float       mGross;     // Gross/max weight
    private float       mEmpty;     // Empty weight
    private float       mCGMin;     // Min CG location
    private float       mCGMax;     // Max CG location
    private String      mCGEnv;    // CG Envelope
    private LinkedList<ArmEntry> mAEList = new LinkedList<ArmEntry>();    // Arm Entry List

    public AircraftSpecs(JSONObject acData) {
        parseFromJSon(acData);
    }

    public void addArm(ArmEntry armEntry) {
        armEntry.mIdx = aeList().size();
        mAEList.add(armEntry);
    }

    public String make()  { return mMake; }
    public String model() { return mModel; }
    public String reg()   { return mReg; }
    public float gross()  { return mGross; }
    public float empty()  { return mEmpty; }
    public float cgMin()  { return mCGMin; }
    public float cgMax()  { return mCGMax; }
    public String cgEnv() { return mCGEnv; }
    public LinkedList<ArmEntry> aeList() { return mAEList; }

    public class ArmEntry {
        private int mIdx;
        private String mDescription;
        private float mLocation;
        private float mWeight;

        public int idx() {
            return mIdx;
        }

        public String description() {
            return mDescription;
        }

        public float location() {
            return mLocation;
        }

        public float weight() {
            return mWeight;
        }

        public ArmEntry(String description, float location, float weight) {
            mDescription = description;
            mLocation = location;
            mWeight = weight;
        }
    }

    // JSON is how the data is saved. Parse the object out to fill our structure
    private void parseFromJSon(JSONObject acData) {

        try {
            String MakeModelReg = acData.getString("name");
            String[] MMR = MakeModelReg.split(" ");
            mMake  = MMR.length > 0 ? MMR[0] : "Exp";
            mModel = MMR.length > 1 ? MMR[1] : "Airplane";
            mReg   = MMR.length > 2 ? MMR[2] : "N123AB";

            mGross = acData.getInt("max_w");
            mEmpty = acData.getInt("min_w");
            mCGMin = acData.getInt("min_a");
            mCGMax = acData.getInt("max_a");
            mCGEnv = acData.getString("points");

            for(int idx = 0; idx <= 9; idx++) {
                try {
                    addArm(new ArmEntry(
                            acData.getString("t_" + Integer.toString(idx)),
                            acData.getInt("a_" + Integer.toString(idx)),
                            acData.getInt("w_" + Integer.toString(idx))));
                } catch (Exception ex) {
                    addArm(new ArmEntry("", 0, 0));
                }
            }
        } catch (Exception ignore) {

        }

    }
}
