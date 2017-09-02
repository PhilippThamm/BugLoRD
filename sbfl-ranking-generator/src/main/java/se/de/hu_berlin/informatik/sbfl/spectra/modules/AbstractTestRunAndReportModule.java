/**
 * 
 */
package se.de.hu_berlin.informatik.sbfl.spectra.modules;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import se.de.hu_berlin.informatik.java7.testrunner.TestWrapper;
import se.de.hu_berlin.informatik.junittestutils.data.StatisticsData;
import se.de.hu_berlin.informatik.junittestutils.data.TestStatistics;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.Pair;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;
import se.de.hu_berlin.informatik.utils.processors.sockets.ProcessorSocket;
import se.de.hu_berlin.informatik.utils.statistics.StatisticsCollector;

/**
 * 
 * 
 * @author Simon Heiden
 */
public abstract class AbstractTestRunAndReportModule<T extends Serializable, R> extends AbstractProcessor<TestWrapper, R> {

	final private StatisticsCollector<StatisticsData> statisticsContainer;
	
	final private static int UNDEFINED_COVERAGE = 0;
	final private static int UNFINISHED_EXECUTION = 1;
	final private static int WRONG_COVERAGE = 2;
	final private static int CORRECT_EXECUTION = 3;

	private int currentState = UNDEFINED_COVERAGE;
	
	final private boolean alwaysUseSeparateJVM;
	
	private int testCounter = 0;
	
	final private Set<String> knownFailingtests;
	private int failedTestCounter = 0;
	private boolean testErrorOccurred = false;
	
	private AbstractTestRunLocallyModule<T> testRunLocallyModule;
	private AbstractTestRunInNewJVMModule<T> testRunInNewJVMModule;
	private AbstractTestRunInNewJVMModuleWithJava7Runner<T> testRunInNewJVMModuleWithJava7Runner;

	public AbstractTestRunAndReportModule(final String testOutput, 
			final boolean debugOutput, Long timeout, final int repeatCount,
			boolean useSeparateJVMalways, String[] failingtests,
			final StatisticsCollector<StatisticsData> statisticsContainer, ClassLoader cl) {
		super();
		if (failingtests == null) {
			knownFailingtests = null;
		} else {
			knownFailingtests = new HashSet<>();
			addKnownFailingTests(failingtests);
		}
		
		this.statisticsContainer = statisticsContainer;

		this.alwaysUseSeparateJVM = useSeparateJVMalways;
		
	}
	
	private AbstractTestRunLocallyModule<T> getTestRunLocallyModule() {
		if (testRunLocallyModule == null) {
			testRunLocallyModule = newTestRunLocallyModule();
		}
		return testRunLocallyModule;
	}
	
	private AbstractTestRunInNewJVMModule<T> getTestRunInNewJVMModule() {
		if (testRunInNewJVMModule == null) {
			testRunInNewJVMModule = newTestRunInNewJVMModule();
		}
		return testRunInNewJVMModule;
	}
	
	private AbstractTestRunInNewJVMModuleWithJava7Runner<T> getTestRunInNewJVMModuleWithJava7Runner() {
		if (testRunInNewJVMModuleWithJava7Runner == null) {
			testRunInNewJVMModuleWithJava7Runner = newTestRunInNewJVMModuleWithJava7Runner();
		}
		return testRunInNewJVMModuleWithJava7Runner;
	}
	
	public abstract AbstractTestRunInNewJVMModule<T> newTestRunInNewJVMModule();
	
	public abstract AbstractTestRunLocallyModule<T> newTestRunLocallyModule();
	
	public abstract AbstractTestRunInNewJVMModuleWithJava7Runner<T> newTestRunInNewJVMModuleWithJava7Runner();	

	private void addKnownFailingTests(String[] failingtests) {
		for (String failingTest : failingtests) {
			// format: qualified.class.name::TestMethodName
			String[] split = failingTest.split("::");
			if (split.length == 2) {
				knownFailingtests.add(failingTest);
			} else {
				Log.err(this, "Given failing test has wrong format: '%s'", failingTest);
			}
		}
	}

	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	@Override
	public R processItem(final TestWrapper testWrapper, ProcessorSocket<TestWrapper, R> socket) {
		socket.allowOnlyForcedTracks();
		socket.forceTrack(testWrapper.toString());
		++testCounter;
//		Log.out(this, "Now processing: '%s'.", testWrapper);
		
		TestStatistics testStatistics = new TestStatistics();

		currentState = UNDEFINED_COVERAGE;
		
		T projectData;
		if (alwaysUseSeparateJVM) {
			projectData = runTestLocallyOrElseInJVM(testWrapper, testStatistics, false);
		} else {
			projectData = runTestLocallyOrElseInJVM(testWrapper, testStatistics, true);
		}
		
		// check for "correct" (intended) test execution
		testErrorOccurred |= testResultErrorOccurred(testWrapper, testStatistics, true);

		if (testStatistics.getErrorMsg() != null) {
			Log.err(this, testStatistics.getErrorMsg());
		}
		
		if (statisticsContainer != null) {
			statisticsContainer.addStatistics(testStatistics);
		}

		if (isCorrectData(projectData)) {
			return generateReport(testWrapper, testStatistics, projectData);
		} else {
			return null;
		}
	}

	public abstract R generateReport(TestWrapper testWrapper, TestStatistics testStatistics, T data);

	private boolean testResultErrorOccurred(final TestWrapper testWrapper, TestStatistics testStatistics, boolean log) {
		// check for "correct" (intended) test execution
		String testName = testWrapper.toString();
		if (testStatistics.couldBeFinished()) {
			if (testStatistics.coverageGenerationFailed()) {
				if (log) {
					testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
							"Test '" + testName + "' coverage generation failed.");
				}
				return true;
			} else {
				if (knownFailingtests != null) {
					if (testStatistics.wasSuccessful()) {
						if (knownFailingtests.contains(testName)) {
							if (log) {
								testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
										"Test '" + testName + "' was successful but should fail.");
							}
							return true;
						}
					} else {
						if (knownFailingtests.contains(testName)) {
							++failedTestCounter;
						} else {
							if (log) {
								testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
										"Test '" + testName + "' failed but should be successful.");
							}
							return true;
						}
					}
				}
			}
		} else {
			if (testStatistics.timeoutOccurred()) {
				if (log) {
					testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
							"Test '" + testName + "' had a timeout.");
				}
			}
			if (testStatistics.exceptionOccured()) {
				if (log) {
					testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
							"Test '" + testName + "' had an exception.");
				}
			}
			if (testStatistics.coverageGenerationFailed()) {
				if (log) {
					testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
							"Test '" + testName + "' coverage generation failed.");
				}
			}
			if (testStatistics.wasInterrupted()) {
				if (log) {
					testStatistics.addStatisticsElement(StatisticsData.ERROR_MSG, 
							"Test '" + testName + "' had an exception.");
				}
			}
			return true;
		}

		return false;
	}

	private boolean isCorrectData(T projectData) {
		return projectData != null && 
				currentState != WRONG_COVERAGE && 
				currentState != UNDEFINED_COVERAGE && 
				currentState != UNFINISHED_EXECUTION;
	}
	
	private T runTestLocallyOrElseInJVM(final TestWrapper testWrapper, 
			final TestStatistics testStatistics, boolean runLocally) {
		T projectData = null;
		if (runLocally) {
			projectData = runTestWithRunner(testWrapper, testStatistics, getTestRunLocallyModule());
		}
		
		if(!isCorrectData(projectData) || testResultErrorOccurred(testWrapper, testStatistics, false)) {
			currentState = UNDEFINED_COVERAGE;
			
			if (runLocally) {
				Log.out(this, "Running test in separate JVM due to error: %s", testWrapper);
			}
			projectData = runTestWithRunner(testWrapper, testStatistics, getTestRunInNewJVMModule());
			testStatistics.addStatisticsElement(StatisticsData.SEPARATE_JVM, 1);
			
			if(!isCorrectData(projectData) || testResultErrorOccurred(testWrapper, testStatistics, false)) {
				currentState = UNDEFINED_COVERAGE;
				
				Log.out(this, "Running test in separate JVM with Java 7 due to error: %s", testWrapper);
				projectData = runTestWithRunner(testWrapper, testStatistics, getTestRunInNewJVMModuleWithJava7Runner());
			}
		}
		
		return projectData;
	}
	
	private T runTestWithRunner(TestWrapper testWrapper, TestStatistics testStatistics, AbstractProcessor<TestWrapper, Pair<TestStatistics, T>> testrunner) {
		T projectData = null;
//		FileUtils.delete(dataFile);
		//(try to) run the test in new JVM and get the statistics
		Pair<TestStatistics, T> testResult = testrunner.submit(testWrapper).getResult();
		testStatistics.mergeWith(testResult.first());
		
		//see if the test was executed and finished execution normally
		if (testResult.first().couldBeFinished()) {
			if (testResult.first().coverageGenerationFailed() 
					|| testResult.second() == null) {
				currentState = WRONG_COVERAGE;
			} else {
				projectData = testResult.second();
				currentState = CORRECT_EXECUTION;
			}
		} else {
			currentState = UNFINISHED_EXECUTION;
		}
		return projectData;
	}

	public abstract R getErrorReport();
	
	@Override
	public R getResultFromCollectedItems() {
		// in the end, check if number of failing tests is correct (if given)
		if (testErrorOccurred) {
			Log.err(this, "Some test execution had the wrong result!");
			return getErrorReport();
		}
		if (knownFailingtests != null) {
			if (knownFailingtests.size() > failedTestCounter) {
				Log.err(this, "Not all specified failing tests have been executed! Expected: %d, Actual: %d", 
						knownFailingtests.size(), failedTestCounter);
				return getErrorReport();
			}
		}
		return super.getResultFromCollectedItems();
	}

	@Override
	public boolean finalShutdown() {
		if (testRunLocallyModule != null) {
			testRunLocallyModule.finalShutdown();
		}
		if (testRunInNewJVMModule != null) {
			testRunInNewJVMModule.finalShutdown();
		}
		if (testRunInNewJVMModuleWithJava7Runner != null) {
			testRunInNewJVMModuleWithJava7Runner.finalShutdown();
		}
		return super.finalShutdown();
	}

}