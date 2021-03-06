/*
Copyright (C) 2013 Boris Timofeev
This file is part of UniPatcher.
UniPatcher is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
UniPatcher is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with UniPatcher.  If not, see <http://www.gnu.org/licenses/>.
*/

package fr.mother3vf.mother3vf.patching.upspatcher;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public abstract class Patcher {
    protected final Context context;

    protected final File patchFile;
    protected final File romFile;
    protected final File outputFile;

    public Patcher(Context c, File patch, File rom, File output) {
        context = c;
        patchFile = patch;
        romFile = rom;
        outputFile = output;
    }

    public abstract void apply(boolean ignoreChecksum) throws PatchException, IOException;
}
