package com.hypersocket.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypersocket.HypersocketVersion;
import com.hypersocket.auth.AbstractAuthenticatedServiceImpl;
import com.hypersocket.http.HttpUtils;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.scheduler.SchedulerService;

@Service
public class OverviewWidgetServiceImpl extends AbstractAuthenticatedServiceImpl
		implements OverviewWidgetService {

	public static final String RESOURCE_BUNDLE = "OverviewWidgetService";
	private List<OverviewWidget> widgetList = new ArrayList<OverviewWidget>();

	private Collection<Link> cachedLinks = null;
	private long lastLinkUpdate = 0;
	
	@Autowired
	I18NService i18nService;

	@Autowired
	SchedulerService schedulerService;

	@Autowired
	PermissionService permissionService;

	@PostConstruct
	private void postConstruct() {
		i18nService.registerBundle(RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.overview");

		for (OverviewPermission p : OverviewPermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		this.registerWidget(new OverviewWidget(2, "overview.yourSerial.title",
				"yourSerial", false));
		this.registerWidget(new OverviewWidget(4, "overview.usefulLinks.title",
				"usefulLinks", false));
		this.registerWidget(new OverviewWidget(1,
				"overview.systemMessages.title", "systemMessages", true));
		this.registerWidget(new OverviewWidget(3, "overview.featureReel.title",
				"featureReel", true));
		this.registerWidget(new OverviewWidget(3, "overview.quickSetup.title",
				"quickSetup", false));
	}

	public void registerWidget(OverviewWidget widget) {
		widgetList.add(widget);
		Collections.sort(widgetList);
	}

	public List<OverviewWidget> getWidgets() throws AccessDeniedException {
		assertPermission(OverviewPermission.READ);
		return widgetList;
	}

	@Override
	public Collection<Link> getLinks() throws ResourceException {

		if(System.currentTimeMillis() - lastLinkUpdate > (24 * 60 * 60 * 1000)) {
			
			ObjectMapper mapper = new ObjectMapper();
	
			try {
				
				List<Link> results = new ArrayList<Link>();
				
				// Get global links
				results.addAll(Arrays.asList(mapper.readValue(HttpUtils.doHttpGet(
						"http://updates.hypersocket.com/messages/links.json",
						true), Link[].class)));
				
				// Get product links
				results.addAll(Arrays.asList(mapper.readValue(HttpUtils.doHttpGet(
						"http://updates.hypersocket.com/messages/" + HypersocketVersion.getProductId() + "/links.json",
						true), Link[].class)));
				
				cachedLinks = results;
				lastLinkUpdate = System.currentTimeMillis();
			} catch (Throwable e) {
				throw new ResourceException(RESOURCE_BUNDLE,
						"error.readingArticleList", e.getMessage());
			}
		}
		
		return cachedLinks;
	}
}
