package se.de.hu_berlin.informatik.defects4j.frontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.tm.modules.ExecuteCommandInSystemEnvironmentAndReturnOutputModule;
import se.de.hu_berlin.informatik.utils.tm.modules.ExecuteCommandInSystemEnvironmentModule;

public class Prop {

	private final static String SEP = File.separator;
	
	public final static String PROP_FILE_NAME = "defects4jProperties.ini";

	public final static String PROP_D4J_DIR = "defects4j_dir";
	public final static String PROP_EXECUTION_DIR = "execution_dir";
	public final static String PROP_ARCHIVE_DIR = "archive_dir";
	public final static String PROP_PLOT_DIR = "plot_dir";
	public final static String PROP_LOG_DIR = "log_dir";
	public final static String PROP_ONLY_RELEVANT_TESTS = "only_relevant_tests";
	public final static String PROP_KENLM_DIR = "kenlm_dir";
	public final static String PROP_SRILM_DIR = "srilm_dir";
	public final static String PROP_GLOBAL_LM = "global_lm_binary";
	public final static String PROP_TMP_DIR = "tmp_dir";
	public final static String PROP_JAVA7_DIR = "java7_dir";
	public final static String PROP_JAVA7_HOME = "java7_home";
	public final static String PROP_JAVA7_JRE = "java7_jre";
	
	public final static String OPT_PROJECT = "p";
	public final static String OPT_BUG_ID = "b";
	public final static String OPT_LOCALIZERS = "l";

	public static String executionMainDir;
	public static String archiveMainDir;
	public static String plotMainDir;
	public static String projectDir;
	public static String archiveProjectDir;
	public static String executionBuggyWorkDir;
	public static String executionFixedWorkDir;
	public static String archiveBuggyWorkDir;
	public static String archiveFixedWorkDir;
	public static String plotWorkDir;
	public static boolean relevant;
	
	public static String defects4jExecutable;
	public static String sriLMmakeBatchCountsExecutable;
	public static String sriLMmergeBatchCountsExecutable;
	public static String sriLMmakeBigLMExecutable;
	public static String kenLMbuildBinaryExecutable;
	public static String kenLMqueryExecutable;
	
	public static String globalLM;
	
	public static String java7BinDir;
	public static String java7home;
	public static String java7jre;
	
	/**
	 * Loads properties from the property file.
	 * @param project
	 * a project identifier, serving as a directory name
	 * @param buggyID
	 * name of buggy version subdirectory
	 * @param fixedID
	 * name of fixed version subdirectory
	 * @return
	 * a Properties object containing all loaded properties
	 */
	public static Properties loadProperties(String project, String buggyID, String fixedID) {
//		File homeDir = new File(System.getProperty("user.home"));
		File propertyFile = new File(Prop.PROP_FILE_NAME);

		Properties props = new Properties();

		if (propertyFile.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(propertyFile);
				props.load(fis);
			} catch (FileNotFoundException e) {
				Misc.abort("No property file found: '" + propertyFile + "'.");
			} catch (IOException e) {
				Misc.abort("IOException while reading property file: '" + propertyFile + "'.");
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException e) {
					// nothing to do
				}
			}
		}
		
		executionMainDir = props.getProperty(Prop.PROP_EXECUTION_DIR, ".");
		archiveMainDir = props.getProperty(Prop.PROP_ARCHIVE_DIR, "." + SEP + "archive");
		plotMainDir = props.getProperty(Prop.PROP_PLOT_DIR, "." + SEP + "plots");
		projectDir = executionMainDir + SEP + project;
		archiveProjectDir = archiveMainDir + SEP + project;
		executionBuggyWorkDir = projectDir + SEP + buggyID;
		executionFixedWorkDir = projectDir + SEP + fixedID;
		archiveBuggyWorkDir = archiveProjectDir + SEP + buggyID;
		archiveFixedWorkDir = archiveProjectDir + SEP + fixedID;
		
		defects4jExecutable = props.getProperty(Prop.PROP_D4J_DIR, ".") + SEP + "defects4j";
		sriLMmakeBatchCountsExecutable = props.getProperty(Prop.PROP_SRILM_DIR, ".") + SEP + "make-batch-counts";
		sriLMmergeBatchCountsExecutable = props.getProperty(Prop.PROP_SRILM_DIR, ".") + SEP + "merge-batch-counts";
		sriLMmakeBigLMExecutable = props.getProperty(Prop.PROP_SRILM_DIR, ".") + SEP + "make-big-lm";
		kenLMbuildBinaryExecutable = props.getProperty(Prop.PROP_KENLM_DIR, ".") + SEP + "build_binary";
		kenLMqueryExecutable = props.getProperty(Prop.PROP_KENLM_DIR, ".") + SEP + "query";
		
		globalLM = props.getProperty(Prop.PROP_GLOBAL_LM, ".");
		
		java7BinDir = props.getProperty(Prop.PROP_JAVA7_DIR, ".");
		java7home = props.getProperty(Prop.PROP_JAVA7_HOME, ".");
		java7jre = props.getProperty(Prop.PROP_JAVA7_JRE, ".");
		
		relevant = props.getProperty(Prop.PROP_ONLY_RELEVANT_TESTS, "true").equals("true") ? true : false;
		
		return props;
	}
	
	
	public static void storeProperties(Properties props) {
		// write the updated properties file to the file system
		FileOutputStream fos = null;
		File propertyFile = new File(PROP_FILE_NAME);
		
		try {
			fos = new FileOutputStream(propertyFile);
			props.store(fos, "property file for Defects4J benchmark experiments");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}
	}
	
	public static boolean validateProjectAndBugID(String project, int parsedID, boolean abortOnError) {
		if (parsedID < 1) {
			if (abortOnError)
				Misc.abort("Bug ID is negative.");
			else
				return false;
		}

		switch (project) {
		case "Lang":
			if (parsedID > 65)
				if (abortOnError)
					Misc.abort("Bug ID may only range from 1 to 65 for project Lang.");
				else
					return false;
			break;
		case "Math":
			if (parsedID > 106)
				if (abortOnError)
					Misc.abort("Bug ID may only range from 1 to 106 for project Math.");
				else
					return false;
			break;
		case "Chart":
			if (parsedID > 26)
				if (abortOnError)
					Misc.abort("Bug ID may only range from 1 to 26 for project Chart.");
				else
					return false;
			break;
		case "Time":
			if (parsedID > 27)
				if (abortOnError)
					Misc.abort("Bug ID may only range from 1 to 27 for project Time.");
				else
					return false;
			break;
		case "Closure":
			if (parsedID > 133)
				if (abortOnError)
					Misc.abort("Bug ID may only range from 1 to 133 for project Closure.");
				else
					return false;
			break;
		default:
			if (abortOnError)
				Misc.abort("Chosen project has to be either 'Lang', 'Chart', 'Time', 'Closure' or 'Math'.");
			else
				return false;
			break;	
		}
		return true;
	}

	/**
	 * Executes a given command in the system's environment, while additionally using a given Java 1.7 environment,
	 * which is required for defects4J to function correctly and to compile the projects. Will abort the
	 * program in case of an error in the executed process.
	 * @param executionDir
	 * an execution directory in which the command shall be executed
	 * @param commandArgs
	 * the command to execute, given as an array
	 */
	public static void executeCommand(File executionDir, String... commandArgs) {
		int executionResult = new ExecuteCommandInSystemEnvironmentModule(executionDir, Prop.java7BinDir)
				.setEnvVariable("JAVA_HOME", Prop.java7home)
				.setEnvVariable("JRE_HOME", Prop.java7jre)
				.submit(commandArgs).getResult();
		
		if (executionResult != 0) {
			Misc.abort("Error while executing command: " + Misc.arrayToString(commandArgs, " ", "", ""));
		}
	}
	
	/**
	 * Executes a given command in the system's environment, while additionally using a given Java 1.7 environment,
	 * which is required for defects4J to function correctly and to compile the projects. Returns either the process'
	 * output to standard out or to error out.
	 * @param executionDir
	 * an execution directory in which the command shall be executed
	 * @param returnErrorOutput
	 * whether to output the error output channel instead of standeard out
	 * @param commandArgs
	 * the command to execute, given as an array
	 * @return
	 * the process' output to standard out or to error out
	 */
	public static String executeCommandWithOutput(File executionDir, boolean returnErrorOutput, String... commandArgs) {
		return new ExecuteCommandInSystemEnvironmentAndReturnOutputModule(executionDir, returnErrorOutput, Prop.java7BinDir)
				.setEnvVariable("JAVA_HOME", Prop.java7home)
				.setEnvVariable("JRE_HOME", Prop.java7jre)
				.submit(commandArgs).getResult();
	}
	
}
