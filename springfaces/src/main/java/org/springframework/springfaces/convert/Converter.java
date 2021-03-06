package org.springframework.springfaces.convert;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * A variation of the JSF {@link javax.faces.convert.Converter} that support generic typing.
 * 
 * @param <T> The type the converter is for.
 * @See {@link ConditionalConverterForClass}
 * 
 * @author Phillip Webb
 */
public interface Converter<T> {

	/**
	 * See {@link javax.faces.convert.Converter#getAsObject(FacesContext, UIComponent, String)}.
	 * @param context the faces context
	 * @param component the component
	 * @param value the string value to convert
	 * @return the object value
	 */
	T getAsObject(FacesContext context, UIComponent component, String value);

	/**
	 * See {@link javax.faces.convert.Converter#getAsString(FacesContext, UIComponent, Object)}.
	 * @param context the faces context
	 * @param component the component
	 * @param value the value to convert
	 * @return the string value
	 */
	String getAsString(FacesContext context, UIComponent component, T value);
}
