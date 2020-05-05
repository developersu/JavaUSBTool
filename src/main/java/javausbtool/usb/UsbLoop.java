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
import org.usb4java.LibUsb;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Tinfoil processing
 * */
class UsbLoop {

    private LogPrinter logPrinter;
    private DeviceHandle handlerNS;
    private Task<Void> task;
    private int readBufferCapacity;
    private long readCounter;
    private File saveRepliesFolder;

    public UsbLoop(DeviceHandle handler,
                    int readBufferCapacity,
                    File fileToSendOnStart,
                    Task<Void> task,
                    LogPrinter logPrinter,
                    String saveRepliesTo,
                    boolean shouldRead
    ){
        this.handlerNS = handler;
        this.task = task;
        this.logPrinter = logPrinter;
        this.readBufferCapacity = readBufferCapacity;
        this.readCounter = 0;

        logPrinter.print("============= UsbProtocol =============");

        this.saveRepliesFolder = new File(saveRepliesTo+File.separator+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss")));
        saveRepliesFolder.mkdirs();

        logPrinter.print("Save replies to dir: "+saveRepliesFolder.getName());

        if (! shouldRead) {
            try {
                writeFile(fileToSendOnStart);
            }
            catch (Exception e){
                e.printStackTrace();
                logPrinter.print(e.getMessage());
                logPrinter.print("Terminating now");
                return;
            }
        }

        readLoop();
    }

    private void readLoop(){
        while (true){
            try {
                dumpData(readUsb());
            }
            catch (InterruptedException ioe){
                logPrinter.print("Execution interrupted");
                return;
            }
            catch (Exception e){
                e.printStackTrace();
                logPrinter.print(e.getMessage());
                logPrinter.print("Terminating now");
                return;
            }
        }
    };

    void writeFile(File file) throws IOException, NullPointerException, ArithmeticException {
        byte[] readBuffer;
        long currentOffset = 0;
        int chunk = 8388608;
        long size = file.length();

        BufferedInputStream bufferedInStream = new BufferedInputStream(new FileInputStream(file));

        while (currentOffset < size) {
            if ((currentOffset + chunk) >= size)
                chunk = Math.toIntExact(size - currentOffset);

            logPrinter.updateProgress((currentOffset + chunk) / (size / 100.0) / 100.0);

            readBuffer = new byte[chunk];

            if (bufferedInStream.read(readBuffer) != chunk)
                throw new IOException("Reading from file stream suddenly ended.");

            if (writeUsb(readBuffer))
                throw new IOException("Failure during file transfer.");
            currentOffset += chunk;
        }
        bufferedInStream.close();
        logPrinter.updateProgress(1.0);
    }

    private void dumpData(byte[] data) throws Exception{
        this.readCounter++;
        File chunkFile = new File(saveRepliesFolder.getAbsolutePath()+File.separator+readCounter+".bin");
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chunkFile, false));
        bos.write(data);
        bos.close();
    }
    /**
     * Sending any byte array to USB device
     * @return 'false' if no issues
     *          'true' if errors happened
     * */
    private boolean writeUsb(byte[] message) {
        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(message.length);   //writeBuffer.order() equals BIG_ENDIAN;
        writeBuffer.put(message);                                             // Don't do writeBuffer.rewind();
        IntBuffer writeBufTransferred = IntBuffer.allocate(1);
        int result;
        //int varVar = 0; //todo:remove
        while (! task.isCancelled()) {
            /*
            if (varVar != 0)
                logPrinter.print("writeUsb() retry cnt: "+varVar, EMsgType.INFO); //NOTE: DEBUG
            varVar++;
            */
            result = LibUsb.bulkTransfer(handlerNS, (byte) 0x01, writeBuffer, writeBufTransferred, 5050);  // last one is TIMEOUT. 0 stands for unlimited. Endpoint OUT = 0x01

            switch (result){
                case LibUsb.SUCCESS:
                    if (writeBufTransferred.get() == message.length)
                        return false;
                    logPrinter.print("TF Data transfer issue [write]" +
                            "\n         Requested: "+message.length+
                            "\n         Transferred: "+writeBufTransferred.get());
                    return true;
                case LibUsb.ERROR_TIMEOUT:
                    //System.out.println("writeBuffer position: "+writeBuffer.position()+" "+writeBufTransferred.get());
                    //writeBufTransferred.clear();    // MUST BE HERE IF WE 'GET()' IT
                    continue;
                default:
                    logPrinter.print("TF Data transfer issue [write]" +
                            "\n         Returned: "+ UsbErrorCodes.getErrCode(result) +
                            "\n         (execution stopped)");
                    return true;
            }
        }
        logPrinter.print("INFO TF Execution interrupted");
        return true;
    }
    /**
     * Reading what USB device responded.
     * @return byte array if data read successful
     *         'null' if read failed
     * */
    private byte[] readUsb() throws Exception{
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(readBufferCapacity);
        // We can limit it to 32 bytes, but there is a non-zero chance to got OVERFLOW from libusb.
        IntBuffer readBufTransferred = IntBuffer.allocate(1);
        int result;
        while (! task.isCancelled()) {
            result = LibUsb.bulkTransfer(handlerNS, (byte) 0x81, readBuffer, readBufTransferred, 1000);  // last one is TIMEOUT. 0 stands for unlimited. Endpoint IN = 0x81

            switch (result) {
                case LibUsb.SUCCESS:
                    int trans = readBufTransferred.get();
                    byte[] receivedBytes = new byte[trans];
                    readBuffer.get(receivedBytes);
                    return receivedBytes;
                case LibUsb.ERROR_TIMEOUT:
                    continue;
                default:
                    throw new Exception("Data transfer issue [read]" +
                            "\n         Returned: " + UsbErrorCodes.getErrCode(result)+
                            "\n         (execution stopped)");
            }
        }
        throw new InterruptedException("Execution interrupted");
    }
}
