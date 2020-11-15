/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.resources;

import com.mycompany.mobileapp.Conversation;
import com.mycompany.mobileapp.Game;
import com.mycompany.mobileapp.authentication.Group;
import com.mycompany.mobileapp.authentication.User;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author sanderkeedklang
 */
@Path("conversation")
@Stateless
@RolesAllowed({Group.USER})
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class ConversationService {
    @PersistenceContext
    EntityManager em;

    @Context
    SecurityContext sc;


    @GET
    public List<Conversation> getConversations() {
        return em.createNamedQuery(Conversation.FIND_BY_USER,Conversation.class)
                 .setParameter("userid", getCurrentUser())
                 .getResultList();
    }
    

    @GET
    @Path("conversationfromdate")
    public List<Conversation> getUpdatedConversations(
                @QueryParam("from") String date) throws ParseException {
        Date from = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss").parse(date);
        return em.createNamedQuery(Conversation.FIND_BY_USER_AND_DATE,Conversation.class)
                .setParameter("userid", getCurrentUser())
                .setParameter("date",from)
                .getResultList();
    }    
    
    
    @POST
    @RolesAllowed({Group.USER})
    @Path("createconversation")
    public Conversation createConversation(
            @FormParam("game") Long gameid) {
        Conversation result = null;

        User owner = getCurrentUser();
        Game game = em.find(Game.class, gameid);
        List<User> recipients = new ArrayList<User>();
        if(owner != null) {
            result = new Conversation();
            result.setRecipients(recipients);
            result.setOwner(owner);
            result.setGame(game);
            result.addPlayer(owner);
            em.persist(result);
        }
        
        return result;
    }
    
    @GET
    @RolesAllowed({Group.USER})
    @Path("getconversationgame")
    public Object getConversationByGame(@QueryParam("gameid") Long gameid){
        User user = getCurrentUser();
        String query = " select c.id, c.created, c.owner.username, c.game.id from Conversation c, User u where u.username= :userid and c.game.id = :gameid and u member of c.recipients";
        return em.createQuery(query,Conversation.class)
                .setParameter("userid", user.getUsername())
                .setParameter("gameid", gameid)
                .getSingleResult();
    }


    @POST
    @Path("updaterecipients")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updaterecipients(
            @DefaultValue("-1") @FormParam("conversationid") Long conversationid,
            @FormParam("recipientids") List<String> recipientIds) {
        Conversation conversation = em.find(Conversation.class,conversationid);
        if(conversation != null) {
            List<User> recipients = findUsersByUserId(recipientIds);
            conversation.setRecipients(recipients);
            em.merge(conversation);
            return Response.ok().build();
        } else {
            return Response.notModified().build();
        }
    }
    

    private List<User> findUsersByUserId(List<String> recipientIds) {
        return recipientIds.size() > 0 ? em.createNamedQuery(User.FIND_USER_BY_IDS, User.class)
                .setParameter("ids",recipientIds)
                .getResultList() : new ArrayList<>();
    }

    public Conversation findById(Long id) {
        return id != null ? em.find(Conversation.class, id) : null;
    }
    private User getCurrentUser(){
        return em.find(User.class, sc.getUserPrincipal().getName());
    }
}
