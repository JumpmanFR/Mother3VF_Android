/*
Copyright (C) 2013, 2016 Boris Timofeev
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

package fr.mother3vf.mother3vf.patcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Utils {
    private static int BUFFER_SIZE = 10240; // 10 Kb

    public static void copy(InputStream from, OutputStream to, long size) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int c;
        while (size > 0) {
            if (size < BUFFER_SIZE) {
                c = from.read(buffer, 0, (int) size);
            } else {
                c = from.read(buffer);
            }
            if (c != -1) {
                to.write(buffer, 0, c);
                size -= c;
            } else {
                copy(size, (byte) 0x0, to);
                size = 0;
            }
        }
    }

    public static void copy(long count, byte b, OutputStream to) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        Arrays.fill(buffer, b);
        while (count > 0) {
            if (count >= BUFFER_SIZE) {
                to.write(buffer);
                count -= BUFFER_SIZE;
            } else {
                to.write(buffer, 0, (int) count);
                count = 0;
            }
        }
    }
}
