/**
 * 
 */
package se.de.hu_berlin.informatik.defects4j.experiments.calls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import se.de.hu_berlin.informatik.changechecker.ChangeChecker;
import se.de.hu_berlin.informatik.constants.Defects4JConstants;
import se.de.hu_berlin.informatik.defects4j.frontend.Defects4JEntity;
import se.de.hu_berlin.informatik.defects4j.frontend.BenchmarkEntity;
import se.de.hu_berlin.informatik.defects4j.frontend.Defects4J;
import se.de.hu_berlin.informatik.utils.fileoperations.FileUtils;
import se.de.hu_berlin.informatik.utils.fileoperations.ListToFileWriterModule;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.eventhandler.EHWithInputAndReturn;
import se.de.hu_berlin.informatik.utils.threaded.disruptor.eventhandler.EHWithInputAndReturnFactory;

/**
 * Runs a single experiment.
 * 
 * @author Simon Heiden
 */
public class ExperimentRunnerCheckoutFixAndCheckForChangesEH extends EHWithInputAndReturn<BenchmarkEntity,BenchmarkEntity> {
	
	public static class Factory extends EHWithInputAndReturnFactory<BenchmarkEntity,BenchmarkEntity> {

		/**
		 * Initializes a {@link Factory} object.
		 */
		public Factory() {
			super(ExperimentRunnerCheckoutFixAndCheckForChangesEH.class);
		}

		@Override
		public EHWithInputAndReturn<BenchmarkEntity, BenchmarkEntity> newFreshInstance() {
			return new ExperimentRunnerCheckoutFixAndCheckForChangesEH();
		}
	}

	/**
	 * Initializes a {@link ExperimentRunnerCheckoutFixAndCheckForChangesEH} object.
	 */
	public ExperimentRunnerCheckoutFixAndCheckForChangesEH() {
		super();
	}
	
	/**
	 * Parses the info file and returns a String which contains all modified
	 * source files with one file per line.
	 * @param infoFile
	 * the path to the info file
	 * @return
	 * modified source files, separated by new lines
	 */
	private List<String> parseInfoFile(String infoFile) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader bufRead = new BufferedReader(new FileReader(infoFile))) {
			String line = null;
			boolean modifiedSourceLine = false;
			while ((line = bufRead.readLine()) != null) {
				if (line.equals("List of modified sources:")) {
					modifiedSourceLine = true;
					continue;
				}
				if (modifiedSourceLine && line.startsWith(" - ")) {
					lines.add(line.substring(3));
				} else {
					modifiedSourceLine = false;
				}
			}
		} catch (FileNotFoundException e) {
			Log.abort(this, "Info file does not exist: '" + infoFile + "'.");
		} catch (IOException e) {
			Log.abort(this, "IOException while reading info file: '" + infoFile + "'.");
		}
		
		return lines;
	}

	@Override
	public void resetAndInit() {
		//not needed
	}

	@Override
	public BenchmarkEntity processInput(BenchmarkEntity buggyEntity) {
		Log.out(this, "Processing %s.", buggyEntity);
		buggyEntity.switchToArchiveDir();

		/* #====================================================================================
		 * # checkout fixed version and check for changes
		 * #==================================================================================== */

		String infoFile = buggyEntity.getWorkDir() + Defects4J.SEP + Defects4JConstants.FILENAME_INFO;
		
		/* #====================================================================================
		 * # prepare checking modifications
		 * #==================================================================================== */
		String archiveBuggyWorkDir = buggyEntity.getWorkDir().toString();
		String modifiedSourcesFile = buggyEntity.getWorkDir() + Defects4J.SEP + Defects4JConstants.FILENAME_INFO_MOD_SOURCES;
		
		List<String> modifiedSources = parseInfoFile(infoFile);
		new ListToFileWriterModule<List<String>>(Paths.get(modifiedSourcesFile), true)
		.submit(modifiedSources);
		
		String buggyMainSrcDir = buggyEntity.getMainSourceDir().toString();
		
		/* #====================================================================================
		 * # checkout fixed version for comparison purposes
		 * #==================================================================================== */
		Defects4JEntity fixedEntity = Defects4JEntity.getFixedDefects4JEntity(
				buggyEntity.getProject(), String.valueOf(buggyEntity.getBugId()));
		fixedEntity.switchToExecutionDir();
		String executionFixedWorkDir = fixedEntity.getWorkDir().toString();
		fixedEntity.resetAndInitialize(true);

		String fixedMainSrcDir = fixedEntity.getMainSourceDir().toString();

		/* #====================================================================================
		 * # check modifications
		 * #==================================================================================== */
		//iterate over all modified source files
		List<String> result = new ArrayList<>();
		for (String modifiedSourceIdentifier : modifiedSources) {
			String path = modifiedSourceIdentifier.replace('.','/') + ".java";
			result.add(Defects4JConstants.PATH_MARK + path);
			
			//extract the changes
			result.addAll(ChangeChecker.checkForChanges(
					Paths.get(archiveBuggyWorkDir, buggyMainSrcDir, path).toFile(), 
					Paths.get(executionFixedWorkDir, fixedMainSrcDir, path).toFile()));
		}
		
		//save the gathered information about modified lines in a file
		new ListToFileWriterModule<List<String>>(Paths.get(archiveBuggyWorkDir, Defects4JConstants.FILENAME_MOD_LINES), true)
		.submit(result);
		
		//delete the fixed version directory, since it's not needed anymore
		fixedEntity.deleteAll();
		
		return buggyEntity;
	}

}

