package org.springframework.springfaces.mvc.navigation.annotation.support;

import javax.faces.component.UIComponent;

import org.springframework.core.MethodParameter;
import org.springframework.springfaces.mvc.navigation.NavigationContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves method arguments against a {@link NavigationContext}. This resolver supports the following:
 * <ul>
 * <li>NavigationContext parameters</li>
 * <li>String parameters (resolved using {@link NavigationContext#getOutcome()})</li>
 * <li>UIComponents (resolved using {@link NavigationContext#getComponent()} when assignment compatible)</li>
 * </ul>
 * @author Phillip Webb
 */
public class NavigationContextMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private NavigationContext navigationContext;

	public NavigationContextMethodArgumentResolver(NavigationContext navigationContext) {
		Assert.notNull(navigationContext, "NavigationContext must not be null");
		this.navigationContext = navigationContext;
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return supportsNavigationContext(parameter) || supportsComponent(parameter) || supportsOutcome(parameter);
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		if (supportsNavigationContext(parameter)) {
			return navigationContext;
		}
		if (supportsComponent(parameter)) {
			return navigationContext.getComponent();
		}
		if (supportsOutcome(parameter)) {
			return navigationContext.getOutcome();
		}
		return null;
	}

	private boolean supportsNavigationContext(MethodParameter parameter) {
		return parameter.getParameterType().equals(NavigationContext.class);
	}

	private boolean supportsComponent(MethodParameter parameter) {
		UIComponent component = navigationContext.getComponent();
		return UIComponent.class.isAssignableFrom(parameter.getParameterType())
				&& (component == null || parameter.getParameterType().isInstance(component));
	}

	private boolean supportsOutcome(MethodParameter parameter) {
		return String.class.equals(parameter.getParameterType());
	}
}
