/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.configuration;


import javax.annotation.security.DeclareRoles;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.PasswordHash;
import com.mycompany.mobileapp.authentication.Group;
import org.eclipse.microprofile.auth.LoginConfig;

/**
 *
 * @author trygve
 */
@DatabaseIdentityStoreDefinition(
        dataSourceLookup = "jdbc/mobileappPool",
        callerQuery="select password from users where uid = ?",
        groupsQuery="select name from ausergroup where uid  = ?",
        hashAlgorithm = PasswordHash.class,
        priority = 80)
@DeclareRoles({Group.ADMIN,Group.USER})
@LoginConfig(authMethod = "MP-JWT",realmName = "template")
public class SecurityConfig {
    
}