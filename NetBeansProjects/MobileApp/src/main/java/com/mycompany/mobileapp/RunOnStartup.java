/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;


import com.mycompany.mobileapp.authentication.Group;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;


@Singleton
@Startup
public class RunOnStartup {
    
    @PersistenceContext
    EntityManager em;
    
      
    @PostConstruct
    public void init() {
        long groups = (long) em.createQuery("select count(g.name) from Group g").getSingleResult();
        if(groups == 0) {
            em.persist(new Group(Group.USER));
            em.persist(new Group(Group.ADMIN));
        }
        
        
    }
}