/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;

import static com.mycompany.mobileapp.Conversation.FIND_BY_ID_AND_USERID;
import static com.mycompany.mobileapp.Conversation.FIND_BY_USER;
import static com.mycompany.mobileapp.Conversation.FIND_BY_USER_AND_DATE;
import com.mycompany.mobileapp.authentication.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 *
 * @author sanderkeedklang
 */
@Entity
@Data 
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude={"messages"})

@NoArgsConstructor
@NamedQuery(name = FIND_BY_USER,
            query = "select distinct c from Conversation c, User u " +
                    "where u.username = :username and (c.owner = u or u member of c.recipients) " +
                    "order by c.created desc")
@NamedQuery(name = FIND_BY_USER_AND_DATE,
            query = "select distinct c from Conversation c, User u inner join c.messages m " +
                    "where u.username = :username and (c.owner = u or u member of c.recipients) " +
                    "and m.created >= :date")
@NamedQuery(name = FIND_BY_ID_AND_USERID,
            query = "select distinct c from Conversation c, User u " +
                    "where u.username = :username and c.id = :cid " +
                    "and c.owner = u ")
public class Conversation {
    public static final String FIND_BY_USER = "Conversation.findByUser";
    public static final String FIND_BY_USER_AND_DATE = "Conversation.findByUserAndDate";
    public static final String FIND_BY_ID_AND_USERID = "Conversation.findByIdAndUserId";
    
    @Id 
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @OneToMany(mappedBy = "conversation",cascade = CascadeType.ALL)
    List<Message> messages;

    
    @ManyToMany(cascade = {CascadeType.PERSIST})
    List<User> recipients;

    @ManyToOne(optional = false,cascade = CascadeType.PERSIST)
    User owner;
    
    @Temporal(javax.persistence.TemporalType.DATE)
    Date created;
    
    @OneToOne
    Game game;
    
    @PrePersist
    protected void onCreate() {
        created = new Date();
    }
    
    public List<Message> getMessages() {
        if(messages == null) {
            messages = new ArrayList<>();
        }
        
        return messages;
    }
    
    public void addMessage(Message message) {
       getMessages().add(message);
    }
    
    public List<User> getUsers(){
        if(recipients == null){
            recipients = new ArrayList<>();
        }
        return recipients;
    }
    
    public void addPlayer(User user){
        this.recipients.add(user);
    }
    
   
}
