package se.de.hu_berlin.informatik.c2r;

import se.de.hu_berlin.informatik.utils.statistics.StatisticsAPI;
import se.de.hu_berlin.informatik.utils.statistics.StatisticsOptions;

public enum StatisticsData implements StatisticsAPI {
	COUNT("executed tests", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	DURATION("test duration (ms)", StatisticType.DOUBLE_VALUE, StatisticsOptions.PREF_BIGGER),
	DIFFERENT_COVERAGE("tests with varying coverage", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	WRONG_COVERAGE("possibly wrong coverage", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	IS_SUCCESSFUL("was successful", StatisticType.BOOLEAN, StatisticsOptions.PREF_FALSE),
	TIMEOUT_OCCURRED("timeout occured", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	EXCEPTION_OCCURRED("exception occurred", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	WAS_INTERRUPTED("was interrupted", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	COULD_BE_FINISHED("could be finished", StatisticType.COUNT, StatisticsOptions.PREF_BIGGER),
	FAILED_TEST_COVERAGE("failed test coverage", StatisticType.STRING, StatisticsOptions.CONCAT),
	ERROR_MSG("error message(s)", StatisticType.STRING, StatisticsOptions.CONCAT);

	final private String label;
	final private StatisticType type;
	final private StatisticsOptions[] options;
	private StatisticsData(String label, StatisticType type, StatisticsOptions... options) {
		this.label = label;
		this.type = type;
		this.options = options;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public StatisticType getType() {
		return type;
	}

	@Override
	public StatisticsOptions[] getOptions() {
		return options;
	}
}