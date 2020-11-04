/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;

import com.mycompany.mobileapp.authentication.Group;
import com.mycompany.mobileapp.authentication.User;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sanderkeedklang
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    private String gameName;
    private String title;
    private String description;
    private int maxPlayers;
    private List<User> players;   
    private String date;
    private String time;
    private User gameOwner;
    
    @OneToMany
    List<Photo> profileImages;
    
      public void addPhoto(Photo photo) {
        if(this.profileImages == null) {
            this.profileImages = new ArrayList<>();
        }
        
        this.profileImages.add(photo);
    }

    public void addPlayer(User player){ 
        if(players == null){
            players = new ArrayList<User>();
        }
        players.add(player);       
       
    }

    public int getPlayerNumber(){
        return players.size();
    }

    public void removePlayer(User player){
        players.remove(player);
    }
}
