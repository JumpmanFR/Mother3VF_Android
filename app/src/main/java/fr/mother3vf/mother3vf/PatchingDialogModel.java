package fr.mother3vf.mother3vf;

import android.os.Parcel;
import android.os.Parcelable;

/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Developed by JumpmanFR
 * Inspired from Paul Kratt’s MultiPatch app for macOS
 ******************************************************************************/
public class PatchingDialogModel implements Parcelable {
    private static PatchingDialogModel instance;

    public static final int STEP_NONE = -1;
    public static final int STEP_FAILED = 0;
    public static final int STEP_RUNNING = 1;
    public static final int STEP_BROWSE = 2;
    public static final int STEP_ALREADY = 3;
    public static final int STEP_SUCCESS = 4;

    private int resultCode = STEP_NONE;
    private String message;
    private String fileParam = "";

    public PatchingDialogModel() {
    }

    protected PatchingDialogModel(Parcel in) {
        resultCode = in.readInt();
        message = in.readString();
        fileParam = in.readString();
    }

    public static final Creator<PatchingDialogModel> CREATOR = new Creator<PatchingDialogModel>() {
        @Override
        public PatchingDialogModel createFromParcel(Parcel in) {
            return new PatchingDialogModel(in);
        }

        @Override
        public PatchingDialogModel[] newArray(int size) {
            return new PatchingDialogModel[size];
        }
    };

    public void set(int code, String msg) {
        resultCode = code;
        message = msg;
    }

    public void set(int code, String msg, String fp) {
        set(code, msg);
        fileParam = fp;
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

    public String getFileParam() {
        return fileParam;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(resultCode);
        parcel.writeString(message);
        parcel.writeString(fileParam);
    }
}
