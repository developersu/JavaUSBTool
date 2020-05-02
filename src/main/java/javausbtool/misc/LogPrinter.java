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
package javausbtool.misc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogPrinter {
    private MessagesConsumer msgConsumer;
    private BlockingQueue<String> msgQueue;
    private BlockingQueue<Double> progressQueue;

    public LogPrinter(){
        this.msgQueue = new LinkedBlockingQueue<>();
        this.progressQueue = new LinkedBlockingQueue<>();
        this.msgConsumer = new MessagesConsumer(this.msgQueue, this.progressQueue);
        this.msgConsumer.start();
    }
    /**
     * This is what will print to textArea of the application.
     * */
    public void print(String message){
        try {
            msgQueue.put(message+"\n");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Update progress for progress bar
     * */
    public void updateProgress(Double value) {
        try {
            progressQueue.put(value);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    /**
     * When we're done - close it
     * */
    public void close(){
        msgConsumer.interrupt();
    }
}
