package de.objektkontor.clp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.objektkontor.clp.ParameterValidator;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandLineValidator {

	Class<? extends ParameterValidator<?>> value();
}
