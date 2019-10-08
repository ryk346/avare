/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.flight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * All lists get stored and get retrieved in JSON format
 * @author zkhan, rwalker
 *
 */
public class WeightAndBalance {
    private JSONObject mWnb;

    public static final int WNB_DEFAULT = 0;
    public static final int WNB_C172R = 1;
    public static final int WNB_PA28R_200B = 2;
    public static final int WNB_PA23_250 = 3;
    public static final int WNB_VANS_RV10 = 4;
    public static final int WNB_GRUMMAN_AA1A = 5;
    public static final int WNB_VANS_RV7A = 6;

    public WeightAndBalance(int type) {
        String ex = getDefault();

        switch (type) {
            case WNB_PA28R_200B:
                ex = getExamplePA28R200B();
                break;

            case WNB_C172R:
                ex = getExampleC172R();
                break;

            case WNB_PA23_250:
                ex = getExamplePA23250();
                break;

            case WNB_VANS_RV10:
                ex = getExampleRV10();
                break;

            case WNB_GRUMMAN_AA1A:
                ex = getExampleAA1A();
                break;

            case WNB_VANS_RV7A:
                ex = getExampleRV7a();
                break;
        }

        try {
            mWnb = new JSONObject(ex);
        } catch (JSONException ignore) { }
    }

    /**
     * Get in JSON format
     * @return the JSON object format for this item
     */
    public JSONObject getJSON() {
        return mWnb;
    }
    
    public static String putWnbsToStorageFormat(LinkedList<AircraftSpecsWB> wnbs) {

        JSONArray jsonArr = new JSONArray();
        for(AircraftSpecsWB w : wnbs) {

            JSONObject o = w.toJSon();
            jsonArr.put(o);
        }
        return jsonArr.toString();
    }

    public static LinkedList<AircraftSpecsWB> getWnbsFromStorageFormat(String json) {
        JSONArray jsonArr;
        LinkedList<AircraftSpecsWB> ret = new LinkedList<>();
        try {
            jsonArr = new JSONArray(json);
        } catch (JSONException e) {
            return ret;
        }

        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                JSONObject o = jsonArr.getJSONObject(i);
                ret.add(new AircraftSpecsWB(o));
            } catch (JSONException ignore) {
            }
        }

        return ret;
    }

    /**
     * 172R example
     * @return String that is a C172R
     */
    private static String getExampleC172R() {
        return "{" +
                "'name'  :'Cessna C172R 123AB'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'1666.8'," +
                "'a_0'   :'39.302',"+
                "'t_1'   :'Oil'," +
                "'w_1'   :'14'," +
                "'a_1'   :'-13.1'," +
                "'t_2'   :'Front passengers'," +
                "'w_2'   :'275'," +
                "'a_2'   :'37'," +
                "'t_3'   :'Fuel'," +
                "'w_3'   :'324'," +
                "'a_3'   :'48'," +
                "'t_4'   :'Back passengers'," +
                "'w_4'   :'110'," +
                "'a_4'   :'73'," +
                "'t_5'   :'Baggage'," +
                "'w_5'   :'30'," +
                "'a_5'   :'95'," +
                "'t_6'   :'Aft Baggage'," +
                "'w_6'   :'0'," +
                "'a_6'   :'123'," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'2450'," +
                "'min_w' :'1650'," +
                "'max_a' :'50'," +
                "'min_a' :'30'," +
                "'points':'47.3,1650 35,1650 35,1950 40,2450 47.3,2450 47.3,1650'" +
                "}";
    }

    private String getExamplePA23250() {
        return "{" +
                "'name'  :'Piper PA23-250 123AB'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'3178'," +
                "'a_0'   :'90.71',"+
                "'t_1'   :'Nose Baggage'," +
                "'w_1'   :'30'," +
                "'a_1'   :'10'," +
                "'t_2'   :'Oil'," +
                "'w_2'   :'45'," +
                "'a_2'   :'55'," +
                "'t_3'   :'Front passengers'," +
                "'w_3'   :'275'," +
                "'a_3'   :'89'," +
                "'t_4'   :'Fuel'," +
                "'w_4'   :'840'," +
                "'a_4'   :'113'," +
                "'t_5'   :'Rear passengers'," +
                "'w_5'   :'140'," +
                "'a_5'   :'126'," +
                "'t_6'   :'Back passengers'," +
                "'w_6'   :'110'," +
                "'a_6'   :'157'," +
                "'t_7'   :'Aft Baggage'," +
                "'w_7'   :'10'," +
                "'a_7'   :'183'," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'5300'," +
                "'min_w' :'2800'," +
                "'max_a' :'101'," +
                "'min_a' :'85'," +
                "'points':'100.5,2800 100.5,5200 99,5200 85.7,3250 86.3,2800 100.5,2800'" +
                "}";
    }

    private String getExamplePA28R200B() {
        return "{" +
                "'name'  :'Piper PA28R-200B N123AB'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'1577'," +
                "'a_0'   :'85.5'," +
                "'t_1'   :'Oil'," +
                "'w_1'   :'15'," +
                "'a_1'   :'29.5'," +
                "'t_2'   :'Front passengers'," +
                "'w_2'   :'275'," +
                "'a_2'   :'85.5'," +
                "'t_3'   :'Fuel'," +
                "'w_3'   :'288'," +
                "'a_3'   :'95'," +
                "'t_4'   :'Back passengers'," +
                "'w_4'   :'110'," +
                "'a_4'   :'118.1'," +
                "'t_5'   :'Baggage'," +
                "'w_5'   :'100'," +
                "'a_5'   :'142.8'," +
                "'t_6'   :''," +
                "'w_6'   :''," +
                "'a_6'   :''," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'2700'," +
                "'min_w' :'1400'," +
                "'max_a' :'96'," +
                "'min_a' :'80'," +
                "'points':'95.5,1400 95.5,2600 90,2600 81,1950 81,1400 95.5,1400'" +
                "}";
    }

    private static String getExampleRV10() {
        return "{" +
                "'name'  :'Vans RV10 N123AB'," +
                "'t_0'   :'Right Main'," +
                "'w_0'   :'579'," +
                "'a_0'   :'124.4',"+
                "'t_1'   :'Left Main'," +
                "'w_1'   :'581'," +
                "'a_1'   :'124.4'," +
                "'t_2'   :'Nose'," +
                "'w_2'   :'342'," +
                "'a_2'   :'50.4'," +
                "'t_3'   :'Fuel'," +
                "'w_3'   :'180'," +
                "'a_3'   :'108.9'," +
                "'t_4'   :'Front Passengers'," +
                "'w_4'   :'185'," +
                "'a_4'   :'114.58'," +
                "'t_5'   :'Rear Passengers'," +
                "'w_5'   :'0'," +
                "'a_5'   :'151.26'," +
                "'t_6'   :'Baggage'," +
                "'w_6'   :'25'," +
                "'a_6'   :'173.5'," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'2700'," +
                "'min_w' :'1502'," +
                "'max_a' :'116.24'," +
                "'min_a' :'107.84'," +
                "'points':''" +
                "}";
    }

    private static String getExampleRV7a() {
        return "{" +
                "'name'  :'Vans RV7a N123AB'," +
                "'t_0'   :'Right Main'," +
                "'w_0'   :'362'," +
                "'a_0'   :'94.83',"+
                "'t_1'   :'Left Main'," +
                "'w_1'   :'361'," +
                "'a_1'   :'95'," +
                "'t_2'   :'Nose'," +
                "'w_2'   :'291'," +
                "'a_2'   :'40'," +
                "'t_3'   :'Fuel'," +
                "'w_3'   :'252'," +
                "'a_3'   :'80'," +
                "'t_4'   :'Front Passengers'," +
                "'w_4'   :'185'," +
                "'a_4'   :'97.48'," +
                "'t_5'   :'Baggage'," +
                "'w_5'   :'25'," +
                "'a_5'   :'126.78'," +
                "'t_6'   :''," +
                "'w_6'   :''," +
                "'a_6'   :''," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'1800'," +
                "'min_w' :'1014'," +
                "'max_a' :'86.82'," +
                "'min_a' :'78.7'," +
                "'points':''" +
                "}";
    }

    private static String getExampleAA1A() {
        return "{" +
                "'name'  :'Grumman AA1A N123AB'," +
                "'t_0'   :'Empty Weight'," +
                "'w_0'   :'1005'," +
                "'a_0'   :'75.227',"+
                "'t_1'   :'Oil'," +
                "'w_1'   :'11'," +
                "'a_1'   :'39'," +
                "'t_2'   :'Fuel'," +
                "'w_2'   :'132'," +
                "'a_2'   :'84.5'," +
                "'t_3'   :'Pilot/Pax'," +
                "'w_3'   :'340'," +
                "'a_3'   :'92.5'," +
                "'t_4'   :'Baggage'," +
                "'w_4'   :'10'," +
                "'a_4'   :'120'," +
                "'t_5'   :''," +
                "'w_5'   :'0'," +
                "'a_5'   :'151.26'," +
                "'t_6'   :''," +
                "'w_6'   :''," +
                "'a_6'   :''," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'1500'," +
                "'min_w' :'1005'," +
                "'max_a' :'87.38'," +
                "'min_a' :'75.227'," +
                "'points':''" +
                "}";
    }

    public static String getDefault() {
        return "{" +
                "'name'  :'Default Aircraft N123AB'," +
                "'t_0'   :'Right Main'," +
                "'w_0'   :'0'," +
                "'a_0'   :'0',"+
                "'t_1'   :'Left Main'," +
                "'w_1'   :'0'," +
                "'a_1'   :'0'," +
                "'t_2'   :'Nose/Tail'," +
                "'w_2'   :'0'," +
                "'a_2'   :'0'," +
                "'t_3'   :'Front Passengers'," +
                "'w_3'   :'0'," +
                "'a_3'   :'0'," +
                "'t_4'   :'Rear Passengers'," +
                "'w_4'   :'0'," +
                "'a_4'   :'0'," +
                "'t_5'   :'Baggage'," +
                "'w_5'   :'0'," +
                "'a_5'   :'0'," +
                "'t_6'   :'Fuel'," +
                "'w_6'   :'0'," +
                "'a_6'   :'0'," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'0'," +
                "'min_w' :'0'," +
                "'max_a' :'0'," +
                "'min_a' :'0'," +
                "'points':''" +
                "}";
    }
}