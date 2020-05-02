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

import javafx.animation.AnimationTimer;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javausbtool.MediatorControl;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class MessagesConsumer extends AnimationTimer {
    private final BlockingQueue<String> msgQueue;
    private final TextArea logsArea;

    private final BlockingQueue<Double> progressQueue;
    private final ProgressBar progressBar;

    private boolean isInterrupted;

    MessagesConsumer(BlockingQueue<String> msgQueue, BlockingQueue<Double> progressQueue){
        this.isInterrupted = false;

        this.msgQueue = msgQueue;
        this.logsArea = MediatorControl.getInstance().getContoller().logArea;

        this.progressQueue = progressQueue;
        this.progressBar = MediatorControl.getInstance().getContoller().progressBar;

        progressBar.setProgress(0.0);

        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    @Override
    public void handle(long l) {
        ArrayList<String> messages = new ArrayList<>();
        int msgRecieved = msgQueue.drainTo(messages);
        if (msgRecieved > 0)
            messages.forEach(logsArea::appendText);

        ArrayList<Double> progress = new ArrayList<>();
        int progressRecieved = progressQueue.drainTo(progress);
        if (progressRecieved > 0) {
            progress.forEach(prg -> {
                if (prg != 1.0)
                    progressBar.setProgress(prg);
                else
                    progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            });
        }

        if (isInterrupted) {
            progressBar.setProgress(0.0);
            this.stop();
        }
    }

    public void interrupt(){
        this.isInterrupted = true;
    }
}