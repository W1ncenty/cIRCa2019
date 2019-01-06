/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.controller;

import irc.IRC;
import irc.model.Chanel;
import irc.utils.Utils;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Weronika
 */
public class CreateChanelController implements Initializable {
    
    @FXML
    private TextField nameField;
    
    private Stage stage;
    private irc.IRC irc;
    @FXML
    private Label errorLabel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
 
        Utils.addTextLimiter(nameField, 32);
    }
    
    @FXML
    private void ok(ActionEvent event) {
        
        if (nameField.getText() != null && !nameField.getText().isEmpty() && irc.getUser().getConnected().get()) {
            Chanel chanel = new Chanel(nameField.getText());
            
            if (Collections.frequency(irc.getAllChanels(), chanel ) < 1) {
                //create chatroom
                irc.getWriter().println("#1%" + nameField.getText() + "$");
            } else {
                errorLabel.setText("Chatroom exists");
            }
            this.stage.close();
        }if (nameField.getText() != null || nameField.getText().isEmpty()){
            errorLabel.setText("Empty name");
        }
        else{
            errorLabel.setText("You are disconnected!");
        }
        
        
    }
    
    @FXML
    private void cancel(ActionEvent event) {
        this.stage.close();
    }
    
    public TextField getNameField() {
        return nameField;
    }
    
    public void setNameField(TextField nameField) {
        this.nameField = nameField;
    }
    
    public Stage getStage() {
        return stage;
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public IRC getIrc() {
        return irc;
    }
    
    public void setIrc(IRC irc) {
        this.irc = irc;
    }
    
}
