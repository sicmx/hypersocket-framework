/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.session;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.hypersocket.auth.AuthenticationScheme;
import com.hypersocket.realm.Principal;
import com.hypersocket.repository.AbstractEntityRepository;

public interface SessionRepository extends AbstractEntityRepository<Session,String> {

	public Session createSession(String remoteAddress, 
			Principal principal, 
			AuthenticationScheme scheme, 
			String userAgent, 
			String userAgentVersion, 
			String os, 
			String osVersion,
			int timeout);
	
	public Session getSessionById(String id);
	
	public void updateSession(Session session);

	public List<Session> getActiveSessions();

	List<Session> getSystemSessions();

	Long getActiveSessionCount();

	Map<String, Long> getBrowserCount();

	Map<String, Long> getBrowserCount(Date startDate, Date endDate);

	Long getSessionCount(Date startDate, Date endDate, boolean distinctUsers);

	Long getActiveSessionCount(boolean distinctUsers);

	Map<String, Long> getOSCount(Date startDate, Date endDate);

	Map<String, Long> getIPCount(Date startDate, Date endDate);
}
