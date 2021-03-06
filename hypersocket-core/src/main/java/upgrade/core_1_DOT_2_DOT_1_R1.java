/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package upgrade;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.config.ConfigurationService;
import com.hypersocket.local.LocalRealmProviderImpl;
import com.hypersocket.local.LocalUserRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRepository;
import com.hypersocket.session.SessionService;

public class core_1_DOT_2_DOT_1_R1 implements Runnable {

	static Logger log = LoggerFactory.getLogger(core_1_DOT_2_DOT_1_R1.class);
	
	@Autowired
	LocalUserRepository repository;
	
	@Autowired
	RealmRepository realmRepository;
	
	@Autowired
	ConfigurationService configurationService; 
	
	@Autowired
	SessionService sessionService; 
	
	@Override
	public void run() {

		configurationService.setCurrentSession(sessionService.getSystemSession(), Locale.getDefault());
		
		try {
			for(Realm realm : realmRepository.allRealms(LocalRealmProviderImpl.REALM_RESOURCE_CATEGORY)) {
				String[] editable = configurationService.getValues(realm, "realm.userEditableProperties");
				for(int i=0;i<editable.length;i++) {
					editable[i] = editable[i].replace("user.", "");
				}
				configurationService.setValues(realm, "realm.userEditableProperties", editable);
				String[] visible = configurationService.getValues(realm, "realm.userVisibleProperties");
				for(int i=0;i<visible.length;i++) {
					visible[i] = visible[i].replace("user.", "");
				}
				configurationService.setValues(realm, "realm.userVisibleProperties", visible);
			}
		} catch(Throwable t) {
			log.error("Failed to process user update", t);
		} finally {
			configurationService.clearPrincipalContext();
		}
	}


	

}
