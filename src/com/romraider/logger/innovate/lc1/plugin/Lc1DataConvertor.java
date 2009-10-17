/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2009 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.innovate.lc1.plugin;

import com.romraider.logger.innovate.generic.plugin.DataConvertor;
import static com.romraider.util.ByteUtil.matchOnes;
import static com.romraider.util.ByteUtil.matchZeroes;
import static com.romraider.util.HexUtil.asHex;
import org.apache.log4j.Logger;

public final class Lc1DataConvertor implements DataConvertor {
    private static final Logger LOGGER = Logger.getLogger(Lc1DataConvertor.class);
    private static final double MAX_AFR = 20.33;

    public double convert(byte[] bytes) {
        /*
       Example bytes: 10110010 10000010 01000011 00010011 00010111 00101111
        */
        LOGGER.trace("Converting LC-1 bytes: " + asHex(bytes));
        if (isLc1(bytes) && isHeaderValid(bytes)) {
            if (isError(bytes)) {
                int error = -1 * getLambda(bytes);
                LOGGER.error("LC-1 error: " + asHex(bytes) + " --> " + error);
                return error;
            }
            if (isOk(bytes)) {
                double afr = getAfr(bytes);
                LOGGER.trace("LC-1 AFR: " + afr);
                return afr > MAX_AFR ? MAX_AFR : afr;
            }
            // out of range value seen on overrun...
            LOGGER.trace("LC-1 response out of range (overrun?): " + asHex(bytes));
            return MAX_AFR;
        }
        LOGGER.error("LC-1 unrecognized response: " + asHex(bytes));
        return 0;
    }

    private double getAfr(byte[] bytes) {
        return (getLambda(bytes) + 500) * getAf(bytes) / 10000.0;
    }

    private int getAf(byte[] bytes) {
        return ((bytes[2] & 1) << 7) | bytes[3];
    }

    // 010xxx1x
    private boolean isLc1(byte[] bytes) {
        return bytes.length >= 6 && matchOnes(bytes[2], 66) && matchZeroes(bytes[2], 160);
    }

    // 1x11xx1x 1xxxxxxx
    private boolean isHeaderValid(byte[] bytes) {
        return matchOnes(bytes[0], 178) && matchOnes(bytes[1], 128);
    }

    // 0100001x or 0100011x
    private boolean isOk(byte[] bytes) {
        if (matchOnes(bytes[2], 66) && matchZeroes(bytes[2], 188)) return true;
        return matchOnes(bytes[2], 70) && matchZeroes(bytes[2], 184);
    }

    // 0101101x
    private boolean isError(byte[] bytes) {
        return matchOnes(bytes[2], 90) && matchZeroes(bytes[2], 164);
    }

    // 01xxxxxx 0xxxxxxx
    private int getLambda(byte[] bytes) {
        return ((bytes[4] & 63) << 7) | bytes[5];
    }
}
