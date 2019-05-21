package de.objektkontor.clp.converter;

import de.objektkontor.clp.ParameterConverter;

public class LongConverter implements ParameterConverter<Long> {

	@Override
	public Long convert(String value) throws Exception {
		return Long.parseLong(value);
	}
}
