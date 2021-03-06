package org.springframework.springfaces.mvc.internal;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.springfaces.mvc.FacesContextSetter;

/**
 * Tests for {@link MvcNavigationActionListener}.
 * 
 * @author Phillip Webb
 */
@RunWith(MockitoJUnitRunner.class)
public class MvcNavigationActionListenerTest {

	private MvcNavigationActionListener listener;

	@Mock
	private ActionListener delegate;

	@Mock
	private ActionEvent event;

	@Mock
	private FacesContext facesContext;

	private Map<Object, Object> attributes = new HashMap<Object, Object>();

	@Before
	public void setup() {
		listener = new MvcNavigationActionListener(delegate);
		given(facesContext.getAttributes()).willReturn(attributes);
		FacesContextSetter.setCurrentInstance(facesContext);
	}

	@After
	public void cleanup() {
		FacesContextSetter.setCurrentInstance(null);
	}

	@Test
	public void shouldStoreActionEvent() throws Exception {
		listener.processAction(event);
		assertTrue(attributes.size() == 1);
		assertSame(event, MvcNavigationActionListener.getLastActionEvent(facesContext));
	}

	@Test
	public void shouldCallDelegate() throws Exception {
		listener.processAction(event);
		verify(delegate).processAction(event);
	}
}
