package com.hypersocket.inbox.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.mail.Address;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.events.EventService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.inbox.EmailAttachment;
import com.hypersocket.inbox.EmailProcessor;
import com.hypersocket.inbox.InboxProcessor;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.tasks.AbstractTaskProvider;
import com.hypersocket.tasks.Task;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.tasks.TaskResult;
import com.hypersocket.triggers.MultipleTaskResults;
import com.hypersocket.triggers.ValidationException;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;

@Component
public class CollectMailTask extends AbstractTaskProvider {

	static Logger log = LoggerFactory.getLogger(CollectMailTask.class);

	public static final String TASK_RESOURCE_KEY = "collectMailTask";

	public static final String RESOURCE_BUNDLE = "CollectMailTask";

	private static final String ATTR_PROTOCOL = "collectMail.protocol";
	private static final String ATTR_HOST = "collectMail.host";
	private static final String ATTR_PORT = "collectMail.port";
	private static final String ATTR_USERNAME = "collectMail.username";
	private static final String ATTR_PASSWORD = "collectMail.password";
	private static final String ATTR_SECURE = "collectMail.secure";
	private static final String ATTR_ATTACHMENTS = "collectMail.attachments";

	@Autowired
	CollectMailTaskRepository repository;

	@Autowired
	TaskProviderService taskService;

	@Autowired
	EventService eventService;

	@Autowired
	FileUploadService fileUploadService;

	@Autowired
	I18NService i18nService;

	public CollectMailTask() {
	}

	@PostConstruct
	private void postConstruct() {
		taskService.registerTaskProvider(this);

		i18nService.registerBundle(RESOURCE_BUNDLE);

		eventService.registerEvent(CollectMailTaskResult.class, RESOURCE_BUNDLE);
	}

	@Override
	public String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public String[] getResourceKeys() {
		return new String[] { TASK_RESOURCE_KEY };
	}

	@Override
	public void validate(Task task, Map<String, String> parameters) throws ValidationException {
	}

	@Override
	public TaskResult execute(final Task task, final Realm currentRealm, SystemEvent event) throws ValidationException {

		// for IMAP
		String protocol = processTokenReplacements(repository.getValue(task, ATTR_PROTOCOL), event);
		String host = processTokenReplacements(repository.getValue(task, ATTR_HOST), event);
		String port = processTokenReplacements(repository.getValue(task, ATTR_PORT), event);
		boolean secure = "true"
				.equals(processTokenReplacements(repository.getValue(task, ATTR_SECURE), event));
		final boolean doAttachments = "true".equals(processTokenReplacements(repository.getValue(task, ATTR_ATTACHMENTS), event));
		
		if (StringUtils.isBlank(port) || port.equals("0")) {
			if (protocol.equals("pop3"))
				port = secure ? "995" : "110";
			else if (protocol.equals("imap"))
				port = secure ? "993" : "143";
		}

		String userName = processTokenReplacements(repository.getValue(task, ATTR_USERNAME), event);
		String password = processTokenReplacements(repository.getValue(task, ATTR_PASSWORD), event);

		InboxProcessor receiver = new InboxProcessor();
		final List<TaskResult> results = new ArrayList<>();

		receiver.downloadEmails(protocol, host, port, userName, password, false, new EmailProcessor() {
			@Override
			public void processEmail(Address[] from, Address[] replyTo, Address[] to, Address[] cc, String subject,
					String textContent, String htmlContent, Date sent, Date received, EmailAttachment... attachments) {
				List<String> attachmentUUIDs = new ArrayList<>();
				if (doAttachments) {
					for (EmailAttachment attachment : attachments) {
						try {
							final InputStream inputStream = attachment.getInputStream();
							try {
								FileUpload upload = fileUploadService.createFile(inputStream, attachment.getFilename(),
										currentRealm, true, attachment.getContentType(),
										fileUploadService.getDefaultStore());
								attachmentUUIDs.add(upload.getName());
							} finally {
								IOUtils.closeQuietly(inputStream);
							}
						} catch (ResourceCreationException e) {
							log.error(String.format("Failed to attach %s", attachment.getFilename()), e);
						} catch (AccessDeniedException e) {
							log.error(String.format("Access denied for attaching %s", attachment.getFilename()), e);
						} catch (IOException e) {
							log.error(String.format("I/O error attaching %s", attachment.getFilename()), e);
						}
					}
				}
				results.add(new CollectMailTaskResult(this, true, currentRealm, task, from, replyTo, to, cc, subject,
						textContent, htmlContent, sent, received, attachmentUUIDs.toArray(new String[0])));
			}
		});
		// Task is performed here
		return new MultipleTaskResults(this, currentRealm, task, results);
	}

	public String[] getResultResourceKeys() {
		return new String[] { CollectMailTaskResult.EVENT_RESOURCE_KEY };
	}

	@Override
	public ResourceTemplateRepository getRepository() {
		return repository;
	}

}