package de.objektkontor.clp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.cli.Option;

import de.objektkontor.clp.ParameterValidator;
import de.objektkontor.clp.ParameterConverter;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CommandLineParameter {

	final static String NULL = "<null>";
    abstract class DefaultConverter implements ParameterConverter<Object> {};
    abstract class DefaultValidator implements ParameterValidator<Object> {};

    String value();

    boolean required() default false;

    String longName() default NULL;

    String argName() default NULL;

    int numberOfArgs() default Option.UNINITIALIZED;

    String description() default NULL;

    Class<? extends ParameterConverter<?>> converter() default DefaultConverter.class;

    Class<? extends ParameterValidator<?>> validator() default DefaultValidator.class;
}
