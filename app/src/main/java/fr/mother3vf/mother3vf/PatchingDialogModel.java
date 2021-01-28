package fr.mother3vf.mother3vf;

/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * Paul Kratt - main MultiPatch application for macOS
 * xperia64 - port to Android support
 * JumpmanFR - adaptation for MOTHER3VF
 ******************************************************************************/
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
    public static final int STEP_FAILED = 0;
    public static final int STEP_RUNNING = 1;
    public static final int STEP_BROWSE = 2;
    public static final int STEP_ALREADY = 3;
    public static final int STEP_SUCCESS = 4;

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
