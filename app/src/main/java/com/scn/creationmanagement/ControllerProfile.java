package com.scn.creationmanagement;

import com.scn.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by imurvai on 2017-12-13.
 */

public final class ControllerProfile {

    //
    // Members
    //

    private static final String TAG = ControllerProfile.class.getSimpleName();

    private String name;

    private List<String> deviceIds = new ArrayList<>();

    //
    // Constructor
    //

    ControllerProfile(String name) {
        Logger.i(TAG, "constructor...");
        this.name = name;
    }

    //
    // API
    //

    public String getName() { return name; }
    public void setName(String value) { name = value; }
}
