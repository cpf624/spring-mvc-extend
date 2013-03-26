# spring-mvc-extend
## what is spring-mvc-extend
expand spring-mvc to support more type of resover

## the useage of spring-mvc-extend
1. config the ApplicationContext

        <mvc:annotation-driven>
            <mvc:argument-resolvers>
   		    	<bean class="org.jhat.spring.mvc.extend.method.annotation.RequestAttributeMethodArgumentResolver"/>
    		    <bean class="org.jhat.spring.mvc.extend.method.annotation.RequestJsonParamMethodArgumentResolver"/>
          	</mvc:argument-resolvers>
        </mvc:annotation-driven>

2. Use **org.jhat.spring.mvc.extend.bind.annotation.RequestAttribute** to resolve named parameter

        URL: /jhat/user.do?user.name=Jhat&user.email=cpf624@126.com
        
        Arg: @RequestAttribute("user") user

3. Use **org.jhat.spring.mvc.extend.bind.annotation.RequestJsonParam** to resolve named json parameter

        URL:/jhat/user.do?user={name: 'Jhat', email: 'cpf624@126.com'}
        
        Arg: @RequestJsonParam("user") user

4. the Validation need handle **MethodArgumentNotValidException** in @Controller

        @ResponseBody
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
            BindingResult result = e.getBindingResult();
            return result.getFieldError().getDefaultMessage();
        }

5. if need **java.util.Map** should use **org.jhat.spring.mvc.extend.util.MapWapper** to replace
