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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javausbtool.controllers.LandingPageController;

import java.util.Locale;
import java.util.ResourceBundle;

public class JFXMain extends Application {

    public static final String appVersion = "v0.2";

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/landingPage.fxml"));

        Parent root = loader.load();

        primaryStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/res/app_icon32x32.png")),
                new Image(getClass().getResourceAsStream("/res/app_icon48x48.png")),
                new Image(getClass().getResourceAsStream("/res/app_icon64x64.png")),
                new Image(getClass().getResourceAsStream("/res/app_icon128x128.png"))
        );

        primaryStage.setTitle("JavaUSBTool "+appVersion);
        primaryStage.setMinWidth(650);
        primaryStage.setMinHeight(450);
        Scene mainScene = new Scene(root,
                AppPreferences.getInstance().getSceneWidth(),
                AppPreferences.getInstance().getSceneHeight()
        );

        mainScene.getStylesheets().add("/res/app_light.css");

        primaryStage.setScene(mainScene);
        primaryStage.show();

        LandingPageController controller = loader.getController();
        primaryStage.setOnHidden(e-> {
            AppPreferences.getInstance().setSceneHeight(mainScene.getHeight());
            AppPreferences.getInstance().setSceneWidth(mainScene.getWidth());
            controller.exit();
        });
    }

    public static void main(String[] args) {
        if ((args.length == 1) && (args[0].equals("-v") || args[0].equals("--version")))
            System.out.println("JavaUSBTool "+JFXMain.appVersion);
        else
            launch(args);
    }
}
