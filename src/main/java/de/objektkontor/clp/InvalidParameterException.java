package de.objektkontor.clp;

import org.apache.commons.cli.Option;

public class InvalidParameterException extends Exception {

	private static final long serialVersionUID = 1;

	public InvalidParameterException() {
		super("Error in command line parameters");
	}

	public InvalidParameterException(String message) {
		super(message);
	}

	public InvalidParameterException(Option option, String message) {
		super("Error in parameter '"
				+ (option.getLongOpt() == null ? option.getOpt() : option.getLongOpt())
				+ "': " + message);
	}
}
