package org.jahia.support.modules.as.cli;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.events.EventService;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.impl.jackrabbit.JackrabbitStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Command(scope = "as", name = "remove", description = "Remove document from index")
@Service
public class RemoveDocumentFromIndex implements Action {

    private static final Logger log = LoggerFactory.getLogger(RemoveDocumentFromIndex.class);
    @Reference
    EventService eventService;

    @Reference
    JCRSessionFactory sessionFactory;

    @Argument(name = "path", description = "The path to remove", required = true, valueToShowInHelp = "/sites/digitall/files/mounted-pdfs/bookofbuildingfires.pdf", index = 0)
    private String path;

    @Option(name = "--test", description = "Test mode, generate 1000 events to run in //", required = false)
    private boolean testMode;

    @Option(name = "--async" , description = "Run in async mode", required = false)
    private boolean async = false;

    @Override
    public Object execute() throws Exception {
        List<ApiEvent> events = new ArrayList<>(1);
        JCRStoreProvider provider = sessionFactory.getProvider(path);
        if (provider instanceof ExternalContentStoreProvider) {
            removeExternalDocument((ExternalContentStoreProvider) provider, events);
        } else if (provider instanceof JackrabbitStoreProvider) {
            removeJCRNode((JackrabbitStoreProvider) provider, events);
        }
        return null;
    }

    private void removeJCRNode(JackrabbitStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        JackrabbitStoreProvider jackrabbitStoreProvider = provider;
        try {
            JCRNodeWrapper node = sessionFactory.getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null).getNode(path);
            ApiEvent apiEvent = new ApiEvent() {
                @Override
                public int getType() {
                    return Event.NODE_REMOVED;
                }

                @Override
                public String getPath() throws RepositoryException {
                    return node.getPath();
                }

                @Override
                public String getUserID() {
                    return "";
                }

                @Override
                public String getIdentifier() throws RepositoryException {
                    return node.getIdentifier();
                }

                @Override
                public Map getInfo() throws RepositoryException {
                    return Collections.emptyMap();
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
            events.add(apiEvent);
        } catch (PathNotFoundException e) {
            ApiEvent apiEvent = new ApiEvent() {
                @Override
                public int getType() {
                    return Event.NODE_REMOVED;
                }

                @Override
                public String getPath() throws RepositoryException {
                    return path;
                }

                @Override
                public String getUserID() {
                    return "";
                }

                @Override
                public String getIdentifier() throws RepositoryException {
                    return "fake-identifier";
                }

                @Override
                public Map getInfo() throws RepositoryException {
                    return Collections.emptyMap();
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
            events.add(apiEvent);
        }
        eventService.sendEvents(events, jackrabbitStoreProvider);
    }

    private void removeExternalDocument(ExternalContentStoreProvider provider, List<ApiEvent> events) throws RepositoryException {
        ExternalContentStoreProvider externalProvider = provider;
        String providerPath = StringUtils.substringAfter(path, externalProvider.getMountPoint());
        ExternalData externalData = null;
        try {
            externalData = externalProvider.getDataSource().getItemByPath(providerPath);
        } catch (PathNotFoundException e) {
            externalData = new ExternalData("fake-identifier",providerPath, "nt:base", Collections.EMPTY_MAP, false);
        }
        ExternalData finalExternalData = externalData;
        ApiEvent apiEvent = new ApiEvent() {
            @Override
            public int getType() {
                return Event.NODE_REMOVED;
            }

            @Override
            public String getPath() throws RepositoryException {
                return finalExternalData.getPath();
            }

            @Override
            public String getUserID() {
                return "";
            }

            @Override
            public String getIdentifier() throws RepositoryException {
                return finalExternalData.getId();
            }

            @Override
            public Map getInfo() throws RepositoryException {
                Map<String, Object> info = new HashMap<>();
                info.put("externalData", finalExternalData);
                return info;
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
        events.add(apiEvent);
        if(testMode) {
            for (int i = 0; i < 1000; i++) {
                events.add(apiEvent);
            }
        }
        if(async) {
            CompletableFuture.supplyAsync(() -> {
                ListUtils.partition(events, 100).forEach(event -> {
                    try {
                        eventService.sendEvents(event, externalProvider);
                    } catch (RepositoryException e) {
                        log.error(e.getMessage(), e);
                    }
                });
                return null;
            });
        } else {
            eventService.sendEvents(events, externalProvider);
        }
    }
}
