package com.gitee.jenkins.util;

import hudson.security.ACL;
import hudson.security.ACLContext;
import org.springframework.security.core.Authentication;


/**
 * @author Robin Müller
 */
public class ACLUtil {

    public static <T> T impersonate(Authentication auth, final Function<T> function) {
        try (ACLContext ignored = ACL.as2(auth)) {
            final ObjectHolder<T> holder = new ObjectHolder<>();
            holder.setValue(function.invoke());
            return holder.getValue();
        }
    }

    public interface Function<T> {
        T invoke();
    }

    private static class ObjectHolder<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
