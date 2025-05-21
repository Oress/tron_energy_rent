package org.ipan.nrgyrent.telegram.statetransitions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ipan.nrgyrent.telegram.States;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MatchState {
    States state() default States.START;

    int updateTypes() default UpdateType.NONE;

    boolean forAdmin() default false;

    String callbackData() default "";
}
