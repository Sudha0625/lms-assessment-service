package com.learninghub.assessment.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controller methods declare a CurrentUser parameter. Defaults: student1 / student. */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    @Override public boolean supportsParameter(MethodParameter p) { return p.getParameterType() == CurrentUser.class; }

    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest r, WebDataBinderFactory f) {
        String u = r.getHeader("X-Username");
        String role = r.getHeader("X-Role");
        return new CurrentUser(u == null || u.isBlank() ? "student1" : u,
                role == null || role.isBlank() ? "student" : role.toLowerCase());
    }
}
