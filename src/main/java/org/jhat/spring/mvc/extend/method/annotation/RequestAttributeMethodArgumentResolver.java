package org.jhat.spring.mvc.extend.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.jhat.spring.mvc.extend.bind.annotation.RequestAttribute;
import org.jhat.spring.mvc.extend.util.MapWapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @project spring-mvc-extend
 * @author jhat
 * @email cpf624@126.com
 * @date Mar 19, 20138:46:15 PM
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RequestAttributeMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
    	return parameter.hasParameterAnnotation(RequestAttribute.class);
    }

    @Override
    public final Object resolveArgument(MethodParameter parameter,
                                        ModelAndViewContainer mavContainer,
                                        NativeWebRequest request,
                                        WebDataBinderFactory binderFactory) throws Exception {
    	RequestAttribute attribute = parameter.getParameterAnnotation(RequestAttribute.class);
        String name = attribute.value();
        Object target = (mavContainer.containsAttribute(name)) ?
                mavContainer.getModel().get(name) : createAttribute(name, parameter, binderFactory, request);
        WebDataBinder binder = binderFactory.createBinder(request, target, name);
        if (target != null) {
            bindRequestParameters(mavContainer, binderFactory, binder, request, parameter, name);
        }
        target = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType());
        mavContainer.addAttribute(name, target);
        return target;
    }


    private Object createAttribute(String attributeName, MethodParameter parameter,
            WebDataBinderFactory binderFactory,  NativeWebRequest request) throws Exception {
        String value = getRequestValueForAttribute(attributeName, request);
        if (value != null) {
            Object attribute = createAttributeFromRequestValue(value, attributeName, parameter, binderFactory, request);
            if (attribute != null) {
                return attribute;
            }
        }
        Class<?> parameterType = parameter.getParameterType();
        if(parameterType.isArray() || List.class.isAssignableFrom(parameterType)) {
            return ArrayList.class.newInstance();
        }
        if(Set.class.isAssignableFrom(parameterType)) {
            return HashSet.class.newInstance();
        }
        if(MapWapper.class.isAssignableFrom(parameterType)) {
            return MapWapper.class.newInstance();
        }
        
        return BeanUtils.instantiateClass(parameterType);
    }
    
    
    private String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
        Map<String, String> variables = getUriTemplateVariables(request);
        String value = variables.get(attributeName);
        return StringUtils.hasText(value) ? value : request.getParameter(attributeName);
    }

    private final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
        Map<String, String> variables = 
            (Map<String, String>) request.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return (variables != null) ? variables : Collections.<String, String>emptyMap();
    }

    private Object createAttributeFromRequestValue(String sourceValue,
                                                 String attributeName, 
                                                 MethodParameter parameter, 
                                                 WebDataBinderFactory binderFactory, 
                                                 NativeWebRequest request) throws Exception {
        DataBinder binder = binderFactory.createBinder(request, null, attributeName);
        ConversionService conversionService = binder.getConversionService();
        if (conversionService != null) {
            TypeDescriptor source = TypeDescriptor.valueOf(String.class);
            TypeDescriptor target = new TypeDescriptor(parameter);
            if (conversionService.canConvert(source, target)) {
                return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
            }
        }
        return null;
    }
    
	private void bindRequestParameters(
            ModelAndViewContainer mavContainer,
            WebDataBinderFactory binderFactory,
            WebDataBinder binder, 
            NativeWebRequest request, 
            MethodParameter parameter,
            String name) throws Exception {
		Object orignalTarget = binder.getTarget();
        Class<?> targetType = binder.getTarget().getClass();
        Map<String, String[]> mapedValues = getMapedValues(orignalTarget, request, parameter);
        WebDataBinder simpleBinder = binderFactory.createBinder(request, null, name);
        List<Annotation> annotations = getValidateAnnotations(parameter);
        if(Collection.class.isAssignableFrom(targetType)) {//bind collection
            Type type = parameter.getGenericParameterType();
            Class<?> componentType = Object.class;
            Class<?> parameterType = parameter.getParameterType();
            Collection target = (Collection) orignalTarget;
            if(type instanceof ParameterizedType) {
                componentType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
            }
            if(parameterType.isArray()) {
                componentType = parameterType.getComponentType();
            }
            Map<String, Object> components = new HashMap<String, Object>();
            Set<String> keys = mapedValues.keySet();
            for(String key : keys) {
                String prefixName = getPrefixName(key);
                if(isSimpleComponent(prefixName)) { //bind simple type 
                    Map<String, Object> paramValues = getMapedValuesStartingWith(mapedValues, prefixName);
                    for(Object value : paramValues.values()) {
                        target.add(simpleBinder.convertIfNecessary(value, componentType));
                    }
                } else {
                	Object component = components.get(prefixName);
                	if (component == null) {
                		component = BeanUtils.instantiate(componentType);
                		WebDataBinder componentBinder = binderFactory.createBinder(request, component, name + prefixName.substring(0, prefixName.length() - 1));
                		PropertyValues pvs = new MutablePropertyValues(getMapedValuesStartingWith(mapedValues, prefixName));
                        componentBinder.bind(pvs);
                        validateIfApplicable(componentBinder, parameter, annotations);
                		components.put(prefixName, component);
                	}
                }
            }
            for(Object key : components.keySet()) {
            	target.add(components.get(key));
            }
        } else if(MapWapper.class.isAssignableFrom(targetType)) { 
            Type type = parameter.getGenericParameterType();
            Class<?> keyType = Object.class;
            Class<?> valueType = Object.class;
            if(type instanceof ParameterizedType) {
                keyType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
                valueType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[1];
            }
            MapWapper target = (MapWapper) orignalTarget;
            for(Object key : mapedValues.keySet()) {
                String prefixName = getPrefixName((String) key);
                Object keyValue = simpleBinder.convertIfNecessary(getMapKey(prefixName), keyType);
                if(isSimpleComponent(prefixName)) { //bind simple type 
                    Map<String, Object> paramValues = getMapedValuesStartingWith(mapedValues, prefixName);
                    for(Object value : paramValues.values()) {
                        target.put(keyValue, simpleBinder.convertIfNecessary(value, valueType));
                    }
                } else {
                	Object component = target.get(keyValue);
                    if (component == null) {
                    	component = BeanUtils.instantiate(valueType);
                    	WebDataBinder componentBinder = binderFactory.createBinder(request, component, name + prefixName.substring(0, prefixName.length() - 1));
                    	PropertyValues pvs = new MutablePropertyValues(getMapedValuesStartingWith(mapedValues, prefixName));
                        componentBinder.bind(pvs);
                        validateIfApplicable(componentBinder, parameter, annotations);
                        target.put(keyValue, component);
                    }
                }
            }
        } else {//bind model
            PropertyValues pvs = new MutablePropertyValues(mapedValues);
            binder.bind(pvs);
            validateIfApplicable(binder, parameter, annotations);
        }
    }


    private Object getMapKey(String prefixName) {
        String key = prefixName;
        if(key.startsWith("['")) {
            key = key.replaceAll("\\[\'", "").replaceAll("\'\\]", "");
        }
        if(key.startsWith("[\"")) {
            key = key.replaceAll("\\[\"", "").replaceAll("\"\\]", "");
        }
        if(key.endsWith(".")) {
            key = key.substring(0, key.length() - 1);
        }
        return key;
    }

    private boolean isSimpleComponent(String prefixName) {
        return !prefixName.endsWith(".");
    }

    private String getPrefixName(String name) {
        int begin = 0;
        int end = name.indexOf("]") + 1;
        if(name.indexOf("].") >= 0) {
            end = end + 1;
        }
        return name.substring(begin, end);
    }

	private Map<String, String[]> getMapedValues(Object target, NativeWebRequest request, MethodParameter parameter) {
        String modelPrefixName = parameter.getParameterAnnotation(RequestAttribute.class).value();
        Map<String, String[]> mapedValues = new HashMap<String, String[]>();
        HttpServletRequest nativeRequest = (HttpServletRequest) request.getNativeRequest();
        Set<Entry<String, String>> variables = this.getUriTemplateVariables(request).entrySet();
        for(Entry<String, String> entry : variables) {
            String parameterName = entry.getKey();
            String value = entry.getValue();
            if(isFormModelAttribute(parameterName, modelPrefixName)) {
            	mapedValues.put(getNewParameterName(parameterName, modelPrefixName), new String[]{value});
            }
        }
        Set<Object> paramters = nativeRequest.getParameterMap().entrySet();
        for(Object parameterEntry : paramters) {
            Entry<String, String[]> entry = (Entry<String, String[]>) parameterEntry;
            String parameterName = entry.getKey();
            String[] value = entry.getValue();
            if(isFormModelAttribute(parameterName, modelPrefixName)) {
            	mapedValues.put(getNewParameterName(parameterName, modelPrefixName), value);
            }
        }
        return mapedValues;
    }

    private String getNewParameterName(String parameterName, String modelPrefixName) {
        int modelPrefixNameLength = modelPrefixName.length();
        if(parameterName.charAt(modelPrefixNameLength) == '.') {
            return parameterName.substring(modelPrefixNameLength + 1);
        }
        if(parameterName.charAt(modelPrefixNameLength) == '[') {
            return parameterName.substring(modelPrefixNameLength);
        }
        throw new IllegalArgumentException("illegal request parameter, can not binding to @RequestAttribute(" + modelPrefixName + ")");
    }

    private boolean isFormModelAttribute(String parameterName, String modelPrefixName) {
        int modelPrefixNameLength = modelPrefixName.length();
        if(parameterName.length() == modelPrefixNameLength) {
            return false;
        }
        if(!parameterName.startsWith(modelPrefixName)) {
            return false;
        }
        char ch = (char) parameterName.charAt(modelPrefixNameLength);
        if(ch == '.' || ch == '[') {
            return true;
        }
        return false;
    }
    
    private List<Annotation> getValidateAnnotations(MethodParameter parameter) {
    	Annotation[] annotations = parameter.getParameterAnnotations();
    	List<Annotation> results = new ArrayList<Annotation>();
        for (Annotation annot : annotations) {
        	Class<?> clazz = annot.getClass();
        	if (Validated.class.isAssignableFrom(clazz) || Valid.class.isAssignableFrom(clazz)) {
        		results.add(annot);
            }
        }
        return results;
    }

    private void validateIfApplicable(WebDataBinder binder, MethodParameter parameter, List<Annotation> annotations) throws MethodArgumentNotValidException {
        for (Annotation annot : annotations) {
        	Object hints = AnnotationUtils.getValue(annot);
            binder.validate(hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
        }
        BindingResult result = binder.getBindingResult();
        if (result.hasErrors()) {
        	throw new MethodArgumentNotValidException(parameter, result);
        }
    }

    private Map<String, Object> getMapedValuesStartingWith(Map<String, String[]> source, String prefix) {
		Enumeration paramNames = Collections.enumeration(source.keySet());
		Map<String, Object> params = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = source.get(paramName);
				if (values == null || values.length == 0) {
					// Do nothing, no values found at all.
				}
				else if (values.length > 1) {
					params.put(unprefixed, values);
				}
				else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}
    
}
