package se.de.hu_berlin.informatik.spectra.core.branch;


import se.de.hu_berlin.informatik.spectra.core.*;
import se.de.hu_berlin.informatik.spectra.core.traces.ExecutionTrace;
import se.de.hu_berlin.informatik.spectra.core.traces.SequenceIndexerCompressed;
import se.de.hu_berlin.informatik.spectra.core.traces.SimpleIntIndexerCompressed;
import se.de.hu_berlin.informatik.spectra.provider.tracecobertura.infrastructure.sequitur.input.InputSequence.TraceIterator;
import se.de.hu_berlin.informatik.spectra.util.CachedIntArrayMap;
import se.de.hu_berlin.informatik.spectra.util.CachedMap;
import se.de.hu_berlin.informatik.spectra.util.SpectraFileUtils;
import se.de.hu_berlin.informatik.utils.files.FileUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.tracking.ProgressTracker;

import java.nio.file.Path;
import java.util.*;


/**
 * Reads a Spectra object and combines sequences of nodes to larger blocks based
 * on whether they are within the same branch.
 *
 * @author Duc Anh Vu
 */
public class StatementSpectraToBranchSpectra {

    /*====================================================================================
     * MAIN FUNCTIONALITY
     *====================================================================================*/

    public static ProgramBranchSpectra<ProgramBranch> generateBranchingSpectraFromStatementSpectra
            (ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra, Path temporaryOutputDir) {
        Log.out(StatementSpectraToBranchSpectra.class, "Generating branch spectra...");

        /*====================================================================================*/
        assert (statementSpectra != null);
//        assert(pathToSpectraZipFile != null);
        /*====================================================================================*/


        // spectra file path should actually be null here, since it's a new spectra that isn't loaded from a zip file
        ProgramBranchSpectra<ProgramBranch> programBranchSpectra = new ProgramBranchSpectra<>(null);
        
        // sets up all statements and the node ID sequences from the indexer
        programBranchSpectra.addFromStatementSpectra(statementSpectra, temporaryOutputDir);
        
//        Map<Integer, INode<ProgramBranch>> branchIdMap = new HashMap<>();
        INode<ProgramBranch> branchNode;
        ProgramBranch programBranch;

        Set<Integer> executedStatements = new HashSet<>();

        // collect all existing branch IDs
        CachedMap<int[]> subTraceIdSequences = programBranchSpectra.getSubTraceSequenceMap();

        /* extract all branches from the statement spectra, i.e.
         *  branches that are covered in a test case
         *  branch := list of statements;
         *
         *  id 0 is reserved for the empty branch (which should not exist)
         */
        for (int executionBranchId = 0; executionBranchId < subTraceIdSequences.size(); ++executionBranchId) {
            programBranch = new ProgramBranch(executionBranchId, programBranchSpectra);
            branchNode = programBranchSpectra.getOrCreateNode(programBranch);
//            branchIdMap.put(executionBranchId, branchNode);

            executedStatements.addAll(programBranch.getExecutedNodeIDs());
        }

        Set<Integer> notExecutedStatements = new HashSet<>();
        // add statements to the new spectra that have not been executed by any test case...
        for (INode<SourceCodeBlock> node : statementSpectra.getNodes()) {
            if (!executedStatements.contains(node.getIndex())) {
            	notExecutedStatements.add(node.getIndex());
            }
        }
        
        if (!notExecutedStatements.isEmpty()) {
        	CachedMap<int[]> nodeSequenceMap = programBranchSpectra.getNodeSequenceMap();
        	int[] nodeSequence = new int[notExecutedStatements.size()];
        	int i = 0;
        	for (int nodeId : notExecutedStatements) {
        		nodeSequence[i++] = nodeId;
        	}
        	// create a placeholder branch that holds all statements that have not been executed
        	branchNode = programBranchSpectra.getOrCreateNode(new ProgramBranch(subTraceIdSequences.size(), programBranchSpectra));
        	subTraceIdSequences.put(subTraceIdSequences.size(), new int[] {nodeSequenceMap.size()});
        	nodeSequenceMap.put(nodeSequenceMap.size(), nodeSequence);
        	nodeSequenceMap.close();
        }

        // in the statement level spectra, branch IDs were mapped to the sequence of node IDs in the respective branch;
        // now we can map the branch IDs to the IDs of the branch...
        // no need to include the single node branches that have not been executed in here!
        Path sequenceZipFile = temporaryOutputDir == null ? 
        		statementSpectra.getPathToSpectraZipFile().getParent().resolve("branchNodeMap.zip") :
        			temporaryOutputDir.resolve("branchNodeMap.zip");
        FileUtils.delete(sequenceZipFile);
		CachedMap<int[]> branchNodeIdSequences = new CachedIntArrayMap(
                sequenceZipFile, SpectraFileUtils.NODE_ID_SEQUENCES_DIR, true);
        for (int i = 0; i < subTraceIdSequences.size(); ++i) {
            // one to one mapping... (still ok for easy future removal of nodes from traces)
            branchNodeIdSequences.put(i, new int[]{i});
        }

        // should be able to reuse the execution trace grammar!
        SequenceIndexerCompressed branchSpectraIndexer = new SimpleIntIndexerCompressed(
                statementSpectra.getIndexer().getGrammarByteArray(), branchNodeIdSequences, null);


        ProgressTracker tracker = new ProgressTracker(false);

        /* the branchingSpectra has the same tests as the statementSpectra
         *  --> copy every test, but set the involvement for each test according to which branches
         *  they cover
         *  (in the statement spectra, the tests only know which statements they cover)
         */
        for (ITrace<SourceCodeBlock> testCase : statementSpectra.getTraces()) {
        	// give some visual progress information
            tracker.track(String.format("test: %s", testCase.getIdentifier()));
            ITrace<ProgramBranch> newTrace = programBranchSpectra.
                    addTrace(testCase.getIdentifier(), testCase.getIndex(), testCase.isSuccessful());

            for (ExecutionTrace executionTrace : testCase.getExecutionTraces()) {
                
                HashSet<Integer> executionBranchIds = new HashSet<>();
                collectExecutionBranchIds(executionBranchIds, executionTrace);
                for (Integer executionBranchId : executionBranchIds) {

                    branchNode = programBranchSpectra.getNode(executionBranchId);
                    if (branchNode != null) {
                        newTrace.setInvolvement(branchNode, true);
                    } else {
                    	// this should not happen!
                    	throw new IllegalStateException("Node not found! id: " + executionBranchId);
                    }
                }

                // since the indices match, we can reuse execution traces from the statement level spectra
                ExecutionTrace branchExecutionTrace =
                        new ExecutionTrace(executionTrace.getTraceByteArray(), branchSpectraIndexer);

                // add branch level execution trace
                newTrace.addExecutionTrace(branchExecutionTrace);
            }
        }

        // don't forget to set the indexer!
        programBranchSpectra.setIndexer(branchSpectraIndexer);

        /*====================================================================================*/
        assert (programBranchSpectra != null);
        // some check for correctness required, e.g. do the resulting branches match the executed statements?
        // --> assert(branchingSpectraMatchesStatementSpectra) or something
        // kind of hard to do now, since the existing data structures don't have easy ways to access
        // the required information
        assert (branchesHaveAtMostKDecisionPoints(programBranchSpectra, 2)); // k=1 --> number of decisions in a branch is at most 1 for now ; next plan is to merge multiple branches therefore they can have multiple decision points
        /*====================================================================================*/

        branchNodeIdSequences.close();
        return programBranchSpectra;

    }

    /*====================================================================================
     * HELPER METHODS
     *====================================================================================*/

//    private static Collection<Integer> collectAllExecutionBranchIds(ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra){
//
//    	/*====================================================================================*/
//    	assert(statementSpectra != null);
//    	/*====================================================================================*/
//
//    	HashSet<Integer> executionBranchIds = new HashSet<Integer>();
//    	
//    	int[][] subTraceIdSequences = statementSpectra.getIndexer().getSubTraceIdSequences();
//
//    	for(int[] subTraceIdSequence : subTraceIdSequences){
//    		for(int subTraceId : subTraceIdSequence){
//    			executionBranchIds.add(subTraceId);
//    		}
//    	}
//
//    	/*====================================================================================*/
//    	assert(executionBranchIds != null);
//    	/*====================================================================================*/
//
//    	return executionBranchIds;
//
//    }

//    private static Collection<Integer> collectExecutionBranchIds(Collection<? extends ITrace<SourceCodeBlock>> testCaseTraces,
//                                                                 ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra){
//
//        /*====================================================================================*/
//        assert(testCaseTraces != null);
//        /*====================================================================================*/
//
//        HashSet<Integer> executionBranchIds = new HashSet<Integer>();
//
//        for(ITrace<SourceCodeBlock> testCaseTrace : testCaseTraces){
//            for(ExecutionTrace executionTrace : testCaseTrace.getExecutionTraces()){
//                collectExecutionBranchIds(executionBranchIds, executionTrace, statementSpectra);
//            }
//        }
//
//        /*====================================================================================*/
//        assert(executionBranchIds != null);
//        /*====================================================================================*/
//
//        return executionBranchIds;
//
//    }

    private static void collectExecutionBranchIds(Set<Integer> executionBranchIds, ExecutionTrace executionTrace) {

        /*====================================================================================*/
        assert (executionTrace != null);
        /*====================================================================================*/

        TraceIterator myExecutionBranchIterator = executionTrace.iterator();

        while (myExecutionBranchIterator.hasNext()) {
            executionBranchIds.add(myExecutionBranchIterator.next());
        }

        /*====================================================================================*/
        assert (executionBranchIds != null);
        /*====================================================================================*/

    }

//    private static List<INode<SourceCodeBlock>> getExecutedStatementsFromBranch(int branchId, ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra) {
//
//        /*====================================================================================*/
//        assert (statementSpectra != null);
//        /*====================================================================================*/
//
//        List<INode<SourceCodeBlock>> statements = null;
//
//        List<Integer> statementIndices = getStatementIndicesFromBranch(branchId, statementSpectra);
//
//        statements = new ArrayList<>(statementIndices.size());
//        for (Integer statementIndex : statementIndices) {
//            statements.add(statementSpectra.getNode(statementIndex));
//        }
//
//        /*====================================================================================*/
//        assert (statements != null);
////        assert (isExecutedListOfStatementsOfThisBranch(branchId, statements, statementSpectra));
//        /*====================================================================================*/
//
//        return statements;
//
//    }

//    private static List<Integer> getStatementIndicesFromBranch(int branchId, ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra) {
//
//        /*====================================================================================*/
//        assert (statementSpectra != null);
//        /*====================================================================================*/
//
//        List<Integer> statementIndices = null;
//
//        statementIndices = new ArrayList<>();
//
//        // iterate over all sub traces in the sequence
//        Iterator<Integer> sequenceIterator = statementSpectra.getIndexer().getSubTraceIdSequenceIterator(branchId);
//        while (sequenceIterator.hasNext()) {
//            // iterate over all statements in the sub trace
//            Integer subTraceId = sequenceIterator.next();
////            statementIndices.add(subTraceId);
//			Iterator<Integer> statementIndicesIterator = statementSpectra.getIndexer().getNodeIdSequenceIterator(subTraceId);
//            while (statementIndicesIterator.hasNext()) {
//                statementIndices.add(statementIndicesIterator.next());
//            }
//        }
//
//        /*====================================================================================*/
//        assert (statementIndices != null);
////        assert (isExecutedListOfStatementIndicesForThisBranch(branchId, statementIndices, statementSpectra));
//        /*====================================================================================*/
//
////        Log.err(null, branchId + ": " + Misc.listToString(statementIndices));
//        return statementIndices;
//
//    }

    /*====================================================================================
     * CHECKS
     *====================================================================================*/

//    private static boolean isBranchingSpectraOfStatementSpectra(ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra, ISpectra<Integer, ? extends ITrace<Integer>> branchingSpectra) {
//        return true;
//    }

//    /**
//     * Exactly means not less and not more statements are covered
//     *
//     * @param testCase
//     * @param statementIndices
//     * @return
//     */
//    private static boolean statementsAreExactlyCoveredByThisTestCase(ITrace<SourceCodeBlock> testCase, Collection<Integer> statementIndices) {
//
//        boolean result = false;
//
//        Collection<Integer> testCaseInvolvedStatementIndices = testCase.getInvolvedNodes();
//
//        boolean isSubset = testCaseInvolvedStatementIndices.containsAll(statementIndices);
//        boolean isSuperset = statementIndices.containsAll(testCaseInvolvedStatementIndices);
//
//        System.out.println("---------------------");
//        System.out.println(testCaseInvolvedStatementIndices);
//        System.out.println(statementIndices);
//        System.out.println(testCaseInvolvedStatementIndices.size() - statementIndices.size());
//        System.out.println("---------------------");
//
//        result = isSubset && isSuperset;
//
//        return result;
//
//    }

//    /**
//     * Exactly means not less and not more is covered
//     *
//     * @param testCase
//     * @param branchIds
//     * @param spectra
//     * @return
//     */
//    private static boolean branchesAreExactlyCoveredByThisTestCase(ITrace<SourceCodeBlock> testCase, Collection<Integer> branchIds, ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> spectra) {
//
//        boolean result = false;
//
//        HashSet<Integer> statementIndicesFromBranches = new HashSet<>();
//
//        for (Integer branchId : branchIds) {
//            statementIndicesFromBranches.addAll(getStatementIndicesFromBranch(branchId, spectra));
//        }
//
//        result = statementsAreExactlyCoveredByThisTestCase(testCase, statementIndicesFromBranches);
//
//        return result;
//
//    }

//    /**
//     * Check if the branchId yields a list of executed statements equal to the list of statements we get from the provided statement indices.
//     *
//     * @param branchId
//     * @param statementIndices
//     * @param statementSpectra
//     * @return
//     */
//    private static boolean isExecutedListOfStatementIndicesForThisBranch(int branchId, List<Integer> statementIndices, ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra) {
//
//        boolean result = false;
//
//        Iterator<Integer> _branchIndicesIterator = statementSpectra.getIndexer().getSubTraceIdSequenceIterator(branchId);
//
//        List<Integer> _statementIndices = new ArrayList<Integer>();
//
//        while (_branchIndicesIterator.hasNext()) {
//        	Integer subTraceId = _branchIndicesIterator.next();
////        	_statementIndices.add(subTraceId);
//        	Iterator<Integer> _statementIndicesIterator = statementSpectra.getIndexer().getNodeIdSequenceIterator(subTraceId);
//        	while (_statementIndicesIterator.hasNext()) {
//        		_statementIndices.add(_statementIndicesIterator.next());
//        	}
//        }
//
//        result = _statementIndices.equals(statementIndices);
//
//        if (!result) {
//        	Log.err(null, Misc.listToString(_statementIndices) + Misc.listToString(statementIndices));
//        }
//        return result;
//
//    }

//    /**
//     * Check if the branchId yields a list of executed statements equal to the list of statements provided as input
//     *
//     * @param branchId
//     * @param statements
//     * @param statementSpectra
//     * @return
//     */
//    private static boolean isExecutedListOfStatementsOfThisBranch(int branchId, List<SourceCodeBlock> statements, ISpectra<SourceCodeBlock, ? extends ITrace<SourceCodeBlock>> statementSpectra) {
//
//        boolean result = false;
//
//        List<Integer> statementIndices = new ArrayList<>();
//
//        for (SourceCodeBlock statement : statements) {
//            statementIndices.add(statementSpectra.getNode(statement).getIndex());
//        }
//
//        result = isExecutedListOfStatementIndicesForThisBranch(branchId, statementIndices, statementSpectra);
//
//        return result;
//
//    }

    /**
     * Check if the branches in this branching spectra have at most k decision points.
     * E.g. k=2 would allow a branch to cover two "if" statements.
     *
     * @param programBranchSpectra
     * @param k
     * @return
     */
    private static boolean branchesHaveAtMostKDecisionPoints(ProgramBranchSpectra<ProgramBranch> programBranchSpectra, int k) {

        boolean result = false;

        ProgramBranch branch;

        Collection<INode<ProgramBranch>> nodes = programBranchSpectra.getNodes();
        for (INode<ProgramBranch> branchNode : nodes) {
        	if (branchNode.getIndex() >= nodes.size() - 1) {
        		continue;
        	}
            branch = branchNode.getIdentifier();
            result = branchHasAtMostKDecisionPoints(branch, k);
            if (result == false) break;
        }

        return result;

    }

    /**
     * Check if the branch has at most k decision points.
     * E.g. k=2 would allow a branch to cover two "if" statements.
     *
     * @param branch
     * @param k
     * @return
     */
    private static boolean branchHasAtMostKDecisionPoints(ProgramBranch branch, int k) {

        boolean result = false;

        int nrOfDecisionPoints = 0;
        Node.NodeType type;

        for (SourceCodeBlock statement : branch) {
            type = statement.getNodeType();
            if (type == Node.NodeType.TRUE_BRANCH || type == Node.NodeType.FALSE_BRANCH) {
                nrOfDecisionPoints++;
            }
        }

        result = nrOfDecisionPoints <= k;

        return result;

    }


    /*====================================================================================
     * FIELDS
     *====================================================================================*/

}

