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

import com.ds.avare.utils.Helper;

import org.json.JSONObject;

import java.util.LinkedList;

/**
 * Object to encapsulate the properties of an aircraft WB info
 * @author Ron Walker
 *
 */

public class AircraftSpecsWB {
    private String      mMake;      // Make
    private String      mModel;     // Model
    private String      mReg;       // Registration
    private float       mGross;     // Gross/max weight
    private float       mEmpty;     // Empty weight
    private float       mCGMin;     // Min CG location
    private float       mCGMax;     // Max CG location
    private String      mCGEnv;     // CG Envelope

    private float       mWeight;    // Calculated takeoff weight
    private float       mCG;        // CG Arm location
    private float       mMoment;    // Calculated CG moment value

    private LinkedList<ArmEntry> mAEList = new LinkedList<>();    // Arm Entry List

    public AircraftSpecsWB() { }

    public AircraftSpecsWB(JSONObject acData) {
        fromJSon(acData);
    }

    public void fromString(String jsonString) {
        try {
            fromJSon(new JSONObject(jsonString));
        } catch (Exception ignore) { }
    }

    private void addArm(ArmEntry armEntry) {
        armEntry.mIdx = mAEList.size();
        mAEList.add(armEntry);
    }

    public void addArm(String description, float location, float weight) {
        addArm(new ArmEntry(description, location, weight));
    }

    public String getName()  { return mMake + " " + mModel + " " + mReg;}
    public String getMake()  { return mMake; }
    public String getModel() { return mModel; }
    public String getReg()   { return mReg; }
    public float getGross()  { return mGross; }
    public float getEmpty()  { return mEmpty; }
    public float getCGMin()  { return mCGMin; }
    public float getCGMax()  { return mCGMax; }
    public String getCGEnv() { return mCGEnv; }
    public float getWeight() { return mWeight; }
    public float getCG()     { return mCG; }
    public float getMoment() { return mMoment; }
    public LinkedList<ArmEntry> getAEList() { return mAEList; }

    public void setMake(String make) { mMake = make; }
    public void setModel(String model) { mModel = model; }
    public void setReg(String reg) { mReg = reg; }
    public void setCGEnv(String cgEnv) { mCGEnv = cgEnv; }
    public void setGross(float gross) { mGross = gross; }
    public void setEmpty(float empty) { mEmpty = empty; }
    public void setCGMin(float cgMin) { mCGMin = cgMin; }
    public void setCGMax(float cgMax) { mCGMax = cgMax; }
    public void setWeight(float weight) {mWeight = weight; }
    public void setCG(float cg) { mCG = cg; }
    public void setMoment(float moment) { mMoment = moment; }

    public class ArmEntry {
        private int    mIdx;
        private String mDescription;
        private float  mLocation;
        private float  mWeight;

        public int idx() { return mIdx; }
        public String description() { return mDescription; }
        public float location() { return mLocation; }
        public float weight() { return mWeight; }

        private ArmEntry(String description, float location, float weight) {
            mDescription = description;
            mLocation = location;
            mWeight = weight;
        }
    }

    // JSON is how the data is saved. Parse the object out to fill our structure
    private void fromJSon(JSONObject acData) {

        try {
            String MakeModelReg = acData.getString("name");
            String[] MMR = MakeModelReg.split(" ");
            mMake  = MMR.length > 0 ? MMR[0] : "Exp";
            mModel = MMR.length > 1 ? MMR[1] : "Airplane";
            mReg   = MMR.length > 2 ? MMR[2] : "N123AB";

            mEmpty  = Helper.parseFloat(acData.getString("min_w"));
            mGross  = Helper.parseFloat(acData.getString("max_w"));
            mCGMin  = Helper.parseFloat(acData.getString("min_a"));
            mCGMax  = Helper.parseFloat(acData.getString("max_a"));
            mCGEnv  = acData.getString("points");
            try {
                // This 'try' is required because these items are not part of legacy/default profiles
                mWeight = Helper.parseFloat(acData.getString("weight"));
                mCG = Helper.parseFloat(acData.getString("cg"));
                mMoment = Helper.parseFloat(acData.getString("moment"));
            } catch ( Exception ignore) { }

            for(int idx = 0; idx <= 9; idx++) {
                try {
                    addArm(acData.getString("t_" + idx),
                           Helper.parseFloat(acData.getString("a_" + idx)),
                           Helper.parseFloat(acData.getString("w_" + idx)));
                } catch (Exception ignore) {
                    addArm("", 0, 0);
                }
            }
        } catch (Exception ignore) {
        }
    }

    // Convert this object to a JSON format
    public JSONObject toJSon() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", mMake + " " + mModel + " " + mReg);
            jsonObject.put("max_w",  Float.toString(mGross));
            jsonObject.put("min_w",  Float.toString(mEmpty));
            jsonObject.put("max_a",  Float.toString(mCGMax));
            jsonObject.put("min_a",  Float.toString(mCGMin));
            jsonObject.put("points", mCGEnv);

            jsonObject.put("weight", Float.toString(mWeight));
            jsonObject.put("cg",     Float.toString(mCG));
            jsonObject.put("moment", Float.toString(mMoment));

            int idx = 0;
            for(ArmEntry ae : mAEList) {
                jsonObject.put("t_" + idx, ae.mDescription);
                jsonObject.put("a_" + idx, Float.toString(ae.mLocation));
                jsonObject.put("w_" + idx, Float.toString(ae.mWeight));
                idx++;
            }

        } catch (Exception ignore) { }

        return jsonObject;
    }
}
