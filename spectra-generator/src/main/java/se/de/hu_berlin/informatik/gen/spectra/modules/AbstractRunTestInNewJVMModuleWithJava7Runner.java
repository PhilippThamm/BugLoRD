/**
 *
 */
package se.de.hu_berlin.informatik.gen.spectra.modules;

import se.de.hu_berlin.informatik.java7.testrunner.TestWrapper;
import se.de.hu_berlin.informatik.java7.testrunner.UnitTestRunnerNoServer;
import se.de.hu_berlin.informatik.junittestutils.data.StatisticsData;
import se.de.hu_berlin.informatik.junittestutils.data.TestStatistics;
import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.miscellaneous.Pair;
import se.de.hu_berlin.informatik.utils.processors.basics.ExecuteMainClassInNewJVM;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

/**
 * Runs a single test inside a new JVM and generates statistics. A timeout may
 * be set such that each executed test that runs longer than this timeout will
 * be aborted and will count as failing.
 *
 * <p>
 * if the test can't be run at all, this information is given in the returned
 * statistics, together with an error message.
 *
 * @author Simon Heiden
 */
public abstract class AbstractRunTestInNewJVMModuleWithJava7Runner<T extends Serializable>
        extends AbstractRunTestInNewJVMModule<T> {

    final private ExecuteMainClassInNewJVM executeModule;

    final private String[] args;

    public AbstractRunTestInNewJVMModuleWithJava7Runner(final String testOutput,
                                                        final boolean debugOutput, final Long timeout, final int repeatCount,
                                                        String instrumentedClassPath, final Path dataFile, final String javaHome, File projectDir,
                                                        String[] customJvmArgs, String... properties) {
        super(testOutput);

        String[] jvmArgs = Misc.joinArrays(customJvmArgs, properties);
        
        this.executeModule = new ExecuteMainClassInNewJVM(
                javaHome,
                UnitTestRunnerNoServer.class,
                instrumentedClassPath,
                projectDir,
                jvmArgs)
                .setEnvVariable("LC_ALL", "en_US.UTF-8")
                .setEnvVariable("TZ", "America/Los_Angeles");

        int arrayLength = 3;
        if (timeout != null) {
            ++arrayLength;
        }
//		if (!debugOutput) {
//			++arrayLength;
//		}

        args = new String[arrayLength];

        args[2] = getStatisticsResultFile().toString();

        if (timeout != null) {
            args[3] = String.valueOf(timeout);
        }

    }

    @Override
    public String[] getArgs(String testClassName, String testMethodName) {
        args[0] = testClassName;
        args[1] = testMethodName;
        return args;
    }

    @Override
    public ExecuteMainClassInNewJVM getMain() {
        return executeModule;
    }

    @Override
    public Pair<TestStatistics, T> getResultAfterTest(TestWrapper testWrapper, int executionResult) {
        TestStatistics statistics = new TestStatistics();
        T projectData = null;
        //see if the test was executed and finished execution normally
        if (executionResult == UnitTestRunnerNoServer.TEST_SUCCESSFUL ||
                executionResult == UnitTestRunnerNoServer.TEST_FAILED) {
            statistics.addStatisticsElement(StatisticsData.COULD_BE_FINISHED, 1);
            if (executionResult == UnitTestRunnerNoServer.TEST_SUCCESSFUL) {
                statistics.addStatisticsElement(StatisticsData.IS_SUCCESSFUL, true);
            } else {
                statistics.addStatisticsElement(StatisticsData.IS_SUCCESSFUL, false);
            }
            // wait for some milliseconds
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // do nothing
            }
            projectData = getDataForExecutedTest();
            if (projectData == null) {
                statistics.addStatisticsElement(StatisticsData.COVERAGE_GENERATION_FAILED, 1);
            }
        } else if (executionResult == UnitTestRunnerNoServer.TEST_TIMEOUT) {
            statistics.addStatisticsElement(StatisticsData.TIMEOUT_OCCURRED, 1);
        } else if (executionResult == UnitTestRunnerNoServer.TEST_EXCEPTION) {
            statistics.addStatisticsElement(StatisticsData.EXCEPTION_OCCURRED, 1);
        }

        return new Pair<>(statistics, projectData);
    }

    public abstract T getDataForExecutedTest();

}
