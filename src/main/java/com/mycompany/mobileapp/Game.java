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
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
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
    private Long id;

    //name of game 
    private String gameName;
    
    //title of own choosing
    private String title;
    
    //dscription
    private String description;
    
    //max players
    private int maxPlayers;
    
    //List of all players
    private List<User> players;
    
    //the game date
    private String date;
    
    //the time the game starts
    private String time;
    
    //person who published it
    private User gameOwner;
    
    //Photo of own choosing
    @OneToMany
    List<Photo> profileImages;
    
    //adds a photo
    public void addPhoto(Photo photo) {
        if(this.profileImages == null) {
            this.profileImages = new ArrayList<>();
        }
        
        this.profileImages.add(photo);
    }

    //adds a player
    public void addPlayer(User player){ 
        if(players == null){
            players = new ArrayList<User>();
        }
        players.add(player);       
       
    }

    //return the number of players
    public int getPlayerNumber(){
        return players.size();
    }

    //removes a player
    public void removePlayer(User player){
        players.remove(player);
    }
}
