package de.objektkontor.clp.converter;

import de.objektkontor.clp.ParameterConverter;

public class ShortConverter implements ParameterConverter<Short> {

	@Override
	public Short convert(String value) throws Exception {
		return Short.parseShort(value);
	}
}
