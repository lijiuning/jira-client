package de.objektkontor.clp.converter;

import de.objektkontor.clp.ParameterConverter;

public class CharacterConverter implements ParameterConverter<Character> {

	@Override
	public Character convert(String value) throws Exception {
		if (value.length() != 1)
			throw new IllegalArgumentException("Invalid character: " + value);
		return value.charAt(0);
	}
}
