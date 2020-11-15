/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp;

import com.mycompany.mobileapp.resources.ConversationService;
import javax.ejb.EJB;
import javax.json.bind.adapter.JsonbAdapter;

/**
 *
 * @author sanderkeedklang
 */
public class ConversationAdapter implements JsonbAdapter<Conversation, Long>{
    @EJB
    ConversationService service;
    
    @Override
    public Long adaptToJson(Conversation conv) throws Exception {        
        return conv != null ? conv.getId() : null;
    }

    @Override
    public Conversation adaptFromJson(Long id) throws Exception {
        return service.findById(id);
    }
    
}
