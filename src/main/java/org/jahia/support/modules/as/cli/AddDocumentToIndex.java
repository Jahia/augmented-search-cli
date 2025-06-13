package org.jahia.support.modules.as.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.impl.jackrabbit.JackrabbitStoreProvider;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(scope = "as", name = "add", description = "Add document to index")
@Service
public class AddDocumentToIndex extends AbstractDocumentIndexOperation {

    @Override
    protected int getEventType() {
        return Event.NODE_ADDED;
    }

    @Override
    protected void handleJCRNode(JackrabbitStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        try {
            JCRNodeWrapper node = sessionFactory.getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null).getNode(path);
            events.add(createApiEvent(node.getPath(), node.getIdentifier(), null));
            sendEvents(events, provider);
        } catch (PathNotFoundException e) {
            log.warn("Node not found at path: {}", path);
        }
    }

    @Override
    protected void handleExternalDocument(ExternalContentStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        String providerPath = StringUtils.substringAfter(path, provider.getMountPoint());
        try {
            ExternalData externalData = provider.getDataSource().getItemByPath(providerPath);

            Map<String, Object> info = new HashMap<>();
            info.put("externalData", externalData);

            events.add(createApiEvent(externalData.getPath(), externalData.getId(), info));
            sendEvents(events, provider);
        } catch (PathNotFoundException e) {
            log.warn("External data not found at path: {}", providerPath);
        }
    }
}
