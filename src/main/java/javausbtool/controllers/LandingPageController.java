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
package javausbtool.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javausbtool.AppPreferences;
import javausbtool.MediatorControl;
import javausbtool.usb.UsbCommunications;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class LandingPageController implements Initializable {
    @FXML
    public TextArea logArea;
    @FXML
    public ProgressBar progressBar;

    @FXML
    private TextField vidTf,
            pidTf,
            devInterfaceTf,
            devConfigurationTf,
            readBufferSizeTf;

    @FXML
    private Label saveToLbl, sendFileLbl, decHexReprLbl;

    @FXML
    private CheckBox handleKernelDrvDetachCb,
            mandatorySoftResetCb;


    @FXML
    private Button changeSaveToBtn, fileToSendBtn, startBtn, stopBtn;

    @FXML
    private RadioButton readOnStartRb,
            writeOnStartRb;

    @FXML
    private MenuButton dexHexMenuItem;

    private String previouslyOpenedPath;
    private File writeOnStartFile;
    private Task<Void> usbCommunications;
    private Thread workThread;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MediatorControl.getInstance().setController(this);

        AppPreferences pref = AppPreferences.getInstance();
        vidTf.setText(Short.toString(pref.getVid()));
        pidTf.setText(Short.toString(pref.getPid()));
        devInterfaceTf.setText(Integer.toString(pref.getInterface()));
        devConfigurationTf.setText(Integer.toString(pref.getDeviceConfiguration()));
        handleKernelDrvDetachCb.setSelected(pref.getHandleKernelDrvAutoDetach());
        mandatorySoftResetCb.setSelected(pref.getSoftResetOnHandle());

        if (pref.getBufAsHex())
            readBufferSizeTf.setText(String.format("%x", pref.getReadBufferSize()));
        else
            readBufferSizeTf.setText(String.format("%d", pref.getReadBufferSize()));

        vidTf.setTextFormatter(getNumericTextFormatter());
        pidTf.setTextFormatter(getNumericTextFormatter());
        devConfigurationTf.setTextFormatter(getNumericTextFormatter());
        devConfigurationTf.setTextFormatter(getNumericTextFormatter());

        MenuItem decItem = new MenuItem("Dec");
        decItem.setOnAction(event -> {
            dexHexMenuItem.setText("Dec");

            String oldValue = decHexReprLbl.getText();

            readBufferSizeTf.setTextFormatter(new TextFormatter<>(change -> {
                String text = change.getControlNewText();
                if (text.matches("^[0-9]{0,}$")) {
                    if (! text.isEmpty()) {
                        try {
                            decHexReprLbl.setText(String.format("0x%x", Integer.parseInt(text)));
                        }
                        catch (Exception e){
                            return null;
                        }
                    }
                    return change;
                }
                return null;
            }));

            if (! oldValue.isEmpty()){
                try {
                    int val = Integer.parseInt(oldValue, 10);
                    readBufferSizeTf.setText(Integer.toString(val));
                    decHexReprLbl.setText(String.format("0x%x", val));
                }
                catch (Exception e){
                    readBufferSizeTf.setText("0");
                    decHexReprLbl.setText("0x0");
                }
            }
        });
        MenuItem hexItem = new MenuItem("Hex");

        hexItem.setOnAction(event -> {
            dexHexMenuItem.setText("Hex");

            String oldValue = decHexReprLbl.getText();

            readBufferSizeTf.setTextFormatter(new TextFormatter<>(change -> {
                String text = change.getControlNewText();
                if (text.matches("^[0-9a-fA-F]{0,}$")) {
                    if (! text.isEmpty() ) {
                        try {
                            decHexReprLbl.setText(String.format("%d", Integer.parseInt(text, 16)));
                        }
                        catch (Exception e){
                            return null;
                        }
                    }
                    return change;
                }
                return null;
            }));

            if (! oldValue.isEmpty()){
                try {
                    int val = Integer.decode(oldValue);
                    readBufferSizeTf.setText(String.format("%x", val));
                    decHexReprLbl.setText(Integer.toString(val));
                }
                catch (Exception e){
                    e.printStackTrace();
                    readBufferSizeTf.setText("0");
                    decHexReprLbl.setText("0");
                }
            }
        });

        dexHexMenuItem.getItems().add(decItem);
        dexHexMenuItem.getItems().add(hexItem);

        saveToLbl.setText(pref.getSaveTo());
        changeSaveToBtn.setOnAction(event -> setSaveToFolder());

        ToggleGroup readWriteToggleGrp = new ToggleGroup();
        readWriteToggleGrp.getToggles().addAll(readOnStartRb, writeOnStartRb);
        readOnStartRb.setSelected(true);

        previouslyOpenedPath = System.getProperty("user.home");

        fileToSendBtn.setOnAction(event -> selectFileToSendInitially());

        startBtn.setOnAction(event -> startProcess());
        stopBtn.setOnAction(event -> stopProcess());

        if (pref.getBufAsHex())
            hexItem.fire();
        else
            decItem.fire();
    }

    private TextFormatter getNumericTextFormatter(){
        return new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("^[0-9]{0,}$"))
                return change;
            return null;
        });
    }

    private int getBufferSize() throws NumberFormatException {
        if (dexHexMenuItem.getText().equals("Dec"))
            return Integer.parseInt(readBufferSizeTf.getText());
        return Integer.parseInt(readBufferSizeTf.getText(), 16);
    }

    private void setSaveToFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Save files to...");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = dirChooser.showDialog(progressBar.getScene().getWindow());
        if (dir != null && dir.exists())
            saveToLbl.setText(dir.getAbsolutePath());
    }

    private void selectFileToSendInitially(){
        File file;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");

        File validator = new File(previouslyOpenedPath);
        if (validator.exists() && validator.isDirectory())
            fileChooser.setInitialDirectory(validator);
        else
            return;

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Any", "*.*"));

        file = fileChooser.showOpenDialog(sendFileLbl.getScene().getWindow());
        if (file != null) {
            sendFileLbl.setText(file.getName());
            previouslyOpenedPath = file.getParent();
            writeOnStartFile = file;
        }
    }

    private void startProcess(){
        System.out.println(getBufferSize());

        logArea.clear();
        if (writeOnStartRb.isSelected() && writeOnStartFile == null){
            logArea.appendText("'Write on start' option selected but no file defined\n");
            return;
        }

        if ((workThread != null && workThread.isAlive())){
            logArea.appendText("Something still running\n");
            return;
        }

        usbCommunications = new UsbCommunications(
                (short) Integer.parseInt(vidTf.getText()),
                (short) Integer.parseInt(pidTf.getText()),
                Integer.parseInt(devInterfaceTf.getText()),
                Integer.parseInt(devConfigurationTf.getText()),
                handleKernelDrvDetachCb.isSelected(),
                mandatorySoftResetCb.isSelected(),
                getBufferSize(),
                saveToLbl.getText(),
                readOnStartRb.isSelected(),
                writeOnStartFile
        );
        workThread = new Thread(usbCommunications);
        workThread.setDaemon(true);
        workThread.start();
    }
    private void stopProcess(){
        if (workThread != null && workThread.isAlive()){
            usbCommunications.cancel(false);
            logArea.appendText("Stop request sent\n");
        }
        else
            logArea.appendText("Nothing to stop\n");
    }

    /**
     * Save any changes made on settings on app
     * */
    public void exit() {
        AppPreferences pref = AppPreferences.getInstance();
        short shortValue;
        try{
            if ((shortValue = Short.parseShort(vidTf.getText())) > 0)
                pref.setVid(shortValue);
        }
        catch (NumberFormatException e){ e.printStackTrace(); }
        try{
            if ((shortValue = Short.parseShort(pidTf.getText())) > 0)
                pref.setPid(shortValue);
        }
        catch (NumberFormatException e){ e.printStackTrace(); }

        int intValue;
        try {
            if ((intValue = Integer.parseInt(devInterfaceTf.getText())) > 0)
                pref.setInterface(intValue);
        }
        catch (NumberFormatException e){ e.printStackTrace(); }
        try {
            if ((intValue = Integer.parseInt(devConfigurationTf.getText())) > 0)
                pref.setDeviceConfiguration(intValue);
        }
        catch (NumberFormatException e){ e.printStackTrace(); }

        pref.setHandleKernelDrvAutoDetach(handleKernelDrvDetachCb.isSelected());
        pref.setSoftResetOnHandle(mandatorySoftResetCb.isSelected());

        pref.setSaveTo(saveToLbl.getText());

        try {
            if ( (intValue = getBufferSize()) > 0)
                pref.setReadBufferSize(intValue);
        }
        catch (NumberFormatException e){ e.printStackTrace(); }

        pref.setBufAsHex(dexHexMenuItem.getText().equals("Hex"));
    }
}
