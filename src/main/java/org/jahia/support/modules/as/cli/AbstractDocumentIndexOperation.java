package org.jahia.support.modules.as.cli;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.events.EventService;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.impl.jackrabbit.JackrabbitStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractDocumentIndexOperation implements Action {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDocumentIndexOperation.class);

    private static final int TEST_EVENT_COUNT = 1000;
    private static final int EVENT_BATCH_SIZE = 100;

    @Reference
    protected EventService eventService;

    @Reference
    protected JCRSessionFactory sessionFactory;

    // Package-private setters: test seams to inject mocks without OSGi.
    void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    void setSessionFactory(JCRSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Argument(name = "path", description = "The document path", required = true)
    protected String path;

    @Option(name = "--test", description = "Test mode, generate 1000 events to run in //")
    protected boolean testMode;

    @Option(name = "--async", description = "Run in async mode")
    protected boolean async = false;

    @Override
    public Object execute() throws Exception {
        List<ApiEvent> events = new ArrayList<>(1);
        JCRStoreProvider provider = sessionFactory.getProvider(path);
        if (provider instanceof ExternalContentStoreProvider) {
            handleExternalDocument((ExternalContentStoreProvider) provider, events);
        } else if (provider instanceof JackrabbitStoreProvider) {
            handleJCRNode((JackrabbitStoreProvider) provider, events);
        } else {
            log.warn("No indexable provider found for path: {}", path);
        }
        return null;
    }

    protected abstract void handleJCRNode(JackrabbitStoreProvider provider, List<ApiEvent> events) throws RepositoryException;

    protected abstract void handleExternalDocument(ExternalContentStoreProvider provider, List<ApiEvent> events) throws RepositoryException;

    protected abstract int getEventType();

    /**
     * Strips the provider mount point from the front of the path. The provider was already resolved
     * from this path, so a prefix check is safe; falls back to substringAfter when the path does not
     * start with the mount point.
     */
    protected static String toProviderPath(String path, String mountPoint) {
        if (path != null && mountPoint != null && path.startsWith(mountPoint)) {
            return path.substring(mountPoint.length());
        }
        return StringUtils.substringAfter(path, mountPoint);
    }

    protected ApiEvent createApiEvent(String nodePath, String identifier, Map<String, Object> info) {
        return new ApiEvent() {
            @Override
            public int getType() {
                return getEventType();
            }

            @Override
            public String getPath() throws RepositoryException {
                return nodePath;
            }

            @Override
            public String getUserID() {
                return "";
            }

            @Override
            public String getIdentifier() throws RepositoryException {
                return identifier;
            }

            @Override
            public Map<String, Object> getInfo() throws RepositoryException {
                return info != null ? info : Collections.emptyMap();
            }

            @Override
            public String getUserData() throws RepositoryException {
                return "";
            }

            @Override
            public long getDate() throws RepositoryException {
                return System.currentTimeMillis();
            }
        };
    }

    protected void sendEvents(List<ApiEvent> events, JCRStoreProvider provider) throws RepositoryException {
        // In test mode, build the batch into a separate list so the caller's list is never mutated.
        final List<ApiEvent> eventsToSend;
        if (testMode && !events.isEmpty()) {
            ApiEvent testEvent = events.get(0);
            List<ApiEvent> testBatch = new ArrayList<>(events);
            for (int i = 0; i < TEST_EVENT_COUNT; i++) {
                testBatch.add(testEvent);
            }
            eventsToSend = testBatch;
        } else {
            eventsToSend = events;
        }

        if (async) {
            CompletableFuture.supplyAsync(() -> {
                ListUtils.partition(eventsToSend, EVENT_BATCH_SIZE).forEach(batch -> {
                    try {
                        eventService.sendEvents(batch, provider);
                    } catch (RepositoryException e) {
                        log.error(e.getMessage(), e);
                    }
                });
                return null;
            }).whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("Async event sending failed", ex);
                } else {
                    log.info("Async event sending completed");
                }
            });
        } else {
            eventService.sendEvents(eventsToSend, provider);
        }
    }
}
