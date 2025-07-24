package io.openems.edge.simulator.datasource.csv.predefined;

public enum Source {
	ZERO("zero.csv"), //
	H0_HOUSEHOLD_SUMMER_WEEKDAY_STANDARD_LOAD_PROFILE("h0-summer-weekday-standard-load-profile.csv"), //
	H0_HOUSEHOLD_SUMMER_WEEKDAY_PV_PRODUCTION("h0-summer-weekday-pv-production.csv"), //
	H0_HOUSEHOLD_SUMMER_WEEKDAY_NON_REGULATED_CONSUMPTION("h0-summer-weekday-non-regulated-consumption.csv"), //
	H0_HOUSEHOLD_SUMMER_WEEKDAY_PV_PRODUCTION2("h0-summer-weekday-pv-production2.csv"), //
	MINI_GRID_LOAD("MINI_GRID_LOAD.csv"), //
	PV_PROD_METER_ACTING("PV_PROD_METER_ACTING.csv");

	public final String filename;

	private Source(String filename) {
		this.filename = filename;
	}
}
