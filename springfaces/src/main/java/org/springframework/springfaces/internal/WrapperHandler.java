package org.springframework.springfaces.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.springfaces.FacesWrapperFactory;
import org.springframework.springfaces.SpringFacesIntegration;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility class that can wrap JSF objects by consulting all {@link FacesWrapperFactory} objects registered within the
 * {@link WebApplicationContext} containing the {@link SpringFacesIntegration} bean.
 * <p>
 * Wrapping will be re-applied if whenever the {@link WebApplicationContext} is reloaded. If no
 * {@link SpringFacesIntegration} is {@link SpringFacesIntegration#isInstalled(ExternalContext) installed} then the
 * original delegate is returned as the wrapped instance.
 * 
 * @param <T> The JSF type being managed
 * @see #getWrapped()
 * 
 * @author Phillip Webb
 */
class WrapperHandler<T> {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Request attribute to hold the current web application context. This attribute is recreated here from
	 * <tt>DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE</tt> to save a hard dependency to Spring MVC.
	 */
	static final String DISPATCHER_SERVLET_WEB_APPLICATION_CONTEXT_ATTRIBUTE = "org.springframework."
			+ "web.servlet.DispatcherServlet.CONTEXT";

	/**
	 * The type of JSF object being managed.
	 */
	private Class<?> typeClass;

	/**
	 * The root delegate.
	 */
	private DelegateAccessor<T> delegate;

	/**
	 * The fully wrapped implementation. This is late binding.
	 * @see #getWrapped()
	 */
	private T wrapped;

	/**
	 * The date that the application context used to create the wrapped object was last refreshed.
	 */
	private Date lastRefreshedDate;

	private boolean warnOnMissingSpringFaces;

	/**
	 * Create a mew WrapperHandler.
	 * @param typeClass The JSF type being wrapped
	 * @param delegate The root delegate
	 */
	public WrapperHandler(Class<T> typeClass, T delegate) {
		Assert.notNull(typeClass, "TypeClass must not be null");
		Assert.notNull(delegate, "Delegate must not be null");
		this.typeClass = typeClass;
		this.delegate = new DirectDelegateAccessor<T>(delegate);
	}

	/**
	 * Create a mew WrapperHandler.
	 * @param typeClass The JSF type being wrapped
	 * @param delegate Access to the root delegate
	 */
	public WrapperHandler(Class<T> typeClass, DelegateAccessor<T> delegate) {
		Assert.notNull(typeClass, "TypeClass must not be null");
		Assert.notNull(delegate, "Delegate must not be null");
		this.typeClass = typeClass;
		this.delegate = delegate;
	}

	/**
	 * Set if a warning message is logged due to {@link SpringFacesIntegration} not being installed. Defaults to false
	 * as most wrappers can be instantiated before Spring.
	 * @param warnOnMissingSpringFaces if a warning message should be logged
	 */
	public void setWarnOnMissingSpringFaces(boolean warnOnMissingSpringFaces) {
		this.warnOnMissingSpringFaces = warnOnMissingSpringFaces;
	}

	/**
	 * Creates a fully wrapped implementation of the delegate by consulting all {@link FacesWrapperFactory factories}
	 * registered with Spring.
	 * @return a wrapped implementation
	 */
	public T getWrapped() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext == null) {
			// Calls to wrapped instances can occur when there is no faces context if JSF has not yet completely
			// intialized. We allow these early calls to proceed to the delegate.
			wrapped = null;
			return delegate.getDelegate(DelegateAccessType.WRAP);
		}
		ExternalContext externalContext = facesContext.getExternalContext();
		if ((wrapped == null)
				|| (SpringFacesIntegration.isInstalled(externalContext) && (!SpringFacesIntegration
						.getLastRefreshedDate(externalContext).equals(lastRefreshedDate)))) {
			DelegateAccessType accessType = (wrapped == null ? DelegateAccessType.WRAP : DelegateAccessType.REWRAP);
			if (logger.isDebugEnabled()) {
				logger.debug((accessType == DelegateAccessType.WRAP ? "Wrapping " : "Rewrapping ")
						+ delegate.getDescription());
			}
			wrapped = wrap(externalContext, delegate.getDelegate(accessType));
			if (SpringFacesIntegration.isInstalled(externalContext)) {
				lastRefreshedDate = SpringFacesIntegration.getLastRefreshedDate(externalContext);
			}
		}
		return wrapped;
	}

	/**
	 * Wrap the specified delegate by consulting all {@link FacesWrapperFactory factories} registered with Spring.
	 * @param externalContext the external context
	 * @param delegate the root delegate
	 * @return a wrapped implementation
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private T wrap(ExternalContext externalContext, T delegate) {
		if (!SpringFacesIntegration.isInstalled(externalContext)) {
			if (logger.isDebugEnabled()) {
				logger.debug("SpringFacesSupport is not yet installed, wrapping will be deferred");
			}
			if (logger.isWarnEnabled() && warnOnMissingSpringFaces) {
				logger.warn("SpringFacesSupport is not installed, full Spring/JSF integration may not be availble");
			}
			return delegate;
		}

		ApplicationContext applicationContext = SpringFacesIntegration.getCurrentInstance(externalContext)
				.getApplicationContext();

		List<Map.Entry<String, FacesWrapperFactory>> orderdBeans = new ArrayList<Map.Entry<String, FacesWrapperFactory>>();
		orderdBeans.addAll(BeanFactoryUtils
				.beansOfTypeIncludingAncestors(applicationContext, FacesWrapperFactory.class).entrySet());
		Collections.sort(orderdBeans, new OrderedMapEntryComparator());
		T rtn = delegate;
		for (Map.Entry<String, FacesWrapperFactory> entry : orderdBeans) {
			FacesWrapperFactory factory = entry.getValue();
			if (isFactorySupported(factory)) {
				T wrapper = (T) factory.newWrapper(typeClass, rtn);
				if (wrapper != null) {
					Assert.isInstanceOf(typeClass, wrapper, "FacesWrapperFactory " + entry.getValue()
							+ " returned incorrect type ");
					if (logger.isDebugEnabled()) {
						logger.debug("Wrapping " + typeClass.getSimpleName() + " with " + wrapper.getClass()
								+ " obtained from FacesWrapperFactory " + entry.getValue());
					}
					postProcessWrapper(wrapper);
					rtn = wrapper;
				}
			}
		}
		return rtn;
	}

	/**
	 * Determine if a given {@link FacesWrapperFactory} is suitable by resolving generic arguments.
	 * @param factory the factory to test
	 * @return <tt>true</tt> if the <tt>factory</tt> is supported, otherwise <tt>false</tt>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isFactorySupported(FacesWrapperFactory factory) {
		Class typeArg = GenericTypeResolver.resolveTypeArgument(factory.getClass(), FacesWrapperFactory.class);
		if (typeArg == null) {
			Class targetClass = AopUtils.getTargetClass(factory);
			if (targetClass != factory.getClass()) {
				typeArg = GenericTypeResolver.resolveTypeArgument(targetClass, FacesWrapperFactory.class);
			}
		}
		return (typeArg == null || typeArg.isAssignableFrom(typeClass));
	}

	/**
	 * Strategy method called after a wrapped instance has been created. Subclasses can implement custom post-processing
	 * as required.
	 * @param wrapped the newly created wrapped instance
	 */
	protected void postProcessWrapper(T wrapped) {
	}

	/**
	 * Convenience factory method to create a {@link WrapperHandler} with the generic type obtained from
	 * <tt>typeClass</tt>
	 * @param <T> the JSF type being managed
	 * @param typeClass the JSF type being managed
	 * @param delegate the delegate
	 * @return a {@link WrapperHandler}
	 */
	public static <T> WrapperHandler<T> get(Class<T> typeClass, T delegate) {
		return new WrapperHandler<T>(typeClass, delegate);
	}

	/**
	 * {@link Comparator} implementation to sort {@link Map.Entry} values by {@link org.springframework.core.Ordered} as
	 * well as the {@link Order} annotation.
	 */
	private static class OrderedMapEntryComparator extends AnnotationAwareOrderComparator {
		@Override
		public int compare(Object o1, Object o2) {
			return super.compare(((Map.Entry<?, ?>) o1).getValue(), ((Map.Entry<?, ?>) o2).getValue());
		}
	}

	/**
	 * The various reasons that a delegate can be accessed.
	 */
	public enum DelegateAccessType {
		/**
		 * The delegate is required for an initial wrap.
		 */
		WRAP,
		/**
		 * The delegate is required for a re-wrap. This can occur if the application context has been refreshed.
		 */
		REWRAP
	};

	/**
	 * Interface to provide access to the underlying delegate. Implementations can return a different delegate if
	 * required.
	 * @param <T> The delegate type
	 */
	public static interface DelegateAccessor<T> {
		/**
		 * Returns a description of the delegate.
		 * @return the description
		 */
		public String getDescription();

		/**
		 * Returns the actual delegate to use.
		 * @param accessType the reason that the delegate is being accessed
		 * @return the delegate
		 */
		public T getDelegate(DelegateAccessType accessType);
	}

	/**
	 * Implementation of {@link WrapperHandler.DelegateAccessor} that simple returns an object instance.
	 * @param <T> the data type
	 */
	private static class DirectDelegateAccessor<T> implements DelegateAccessor<T> {

		private T delegate;

		public DirectDelegateAccessor(T delegate) {
			this.delegate = delegate;
		}

		public String getDescription() {
			return delegate.getClass().getName();
		}

		public T getDelegate(DelegateAccessType accessType) {
			return delegate;
		}
	}
}
