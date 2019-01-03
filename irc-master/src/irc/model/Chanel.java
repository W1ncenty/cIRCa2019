/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Weronika
 */
public class Chanel {
    
    private String chanelName;
    private String id;
    private ObservableList<User> users = FXCollections.observableArrayList(); ;
    private ObservableList<Message> messages = FXCollections.observableArrayList();

    public Chanel(String chanelName, String id) {
        this.chanelName = chanelName;
        this.id = id;
    }

    public Chanel(String chanelName) {
        this.chanelName = chanelName;
    }

    
    
    public String getChanelName() {
        return chanelName;
    }

    public void setChanelName(String chanelName) {
        this.chanelName = chanelName;
    }

    public ObservableList<User> getUsers() {
        return users;
    }

    public void setUsers(ObservableList<User> users) {
        this.users = users;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ObservableList<Message> messages) {
        this.messages = messages;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Chanel other = (Chanel) obj;
        if (!Objects.equals(this.chanelName, other.chanelName)) {
            return false;
        }
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    
   
    
}
