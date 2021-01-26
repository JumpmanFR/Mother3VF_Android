package fr.mother3vf.mother3vf;

import android.os.Bundle;

/**
 * Created by simon on 15/05/2017.
 */

public class PatchingDialogModel {
    private static PatchingDialogModel instance;

    private PatchingDialogModel() {}

    public static PatchingDialogModel getInstance() {
        if (instance == null) {
            instance = new PatchingDialogModel();
        }
        return instance;
    }

    public static final int STEP_NONE = -1;
    public static final int STEP_DONE = 0;
    public static final int STEP_RUNNING = 1;
    public static final int STEP_BROWSE = 2;
    public static final int STEP_SUCCESS = 3;

    private int resultCode = STEP_NONE;
    private String message;
    private String docFile = "";

    public void set(int code, String msg) {
        resultCode = code;
        message = msg;
    }

    public void set(int code, String msg, String df) {
        set(code, msg);
        docFile = df;
    }

    public void reset() {
        set(STEP_NONE, "");
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public String getDocFile() {
        return docFile;
    }
}
