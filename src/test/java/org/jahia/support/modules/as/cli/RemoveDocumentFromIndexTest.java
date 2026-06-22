package org.jahia.support.modules.as.cli;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.events.EventService;
import org.jahia.services.content.ApiEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoveDocumentFromIndexTest {

    @Mock
    private EventService eventService;

    @Mock
    private ExternalContentStoreProvider provider;

    @Mock
    private ExternalDataSource dataSource;

    @Captor
    private ArgumentCaptor<Iterable<? extends ApiEvent>> eventsCaptor;

    private RemoveDocumentFromIndex operation;

    @Before
    public void setUp() {
        operation = new RemoveDocumentFromIndex();
        operation.setEventService(eventService);
        operation.path = "/mount/missing";
        when(provider.getMountPoint()).thenReturn("/mount");
        when(provider.getDataSource()).thenReturn(dataSource);
    }

    @Test
    public void handleExternalDocument_pathNotFound_stillSendsSyntheticEvent() throws RepositoryException {
        when(dataSource.getItemByPath(anyString())).thenThrow(new PathNotFoundException("missing"));

        List<ApiEvent> events = new ArrayList<>();
        operation.handleExternalDocument(provider, events);

        verify(eventService, times(1)).sendEvents(eventsCaptor.capture(), eq(provider));
        List<ApiEvent> sent = new ArrayList<>();
        eventsCaptor.getValue().forEach(sent::add);
        assertEquals(1, sent.size());
        assertEquals("fake-identifier", sent.get(0).getIdentifier());
    }

    @Test
    public void handleExternalDocument_found_sendsRealEvent() throws RepositoryException {
        ExternalData data = new ExternalData("real-id", "/missing", "nt:base", new java.util.HashMap<>());
        when(dataSource.getItemByPath(anyString())).thenReturn(data);

        List<ApiEvent> events = new ArrayList<>();
        operation.handleExternalDocument(provider, events);

        verify(eventService, times(1)).sendEvents(any(), eq(provider));
        assertFalse(events.isEmpty());
        assertEquals("real-id", events.get(0).getIdentifier());
    }
}
