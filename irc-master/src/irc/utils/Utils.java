/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

/**
 *
 * @author Weronika
 */
public class Utils {
    
    public static String IP = "127.0.0.1";
    public static int PORT = 2001;
   
    
    
    public static void addTextLimiter(final TextField tf, final int maxLength) {
    tf.textProperty().addListener(new ChangeListener<String>() {
        
        public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
            if (tf.getText().length() > maxLength) {
                String s = tf.getText().substring(0, maxLength);
                tf.setText(s);
            }
        }
    });
    }
    
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);  
    }
    
    
 
//#dla każdego pokoju:
//    @nazwa
//    %username;oddzielone;srednikami
//    %autorwiadomosci1;czas1;wiadomosc1;autorwiadomosci2;czas2;wiadomosc2
//$
//
//Przykład:
//
//#
//    @polibuda
//    %pawel;gawel;stasiu;frajer
//    %pawel;11:11:11;hejka;gawel;11:11:13;sklejka;pawel;11:11:19;XD oblałem 	prologa@domowy%mama;tata;sebek%mama;21:37:00;obiad!;tata;04:20:00;znów wege, Grażyna?$
//
//
//
    
    

//    
    /*public static ArrayList<String[]> decodeFromServer(String string){
        ArrayList<String[]> list = new ArrayList<String[]>();
        String[] data = string.substring(1, string.length()-1).split("@");
        for (int i = 0; i < data.length; i++) {
            String[] string1 = data[i].split("%");
            //String chatroomName = string1[0];// - nazwa chatroomu
            list.add(string1);
            //System.out.println(Arrays.toString(string1));
   
        }
        return list;        
    }*/
}
