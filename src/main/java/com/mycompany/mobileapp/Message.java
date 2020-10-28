/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.mycompany.mobileapp.Message.FIND_ALL_MESSAGES;
import com.mycompany.mobileapp.authentication.User;
import static com.mycompany.mobileapp.Message.FIND_MESSAGES_BY_USERID;
import java.util.Date;
/**
 *
 * @author sanderkeedklang
 */
@Entity
@Data
@AllArgsConstructor
@NamedQuery(name = FIND_MESSAGES_BY_USERID,
            query = "select m from Message m " +
                    "where m.conversation.id = :cid and " +
                    "(m.conversation.owner.username = :username or :username member of m.conversation.recipients)")
@NamedQuery(name = FIND_ALL_MESSAGES,
            query = "select m from Message m")
public class Message {
    public static final String FIND_ALL_MESSAGES = "Message.findAllUsers";
    public static final String FIND_MESSAGES_BY_USERID = "Message.findByUserId";
    
    @Id @GeneratedValue
    private String id;

    String text;
    
    @ManyToOne
    User sender;

    @ManyToOne
    Conversation conversation;
    
    @OneToMany
    List<Photo> photos;
    
    @Temporal(javax.persistence.TemporalType.DATE)
    Date created;
    
    protected Message() {}
    
    public Message(String text, User sender, Conversation conversation) {
        this.text = text;
        this.sender = sender;
        this.conversation = conversation;
        this.conversation.getMessages().add(this);
    }
    public String getId() {
        return id;
    }

    public void addPhoto(Photo photo) {
        if(this.photos == null) {
            this.photos = new ArrayList<>();
        }
        
        this.photos.add(photo);
    }
    
    public String getConversationId() {
        return conversation != null ? conversation.getId() : null;
    }
}
