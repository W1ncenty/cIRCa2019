/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.controller;

import irc.IRC;
import irc.model.Chanel;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Weronika
 */
public class JoinChanelsController implements Initializable {

    private Stage stage;

    @FXML
    private ChoiceBox<String> choiceBox;

    private String choosenChatroom;

    private irc.IRC irc;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        irc.allChanels.forEach(x -> {
            choiceBox.getItems().add(x.getChanelName());
        });
        

        choiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

            public void changed(ObservableValue ov, String value, String new_value) {
                choosenChatroom = new_value;
            }
        });

    }

    @FXML
    private void chooseChatRoom(MouseEvent event) {
    }

    @FXML
    private void ok(ActionEvent event) {

        if (choosenChatroom != null) {
            Chanel result = null;
            for (Chanel c : irc.getAllChanels()) {
                if (choosenChatroom.equals(c.getChanelName())) {
                    result = c;
                    break;
                }
            }
            
            //#2%chanelname$
            irc.getWriter().println("#2%" + result.getChanelName()+ "$");

            cancel(event);
        }

    }

    @FXML
    private void cancel(ActionEvent event) {
        this.stage.close();

    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getChoosenChatroom() {
        return choosenChatroom;
    }

    public void setChoosenChatroom(String choosenChatroom) {
        this.choosenChatroom = choosenChatroom;
    }

    public IRC getIrc() {
        return irc;
    }

    public void setIrc(IRC irc) {
        this.irc = irc;
    }

}
