/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.resources;

import com.mycompany.mobileapp.BoardGame;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
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

    @GET
    @Path("games")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public List<Game> getGames() {
        return entityManager.createNativeQuery("SELECT * FROM Game", Game.class).getResultList();
    }
    
    @GET
    @Path("usersgames")
    @RolesAllowed({Group.USER, Group.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Game> getCurrentUserGames() {
        User user = this.getCurrentUser();
        //String query = "select * from game inner join game_users gu on game.id = gu.game_id where gu.players_uid = "+ user.getUsername();
        String query = "select * from game inner join game_users gu on game.id = gu.game_id where players_uid ='"+ user.getUsername()+"'";

        return entityManager.createNativeQuery(query, Game.class).getResultList();
    }
    
    @GET
    @Path("boardgames")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public List<BoardGame> getboards() {
        return entityManager.createNativeQuery("SELECT * FROM BoardGame", BoardGame.class).getResultList();
    }
    

    
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
                         @FormDataParam("time")String time
){
            User user = getCurrentUser();
            System.out.println(user.getUsername());
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
            
            return entityManager.merge(game);
    }

    
    @POST
    @Path("add-boardgame")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({Group.ADMIN})
    public Response addBoardGame(
            @FormDataParam("name")String name,
            @FormDataParam("players")int players,
            FormDataMultiPart photos){
        BoardGame boardgame = entityManager.find(BoardGame.class, name);
        if (boardgame != null) {
            throw new IllegalArgumentException("Boardgame " + name + " already exists");
        } else {
             
        User user = this.getCurrentUser();
        BoardGame game = new BoardGame();

        game.setName(name);
        game.setPlayers(players);
        game.setGameOwner(user.getUsername());
        
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
        entityManager.persist(game);

        return Response.ok().build();
        }
      
    }
    
    @GET
    @Path("image/{name}")
    @Produces("image/jpeg")
    public Response getPhoto(@PathParam("name") String name, @QueryParam("width") int width) {
        if(entityManager.find(Photo.class, name) != null) {
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
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }


    private User getCurrentUser(){
        return entityManager.find(User.class, securityContext.getUserPrincipal().getName());
    }
    
    @PUT
    @Path("joingame")
    @RolesAllowed({Group.USER})
    public Response joinGame(@QueryParam("gameid") String gameid) {

        User currentuser = entityManager.find(User.class, principal.getName());

        if (currentuser != null){
            Game currentGame = entityManager.find(Game.class, gameid);
            if(!currentGame.getPlayers().contains(currentuser)){
                currentGame.addPlayer(currentuser);
                return Response.ok().build();
            }

        }
        return Response.notModified().build();

    }

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