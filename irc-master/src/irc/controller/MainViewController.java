/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.controller;

import irc.IRC;
import irc.model.User;
import irc.utils.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Weronika
 */
public class MainViewController implements Initializable {

    @FXML
    private Label connectionLabel;

    @FXML
    private TextField nicknameTextArea;

    @FXML
    private Button joinButton;

    private irc.IRC irc;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.connectionLabel.setText("Disconnected");

        Utils.addTextLimiter(nicknameTextArea, 18);

    }

    @FXML
    private void connect(ActionEvent event) {

        if (irc.getUser().getUsername() == null && !nicknameTextArea.getText().isEmpty()) {
            bindUser();
            irc.getChatRoomController().displayChatroomList();

            try {
                Socket socket = new Socket(Utils.IP, Utils.PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                
                irc.setSocket(socket);
                irc.setReader(reader);
                irc.setWriter(writer);
                
                System.out.println("Connected: " + socket);
                
               

                //Sprawdz dostepnosc nickname na serwerze
                irc.getUser().setUsername(nicknameTextArea.getText());
                irc.getWriter().println("0" + irc.getUser().getUsername());
                irc.getUser().setConnected(true);

                irc.getChatRoomController().getWaitingForMessagesRunnable().setStopped(false);
                irc.getChatRoomController().displayMessages();

                this.connectionLabel.setTextFill(Color.web("00ff00"));
            } catch (IOException ex) {
                Logger.getLogger(MainViewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @FXML
    private void disconnect(ActionEvent event) {

        if (irc.getUser().getConnected().get()) {

            irc.getChatRoomController().getWaitingForMessagesRunnable().setStopped(true);

            this.connectionLabel.setTextFill(Color.web("ff0000"));
            try {
                irc.getSocket().close();
            } catch (IOException ex) {
                Logger.getLogger(MainViewController.class.getName()).log(Level.SEVERE, null, ex);
            }

            irc.getUser().setUsername(null);
            irc.getUser().setConnected(false);

        }

    }

    public IRC getIrc() {
        return irc;
    }

    public void setIrc(IRC irc) {
        this.irc = irc;
        
    }

    public void bindUser() {
        this.connectionLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (irc.getUser().getConnected().get()) {
                return "Connected";
            }
            return "Disconnected";
        }, irc.getUser().getConnected()));
    }

    @FXML
    private void join(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(irc.getClass().getResource("view/JoinChanels.fxml"));
            AnchorPane chooseChanel = loader.load();

            Stage chanelsStage = new Stage();
            chanelsStage.setTitle("Choose chanel");
            chanelsStage.initModality(Modality.WINDOW_MODAL);
            chanelsStage.initOwner(irc.getStage());

            Scene scene = new Scene(chooseChanel);
            chanelsStage.setScene(scene);

            JoinChanelsController controller = loader.getController();
            controller.setStage(chanelsStage);
            controller.setIrc(irc);

            chanelsStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void leaveChatroom(ActionEvent event) {
        
    }

    @FXML
    private void createChatroom(ActionEvent event) {
        
    }

}
