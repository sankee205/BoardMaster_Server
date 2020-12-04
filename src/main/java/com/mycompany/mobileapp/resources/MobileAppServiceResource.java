/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.resources;

import com.mycompany.mobileapp.BoardGame;
import com.mycompany.mobileapp.Conversation;
import com.mycompany.mobileapp.Game;
import com.mycompany.mobileapp.Photo;
import com.mycompany.mobileapp.authentication.AuthenticationService;
import com.mycompany.mobileapp.authentication.Group;
import com.mycompany.mobileapp.authentication.User;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import net.coobird.thumbnailator.Thumbnails;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;


/**
 *
 * @author sanderkeedklang
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("boardmaster")
@Stateless
public class MobileAppServiceResource {
       
    @Inject
    AuthenticationService authenticationService;

    @Context
    SecurityContext securityContext;

    @Inject
    JsonWebToken principal;

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "photo.storage.path", defaultValue = "mobileapp_images")
    String photoPath;

    /**
     * returns all games
     * @return 
     */
    @GET
    @Path("games")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public List<Game> getGames() {
        return entityManager.createNativeQuery("SELECT * FROM Game order by created", Game.class).getResultList();
    }
    
    /**
     * returns all the games the user is attached to
     * @return 
     */
    @GET
    @Path("usersgames")
    @RolesAllowed({Group.USER, Group.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Game> getCurrentUserGames() {
        User user = this.getCurrentUser();
        String query = "select * from game inner join game_users gu on game.id = gu.game_id where players_uid ='"+ user.getUsername()+"'";

        return entityManager.createNativeQuery(query, Game.class).getResultList();
    }
    
    /**
     * returns the list of boardgames
     * @return 
     */
    @GET
    @Path("boardgames")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public List<BoardGame> getboards() {
        return entityManager.createNativeQuery("SELECT * FROM BoardGame", BoardGame.class).getResultList();
    }
    

    /**
     * creates a game from the paramatic inputs 
     * @param title
     * @param gameTitle
     * @param desc
     * @param players
     * @param date
     * @param time
     * @param photoId
     * @param photos
     * @return 
     */
    @POST
    @Path("add-game")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Group.USER})
    public Game addGame( @FormDataParam("title")String title,
                         @FormDataParam("game")String gameTitle,
                         @FormDataParam("desc")String desc,
                         @FormDataParam("players")String players,
                         @FormDataParam("date")String date,
                         @FormDataParam("time")String time,
                         @FormDataParam("photoId")String photoId,
                        
                         FormDataMultiPart photos){
            User user = getCurrentUser();
            int numberOfPlayers = Integer.parseInt(players);
            
            Game game = new Game();
            game.addPlayer(user);
            
            
            if(title.isEmpty()){
                game.setTitle(gameTitle);
            }
            else{
                game.setTitle(title);

            }
              
            
            game.setDescription(desc);
            game.setMaxPlayers(numberOfPlayers);
            game.setGameName(gameTitle);
            game.setDate(date);
            game.setTime(time);
            game.setGameOwner(user);
            
            if(photoId != null){
                Photo photo =entityManager.find(Photo.class, photoId);
                game.addPhoto(photo);
            }
            else{
                ArrayList<Photo> p = new ArrayList<>();
                try{
                    List<FormDataBodyPart> images = photos.getFields("image");
                    if(images != null) {
                        for (FormDataBodyPart part : images) {
                            InputStream is = part.getEntityAs(InputStream.class);
                            ContentDisposition meta = part.getContentDisposition();
                            String pid = UUID.randomUUID().toString();
                            Files.copy(is, Paths.get(getPhotoPath(), pid));
                            Photo photo = new Photo();
                            photo.setId(pid);
                            photo.setFilesize(meta.getSize());
                            photo.setMimeType(meta.getType());
                            photo.setName(meta.getFileName());
                            p.add(photo);
                            entityManager.persist(photo);
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                game.setProfileImages(p);
            }
            return entityManager.merge(game);
    }

    /**
     * adds a boardgame to the boardgame list if it does not already exist
     * @param name
     * @param players
     * @param photos
     * @return 
     */
    @POST
    @Path("add-boardgame")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({Group.ADMIN})
    public BoardGame addBoardGame(
            @FormDataParam("name")String name,
            @FormDataParam("players")int players,
            FormDataMultiPart photos){
        String query = "select b from BoardGame b where b.name = :boardname";
        List<BoardGame> boardgames = entityManager.createQuery(query, BoardGame.class).setParameter("boardname", name).getResultList();
        if (!boardgames.isEmpty()) {
            throw new IllegalArgumentException("Boardgame " + name + " already exists");
        } else {
             
        User user = this.getCurrentUser();
        BoardGame game = new BoardGame();

        game.setName(name);
        game.setPlayers(players);
        game.setGameOwner(user);
        
        ArrayList<Photo> p = new ArrayList<>();


        try{

            List<FormDataBodyPart> images = photos.getFields("image");

            if(images != null) {


                for (FormDataBodyPart part : images) {
                    InputStream is = part.getEntityAs(InputStream.class);
                    ContentDisposition meta = part.getContentDisposition();

                    String pid = UUID.randomUUID().toString();
                    Files.copy(is, Paths.get(getPhotoPath(), pid));

                    Photo photo = new Photo();
                    photo.setId(pid);
                    photo.setFilesize(meta.getSize());
                    photo.setMimeType(meta.getType());
                    photo.setName(meta.getFileName());

                    p.add(photo);

                    entityManager.persist(photo);
                }

            }

        } catch (Exception e){
            e.printStackTrace();
        }
        game.setBoardImages(p);

        return entityManager.merge(game);
        }
      
    }
    
    /**
     * gets the photo of the boardgame
     * @param name
     * @return 
     */
    @GET
    @Path("image/{name}")
    public String getPhoto(@PathParam("name") String name) {
        String query = "select b from BoardGame b where b.name = '"+ name + "'";
        BoardGame boardGame = entityManager.createQuery(query,BoardGame.class).getSingleResult(); 
        return boardGame.getBoardImages().get(0).getId()  ;
    }


    private User getCurrentUser(){
        return entityManager.find(User.class, securityContext.getUserPrincipal().getName());
    }
    
    /**
     * adds a user to a game
     * @param gameid
     * @return 
     */
    @PUT
    @Path("joingame")
    @RolesAllowed({Group.USER})
    public Response joinGame(@QueryParam("gameid") Long gameid) {

        User currentuser = entityManager.find(User.class, principal.getName());

        if (currentuser != null){
            Game currentGame = entityManager.find(Game.class, gameid);
            Conversation gameConversation = entityManager.createQuery("select c from Conversation c where c.game.id = :game", Conversation.class)
                    .setParameter("game", gameid)
                    .getSingleResult();
            if(!currentGame.getPlayers().contains(currentuser)){
                currentGame.addPlayer(currentuser);
                gameConversation.addPlayer(currentuser);
                return Response.ok().build();
            }

        }
        return Response.notModified().build();

    }

    /**
     * removes a user from a game
     * @param gameid
     * @return 
     */
    @DELETE
    @Path("exitgame")
    @RolesAllowed({Group.USER})
    public Response exitGame(@QueryParam("gameid") String gameid) {
       
        User currentuser = entityManager.find(User.class, principal.getName());

        if (currentuser != null){
            Game currentGame = entityManager.find(Game.class, gameid);
            if(!currentGame.getPlayers().contains(currentuser)){
                currentGame.removePlayer(currentuser);
                return Response.ok().build();
            }

        }
        return Response.notModified().build();
    }
     
    /**
     * 
     * @return 
     */
    private String getPhotoPath() {
        if(photoPath== null){
            File f = new File("F:\\Storage/Photos/mobileapp_images"); 
  
        // check if the directory can be created 
        // using the abstract path name 
        if (f.mkdir()) { 
  
            // display that the directory is created 
            // as the function returned true 
            System.out.println("Directory is created"); 
        } 
        else { 
            // display that the directory cannot be created 
            // as the function returned false 
            System.out.println("Directory cannot be created"); 
        } 
        }
        return photoPath;
    }
}