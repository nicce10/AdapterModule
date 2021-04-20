package com.bmt.po.adapter.dev;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.lib.mp.module.ModuleHome;
import com.sap.aii.af.lib.mp.module.ModuleLocal;
import com.sap.aii.af.lib.mp.module.ModuleLocalHome;
import com.sap.aii.af.lib.mp.module.ModuleRemote;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;

/**
 * SavePayloadBean saves payload of input or output message
 * Specifically useful for synchronous messages when sync logging deactivated
 * Stored in https://github.com/nicce10/AdapterModule 
 */
@Stateless(name="SavePayloadBean")
@Local(value={ModuleLocal.class})
@Remote(value={ModuleRemote.class})
@LocalHome(value=ModuleLocalHome.class)
@RemoteHome(value=ModuleHome.class)
public class SavePayloadBean implements Module {   

	@SuppressWarnings("null")
	@Override
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
		// TODO Auto-generated method stub
		AuditAccess audit = null;
		Object obj = null;
		Message msg = null;
		MessageKey key = null;

		try {
			obj = inputModuleData.getPrincipalData();
           msg = (Message) obj;
           key = new MessageKey(msg.getMessageId(), msg.getMessageDirection());
 			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
 			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS, "SavePayloadBean Module called!!");
//			Set<MessagePropertyKey> mpks = msg.getMessagePropertyKeys();
//			Iterator<MessagePropertyKey> mp1 = mpks.iterator();
//			while (mp1.hasNext())
//			audit.addAuditLogEntry(key, AuditLogStatus.SUCCESS, mp1.next().getPropertyName());   
 			InputStream is = msg.getDocument().getInputStream();
 			
 			String slash = "/";
 			String fs = null;
 			String ts = null;
 			// Sender and receivre systems could also be useful, not used now though
  			fs = msg.getFromService().toString();
 			ts = msg.getToService().toString();

           String fn = null;
   			String fd = null;
   			// get file location as input parameter
      		fd = (String) moduleContext.getContextData("FileDestination");     	
      		// if synchronous message, save the reference to request in response filename
      		String refId = "";
      		refId = msg.getRefToMessageId();
			fd = fd.concat(slash);
			// Inbound is receiver channel
			if (msg.getMessageDirection()== MessageDirection.INBOUND) {
				if (refId != null) {
				fn = fd.concat("Response."+msg.getMessageId()).concat("_Ref.").concat(refId).concat("-out.xml");
				} else {fn = fd.concat("Request."+msg.getMessageId()).concat("-out.xml");
						}
				}
			// Outbound is sender channel
			else {	
				if (refId != null) {
					fn = fd.concat("Response."+msg.getMessageId()).concat("_Ref.").concat(refId).concat("-in.xml");
					} else {fn = fd.concat("Request."+msg.getMessageId()).concat("-in.xml");
							}	          				
			}			
			File outFile = new File(fn);
			// write to file
			java.nio.file.Files.copy(
				      is, 
				      outFile.toPath(), 
				      StandardCopyOption.REPLACE_EXISTING);
           
		}
		catch (Exception e)
		{
			audit.addAuditLogEntry(key, AuditLogStatus.ERROR,"Module Exception Caught.");
			ModuleException me = new ModuleException(e.getMessage() + " and TRACE !! - >" + e.getStackTrace());
			throw me;
		}

		return inputModuleData;		
		
	}

}
