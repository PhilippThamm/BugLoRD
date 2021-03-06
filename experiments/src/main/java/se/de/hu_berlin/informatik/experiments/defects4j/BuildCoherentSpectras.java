package se.de.hu_berlin.informatik.experiments.defects4j;

import org.apache.commons.cli.Option;
import se.de.hu_berlin.informatik.benchmark.api.BugLoRDConstants;
import se.de.hu_berlin.informatik.benchmark.api.BuggyFixedEntity;
import se.de.hu_berlin.informatik.benchmark.api.Entity;
import se.de.hu_berlin.informatik.benchmark.api.defects4j.Defects4J;
import se.de.hu_berlin.informatik.benchmark.api.defects4j.Defects4JBuggyFixedEntity;
import se.de.hu_berlin.informatik.spectra.core.INode;
import se.de.hu_berlin.informatik.spectra.core.ISpectra;
import se.de.hu_berlin.informatik.spectra.core.SourceCodeBlock;
import se.de.hu_berlin.informatik.spectra.core.manipulation.BuildCoherentSpectraModule;
import se.de.hu_berlin.informatik.spectra.core.manipulation.FilterSpectraModule;
import se.de.hu_berlin.informatik.spectra.core.manipulation.SaveSpectraModule;
import se.de.hu_berlin.informatik.spectra.util.SpectraFileUtils;
import se.de.hu_berlin.informatik.utils.files.FileUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.optionparser.OptionParser;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapper;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapperInterface;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;
import se.de.hu_berlin.informatik.utils.processors.basics.ThreadedProcessor;
import se.de.hu_berlin.informatik.utils.processors.sockets.pipe.PipeLinker;
import se.de.hu_berlin.informatik.utils.threaded.SemaphoreThreadLimit;
import se.de.hu_berlin.informatik.utils.threaded.ThreadLimit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores the generated spectra for future usage.
 *
 * @author SimHigh
 */
public class BuildCoherentSpectras {

    public enum CmdOptions implements OptionWrapperInterface {
        /* add options here according to your needs */
//		OUTPUT("o", "output", true, "Path to output csv statistics file (e.g. '~/outputDir/project/bugID/data.csv').", true)
        PROJECTS(Option.builder("p").longOpt("projects").hasArgs().desc(
                "A list of projects to consider of the Defects4J benchmark. "
                        + "Should be either 'Lang', 'Chart', 'Time', 'Closure', 'Mockito' or 'Math'. Set this to 'all' to "
                        + "iterate over all projects.")
                .build()),
        BUG_IDS(Option.builder("b").longOpt("bugIDs").hasArgs().desc(
                "A list of numbers indicating the ids of buggy project versions to consider. "
                        + "Value ranges differ based on the project. Set this to 'all' to "
                        + "iterate over all bugs in a project.")
                .build()),
        FILTER_SPECTRA("f", "filter", false, "Whether the altered spectra should be filtered.", false);

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
                   final boolean hasArg, final String description, int groupId) {
            this.option = new OptionWrapper(
                    Option.builder(opt).longOpt(longOpt).required(false).
                            hasArg(hasArg).desc(description).build(), groupId);
        }

        //adds the given option that will be part of the group with the given id
        CmdOptions(Option option, int groupId) {
            this.option = new OptionWrapper(option, groupId);
        }

        //adds the given option that will be part of no group
        CmdOptions(Option option) {
            this(option, NO_GROUP);
        }

        @Override
        public String toString() {
            return option.getOption().getOpt();
        }

        @Override
        public OptionWrapper getOptionWrapper() {
            return option;
        }
    }

    /**
     * @param args command line arguments
     */
    public static void main(String[] args) {

        OptionParser options = OptionParser.getOptions("BuildCoherentSpectras", true, CmdOptions.class, args);

        //		AbstractEntity mainEntity = Defects4JEntity.getDummyEntity();
        //		
        //		File archiveMainDir = mainEntity.getBenchmarkDir(false).toFile();
        //		
        //		if (!archiveMainDir.exists()) {
        //			Log.abort(GenerateSpectraArchive.class, 
        //					"Archive main directory doesn't exist: '" + mainEntity.getBenchmarkDir(false) + "'.");
        //		}

        /* #====================================================================================
         * # load the compressed spectra files and generate/save coherent spectras
         * #==================================================================================== */

        int numberOfThreads = options.getNumberOfThreads();
        ThreadLimit limit = new SemaphoreThreadLimit(numberOfThreads);

        PipeLinker linker = new PipeLinker().append(
                new ThreadedProcessor<>(numberOfThreads, limit,
                        new CoherentProcessor(null, options.hasOption(CmdOptions.FILTER_SPECTRA))),
                new ThreadedProcessor<>(numberOfThreads, limit,
                        new CoherentProcessor(BugLoRDConstants.DIR_NAME_JACOCO, options.hasOption(CmdOptions.FILTER_SPECTRA))),
                new ThreadedProcessor<>(numberOfThreads, limit,
                        new CoherentProcessor(BugLoRDConstants.DIR_NAME_COBERTURA, options.hasOption(CmdOptions.FILTER_SPECTRA))),
                new ThreadedProcessor<>(numberOfThreads, limit,
                        new CoherentProcessor(BugLoRDConstants.DIR_NAME_TRACE_COBERTURA, options.hasOption(CmdOptions.FILTER_SPECTRA)))
        );
        
        String[] projects = options.getOptionValues(CmdOptions.PROJECTS);
        String[] ids = options.getOptionValues(CmdOptions.BUG_IDS);

        boolean all = ids == null || ids[0].equals("all");

        if (projects == null || projects[0].equals("all")) {
            projects = Defects4J.getAllProjectIDs();
        }

        // iterate over all projects
        for (String project : projects) {
        	if (all) {
        		ids = Defects4J.getAllBugIDs(project);
        	}
        	for (String id : ids) {
        		linker.submit(new Defects4JBuggyFixedEntity(project, id));
        	}
        }
        linker.shutdown();

        Log.out(BuildCoherentSpectras.class, "All done!");

    }

    private static class CoherentProcessor extends AbstractProcessor<BuggyFixedEntity<?>, BuggyFixedEntity<?>> {

        private final String subDirName;
        private boolean filterSpectra;

        public CoherentProcessor(String subDirName, boolean filterSpectra) {
            this.subDirName = subDirName;
            this.filterSpectra = filterSpectra;
        }

        @Override
        public BuggyFixedEntity<?> processItem(BuggyFixedEntity<?> input) {
            Log.out(BuildCoherentSpectras.class, "Processing %s with sub directory '%s'.",
                    input, subDirName == null ? "<none>" : subDirName);
            Entity bug = input.getBuggyVersion();
            Path spectraFile = BugLoRD.getSpectraFilePath(bug, subDirName);

            if (!spectraFile.toFile().exists()) {
                Log.err(BuildCoherentSpectras.class, "Spectra file does not exist for %s with sub directory '%s'.",
                        input, subDirName == null ? "<none>" : subDirName);
                return input;
            }
            
         // copy spectra file to execution directory for faster loading/saving...
        	Path spectraDestination = bug.getWorkDir(true).resolve(subDirName)
                    .resolve(BugLoRDConstants.SPECTRA_FILE_NAME).toAbsolutePath();
            try {
                FileUtils.copyFileOrDir(spectraFile.toFile(), spectraDestination.toFile(), StandardCopyOption.REPLACE_EXISTING);
                Log.out(this, "Copied spectra '%s' to '%s'.", spectraFile.toFile(), spectraDestination);
            } catch (IOException e) {
                Log.err(this, "Found spectra '%s', but could not copy to '%s'.", spectraFile.toFile(), spectraDestination);
                return null;
            }

            //load the full spectra
            ISpectra<SourceCodeBlock, ?> spectra = SpectraFileUtils.loadSpectraFromZipFile(SourceCodeBlock.DUMMY, spectraDestination);

            //generate the coherent version
            spectra = new BuildCoherentSpectraModule().submit(spectra).getResult();

            //save the coherent full spectra
            new SaveSpectraModule<>(spectraDestination).submit(spectra);

            if (filterSpectra) {
                Path spectraFileFiltered = BugLoRD.getFilteredSpectraFilePath(bug, subDirName);

                //generate the filtered coherent spectra
                //(building a coherent spectra from an already filtered spectra may yield wrong results
                //by generating blocks that reach over filtered out nodes...)
                spectra = new FilterSpectraModule<SourceCodeBlock>(INode.CoverageType.EF_EQUALS_ZERO).submit(spectra).getResult();

                //save the filtered spectra
                new SaveSpectraModule<>(spectraFileFiltered).submit(spectra);
            }
            
            try {
                FileUtils.copyFileOrDir(spectraDestination.toFile(), spectraFile.toFile(), StandardCopyOption.REPLACE_EXISTING);
                Log.out(this, "Copied spectra '%s' to '%s'.", spectraDestination, spectraFile);
            } catch (IOException e) {
                Log.err(this, "Found spectra '%s', but could not copy to '%s'.", spectraDestination, spectraFile);
                return null;
            }
        	
        	FileUtils.delete(spectraDestination);

            return input;
        }
    }

}
