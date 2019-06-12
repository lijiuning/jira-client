package com.ljn;

import de.objektkontor.clp.annotation.CommandLineParameter;

public class CommandArgs {

	@CommandLineParameter(value = "s", required = true) private int sprint;
	public int getSprint() {
		return sprint;
	}

	@CommandLineParameter(value = "t", required = true) private int team;
	public int getTeam() {
		return team;
	}

	@CommandLineParameter(value = "v") private boolean verify;
	public boolean isVerify() {
		return verify;
	}

	@CommandLineParameter(value = "p") private boolean prod;
	public boolean isProd() {
		return prod;
	}

	@CommandLineParameter(value = "i") private boolean isd;
	public boolean isIsd() {
		return isd;
	}

	@CommandLineParameter(value = "e") private boolean export;
	public boolean isExport() {
		return export;
	}


}
