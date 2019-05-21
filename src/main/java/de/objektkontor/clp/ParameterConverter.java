package de.objektkontor.clp;

public interface ParameterConverter<V> {

	V convert(String value) throws Exception;
}
