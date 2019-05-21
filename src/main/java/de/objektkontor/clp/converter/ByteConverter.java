package de.objektkontor.clp.converter;

import de.objektkontor.clp.ParameterConverter;

public class ByteConverter implements ParameterConverter<Byte> {

	@Override
	public Byte convert(String value) throws Exception {
		return Byte.parseByte(value);
	}
}
