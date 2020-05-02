/*
    Copyright 2020 Dmitry Isaenko

    This file is part of JavaUSBTool.

    JavaUSBTool is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JavaUSBTool is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JavaUSBTool.  If not, see <https://www.gnu.org/licenses/>.
*/
package javausbtool.usb;

import javafx.concurrent.Task;
import javausbtool.misc.LogPrinter;
import org.usb4java.DeviceHandle;

import java.io.File;

public class UsbCommunications extends Task<Void> {

    private LogPrinter logPrinter;

    private short vid;
    private short pid;
    private int interfaceNumber;
    private int configurationNumber;
    private boolean handleAutoKernelDriverDetach;
    private boolean mandatoryDeviceReset;
    private boolean shouldRead;
    private File writeOnStartFile;

    private int readBufferCapacity;
    private String saveRepliesTo;

    public UsbCommunications(
                             short vid,
                             short pid,
                             int interfaceNumber,
                             int configurationNumber,
                             boolean handleAutoKernelDriverDetach,
                             boolean mandatoryDeviceReset,

                             int readBufferCapacity,
                             String saveRepliesTo,
                             boolean shouldRead,
                             File writeOnStartFile
                             )
    {
        this.logPrinter = new LogPrinter();
        this.vid = vid;
        this.pid = pid;
        this.interfaceNumber = interfaceNumber;
        this.configurationNumber = configurationNumber;
        this.handleAutoKernelDriverDetach = handleAutoKernelDriverDetach;
        this.mandatoryDeviceReset = mandatoryDeviceReset;
        this.readBufferCapacity = readBufferCapacity;
        this.saveRepliesTo = saveRepliesTo;
        this.shouldRead = shouldRead;
        this.writeOnStartFile = writeOnStartFile;
    }

    @Override
    protected Void call() {
        logPrinter.print("\tStart UsbCommunications()");

        UsbConnect usbConnect = UsbConnect.connect(logPrinter,
                                                    vid,
                                                    pid,
                                                    interfaceNumber,
                                                    configurationNumber,
                                                    handleAutoKernelDriverDetach,
                                                    mandatoryDeviceReset);

        if (! usbConnect.isConnected()){
            logPrinter.close();
            return null;
        }

        DeviceHandle handler = usbConnect.getNsHandler();

        new UsbLoop(handler,
                readBufferCapacity,
                writeOnStartFile,
                this,
                logPrinter,
                saveRepliesTo,
                shouldRead
        );

        usbConnect.close();

        logPrinter.close();

        return null;
    }

}