/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.resources;


import com.mycompany.mobileapp.Conversation;
import com.mycompany.mobileapp.Game;
import com.mycompany.mobileapp.Message;
import com.mycompany.mobileapp.Photo;
import com.mycompany.mobileapp.authentication.Group;
import com.mycompany.mobileapp.authentication.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import net.coobird.thumbnailator.Thumbnails;
import com.mycompany.mobileapp.authentication.AuthenticationService;
import javax.annotation.security.DeclareRoles;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author sanderkeedklang
 */
@Path("chat")
@Stateless
@DeclareRoles({Group.USER})
public class ChatService {
    @Inject
    AuthenticationService authService;    
    
    @Context
    SecurityContext sc;
        
    @PersistenceContext
    EntityManager em;
    
    /** path to store photos */
    @Inject
    @ConfigProperty(name = "photo.storage.path", defaultValue = "mobileapp_images")
    String photoPath;
        
    /** CDI Event used too generate events when new message is created */
    @Inject
    Event<Message> messageEvent;
        
    /**
     * Returns list of all users
     * 
     * @return all users
     */
    @GET
    @Path("users")
    @RolesAllowed({Group.USER, Group.ADMIN})
    public List<User> getAllUsers() {
        return em.createNamedQuery(User.FIND_ALL_USERS,User.class).getResultList();
    }

    
    /**
     * All messages
     * 
     * @return Returns all messages of the system.
     */
    @GET
    @Path("messages")
    @RolesAllowed({Group.ADMIN})
    public List<Message> getMessages() {
        return em.createNamedQuery(Message.FIND_ALL_MESSAGES,Message.class).getResultList();
    }
    
    /**
     * Get messages of a specific conversation
     * 
     * @param conversationid id of conversation
     * @return list of messages belonging to a specific conversation
     */
    @GET
    @Path("messages/{conversationid}")
    @RolesAllowed({Group.USER})
    public List<Message> getMessages(@DefaultValue("-1") @PathParam("conversationid")Long conversationid) {
        return em.createNamedQuery(Message.FIND_MESSAGES_BY_USERID,Message.class)
            .setParameter("cid", conversationid)
            .setParameter("userid", sc.getUserPrincipal().getName())
            .getResultList();
    }
    
   

    
    private String getPhotoPath() {
        return photoPath;
    }

    /**
     * TODO: Check if user is part of receivers
     *
     * @param conversationid
     * @return
     */
    private Conversation getConversation(String conversationid) {
        return em.createNamedQuery(Conversation.FIND_BY_ID_AND_USERID,Conversation.class)
                 .setParameter("cid", conversationid)
                 .setParameter("userid", sc.getUserPrincipal().getName())
                 .getSingleResult();
    }

    /**
     * Accepts a multipart POST image
     * 
     * @param conversationid id of conversation to attach message
     * @param text text of message
     * @param multiPart used to extract the image data
     * @return 
     */
    @POST
    @Path("send")    
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Group.USER})
    public Response sendMessage(@FormDataParam("conversationid")String conversationid,
                                @FormDataParam("message")String text,
                                FormDataMultiPart multiPart) {
        Message message;
        try {
            User user = em.find(User.class,sc.getUserPrincipal().getName());
            Conversation conversation = getConversation(conversationid);
            message = new Message(text,user, conversation);
            List<FormDataBodyPart> images = multiPart.getFields("image");
            if(images != null) {
                for(FormDataBodyPart part : images) {
                    InputStream is = part.getEntityAs(InputStream.class);
                    ContentDisposition meta = part.getContentDisposition();            

                    String pid = UUID.randomUUID().toString();
                    Files.copy(is, Paths.get(getPhotoPath(),pid));

                    Photo photo = new Photo();
                    photo.setId(pid);
                    photo.setFilesize(meta.getSize());
                    photo.setMimeType(meta.getType());
                    photo.setName(meta.getFileName());
                    
                    em.persist(photo);
                    message.addPhoto(photo);
                }
            }

            em.persist(message);
            
            // Send async server event. Will be handled by MailService
            messageEvent.fireAsync(message);
        } catch (IOException ex) {
            Logger.getLogger(ChatService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        }
        
        return Response.ok(message).build();
    }
    
    @GET
    @Path("create_group")    
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Group.USER})
    public Response createConversation() {
        User user = em.find(User.class,sc.getUserPrincipal().getName());
        Conversation conversation = new Conversation(user);

        return Response.ok().build();
    }
    

    /**
     * Streams an image to the browser(the actual compressed pixels). The image
     * will be scaled to the appropriate width if the with parameter is provided.
     *
     * @param name the filename of the image
     * @param width the required scaled with of the image
     * 
     * @return the image in original format or in jpeg if scaled
     */
    @GET
    @Path("image/{name}")
    @Produces("image/jpeg")
    public Response getImage(@PathParam("name") String name, 
                             @QueryParam("width") int width) {
        if(em.find(Photo.class, name) != null) {
            StreamingOutput result = (OutputStream os) -> {
                java.nio.file.Path image = Paths.get(getPhotoPath(),name);
                if(width == 0) {
                    Files.copy(image, os);
                    os.flush();
                } else {
                    Thumbnails.of(image.toFile())
                              .size(width, width)
                              .outputFormat("jpeg")
                              .toOutputStream(os);
                }
            };

            // Ask the browser to cache the image for 24 hours
            CacheControl cc = new CacheControl();
            cc.setMaxAge(86400);
            cc.setPrivate(true);

            return Response.ok(result).cacheControl(cc).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }    
      
}
