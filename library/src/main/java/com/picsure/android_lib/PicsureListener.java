package com.picsure.android_lib;

import org.json.JSONObject;

/**
 * Created by fbischof on 17.03.17.
 */

public interface PicsureListener {

    void onResponse(JSONObject response);

    void onError(String errorMessage);

}
