/**
 * 
 */
package se.de.hu_berlin.informatik.c2r;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.jacoco.agent.AgentJar;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.junit.runner.Request;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import se.de.hu_berlin.informatik.benchmark.api.BugLoRDConstants;
import se.de.hu_berlin.informatik.c2r.modules.JaCoCoAddReportToProviderAndGenerateSpectraModule;
import se.de.hu_berlin.informatik.c2r.modules.JaCoCoTestRunAndReportModule;
import se.de.hu_berlin.informatik.c2r.modules.TraceFileModule;
import se.de.hu_berlin.informatik.stardust.localizer.SourceCodeBlock;
import se.de.hu_berlin.informatik.stardust.spectra.INode;
import se.de.hu_berlin.informatik.stardust.spectra.manipulation.FilterSpectraModule;
import se.de.hu_berlin.informatik.stardust.spectra.manipulation.SaveSpectraModule;
import se.de.hu_berlin.informatik.utils.files.FileUtils;
import se.de.hu_berlin.informatik.utils.files.processors.FileLineProcessor;
import se.de.hu_berlin.informatik.utils.files.processors.FileLineProcessor.StringProcessor;
import se.de.hu_berlin.informatik.utils.miscellaneous.ClassPathParser;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapperInterface;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;
import se.de.hu_berlin.informatik.utils.processors.basics.ExecuteMainClassInNewJVM;
import se.de.hu_berlin.informatik.utils.processors.sockets.ProcessorSocket;
import se.de.hu_berlin.informatik.utils.processors.sockets.pipe.PipeLinker;
import se.de.hu_berlin.informatik.utils.statistics.StatisticsCollector;
import se.de.hu_berlin.informatik.utils.optionparser.OptionParser;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapper;


/**
 * Computes SBFL rankings or hit traces from a list of tests or a list of test classes
 * with the support of the stardust API.
 * Instruments given classes with JaCoCo and may list all tests of given test classes
 * at the beginning for convenience.
 * 
 * @author Simon Heiden
 */
final public class JaCoCoToSpectra {

	private JaCoCoToSpectra() {
		//disallow instantiation
	}

	public static enum CmdOptions implements OptionWrapperInterface {
		/* add options here according to your needs */
		JAVA_HOME_DIR("java", "javaHomeDir", true, "Path to a Java home directory (at least v1.8). Set if you encounter any version problems. "
				+ "If not set, the default JRE is used.", false),
		CLASS_PATH("cp", "classPath", true, "An additional class path which may be needed for the execution of tests. "
				+ "Will be appended to the regular class path if this option is set.", false),
		TIMEOUT("tm", "timeout", true, "A timeout (in seconds) for the execution of each test. Tests that run "
				+ "longer than the timeout will abort and will count as failing.", false),
		REPEAT_TESTS("r", "repeatTests", true, "Execute each test a set amount of times to (hopefully) "
				+ "generate correct coverage data. Default is '1'.", false),
		FULL_SPECTRA("f", "fullSpectra", false, "Set this if a full spectra should be generated with all executable statements. Otherwise, only "
				+ "these statements are included that are executed by at least one test case.", false),
		SEPARATE_JVM("jvm", "separateJvm", false, "Set this if each test shall be run in a separate JVM.", false),
		TEST_LIST("t", "testList", true, "File with all tests to execute.", 0),
		TEST_CLASS_LIST("tc", "testClassList", true, "File with a list of test classes from which all tests shall be executed.", 0),
		INSTRUMENT_CLASSES(Option.builder("c").longOpt("classes").required()
				.hasArgs().desc("A list of classes/directories to instrument with JaCoCo.").build()),
		PROJECT_DIR("pd", "projectDir", true, "Path to the directory of the project under test.", true),
		SOURCE_DIR("sd", "sourceDir", true, "Relative path to the main directory containing the sources from the project directory.", true),
		TEST_CLASS_DIR("td", "testClassDir", true, "Relative path to the main directory containing the needed test classes from the project directory.", true),
		OUTPUT("o", "output", true, "Path to output directory.", true);

		/* the following code blocks should not need to be changed */
		final private OptionWrapper option;

		//adds an option that is not part of any group
		CmdOptions(final String opt, final String longOpt, 
				final boolean hasArg, final String description, final boolean required) {
			this.option = new OptionWrapper(
					Option.builder(opt).longOpt(longOpt).required(required).
					hasArg(hasArg).desc(description).build(), NO_GROUP);
		}

		//adds an option that is part of the group with the specified index (positive integer)
		//a negative index means that this option is part of no group
		//this option will not be required, however, the group itself will be
		CmdOptions(final String opt, final String longOpt, 
				final boolean hasArg, final String description, final int groupId) {
			this.option = new OptionWrapper(
					Option.builder(opt).longOpt(longOpt).required(false).
					hasArg(hasArg).desc(description).build(), groupId);
		}

		//adds the given option that will be part of the group with the given id
		CmdOptions(final Option option, final int groupId) {
			this.option = new OptionWrapper(option, groupId);
		}

		//adds the given option that will be part of no group
		CmdOptions(final Option option) {
			this(option, NO_GROUP);
		}

		@Override public String toString() { return option.getOption().getOpt(); }
		@Override public OptionWrapper getOptionWrapper() { return option; }
	}

	/**
	 * @param args
	 * command line arguments
	 */
	public static void main(final String[] args) {

		final OptionParser options = OptionParser.getOptions("JaCoCoToSpectra", false, CmdOptions.class, args);

		final Path projectDir = options.isDirectory(CmdOptions.PROJECT_DIR, true);
		options.isDirectory(projectDir, CmdOptions.SOURCE_DIR, true);
		final Path testClassDir = options.isDirectory(projectDir, CmdOptions.TEST_CLASS_DIR, true);
		final String outputDir = options.isDirectory(CmdOptions.OUTPUT, false).toAbsolutePath().toString();

		//		if (!options.hasOption("ht") && options.getOptionValues('l') == null) {
		//			Misc.err("No localizers given. Only generating the compressed spectra.");
		//		}

		final Path instrumentedDir = Paths.get(outputDir, "instrumented").toAbsolutePath();

		final String[] classesToInstrument = options.getOptionValues(CmdOptions.INSTRUMENT_CLASSES);

		final String javaHome = options.getOptionValue(CmdOptions.JAVA_HOME_DIR, null);
		
		String systemClassPath = new ClassPathParser().parseSystemClasspath().getClasspath();
		
		String testClassPath = options.getOptionValue(CmdOptions.CLASS_PATH, null);
		
		if (testClassPath != null) {
			List<URL> testClassPathList = new ClassPathParser().addClassPathToClassPath(testClassPath).getUniqueClasspathElements();
			
			ClassPathParser reducedtestClassPath = new ClassPathParser();
			for (URL element : testClassPathList) {
				String path = element.getPath().toLowerCase();
				if (//path.contains("junit") || 
						path.contains("cobertura")) {
					Log.out(JaCoCoToSpectra.class, "filtered out '%s'.", path);
					continue;
				}
				reducedtestClassPath.addElementToClassPath(element);
			}
			testClassPath = reducedtestClassPath.getClasspath();
		}


		/* #====================================================================================
		 * # instrumentation
		 * #==================================================================================== */
		
		//build arguments for instrumentation
		String[] instrArgs = { 
				Instrument.CmdOptions.OUTPUT.asArg(), Paths.get(outputDir).toAbsolutePath().toString()};

		if (testClassPath != null) {
			instrArgs = Misc.addToArrayAndReturnResult(instrArgs, 
					Instrument.CmdOptions.CLASS_PATH.asArg(), testClassPath);
		}

		if (classesToInstrument != null) {
			instrArgs = Misc.addToArrayAndReturnResult(instrArgs, Instrument.CmdOptions.INSTRUMENT_CLASSES.asArg());
			instrArgs = Misc.joinArrays(instrArgs, classesToInstrument);
		}

		//final File coberturaDataFile = Paths.get(outputDir, "cobertura.ser").toAbsolutePath().toFile();

		//we need to run the tests in a new jvm that uses the given Java version
		int instrumentationResult = new ExecuteMainClassInNewJVM(javaHome, 
				Instrument.class, 
				//classPath,
				systemClassPath + (testClassPath != null ? File.pathSeparator + testClassPath : ""),
				projectDir.toFile()//, 
				//"-Dnet.sourceforge.cobertura.datafile=" + coberturaDataFile.getAbsolutePath().toString()
				)
				.submit(instrArgs)
				.getResult();

		if (instrumentationResult != 0) {
			Log.abort(JaCoCoToSpectra.class, "Instrumentation failed.");
		}

		
		/* #====================================================================================
		 * # generate class path for test execution
		 * #==================================================================================== */

		//generate modified class path with instrumented classes at the beginning
		final ClassPathParser cpParser = new ClassPathParser()
//				.parseSystemClasspath()
				.addElementAtStartOfClassPath(testClassDir.toAbsolutePath().toFile());
		for (final String item : classesToInstrument) {
			cpParser.addElementAtStartOfClassPath(Paths.get(item).toAbsolutePath().toFile());
		}
		cpParser.addElementAtStartOfClassPath(instrumentedDir.toAbsolutePath().toFile());
		String testAndInstrumentClassPath = cpParser.getClasspath();

		//append a given class path for any files that are needed to run the tests
		testAndInstrumentClassPath += (testClassPath != null ? File.pathSeparator + testClassPath : "");

		File jacocoAgentJar = null; 
		try {
			jacocoAgentJar = AgentJar.extractToTempLocation();
		} catch (IOException e) {
			Log.abort(JaCoCoToSpectra.class, e, "Could not create JaCoCo agent jar file.");
		}
		
		testAndInstrumentClassPath += (jacocoAgentJar != null ? File.pathSeparator + jacocoAgentJar.getAbsolutePath() : "");
		
		/* #====================================================================================
		 * # run tests and generate spectra
		 * #==================================================================================== */
		
		//build arguments for the "real" application (running the tests...)
		String[] newArgs = { 
				RunTestsAndGenSpectra.CmdOptions.PROJECT_DIR.asArg(), options.getOptionValue(CmdOptions.PROJECT_DIR), 
				RunTestsAndGenSpectra.CmdOptions.SOURCE_DIR.asArg(), options.getOptionValue(CmdOptions.SOURCE_DIR),
				RunTestsAndGenSpectra.CmdOptions.OUTPUT.asArg(), Paths.get(outputDir).toAbsolutePath().toString(),
				RunTestsAndGenSpectra.CmdOptions.CLASS_PATH.asArg(), testAndInstrumentClassPath,
				RunTestsAndGenSpectra.CmdOptions.ORIGINAL_CLASSES.asArg()};
		
		newArgs = Misc.joinArrays(newArgs, classesToInstrument);
		
		if (javaHome != null) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, javaHome);
		}

		if (options.hasOption(CmdOptions.TEST_CLASS_LIST)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.TEST_CLASS_LIST.asArg(), String.valueOf(options.getOptionValue(CmdOptions.TEST_CLASS_LIST)));
		} else if (options.hasOption(CmdOptions.TEST_LIST)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.TEST_LIST.asArg(), String.valueOf(options.getOptionValue(CmdOptions.TEST_LIST)));
		}
		
		if (options.hasOption(CmdOptions.FULL_SPECTRA)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.FULL_SPECTRA.asArg());
		}
		
		if (options.hasOption(CmdOptions.SEPARATE_JVM)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.SEPARATE_JVM.asArg());
		}
		
		if (options.hasOption(CmdOptions.TIMEOUT)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.TIMEOUT.asArg(), String.valueOf(options.getOptionValue(CmdOptions.TIMEOUT)));
		}
		
		if (options.hasOption(CmdOptions.REPEAT_TESTS)) {
			newArgs = Misc.addToArrayAndReturnResult(newArgs, RunTestsAndGenSpectra.CmdOptions.REPEAT_TESTS.asArg(), String.valueOf(options.getOptionValue(CmdOptions.REPEAT_TESTS)));
		}
		
//		Log.out(CoberturaToSpectra.class, systemClassPath);
//		
//		List<File> systemCPList = new ClassPathParser().parseSystemClasspath().getUniqueClasspathElements();
//		
//		ClassPathParser reducedSystemCP = new ClassPathParser();
//		for (File element : systemCPList) {
//			String path = element.toString().toLowerCase();
//			if (//path.contains("junit") || 
//					path.contains("mockito")) {
//				Log.out(CoberturaToSpectra.class, "filtered out '%s'.", path);
//				continue;
//			}
//			reducedSystemCP.addElementToClassPath(element);
//		}
		
		//we need to run the tests in a new jvm that uses the given Java version
		new ExecuteMainClassInNewJVM(javaHome, 
				RunTestsAndGenSpectra.class,
				testAndInstrumentClassPath + File.pathSeparator + 
				systemClassPath,
//				reducedSystemCP.getClasspath(),
//				new ClassPathParser().parseSystemClasspath().getClasspath(),
				projectDir.toFile(), 
//				"-Dnet.sourceforge.cobertura.datafile=" + coberturaDataFile.getAbsolutePath().toString(), 
				"-Djacoco-agent.dumponexit=false", 
				"-Djacoco-agent.output=tcpserver",
				"-Djacoco-agent.excludes=*",
				"-XX:+UseNUMA", "-XX:+UseConcMarkSweepGC"//, "-Xmx2G"
				)
		.setEnvVariable("TZ", "America/Los_Angeles")
		.submit(newArgs);

		
		/* #====================================================================================
		 * # delete instrumented classes
		 * #==================================================================================== */
		
		FileUtils.delete(instrumentedDir);

	}

	public final static class Instrument {

		private Instrument() {
			//disallow instantiation
		}

		public static enum CmdOptions implements OptionWrapperInterface {
			/* add options here according to your needs */
			CLASS_PATH("cp", "classPath", true, "An additional class path which may be needed for the execution of tests. "
					+ "Will be appended to the regular class path if this option is set.", false),
			INSTRUMENT_CLASSES(Option.builder("c").longOpt("classes").required()
					.hasArgs().desc("A list of classes/directories to instrument with Cobertura.").build()),
			OUTPUT("o", "output", true, "Path to output directory.", true);

			/* the following code blocks should not need to be changed */
			final private OptionWrapper option;

			//adds an option that is not part of any group
			CmdOptions(final String opt, final String longOpt, 
					final boolean hasArg, final String description, final boolean required) {
				this.option = new OptionWrapper(
						Option.builder(opt).longOpt(longOpt).required(required).
						hasArg(hasArg).desc(description).build(), NO_GROUP);
			}

			//adds an option that is part of the group with the specified index (positive integer)
			//a negative index means that this option is part of no group
			//this option will not be required, however, the group itself will be
			CmdOptions(final String opt, final String longOpt, 
					final boolean hasArg, final String description, final int groupId) {
				this.option = new OptionWrapper(
						Option.builder(opt).longOpt(longOpt).required(false).
						hasArg(hasArg).desc(description).build(), groupId);
			}

			//adds the given option that will be part of the group with the given id
			CmdOptions(final Option option, final int groupId) {
				this.option = new OptionWrapper(option, groupId);
			}

			//adds the given option that will be part of no group
			CmdOptions(final Option option) {
				this(option, NO_GROUP);
			}

			@Override public String toString() { return option.getOption().getOpt(); }
			@Override public OptionWrapper getOptionWrapper() { return option; }
		}
		
		private static Instrumenter instrumenter;
		
		private static List<File> source = new ArrayList<File>();

		/**
		 * @param args
		 * command line arguments
		 */
		public static void main(final String[] args) {

//			if (System.getProperty("net.sourceforge.cobertura.datafile") == null) {
//				Log.abort(Instrument.class, "Please include property '-Dnet.sourceforge.cobertura.datafile=.../cobertura.ser' in the application's call.");
//			}

			final OptionParser options = OptionParser.getOptions("Instrument", false, CmdOptions.class, args);

			final String outputDir = options.isDirectory(CmdOptions.OUTPUT, false).toString();
//			final Path coberturaDataFile = Paths.get(System.getProperty("net.sourceforge.cobertura.datafile"));
//			Log.out(Instrument.class, "Cobertura data file: '%s'.", coberturaDataFile);

			final Path instrumentedDir = Paths.get(outputDir, "instrumented").toAbsolutePath();
			final String[] classesToInstrument = options.getOptionValues(CmdOptions.INSTRUMENT_CLASSES);
			
			for (String file : classesToInstrument) {
				source.add(new File(file).getAbsoluteFile());
			}
			
			final File absoluteDest = instrumentedDir.toFile().getAbsoluteFile();
			instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
			int total = 0;
			for (final File s : source) {
				if (s.isFile()) {
					try {
						total += instrument(s, new File(absoluteDest, s.getName()));
//						Log.out(Instrument.class, "Instrumented %s.", s);
					} catch (IOException e) {
						Log.err(Instrument.class, e, "Could not instrument '%s' with target '%s'.", s, new File(absoluteDest, s.getName()));
					}
				} else {
					try {
						total += instrumentRecursive(s, absoluteDest);
					} catch (IOException e) {
						Log.err(Instrument.class, e, "Could not instrument folder '%s' with target '%s'.", s, absoluteDest);
					}
				}
			}
			Log.out(Instrument.class, "%s classes instrumented to %s.",
					Integer.valueOf(total), absoluteDest);
		}
		
		private static int instrumentRecursive(final File src, final File dest)
				throws IOException {
			int total = 0;
			if (src.isDirectory()) {
				for (final File child : src.listFiles()) {
					total += instrumentRecursive(child,
							new File(dest, child.getName()));
				}
			} else {
				total += instrument(src, dest);
//				Log.out(Instrument.class, "Instrumented %s.", src);
			}
			return total;
		}

		private static int instrument(final File src, final File dest) throws IOException {
			dest.getParentFile().mkdirs();
			final InputStream input = new FileInputStream(src);
			try {
				final OutputStream output = new FileOutputStream(dest);
				try {
					return instrumenter.instrumentAll(input, output,
							src.getAbsolutePath());
				} finally {
					output.close();
				}
			} catch (final IOException e) {
				dest.delete();
				throw e;
			} finally {
				input.close();
			}
	}

	}

	public final static class RunTestsAndGenSpectra {

		private RunTestsAndGenSpectra() {
			//disallow instantiation
		}

		public static enum CmdOptions implements OptionWrapperInterface {
			/* add options here according to your needs */
			JAVA_HOME_DIR("java", "javaHomeDir", true, "Path to a Java home directory (at least v1.8). Set if you encounter any version problems. "
					+ "If not set, the default JRE is used.", false),
			CLASS_PATH("cp", "classPath", true, "An additional class path which may be needed for the execution of tests. "
					+ "Will be appended to the regular class path if this option is set.", false),
			ORIGINAL_CLASSES(Option.builder("oc").longOpt("originalClasses")
	        		.hasArgs().desc("The original (not instrumented) classes.").required()
	        		.build()),
			TEST_LIST("t", "testList", true, "File with all tests to execute.", 0),
			TEST_CLASS_LIST("tc", "testClassList", true, "File with a list of test classes from which all tests shall be executed.", 0),
			TIMEOUT("tm", "timeout", true, "A timeout (in seconds) for the execution of each test. Tests that run "
					+ "longer than the timeout will abort and will count as failing.", false),
			REPEAT_TESTS("r", "repeatTests", true, "Execute each test a set amount of times to (hopefully) "
					+ "generate correct coverage data. Default is '1'.", false),
			FULL_SPECTRA("f", "fullSpectra", false, "Set this if a full spectra should be generated with all executable statements. Otherwise, only "
					+ "these statements are included that are executed by at least one test case.", false),
			SEPARATE_JVM("jvm", "separateJvm", false, "Set this if each test shall be run in a separate JVM.", false),
			PROJECT_DIR("pd", "projectDir", true, "Path to the directory of the project under test.", true),
			SOURCE_DIR("sd", "sourceDir", true, "Relative path to the main directory containing the sources from the project directory.", true),
			OUTPUT("o", "output", true, "Path to output directory.", true);

			/* the following code blocks should not need to be changed */
			final private OptionWrapper option;

			//adds an option that is not part of any group
			CmdOptions(final String opt, final String longOpt, 
					final boolean hasArg, final String description, final boolean required) {
				this.option = new OptionWrapper(
						Option.builder(opt).longOpt(longOpt).required(required).
						hasArg(hasArg).desc(description).build(), NO_GROUP);
			}

			//adds an option that is part of the group with the specified index (positive integer)
			//a negative index means that this option is part of no group
			//this option will not be required, however, the group itself will be
			CmdOptions(final String opt, final String longOpt, 
					final boolean hasArg, final String description, final int groupId) {
				this.option = new OptionWrapper(
						Option.builder(opt).longOpt(longOpt).required(false).
						hasArg(hasArg).desc(description).build(), groupId);
			}

			//adds the given option that will be part of the group with the given id
			CmdOptions(final Option option, final int groupId) {
				this.option = new OptionWrapper(option, groupId);
			}

			//adds the given option that will be part of no group
			CmdOptions(final Option option) {
				this(option, NO_GROUP);
			}

			@Override public String toString() { return option.getOption().getOpt(); }
			@Override public OptionWrapper getOptionWrapper() { return option; }
		}

		/**
		 * @param args
		 * command line arguments
		 */
		public static void main(final String[] args) {

//			if (System.getProperty("net.sourceforge.cobertura.datafile") == null) {
//				Log.abort(RunTestsAndGenSpectra.class, "Please include property '-Dnet.sourceforge.cobertura.datafile=.../cobertura.ser' in the application's call.");
//			}

			final OptionParser options = OptionParser.getOptions("RunTestsAndGenSpectra", false, CmdOptions.class, args);

			final Path projectDir = options.isDirectory(CmdOptions.PROJECT_DIR, true);
			final Path srcDir = options.isDirectory(projectDir, CmdOptions.SOURCE_DIR, true);
			final String outputDir = options.isDirectory(CmdOptions.OUTPUT, false).toString();
//			final Path coberturaDataFile = Paths.get(System.getProperty("net.sourceforge.cobertura.datafile"));
//			Log.out(RunTestsAndGenSpectra.class, "Cobertura data file: '%s'.", coberturaDataFile);
			
			final StatisticsCollector<StatisticsData> statisticsContainer = new StatisticsCollector<>(StatisticsData.class);
			
			final String javaHome = options.getOptionValue(CmdOptions.JAVA_HOME_DIR, null);
//			String testAndInstrumentClassPath = options.hasOption(CmdOptions.CLASS_PATH) ? options.getOptionValue(CmdOptions.CLASS_PATH) : null;
			
//			List<URL> cpURLs = new ArrayList<>();
//			
//			if (testAndInstrumentClassPath != null) {
//				Log.out(RunTestsAndGenSpectra.class, testAndInstrumentClassPath);
//				String[] cpArray = testAndInstrumentClassPath.split(File.pathSeparator);
//				for (String cpElement : cpArray) {
//					try {
//						cpURLs.add(new File(cpElement).toURI().toURL());
//					} catch (MalformedURLException e) {
//						Log.err(RunTestsAndGenSpectra.class, e, "Could not parse URL from '%s'.", cpElement);
//					}
////					break;
//				}
//			}
//			
//			ClassLoader instrumentedClassesLoader = 
//					Thread.currentThread().getContextClassLoader(); 
//					new CustomClassLoader(cpURLs, true);
			
//			Thread.currentThread().setContextClassLoader(instrumentedClassesLoader);
			
//			Log.out(RunTestsAndGenSpectra.class, Misc.listToString(cpURLs));

			PipeLinker linker = new PipeLinker();
			
			Path testFile = null;
			if (options.hasOption(CmdOptions.TEST_CLASS_LIST)) { //has option "tc"
				testFile = options.isFile(CmdOptions.TEST_CLASS_LIST, true);
				
				linker.append(
						new FileLineProcessor<String>(new StringProcessor<String>() {
							private String clazz = null;
							@Override public boolean process(String clazz) {
								this.clazz = clazz;
								return true;
							}
							@Override public String getLineResult() {
								String temp = clazz;
								clazz = null;
								return temp;
							}
						}),
						new AbstractProcessor<String, TestWrapper>() {
							@Override
							public TestWrapper processItem(String className, ProcessorSocket<String, TestWrapper> socket) {
								try {
//									Class<?> testClazz = Class.forName(className, true, instrumentedClassesLoader);
									Class<?> testClazz = Class.forName(className);
									
									JUnit4TestAdapter tests = new JUnit4TestAdapter(testClazz);
									for (Test t : tests.getTests()) {
										if (t.toString().startsWith("initializationError(")) {
											Log.err(this, "Test could not be initialized: %s", t.toString());
											continue;
										}
//										socket.produce(new TestWrapper(instrumentedClassesLoader, t, testClazz));
										socket.produce(new TestWrapper(null, t, testClazz));
									}
									
//									BlockJUnit4ClassRunner runner = new BlockJUnit4ClassRunner(testClazz);
//									List<FrameworkMethod> list = runner.getTestClass().getAnnotatedMethods(org.junit.Test.class);
//									
//									for (FrameworkMethod method : list) {
//										producer.produce(new TestWrapper(instrumentedClassesLoader, testClazz, method));
//									}
//								} catch (InitializationError e) {
//									Log.err(this, e, "Test adapter could not be initialized with class '%s'.", className);
								} 
								catch (ClassNotFoundException e) {
									Log.err(this, "Class '%s' not found.", className);
								}
								return null;
							}
						});
			} else { //has option "t"
				testFile = options.isFile(CmdOptions.TEST_LIST, true);
				
				linker.append(
						new FileLineProcessor<TestWrapper>(new StringProcessor<TestWrapper>() {
							private TestWrapper testWrapper;
							@Override public boolean process(String testNameAndClass) {
								//format: test.class::testName
								final String[] test = testNameAndClass.split("::");
								if (test.length != 2) {
									Log.err(JaCoCoToSpectra.class, "Wrong test identifier format: '" + testNameAndClass + "'.");
									return false;
								} else {
									Class<?> testClazz = null;
									try {
//										testClazz = Class.forName(test[0], true, instrumentedClassesLoader);
										testClazz = Class.forName(test[0]);
									} catch (ClassNotFoundException e) {
										Log.err(JaCoCoToSpectra.class, "Class '%s' not found.", test[0]);
										return false;
									}
									Request request = Request.method(testClazz, test[1]);
//									testWrapper = new TestWrapper(instrumentedClassesLoader, request, test[0], test[1]);
									testWrapper = new TestWrapper(null, request, test[0], test[1]);
								}
								return true;
							}
							@Override public TestWrapper getLineResult() {
								TestWrapper temp = testWrapper;
								testWrapper = null;
								return temp;
							}
						}));
			}
			
			linker.append(
					new JaCoCoTestRunAndReportModule(outputDir, srcDir.toString(), options.getOptionValues(CmdOptions.ORIGINAL_CLASSES), false, 
							options.hasOption(CmdOptions.TIMEOUT) ? Long.valueOf(options.getOptionValue(CmdOptions.TIMEOUT)) : null,
									options.hasOption(CmdOptions.REPEAT_TESTS) ? Integer.valueOf(options.getOptionValue(CmdOptions.REPEAT_TESTS)) : 1,
//											testAndInstrumentClassPath + File.pathSeparator + 
											new ClassPathParser().parseSystemClasspath().getClasspath(), 
											javaHome, options.hasOption(CmdOptions.SEPARATE_JVM), statisticsContainer)
//					.asPipe(instrumentedClassesLoader)
					.asPipe().enableTracking().allowOnlyForcedTracks(),
					new JaCoCoAddReportToProviderAndGenerateSpectraModule(true, outputDir + File.separator + "fail", options.hasOption(CmdOptions.FULL_SPECTRA)),
					new SaveSpectraModule<SourceCodeBlock>(SourceCodeBlock.DUMMY, Paths.get(outputDir, BugLoRDConstants.SPECTRA_FILE_NAME)),
					new TraceFileModule<SourceCodeBlock>(outputDir),
					new FilterSpectraModule<SourceCodeBlock>(INode.CoverageType.EF_EQUALS_ZERO),
					new SaveSpectraModule<SourceCodeBlock>(SourceCodeBlock.DUMMY, Paths.get(outputDir, BugLoRDConstants.FILTERED_SPECTRA_FILE_NAME)))
			.submitAndShutdown(testFile);
			
			EnumSet<StatisticsData> stringDataEnum = EnumSet.noneOf(StatisticsData.class);
			stringDataEnum.add(StatisticsData.ERROR_MSG);
			stringDataEnum.add(StatisticsData.FAILED_TEST_COVERAGE);
			String statsWithoutStringData = statisticsContainer.printStatistics(EnumSet.complementOf(stringDataEnum));
			
			Log.out(JaCoCoToSpectra.class, statsWithoutStringData);
			
			String stats = statisticsContainer.printStatistics(stringDataEnum);
			try {
				FileUtils.writeStrings2File(Paths.get(outputDir, testFile.getFileName() + "_stats").toFile(), statsWithoutStringData, stats);
			} catch (IOException e) {
				Log.err(JaCoCoToSpectra.class, "Can not write statistics to '%s'.", Paths.get(outputDir, testFile.getFileName() + "_stats"));
			}
		}

	}


	/**
	 * Convenience method for easier use in a special case.
	 * @param javaHome
	 * a Java version to use (path to the home directory)
	 * @param workDir
	 * directory of a buggy Defects4J project version
	 * @param mainSrcDir
	 * path to main source directory
	 * @param testBinDir
	 * path to main directory of binary test classes
	 * @param testCP
	 * class path needed to execute tests
	 * @param mainBinDir
	 * path to main directory of binary program classes
	 * @param testClassesFile
	 * path to a file that contains a list of all test classes to consider
	 * @param rankingDir
	 * output path of generated rankings
	 * @param timeout
	 * timeout (in seconds) for each test execution
	 * @param repeatCount
	 * number of times to execute each test case
	 * @param fullSpectra
	 * whether a full spectra should be created
	 * @param alwaysUseSeparateJVM
	 * whether a separate JVM shall be used for each test to run
	 */
	public static void generateRankingForDefects4JElement(
			final String javaHome, final String workDir, final String mainSrcDir, final String testBinDir, 
			final String testCP, final String mainBinDir, final String testClassesFile, 
			final String rankingDir, final Long timeout, final Integer repeatCount, 
			final boolean fullSpectra, final boolean alwaysUseSeparateJVM) {
		String[] args = { 
				CmdOptions.PROJECT_DIR.asArg(), workDir, 
				CmdOptions.SOURCE_DIR.asArg(), mainSrcDir,
				CmdOptions.TEST_CLASS_DIR.asArg(), testBinDir,
				CmdOptions.INSTRUMENT_CLASSES.asArg(), mainBinDir,
				CmdOptions.TEST_CLASS_LIST.asArg(), testClassesFile,
				CmdOptions.OUTPUT.asArg(), rankingDir};
		
		if (fullSpectra) {
			args = Misc.addToArrayAndReturnResult(args, CmdOptions.FULL_SPECTRA.asArg());
		}
		
		if (alwaysUseSeparateJVM) {
			args = Misc.addToArrayAndReturnResult(args, CmdOptions.SEPARATE_JVM.asArg());
		}
		
		if (javaHome != null) {
			args = Misc.addToArrayAndReturnResult(args, CmdOptions.JAVA_HOME_DIR.asArg(), javaHome);
		}
		
		if (testCP != null) {
			args = Misc.addToArrayAndReturnResult(args, CmdOptions.CLASS_PATH.asArg(), testCP);
		}
		
		if (timeout != null) {
			args = Misc.addToArrayAndReturnResult(args, CmdOptions.TIMEOUT.asArg(), String.valueOf(timeout));
		}
		
//		if (repeatCount != null) {
//			args = Misc.addToArrayAndReturnResult(args, CmdOptions.REPEAT_TESTS.asArg(), String.valueOf(repeatCount));
//		}

		main(args);
	}

}