package de.objektkontor.clp.converter;

import de.objektkontor.clp.ParameterConverter;

public class IntegerConverter implements ParameterConverter<Integer> {

	@Override
	public Integer convert(String value) throws Exception {
		return Integer.parseInt(value);
	}
}
