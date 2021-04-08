/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2021 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpos.iso.channel;

import org.jpos.iso.*;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Implements an ISOChannel able to exchange messages with ACI's BASE24 over a
 * TCP link, using 2 bytes length encoding without message trailer. Support both
 * Include and exclude types.
 *
 * @author sybond@gmail.com
 *
 * @version $Id$
 *
 * @see ISOMsg
 * @see ISOException
 * @see ISOChannel
 */
@SuppressWarnings("deprecation")
public class BSSCustomChannel extends BaseChannel {

    protected int lengthDigits;
    protected int lengthType;
    protected static final String[] TYPE_DESC = {"ExcludeHeader", "IncludeHeader"};

    /**
     * Public constructor (used by Class.forName("...").newInstance())
     */
    public BSSCustomChannel() {
        super();
    }

    /**
     * Construct client ISOChannel
     *
     * @param host server TCP Address
     * @param port server port number
     * @param p an ISOPackager
     * @see ISOPackager
     */
    public BSSCustomChannel(String host, int port, ISOPackager p) {
        super(host, port, p);
    }

    /**
     * Construct server ISOChannel
     *
     * @param p an ISOPackager
     * @see ISOPackager
     * @exception IOException
     */
    public BSSCustomChannel(ISOPackager p) throws IOException {
        super(p);
    }

    /**
     * constructs a server ISOChannel associated with a Server Socket
     *
     * @param p an ISOPackager
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see ISOPackager
     */
    public BSSCustomChannel(ISOPackager p, ServerSocket serverSocket)
            throws IOException {
        super(p, serverSocket);
    }

    protected void sendMessageLength(int len) throws IOException {
        Logger.log(new LogEvent(this, "MsgLenType", TYPE_DESC[lengthType]));
        Logger.log(new LogEvent(this, "ActualLen", Integer.toString(len)));
        switch (lengthType) {
            case 0:
                // Exclude 
                break;
            case 1:
                // Include the length bytes
                len = len + lengthDigits;
                break;
            default:
        }
        Logger.log(new LogEvent(this, "ComputedLen", Integer.toString(len)));
        serverOut.write(len >> 8);
        serverOut.write(len);
    }

    protected int getMessageLength() throws IOException, ISOException {
        int l = 0;

        byte[] b = new byte[2];
        while (l == 0) {
            serverIn.readFully(b, 0, 2);
            l = ((int) b[0] & 0xFF) << 8 | (int) b[1] & 0xFF;
            if (l == 0) {
                serverOut.write(b);
                serverOut.flush();
            }
        }
        switch (lengthType) {
            case 0:
                // Exclude 
                break;
            case 1:
                // Include the length bytes
                l = l - lengthDigits;
                break;
            default:
        }
        Logger.log(new LogEvent(this, "getMessageLength", Integer.toString(l)));//        Logger.log(new LogEvent(this, String.format("Computed received len=%d; Type=%d - %s;", l, lengthType, TYPE_DESC[lengthType])));
        return l;
    }

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        super.setConfiguration(cfg);
        lengthDigits = cfg.getInt("length-digits", 2);
        lengthType = cfg.getInt("length-encode", 0);
        Logger.log(new LogEvent(this, "Reading config(s)", String.format("LenBytes=%d Type=%d", lengthDigits, lengthType)));
    }
}
