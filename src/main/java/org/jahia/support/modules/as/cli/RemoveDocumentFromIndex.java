package org.jahia.support.modules.as.cli;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(scope = "as", name = "remove", description = "Remove document from index")
@Service
public class RemoveDocumentFromIndex extends AbstractDocumentIndexOperation {

    @Override
    protected int getEventType() {
        return Event.NODE_REMOVED;
    }

    @Override
    protected void handleJCRNode(JackrabbitStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        try {
            JCRNodeWrapper node = sessionFactory.getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null).getNode(path);
            events.add(createApiEvent(node.getPath(), node.getIdentifier(), null));
        } catch (PathNotFoundException e) {
            log.warn("JCR node not found at {}, sending synthetic removal event", path);
            events.add(createApiEvent(path, "fake-identifier", null));
        }

        sendEvents(events, provider);
    }

    @Override
    protected void handleExternalDocument(ExternalContentStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        String providerPath = toProviderPath(path, provider.getMountPoint());
        ExternalData externalData;
        try {
            externalData = provider.getDataSource().getItemByPath(providerPath);
        } catch (PathNotFoundException e) {
            log.warn("External data not found at {}, sending synthetic removal event", providerPath);
            externalData = new ExternalData("fake-identifier", providerPath, "nt:base", Collections.emptyMap(), false);
        }

        Map<String, Object> info = new HashMap<>();
        info.put("externalData", externalData);

        events.add(createApiEvent(externalData.getPath(), externalData.getId(), info));

        sendEvents(events, provider);
    }
}
