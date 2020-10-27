/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;

import com.mycompany.mobileapp.authentication.User;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
public class Photo implements Serializable {

    @Id
    String id;
    
    String name;
    
    long filesize;
    String mimeType;
   
}
