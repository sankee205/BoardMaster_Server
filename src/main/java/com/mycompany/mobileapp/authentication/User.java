/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.authentication;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mycompany.mobileapp.Photo;
import com.mycompany.mobileapp.PhotoAdapter;
import com.mycompany.mobileapp.authentication.Group;
import static com.mycompany.mobileapp.authentication.User.FIND_ALL_USERS;
import static com.mycompany.mobileapp.authentication.User.FIND_USER_BY_IDS;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Version;
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
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude={"groups"})
@Table(name = "users",  schema = "public")
@NamedQuery(name = FIND_ALL_USERS, query = "select u from User u order by u.firstname")
@NamedQuery(name = FIND_USER_BY_IDS, query = "select u from User u where u.username in :ids")
public class User implements Serializable{
    public static final String FIND_USER_BY_IDS = "User.findUserByIds";
    public static final String FIND_ALL_USERS = "User.findAllUsers";
    
    public enum State {
        ACTIVE, INACTIVE
    }
    
    private static final long serialVersionUID = 1L;
    
    
    @Column(name = "fname", nullable = false)
    private String firstname;
    
    @Column(name = "lname", nullable = false)
    private String lastname;
    
    @Id
    @Column(name = "uid", nullable = false)
    private String username;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @JsonbTransient
    @Column(name = "password", nullable = false)
    private String password;
   
    @ManyToMany
    @JoinTable(name="AUSERGROUP",
            joinColumns = @JoinColumn(name="uid", referencedColumnName = "uid"),
            inverseJoinColumns = @JoinColumn(name="name",referencedColumnName = "name"))
    List<Group> groups;
    
    @OneToMany
    List<Photo> profileImages;
    
    
    
    public List<Group> getGroups() {
        if(groups == null) {
            groups = new ArrayList<>();
        }
        return groups;
    }
    
      public void addPhoto(Photo photo) {
        if(this.profileImages == null) {
            this.profileImages = new ArrayList<>();
        }
        
        this.profileImages.add(photo);
    }
    
}
