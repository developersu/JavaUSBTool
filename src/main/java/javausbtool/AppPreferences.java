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
package javausbtool;

import java.util.prefs.Preferences;

public class AppPreferences {
    private static final AppPreferences INSTANCE = new AppPreferences();
    public static AppPreferences getInstance() { return INSTANCE; }

    private Preferences preferences;

    private AppPreferences(){
        preferences = Preferences.userRoot().node("javausbtool");
    }

    public void setSaveTo(String path){preferences.put("save_to_path", path);}
    public String getSaveTo(){return preferences.get("save_to_path", System.getProperty("user.dir"));}

    public String getRecentPath(){return preferences.get("recent_path", System.getProperty("user.home"));}
    public void setRecentPath(String path){preferences.put("recent_path", path);}

    public double getSceneWidth(){ return preferences.getDouble("window_width", 850.0); }
    public void setSceneWidth(double value){ preferences.putDouble("window_width", value); }

    public double getSceneHeight(){ return preferences.getDouble("window_height", 525.0); }
    public void setSceneHeight(double value){ preferences.putDouble("window_height", value); }

    // Usb setup
    public short getVid(){ return (short) preferences.getInt("vid", 0x057E); }
    public void setVid(short value){ preferences.putInt("vid", value); }

    public short getPid(){ return (short) preferences.getInt("pid", 0x3000); }
    public void setPid(short value){ preferences.putInt("pid", value); }

    public int getInterface(){ return preferences.getInt("interface", 1); }
    public void setInterface(int value){ preferences.putInt("interface", value); }

    public int getDeviceConfiguration(){ return preferences.getInt("devconfiguration", 0); }
    public void setDeviceConfiguration(int value){ preferences.putInt("devconfiguration", value); }

    public boolean getHandleKernelDrvAutoDetach(){ return preferences.getBoolean("handle_kernel_driver", true); }
    public void setHandleKernelDrvAutoDetach(boolean value){preferences.putBoolean("handle_kernel_driver", value);}

    public boolean getSoftResetOnHandle(){ return preferences.getBoolean("soft_reset_on_handle", false); }
    public void setSoftResetOnHandle(boolean value){preferences.putBoolean("soft_reset_on_handle", value);}

    public int getReadBufferSize(){ return preferences.getInt("read_buffer_size", 512); }
    public void setReadBufferSize(int value){ preferences.putInt("read_buffer_size", value); }
}