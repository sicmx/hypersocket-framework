package com.hypersocket.triggers.actions.ip;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.hypersocket.properties.ResourceTemplateRepositoryImpl;

@Repository
public class BlockIPTaskRepositoryImpl extends
		ResourceTemplateRepositoryImpl implements BlockIPTaskRepository {

	@PostConstruct
	private void postConstruct() {
		loadPropertyTemplates("tasks/blockIP.xml");
	}
}

