package org.jahia.support.modules.as.cli;

import org.jahia.modules.external.events.EventService;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AbstractDocumentIndexOperationTest {

    @Mock
    private EventService eventService;

    @Mock
    private JCRSessionFactory sessionFactory;

    @Mock
    private JCRStoreProvider provider;

    @Captor
    private ArgumentCaptor<Iterable<? extends ApiEvent>> eventsCaptor;

    private TestableOperation operation;

    /** Minimal concrete subclass to exercise the protected methods of the abstract class. */
    private static class TestableOperation extends AbstractDocumentIndexOperation {
        @Override
        protected void handleJCRNode(org.jahia.services.content.impl.jackrabbit.JackrabbitStoreProvider p, List<ApiEvent> events) {
            // not exercised here
        }

        @Override
        protected void handleExternalDocument(org.jahia.modules.external.ExternalContentStoreProvider p, List<ApiEvent> events) {
            // not exercised here
        }

        @Override
        protected int getEventType() {
            return Event.NODE_ADDED;
        }
    }

    @Before
    public void setUp() {
        operation = new TestableOperation();
        operation.setEventService(eventService);
        operation.setSessionFactory(sessionFactory);
    }

    @Test
    public void createApiEvent_returnsEventMatchingInputs() throws RepositoryException {
        Map<String, Object> info = new HashMap<>();
        info.put("key", "value");

        ApiEvent event = operation.createApiEvent("/sites/foo", "id-123", info);

        assertEquals(Event.NODE_ADDED, event.getType());
        assertEquals("/sites/foo", event.getPath());
        assertEquals("id-123", event.getIdentifier());
        assertEquals(info, event.getInfo());
        assertEquals("", event.getUserID());
        assertTrue(event.getDate() > 0);
    }

    @Test
    public void createApiEvent_returnsEmptyMapWhenInfoNull() throws RepositoryException {
        ApiEvent event = operation.createApiEvent("/sites/foo", "id-123", null);

        assertNotNull(event.getInfo());
        assertTrue(event.getInfo().isEmpty());
    }

    @Test
    public void sendEvents_syncPath_sendsListOnce() throws RepositoryException {
        List<ApiEvent> events = new ArrayList<>();
        events.add(operation.createApiEvent("/p", "id", null));

        operation.sendEvents(events, provider);

        verify(eventService, times(1)).sendEvents(events, provider);
    }

    @Test
    public void sendEvents_testMode_sendsThousandEventsAndDoesNotAliasCallerList() throws RepositoryException {
        operation.testMode = true;
        List<ApiEvent> events = new ArrayList<>();
        events.add(operation.createApiEvent("/p", "id", null));
        int originalSize = events.size();

        operation.sendEvents(events, provider);

        // Caller's original list must remain untouched (no aliasing).
        assertEquals(originalSize, events.size());

        verify(eventService).sendEvents(eventsCaptor.capture(), org.mockito.ArgumentMatchers.eq(provider));
        List<ApiEvent> sent = toList(eventsCaptor.getValue());
        // original event + TEST_EVENT_COUNT copies
        assertEquals(originalSize + 1000, sent.size());
    }

    private static List<ApiEvent> toList(Iterable<? extends ApiEvent> iterable) {
        List<ApiEvent> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    @Test
    public void toProviderPath_stripsMountPointPrefix() {
        assertEquals("/file.txt", AbstractDocumentIndexOperation.toProviderPath("/mount/file.txt", "/mount"));
    }

    @Test
    public void toProviderPath_returnsPathUnchangedWhenNoPrefixMatch() {
        // Degenerate case: mount point is not a prefix -> return the path unchanged (not "")
        assertEquals("/other/file.txt", AbstractDocumentIndexOperation.toProviderPath("/other/file.txt", "/mount"));
    }
}
