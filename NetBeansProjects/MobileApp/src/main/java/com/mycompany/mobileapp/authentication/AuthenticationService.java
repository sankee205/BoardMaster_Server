package com.mycompany.mobileapp.authentication;

import com.mycompany.mobileapp.BoardGame;
import com.mycompany.mobileapp.Photo;
import com.mycompany.mobileapp.authentication.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.extern.java.Log;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;


import org.eclipse.microprofile.jwt.JsonWebToken;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Authentication REST service used for login, logout and to register new users
 *
 * @Path("auth) makes this class into a JAX-RS REST service. "auth" specifies 
 * that the URL of this service would begin with "domainname/chat/api/auth"
 * depending on the domain, context path of project and the JAX-RS base configuration
 * @Produces(MediaType.APPLICATION_JSON) instructs JAX-RS that the default result 
 * of a method is to be marshalled as JSON
 * 
 * @Stateless makes this class into a transactional stateless EJB, which is a 
 * requirement of using the JPA EntityManager to communicate with the database.
 * 
 * @DeclareRoles({UserGroup.ADMIN,UserGroup.USER}) specifies the roles used in
 * this EJB.
 * 
 * @author mikael
 */
@Path("auth")
@Stateless
@Log
public class AuthenticationService {

    private static final String INSERT_USERGROUP = "INSERT INTO USERGROUP(NAME,UID) VALUES (?,?)";
    private static final String DELETE_USERGROUP = "DELETE FROM USERGROUP WHERE NAME = ? AND UID = ?";

    @Inject
    KeyService keyService;

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "issuer")
    String issuer;

    /** 
     * The application server will inject a DataSource as a way to communicate 
     * with the database.
     */
    @Resource(lookup = "jdbc/postgrespool")
    DataSource dataSource;
    
    /** 
     * The application server will inject a EntityManager as a way to communicate 
     * with the database via JPA.
     */
    @PersistenceContext
    EntityManager em;

    @Inject
    PasswordHash hasher;

    @Inject
    JsonWebToken principal;
    
    @Inject
    @ConfigProperty(name = "photo.storage.path", defaultValue = "mobileapp_images")
    String photoPath;

    /**
     *
     * @param uid
     * @param pwd
     * @param request
     * @return
     */
    @GET
    @PermitAll
    @Path("login")
    public Response login(
            @QueryParam("username") @NotBlank String uid,
            @QueryParam("password") @NotBlank String pwd,
            @Context HttpServletRequest request) {
        CredentialValidationResult result = identityStoreHandler.validate(
                new UsernamePasswordCredential(uid, pwd));

        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            String token = issueToken(result.getCallerPrincipal().getName(),
                    result.getCallerGroups(), request);
            return Response
                    .ok(token)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    /**
     *
     * @param name
     * @param groups
     * @param request
     * @return
     */
    private String issueToken(String name, Set<String> groups, HttpServletRequest request) {
        try {
            Date now = new Date();
            Date expiration = Date.from(LocalDateTime.now().plusDays(1L).atZone(ZoneId.systemDefault()).toInstant());
            JwtBuilder jb = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setHeaderParam("kid", "abc-1234567890")
                    .setSubject(name)
                    .setId("a-123")
                    //.setIssuer(issuer)
                    .claim("iss", issuer)
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .claim("upn", name)
                    .claim("groups", groups)
                    .claim("aud", "aud")
                    .claim("auth_time", now)
                    .signWith(keyService.getPrivate());
            return jb.compact();
        } catch (InvalidKeyException t) {
            log.log(Level.SEVERE, "Failed to create token", t);
            throw new RuntimeException("Failed to create token", t);
        }
    }

    /**
     * Does an insert into the USER and AUSERGROUP tables. It creates a SHA-256
     * hash of the password and Base64 encodes it before the user is created in
     * the database. The authentication system will read the ASER table when
     * doing an authentication.
     *
     * @param uid
     * @param pwd
     * @return
     */


    @POST
    @PermitAll
    @Path("create")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public User createUser(
           @FormDataParam("firstname")String firstname,
            @FormDataParam("lastname")String lastname,
            @FormDataParam("username")String username,
            @FormDataParam("password")String password,
            @FormDataParam("email")String email,
            FormDataMultiPart photos){
        User user = em.find(User.class, username);
        if (user != null) {
            log.log(Level.INFO, "user already exists {0}", username);
            throw new IllegalArgumentException("User " + username + " already exists");
        } else {
            user = new User();
            user.setUsername(username);
            user.setPassword(hasher.generate(password.toCharArray()));
            user.setEmail(email);
            user.setFirstname(firstname);
            user.setLastname(lastname);
            Group usergroup = em.find(Group.class, Group.USER);
            user.getGroups().add(usergroup);
            
            List<Photo> p = new ArrayList<>();


            try{

                List<FormDataBodyPart> images = photos.getFields("image");

                if(images != null) {
                    System.out.println("incoming image");
                    

                    for (FormDataBodyPart part : images) {
                    
                        InputStream is = part.getEntityAs(InputStream.class);
                        ContentDisposition meta = part.getContentDisposition();

                        String pid = UUID.randomUUID().toString();
                        String path = getPhotoPath();
                        Files.copy(is, Paths.get(path,pid));
                        
                        Photo photo = new Photo();
                        photo.setId(pid);
                        photo.setFilesize(meta.getSize());
                        photo.setMimeType(meta.getType());
                        photo.setName(meta.getFileName());

                        p.add(photo);

                        em.persist(photo);
                    }

                }

            } 
            catch (Exception e){
                e.printStackTrace();
            }
            
            user.setProfileImages(p);
            em.merge(user);
    
            user.setPassword("");
            return user;
        }        
    }
    
    @PUT
    @Path("editprofile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({Group.USER})
    public Response editProfile(
            @FormDataParam("firstname")String firstname,
            @FormDataParam("lastname")String lastname,
            @FormDataParam("username")String username,
            @FormDataParam("email")String email,
            FormDataMultiPart photos
    ){
        User user = em.find(User.class, username);
        if (user == null) {
            throw new IllegalArgumentException("User " + username + " doesnt exist");
        } else {
             
        
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setUsername(username);
        user.setEmail(email);
        

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
                    
                    user.addPhoto(photo);

                    em.persist(photo);
                }

            }

        } catch (Exception e){
            e.printStackTrace();
        }
        em.persist(user);

        return Response.ok().build();
        }
      
    }

    
    /**
     *
     * @return
     */
    @GET
    @Path("currentuser")    
    @RolesAllowed(value = {Group.USER})
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrentUser() {
        User currentuser = em.find(User.class, principal.getName());

        User user = new User();
        
        user.setEmail(currentuser.getEmail());
        user.setFirstname(currentuser.getFirstname());
        user.setLastname(currentuser.getLastname());
        user.setUsername(currentuser.getUsername());
        user.setGroups(currentuser.getGroups());
        user.setProfileImages(currentuser.getProfileImages());
        user.setPassword("secret");
        return user;
    }

    
    
    /**
     *
     * @param uid
     * @param role
     * @return
     */
    @PUT
    @Path("addrole")
    @RolesAllowed(value = {Group.ADMIN})
    public Response addRole(@QueryParam("uid") String uid, @QueryParam("role") String role) {
        if (!roleExists(role)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (Connection c = dataSource.getConnection();
            PreparedStatement psg = c.prepareStatement(INSERT_USERGROUP)) {
            psg.setString(1, role);
            psg.setString(2, uid);
            psg.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    /**
     *
     * @param role
     * @return
     */
    private boolean roleExists(String role) {
        boolean result = false;

        if (role != null) {
            switch (role) {
                case Group.ADMIN:
                case Group.USER:
                    result = true;
                    break;
            }
        }

        return result;
    }

    /**
     *
     * @param uid
     * @param role
     * @return
     */
    @PUT
    @Path("removerole")
    @RolesAllowed(value = {Group.ADMIN})
    public Response removeRole(@QueryParam("uid") String uid, @QueryParam("role") String role) {
        if (!roleExists(role)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (Connection c = dataSource.getConnection();
                PreparedStatement psg = c.prepareStatement(DELETE_USERGROUP)) {
            psg.setString(1, role);
            psg.setString(2, uid);
            psg.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    /**
     *
     * @param uid
     * @param password
     * @param sc
     * @return
     */
    @PUT
    @Path("changepassword")
    @RolesAllowed(value = {Group.USER})
    public Response changePassword(@QueryParam("uid") String uid,
            @QueryParam("pwd") String password,
            @Context SecurityContext sc) {
        String authuser = sc.getUserPrincipal() != null ? sc.getUserPrincipal().getName() : null;
        if (authuser == null || uid == null || (password == null || password.length() < 3)) {
            log.log(Level.SEVERE, "Failed to change password on user {0}", uid);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (authuser.compareToIgnoreCase(uid) != 0 && !sc.isUserInRole(Group.ADMIN)) {
            log.log(Level.SEVERE,
                    "No admin access for {0}. Failed to change password on user {1}",
                    new Object[]{authuser, uid});
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            User user = em.find(User.class, uid);
            user.setPassword(hasher.generate(password.toCharArray()));
            em.merge(user);
            return Response.ok().build();
        }
    }
    
    
    private String getPhotoPath() {
        String path = "mobileapp_images";
        //Creating a File object
        File file = new File(path);
        //Creating the directory
        if(!file.exists()){
            boolean bool = file.mkdir();
            if(bool){
              System.out.println("Directory created successfully");
            }
            else{
              System.out.println("Sorry couldn’t create specified directory");
            }
        }
        return photoPath;
    }
}
