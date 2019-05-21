package de.objektkontor.clp.converter;

public class EnumConverter {

	public <T extends Enum<T>> T convert(String value, Class<T> type) throws Exception {
		return Enum.valueOf(type, value);
	}
}
