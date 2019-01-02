/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import irc.utils.Utils;

/**
 *
 * @author weronika
 */
public class Message {
    
    private String chanelName;
    private String username;
    private String time;
    private String content;

    public Message(String chanelName, String username,String time, String content) {
        this.chanelName = chanelName;
        this.username = username;
        this.time = time;
        this.content = content;
    }

    
    
    public String getChanelName() {
        return chanelName;
    }

    public void setChanelName(String chanelName) {
        this.chanelName = chanelName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return this.time + Utils.padLeft(this.username,20) + " > " + this.content;
    }
    
    
    
    
}