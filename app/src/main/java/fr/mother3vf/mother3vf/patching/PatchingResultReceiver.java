package fr.mother3vf.mother3vf.patching;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

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
public class PatchingResultReceiver extends ResultReceiver {
    private Receiver receiver;

    public PatchingResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }


    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (receiver != null) {
            receiver.onPatchingResult(resultCode, resultData);
        }
    }

    public interface Receiver {
        void onPatchingResult(int resultCode, Bundle resultData);
    }
}
