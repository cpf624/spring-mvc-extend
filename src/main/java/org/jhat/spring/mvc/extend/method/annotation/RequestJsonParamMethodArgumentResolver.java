package org.jhat.spring.mvc.extend.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import org.jhat.spring.mvc.extend.bind.annotation.RequestJsonParam;
import org.jhat.spring.mvc.extend.util.MapWapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestJsonParamMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestJsonParam.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		RequestJsonParam attribute = parameter.getParameterAnnotation(RequestJsonParam.class);
        String name = attribute.value();
		String value = webRequest.getParameter(name);
		Class<?> paramType = parameter.getParameterType();
		List<Annotation> annotations = getValidateAnnotations(parameter);
		Object result = null;
		WebDataBinder binder = null;
		if (value == null) {
			if (attribute.required()) {
				binder = binderFactory.createBinder(webRequest, result, name);
				BindingResult bindingResult = binder.getBindingResult();
				bindingResult.addError(new FieldError(name, name, name + " is required"));
				throw new MethodArgumentNotValidException(parameter, bindingResult);
			}
		} else {
			try {
				
			} catch (JsonSyntaxException e) {
			}
			Gson gson = new Gson();
			if(MapWapper.class.isAssignableFrom(paramType)) {
				Map<Object, Object> origin = gson.fromJson(value, Map.class);
				if (origin != null) {
					MapWapper<Object, Object> real = new MapWapper<Object, Object>();
					Type type = parameter.getGenericParameterType();
		            Class<?> keyType = Object.class;
		            Class<?> valueType = Object.class;
		            if(type instanceof ParameterizedType) {
		                keyType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
		                valueType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[1];
		            }
					Set<Object> keys = origin.keySet();
					Object realKey = null;
					Object realValue = null;
					Object orignValue = null;
					for (Object key : keys) {
						if (!String.class.equals(key.getClass())) {
							key = gson.toJson(key);
						}
						if (Object.class.equals(keyType) || String.class.equals(keyType)) {
							realKey = key;
						} else {
							realKey = gson.fromJson((String) key, keyType);
						}
						orignValue = origin.get(key);
						if (!String.class.equals(orignValue.getClass())) {
							orignValue = gson.toJson(orignValue);
						}
						if (Object.class.equals(valueType) || String.class.equals(valueType)) {
							realValue = orignValue;
						} else {
							realValue = gson.fromJson((String) orignValue, valueType);
						}
						binder = binderFactory.createBinder(webRequest, realValue, name + "[" + key + "]");
						realValue = binder.convertIfNecessary(realValue, valueType, null);
						validateIfApplicable(binder, parameter, annotations);
						real.put(realKey, realValue);
					}
					result = real;
				}
	        } else {
	        	Object target = null;
	        	result = gson.fromJson(value, parameter.getGenericParameterType());
	        	if(paramType.isArray()) {
	        		if (result != null) {
	            		Object[] targets = (Object[]) result;
	            		for (int i = 0; i < targets.length; i++) {
	            			target = targets[i];
	            			binder = binderFactory.createBinder(webRequest, target, name + "[" + i + "]");
	    					validateIfApplicable(binder, parameter, annotations);
	            		}
	            	}
	        	} else if(List.class.isAssignableFrom(paramType)) {
	        		if (result != null) {
	        			List<Object> targets = (List<Object>) result;
	        			int size = targets.size();
	            		for (int i = 0; i < size; i++) {
	            			target = targets.get(i);
	            			binder = binderFactory.createBinder(webRequest, target, name + "[" + i + "]");
	    					validateIfApplicable(binder, parameter, annotations);
	            		}
	            	}
	            } else if (Set.class.isAssignableFrom(paramType)) {
	            	if (result != null) {
	            		Set<Object> targets = (Set<Object>) result;
	            		int i = 0;
	            		for (Object t : targets) {
	            			binder = binderFactory.createBinder(webRequest, t, name + "[" + i + "]");
	    					validateIfApplicable(binder, parameter, annotations);
	            		}
	            	}
	            }
	        }
		}
		if (result != null) {
			binder = binderFactory.createBinder(webRequest, result, name);
			result = binder.convertIfNecessary(result, paramType, parameter);
			validateIfApplicable(binder, parameter, annotations);
			mavContainer.addAttribute(name, result);
		}
		return result;
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

}