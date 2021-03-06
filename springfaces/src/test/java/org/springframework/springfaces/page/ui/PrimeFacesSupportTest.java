package org.springframework.springfaces.page.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.springfaces.page.model.PagedDataModel;
import org.springframework.springfaces.page.model.PagedDataRows;
import org.springframework.springfaces.page.model.PrimeFacesPagedDataModel;

/**
 * Tests for {@link PrimeFacesSupport}.
 * 
 * @author Phillip Webb
 */
@RunWith(MockitoJUnitRunner.class)
public class PrimeFacesSupportTest {

	@Mock
	private PagedDataModel<Object> pagedDataRows;

	@After
	public void resetHasPrimeFaces() {
		PrimeFacesSupport.setHasPrimeFaces(true);
	}

	@Test
	public void shouldNotWrapWithoutPrimeFaces() throws Exception {
		PrimeFacesSupport.setHasPrimeFaces(false);
		PagedDataRows<Object> wrapped = PrimeFacesSupport.getInstance().wrapPagedDataRows(pagedDataRows);
		assertSame(wrapped, pagedDataRows);
	}

	@Test
	public void shouldWrapWithPrimeFaces() throws Exception {
		PagedDataRows<Object> wrapped = PrimeFacesSupport.getInstance().wrapPagedDataRows(pagedDataRows);
		assertThat(wrapped, is(PrimeFacesPagedDataModel.class));
		wrapped.getPageSize();
		verify(pagedDataRows).getPageSize();
	}
}
