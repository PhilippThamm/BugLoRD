package se.de.hu_berlin.informatik.gen.spectra;

import se.de.hu_berlin.informatik.gen.spectra.modules.AbstractRunSingleTestAndReportModule;
import se.de.hu_berlin.informatik.junittestutils.data.StatisticsData;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.optionparser.OptionParser;
import se.de.hu_berlin.informatik.utils.processors.AbstractConsumingProcessor;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;
import se.de.hu_berlin.informatik.utils.statistics.StatisticsCollector;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * A factory that has to provide methods for providing necessary functionality
 * to generate program spectra by running tests and collecting coverage.
 *
 * @param <T> the type of coverage data object that is collected/handled
 * @param <R> the type of coverage report object that is generated based on the coverage data in the end
 * @param <S> the type of spectra that is generated (and saved to disk)
 * @author Simon
 */
public abstract class AbstractSpectraGenerationFactory<T extends Serializable, R, S> {

    public static final String MAX_HEAP = "-Xmx6g";
    public static final String INITIAL_HEAP = ""; //"-Xms1g";
    public static final String GC = "-XX:+UseG1GC"; //"-XX:+UseParallelGC";
    public static final String MAX_DIRECT_MEMORY = ""; //"-XX:MaxDirectMemorySize=512m";
    public static final String NUMA = ""; //"-XX:+UseNUMA";
    
    public static final String MAX_HEAP_SMALL = "-Xmx2g";
    public static final String INITIAL_HEAP_SMALL = ""; //"-Xms1g";
    public static final String GC_SMALL = "-XX:+UseG1GC"; //"-XX:+UseParallelGC";
    public static final String MAX_DIRECT_MEMORY_SMALL = ""; //"-XX:MaxDirectMemorySize=512m";
    public static final String NUMA_SMALL = ""; //"-XX:+UseNUMA";
    
    private static String[] getDefaultJVMConfigArguments() {
    	return new String[] {INITIAL_HEAP, MAX_HEAP, GC, MAX_DIRECT_MEMORY, NUMA};  
    }
    
    private static String[] getDefaultSmallJVMConfigArguments() {
    	return new String[] {INITIAL_HEAP_SMALL, MAX_HEAP_SMALL, GC_SMALL, MAX_DIRECT_MEMORY_SMALL, NUMA_SMALL};  
    }

	private String[] customSmallJvmArgs;
    
    public void setCustomSmallJvmArgs(String[] customJvmArgs) {
		this.customSmallJvmArgs = customJvmArgs;
	}
	
	private String[] customJvmArgs;
    
    public void setCustomJvmArgs(String[] customJvmArgs) {
		this.customJvmArgs = customJvmArgs;
	}
	
	public String[] getJVMConfigArguments() {
    	String[] args = customJvmArgs == null ? getDefaultJVMConfigArguments() : customJvmArgs;
    	Log.out(this, "Main JVM arguments: " + Arrays.toString(args));
		return args;
    }
	
	public String[] getSmallJVMConfigArguments() {
    	String[] args = customSmallJvmArgs == null ? getDefaultSmallJVMConfigArguments() : customSmallJvmArgs;
    	Log.out(this, "Test Runner JVM arguments: " + Arrays.toString(args));
		return args;
    }

//	private static void addArg(StringBuilder sb, String arg) {
//		if (!arg.isEmpty()) {
//    		sb.append(arg).append(" ");
//    	}
//	}

    /**
     * Defines which tool (or modified tool...) to use.
     * Add new tools as options here and update the switch statement
     * in the main method of RunAllTestsAndGenSpectra.java.
     * The return value of the toString() method is used as an option parameter value,
     * so it needs to be unique.
     */
    public enum Strategy {
        COBERTURA,
        JACOCO,
        TRACE_COBERTURA;

        @Override
        public String toString() {
            switch (this) {
                case TRACE_COBERTURA:
                    return "trace_cobertura";
                case COBERTURA:
                    return "cobertura";
                case JACOCO:
                    return "jacoco";
                default:
                    throw new UnsupportedOperationException("Not implemented.");
            }
        }
    }

    /**
     * @return the strategy identifier
     */
    public abstract Strategy getStrategy();

    /**
     * Provides an "instrumenter" object that provides methods to instrument binary
     * class files, if necessary, so that coverage can be collected afterwards after
     * running tests and using the instrumented class files.
     *
     * @param projectDir      path to the main project directory
     * @param outputDir       path to which the instrumented classes shall be written to
     * @param classPath       a class path that is necessary for loading the binaries
     * @param pathsToBinaries paths to the binaries
     * @return the instrumenter object
     */
    public abstract AbstractInstrumenter getInstrumenter(final Path projectDir, final String outputDir,
                                                         String classPath, String... pathsToBinaries);

    /**
     * May provide paths to additional resources that are specific to the used coverage generation
     * tool and have to be added to the class path.
     *
     * @return the additional paths; null otherwise
     */
    public abstract String[] getElementsToAddToTestClassPathForMainTestRunner();

    /**
     * May provide properties for the execution of the main test runner that are specific to the used tool.
     *
     * @param projectDir     the main project directory
     * @param useSeparateJVM whether a separate JVM is always used to run the tests
     * @return the properties; null otherwise
     */
    public abstract String[] getPropertiesForMainTestRunner(final Path projectDir, boolean useSeparateJVM);

    /**
     * May provide arguments for the execution of the main test runner that are specific to the used tool.
     *
     * @return the arguments; null otherwise
     */
    public abstract String[] getSpecificArgsForMainTestRunner();

    /**
     * Gets a module that runs a single test and generates a coverage report in the end.
     *
     * @param options                      an object that holds options relevant to execution
     * @param testAndInstrumentClassLoader a class loader that is used to run the test (loads the instrumented classes, specifically)
     * @param testClassPath                the class path that is necessary for execution of the test classes
     * @param statisticsContainer          a container for collecting statistics
     * @return the test runner module
     */
    public abstract AbstractRunSingleTestAndReportModule<T, R> getTestRunnerModule(OptionParser options,
                                                                                   ClassLoader testAndInstrumentClassLoader, String testClassPath, StatisticsCollector<StatisticsData> statisticsContainer);

    /**
     * Gets a module that collects all the reports generated by running the tests and generates
     * a spectra at the end, when all tests have been run.
     *
     * @param options             an object that holds options relevant to execution
     * @param statisticsContainer a container for collecting statistics
     * @return the module to generate a spectra from the given reports
     */
    public abstract AbstractProcessor<R, S> getReportToSpectraProcessor(
            OptionParser options, StatisticsCollector<StatisticsData> statisticsContainer);

    /**
     * Gets a module that handles the resulting spectra after its creation. (Storing the spectra to disk, etc.)
     *
     * @param options an object that holds options relevant to execution
     * @return the module
     */
    public abstract AbstractConsumingProcessor<S> getSpectraProcessor(
            OptionParser options);

}
