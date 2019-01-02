/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Weronika
 */
public class User {

    private String username;
    BooleanProperty connected;
    transient private ObservableList<Chanel> chanels = FXCollections.observableArrayList();

    public User(String username, boolean connected) {
        this.username = username;
        this.connected = new SimpleBooleanProperty(false);
    }

    public User(boolean connected) {
        this.connected = new SimpleBooleanProperty(connected);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BooleanProperty getConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    public ObservableList<Chanel> getChanels() {
        return chanels;
    }

    public void setChanels(ObservableList<Chanel> chanels) {
        this.chanels = chanels;
    }

}
