package de.objektkontor.clp;

import static java.lang.String.format;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.objektkontor.clp.annotation.CommandLineParameter;
import de.objektkontor.clp.annotation.CommandLineValidator;
import de.objektkontor.clp.converter.ByteConverter;
import de.objektkontor.clp.converter.CharacterConverter;
import de.objektkontor.clp.converter.EnumConverter;
import de.objektkontor.clp.converter.IntegerConverter;
import de.objektkontor.clp.converter.LongConverter;
import de.objektkontor.clp.converter.ShortConverter;

public class Parser<V> {

	private final String cmdLineSyntax;
	private final Map<Field, Parameter> parametersMap = new LinkedHashMap<>();
	private final Options options;
	private final boolean hasHelp;

	private ParameterValidator<V> parameterValidator;

	public Parser(String cmdLineSyntax, Class<V> type) {
		this(cmdLineSyntax, type, true);
	}

	public Parser(String cmdLineSyntax, Class<V> type, boolean addHelpOption) {
		this.cmdLineSyntax = cmdLineSyntax;
		buildParameters(type);
		options = new Options();
		for (Parameter parameter : parametersMap.values()) {
			options.addOption(parameter.option);
		}
		hasHelp = addHelpOption;
		if (hasHelp) {
			options.addOption(Option.builder("h").longOpt("help").desc("Show help").build());
		}
	}

	public Options getOptions() {
		return options;
	}

	public void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(cmdLineSyntax, options, true);
	}

	public V parse(V value, String... args) throws InvalidParameterException {
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine commandLine = parser.parse(options, args);
			if (hasHelp && commandLine.hasOption("h")) {
				printUsage();
				return null;
			}
			applyValues(commandLine, value);
			validateParameters(value);
			return value;
		} catch (ParseException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}

	public String dumpParameters(V value) {
		List<List<String>> table = new LinkedList<>();
		int nameMaxLength = 0;
		for (Map.Entry<Field, Parameter> entry : parametersMap.entrySet()) {
			Field field = entry.getKey();
			Parameter parameter = entry.getValue();
			List<String> row = new LinkedList<>();
			String parameterName = "( -" + parameter.option.getOpt() + " ) " + field.getName();
			row.add(parameterName);
			nameMaxLength = nameMaxLength < parameterName.length() ? parameterName.length() : nameMaxLength;
			try {
				field.setAccessible(true);
				Object parameterValue = field.get(value);
				row.add(dumpValue(parameterValue));
			} catch (Exception e) {
				row.add(e.getMessage());
			}
			table.add(row);
		}
		StringBuilder buffer = new StringBuilder();
		for (List<String> row : table) {
			buffer.append(format("  %-" + nameMaxLength + "s : %s\n", row.get(0), row.get(1)));
		}
		return buffer.toString();
	}

	private String dumpValue(Object value) {
		if (value == null)
			return "";
		Class<?> type = value.getClass();
		if (type.isArray()) {
			if (type.getComponentType().isPrimitive()) {
				int length = Array.getLength(value);
				Object[] boxed = (Object[]) Array.newInstance(Object.class, length);
				for (int i = 0; i < length; i++) {
					boxed[i] = Array.get(value, i);
				}
				return Arrays.toString(boxed);
			}
			return Arrays.toString((Object[]) value);
		}
		return String.valueOf(value);
	}

	private void buildParameters(Class<?> type) {
		Class<?> currentType = type;
		while (currentType != Object.class) {
			for (Field field : currentType.getDeclaredFields()) {
				CommandLineParameter[] annotations = field.getAnnotationsByType(CommandLineParameter.class);
				if (annotations.length > 0) {
					CommandLineParameter parameter = annotations[0];
					try {
						Option option = buildOption(parameter, field);
						ParameterConverter<?> converter = buildConverter(parameter, field);
						ParameterValidator<?> validator = buildValidator(parameter);
						parametersMap.put(field, new Parameter(option, converter, validator));
					} catch (Exception e) {
						throw new IllegalStateException("Error initializing parameter field: " + field, e);
					}
				}
			}
			currentType = currentType.getSuperclass();
		}
		CommandLineValidator[] validators = type.getAnnotationsByType(CommandLineValidator.class);
		if (validators.length > 0) {
			Class<? extends ParameterValidator<?>> validatorType = validators[0].value();
			try {
				@SuppressWarnings("unchecked")
				ParameterValidator<V> parameterValidator = (ParameterValidator<V>) validatorType.newInstance();
				this.parameterValidator = parameterValidator;
			} catch (Exception e) {
				throw new IllegalStateException("Error initializing validator class: " + validatorType, e);
			}
		}
	}

	private Option buildOption(CommandLineParameter parameter, Field field) {
		Class<?> parameterType = field.getType();
		boolean booleanParameter = parameterType == Boolean.class || parameterType == boolean.class;
		boolean hasLongName = !CommandLineParameter.NULL.equals(parameter.longName());
		boolean hasArgName = !CommandLineParameter.NULL.equals(parameter.argName());
		boolean hasNumberOfArgs = parameter.numberOfArgs() != Option.UNINITIALIZED;
		boolean hasDescription = !CommandLineParameter.NULL.equals(parameter.description());
		Builder builder = Option.builder(parameter.value());
		builder.required(parameter.required());
		if (hasLongName) {
			builder.longOpt(parameter.longName());
		} else {
			builder.longOpt(field.getName());
		}
		if (hasArgName || !booleanParameter) {
			builder.hasArg(true);
			builder.argName(hasArgName ? parameter.argName() : "value");
			builder.type(parameterType);
			if (parameterType.isArray()) {
				if (hasNumberOfArgs) {
					builder.numberOfArgs(parameter.numberOfArgs());
				} else {
					builder.numberOfArgs(Option.UNLIMITED_VALUES);
				}
			}
		} else {
			if (!booleanParameter)
				throw new IllegalStateException("Only boolean type is allowed for field: " + field);
		}
		if (hasDescription) {
			builder.desc(parameter.description());
		}
		return builder.build();
	}

	private ParameterConverter<?> buildConverter(CommandLineParameter parameter, Field field) throws Exception {
		Class<?> parameterType = field.getType().isArray() ? field.getType().getComponentType() : field.getType();
		if (parameterType == boolean.class
				|| parameterType == Boolean.class
				|| parameterType == String.class
				|| Enum.class.isAssignableFrom(parameterType))
			return null;
		Class<? extends ParameterConverter<?>> type = parameter.converter();
		if (type == CommandLineParameter.DefaultConverter.class)
			return getDefaultConverter(parameterType);
		return type.newInstance();
	}

	private ParameterValidator<?> buildValidator(CommandLineParameter parameter) throws Exception {
		Class<? extends ParameterValidator<?>> type = parameter.validator();
		if (type == CommandLineParameter.DefaultValidator.class)
			return null;
		return type.newInstance();
	}

	private void applyValues(CommandLine commandLine, V value) throws InvalidParameterException {
		for (Map.Entry<Field, Parameter> entry : parametersMap.entrySet()) {
			Field field = entry.getKey();
			Parameter parameter = entry.getValue();
			Object parameterValue;
			try {
				parameterValue = parameter.getValue(commandLine, field.getType());
				field.setAccessible(true);
				field.set(value, parameterValue);
			} catch (Exception e) {
				throw new InvalidParameterException(parameter.option, "Error setting parameter value: " + e.getMessage());
			}
			parameter.validate(parameterValue);
		}
	}

	private void validateParameters(V value) throws InvalidParameterException {
		if (parameterValidator != null) {
			String error;
			try {
				error = parameterValidator.validate(value);
			} catch (ClassCastException e) {
				throw new IllegalStateException("Error calling parameters validator. Check value type", e);
			} catch (Exception e) {
				throw new InvalidParameterException("Error validating parameters: " + e.getMessage());
			}
			if (error != null)
				throw new InvalidParameterException("Error validating parameters: " + error);
		}
	}

	private static ParameterConverter<?> getDefaultConverter(Class<?> type) throws Exception {
		ParameterConverter<?> converter = defaultConverters.get(type);
		if (converter == null)
			throw new IllegalArgumentException("No default converter exists for parameter type: " + type);
		return converter;
	}

	private static class Parameter {

		private final Option option;
		private final ParameterConverter<?> converter;
		private final ParameterValidator<?> validator;

		public Parameter(Option option, ParameterConverter<?> converter, ParameterValidator<?> validator) {
			super();
			this.option = option;
			this.converter = converter;
			this.validator = validator;
		}

		public Object getValue(CommandLine commandLine, Class<?> type) throws Exception {
			if (type.isArray())
				return getArrayValue(commandLine, type.getComponentType());
			return getObjectValue(commandLine, type);
		}

		public void validate(Object value) throws InvalidParameterException {
			if (validator != null) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				String error = ((ParameterValidator) validator).validate(value);
				if (error != null)
					throw new InvalidParameterException(option, error);
			}
		}

		private Object getObjectValue(CommandLine commandLine, Class<?> type) throws Exception {
			if (type == boolean.class || type == Boolean.class)
				return commandLine.hasOption(option.getOpt());
			String value = commandLine.getOptionValue(option.getOpt());
			return convertValue(value, type);
		}

		private Object getArrayValue(CommandLine commandLine, Class<?> type) throws Exception {
			String[] values = commandLine.getOptionValues(option.getOpt());
			Object result = Array.newInstance(type, values.length);
			for (int i = 0; i < values.length; i++) {
				Object value = convertValue(values[i], type);
				Array.set(result, i, value);
			}
			return result;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Object convertValue(String value, Class<?> type) throws Exception {
			if (type == String.class)
				return value;
			if (Enum.class.isAssignableFrom(type))
				return enumConverter.convert(value, (Class) type);
			Object result = ((ParameterConverter) converter).convert(value);
			return result;
		}
	}

	private final static EnumConverter enumConverter = new EnumConverter();
	private final static Map<Class<?>, ParameterConverter<?>> defaultConverters = new HashMap<>();

	static {
		ByteConverter byteConverter = new ByteConverter();
		defaultConverters.put(byte.class, byteConverter);
		defaultConverters.put(Byte.class, byteConverter);

		CharacterConverter characterConverter = new CharacterConverter();
		defaultConverters.put(char.class, characterConverter);
		defaultConverters.put(Character.class, characterConverter);

		ShortConverter shortConverter = new ShortConverter();
		defaultConverters.put(short.class, shortConverter);
		defaultConverters.put(Short.class, shortConverter);

		IntegerConverter integerConverter = new IntegerConverter();
		defaultConverters.put(int.class, integerConverter);
		defaultConverters.put(Integer.class, integerConverter);

		LongConverter longConverter = new LongConverter();
		defaultConverters.put(long.class, longConverter);
		defaultConverters.put(Long.class, longConverter);
	}
}
