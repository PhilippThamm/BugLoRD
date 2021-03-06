package se.de.hu_berlin.informatik.javatokenizer.tokenizelines;

import se.de.hu_berlin.informatik.astlmbuilder.ASTTokenReader;
import se.de.hu_berlin.informatik.astlmbuilder.mapping.mapper.IBasicNodeMapper;
import se.de.hu_berlin.informatik.astlmbuilder.wrapper.Node2TokenWrapperMapping;
import se.de.hu_berlin.informatik.astlmbuilder.wrapper.TokenWrapper;
import se.de.hu_berlin.informatik.javatokenizer.tokenizer.SemanticMapper;
import se.de.hu_berlin.informatik.utils.files.processors.SearchFileOrDirToListProcessor;
import se.de.hu_berlin.informatik.utils.miscellaneous.ComparablePair;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * Module that tokenizes lines of files that are given by a provided {@link Map}
 * that links file paths given as {@link String}s with a {@link Set} of line
 * numbers and populates a given map that links trace file lines to tokenized
 * sentences.
 *
 * @author Simon Heiden
 */
public class SemanticTokenizeLines
        extends AbstractProcessor<Map<String, Set<ComparablePair<Integer, Integer>>>, Map<String, String>> {

    private final Path src_path;
    private final boolean use_context;
    private final boolean startFromMethods;
    private final int order;
    private final int preTokenCount;
    private final int postTokenCount;

    private final ASTTokenReader<TokenWrapper> reader;

    // maps trace file lines to sentences
    private final Map<String, String> sentenceMap;

    /**
     * Creates a new {@link SemanticTokenizeLines} object with the given
     * parameters.
     *
     * @param src_path            is the path to the source folder
     * @param use_context         sets if each sentence should contain a context of previous tokens
     * @param startFromMethods    sets if the context (if used) should only go back to the start of a
     *                            method. (Currently, the implementation only goes back to the last opening
     *                            curly bracket, which doesn't have to be the start of a method.)
     * @param order               the n-gram order (only important for the length of the context)
     * @param long_tokens         whether long tokens should be produced
     * @param depth               the maximum depth of constructing the tokens, where 0 equals total
     *                            abstraction and -1 means unlimited depth
     * @param includeParent       whether to include information about the parent node in the tokens
     * @param childCountStepWidth the grouping step width to use for grouping nodes based on their number of child nodes (log-based)
     * @param preTokenCount       the number of tokens to include that occur before the actual line
     * @param postTokenCount      the number of tokens to include that occur after the actual line
     */
    public SemanticTokenizeLines(String src_path, boolean use_context, boolean startFromMethods, int order,
                                 boolean long_tokens, int depth, boolean includeParent, int childCountStepWidth, int preTokenCount, int postTokenCount) {
        this.sentenceMap = new HashMap<>();
        this.src_path = Paths.get(src_path);
        this.use_context = use_context;
        this.startFromMethods = startFromMethods;
        this.order = order;
        this.preTokenCount = preTokenCount;
        this.postTokenCount = postTokenCount;

        IBasicNodeMapper<String> mapper = new SemanticMapper(long_tokens, childCountStepWidth).getMapper();

        reader = new ASTTokenReader<>(new Node2TokenWrapperMapping(mapper), null, null, startFromMethods,
                true, depth, includeParent);
    }

    /**
     * Tokenizes the specified lines in all files provided in the given
     * {@link HashMap} and updates the sentence map.
     *
     * @param map holds source code file paths, each associated with an {@link Set} of line
     *            numbers.
     */
    private void createTokenizedLinesOutput(final Map<String, Set<ComparablePair<Integer, Integer>>> map) {

        for (Entry<String, Set<ComparablePair<Integer, Integer>>> i : map.entrySet()) {
            createTokenizedLinesOutput(i.getKey(), src_path, i.getKey(), i.getValue());
        }
    }

    /**
     * Tokenizes the specified lines in the given file and updates the sentence
     * map.
     *
     * @param prefixForMap   a prefix (path) that completes a trace file line together with a line
     *                       number
     * @param parentPath     holds the parent {@link Path}
     * @param childPath      the path to the source file, relative to the parent path
     * @param lineNumbersSet holds the line numbers of the lines to be tokenized
     */
    private void createTokenizedLinesOutput(String prefixForMap, final Path parentPath, String childPath,
                                            final Set<ComparablePair<Integer, Integer>> lineNumbersSet) {
        // sort the line numbers
        List<ComparablePair<Integer, Integer>> lineNumbers = asSortedList(lineNumbersSet);

        Path inputFile = parentPath.resolve(childPath);

        List<List<TokenWrapper>> sentences = null;
        if (inputFile.toFile().exists()) {
            sentences = ASTTokenReader.getAllTokenSequencesAndFixCertainErrors(reader, inputFile);
        } else {
            // sometimes, the parent path is not correct, so try to find the file from the super directory...
            List<Path> result = new SearchFileOrDirToListProcessor("**" + childPath, true)
                    .searchForFiles()
                    .submit(parentPath.getParent())
                    .getResult();
            if (result.size() == 1) {
                inputFile = result.get(0);
                sentences = ASTTokenReader.getAllTokenSequencesAndFixCertainErrors(reader, inputFile);
            } else {
                Log.err(this, "File '%s' not existing.", parentPath.resolve(childPath));
                sentences = Collections.emptyList();
            }
        }

        final int contextLength = order - 1;

        List<TokenWrapper> context = new ArrayList<>();
        List<TokenWrapper> possibleLineTokens = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder contextLine = new StringBuilder();

        // iterate over all line numbers
        // we start all over again after each line which is surely not very
        // efficient, but everything
        // else is getting much too complicated...
        for (ComparablePair<Integer, Integer> parsedLineNumber : lineNumbers) {

            // clear context list and list of possible line tokens at the start
            context.clear();
            possibleLineTokens.clear();

            boolean addedLine = false;

            // iterate over all tokens
            Iterator<List<TokenWrapper>> sentencesIterator = sentences.iterator();
            while (!addedLine && sentencesIterator.hasNext()) {
                Iterator<TokenWrapper> tokenIterator = sentencesIterator.next().iterator();

                // only use the context starting at the beginning of a method
                if (startFromMethods) {
                    context.clear();
                    possibleLineTokens.clear();
                }

                boolean endOfLineReached = false;
                TokenWrapper tokenWrapper = null;
                while (tokenIterator.hasNext()) {
                    tokenWrapper = tokenIterator.next();

                    // check if the token covers the first line, but starts
                    // before it
                    if (tokenWrapper.getStartLineNumber() < parsedLineNumber.first()
                            && tokenWrapper.getEndLineNumber() >= parsedLineNumber.first()) {
                        // discard the current possible line token list and add
                        // it to the context
                        context.addAll(possibleLineTokens);
                        possibleLineTokens.clear();
                        // start a new possible token list
                        possibleLineTokens.add(tokenWrapper);
                    } else if (tokenWrapper.getStartLineNumber() < parsedLineNumber.first()) {
                        // if the token still starts before the first line
                        // check if the list of possible line tokens is empty
                        if (possibleLineTokens.isEmpty()) {
                            // if so, then there has not been a token that
                            // covered the first parsed line, yet
                            context.add(tokenWrapper);
                        } else {
                            // otherwise, add it to the possible line token
                            // list, even if it does not cover the line itself
                            // (to keep the correct sequence of tokens)
                            possibleLineTokens.add(tokenWrapper);
                        }
                    } else if (tokenWrapper.getStartLineNumber() == parsedLineNumber.first()) {
                        // if the current token starts at the first line,
                        // discard the current possible line token list and add
                        // it to the context, since we found a matching token
                        context.addAll(possibleLineTokens);
                        possibleLineTokens.clear();
                        // then, add the token to the current line
                        line.append(tokenWrapper.getToken()).append(" ");
                    } else if (tokenWrapper.getStartLineNumber() <= parsedLineNumber.second()) {
                        // if the start of the token was somewhere between first
                        // and last parsed line, append previously stored tokens 
                        // that started before the first line, if any
                        appendPossibleLineTokens(possibleLineTokens, context, line);
                        // add the token to the current line
                        line.append(tokenWrapper.getToken()).append(" ");
                    } else {
                        // the start line number of the token is greater than the 
                        // end line number of the specified range...
                        // append any possible stored line tokens
                        appendPossibleLineTokens(possibleLineTokens, context, line);
                        // communicate the beginning of a new line
                        endOfLineReached = true;
                    }

                    // if it's the start of the next line
                    if (endOfLineReached) {
                        addLineToSentenceMap(
                                prefixForMap, contextLength, context, line, contextLine, parsedLineNumber,
                                tokenWrapper, tokenIterator);

                        addedLine = true;
                        break;
                    }

                }
            }

            if (!addedLine) {
                appendPossibleLineTokens(possibleLineTokens, context, line);
                addLineToSentenceMap(prefixForMap, contextLength, context, line, contextLine, parsedLineNumber, null, null);
            }
        }
    }

    private void appendPossibleLineTokens(List<TokenWrapper> possibleLineTokens, List<TokenWrapper> context,
                                          StringBuilder line) {
        // only use tokens from the last line
        if (!possibleLineTokens.isEmpty()) {
            int lastLineNumber = possibleLineTokens.get(possibleLineTokens.size() - 1).getStartLineNumber();
            for (TokenWrapper token : possibleLineTokens) {
                if (token.getStartLineNumber() == lastLineNumber) {
                    line.append(token.getToken()).append(" ");
                } else {
                    context.add(token);
                }
            }
        }
        // // restrict the number of tokens added to the line to 5...
        // int max = 5;
        // int size = possibleLineTokens.size();
        // // add the possible line token list to the current line
        // for (int i = 0; i < size; ++i) {
        // if (i < size - max) {
        // context.add(possibleLineTokens.get(i));
        // } else {
        // line.append(possibleLineTokens.get(i) + " ");
        // }
        // }
        // discard the current possible line token list
        possibleLineTokens.clear();
    }

    private void addLineToSentenceMap(String prefixForMap, final int contextLength, List<TokenWrapper> context,
                                      StringBuilder line, StringBuilder contextLine, ComparablePair<Integer, Integer> parsedLineNumber,
                                      TokenWrapper lastToken, Iterator<TokenWrapper> tokenIterator) {
        if (line.length() != 0) {
            // delete the last space
            line.deleteCharAt(line.length() - 1);
        }

        // add the context, if enabled
        if (use_context) {
            int index = context.size() - contextLength - preTokenCount;

            int count = 0;
            for (ListIterator<TokenWrapper> i = context.listIterator(index < 0 ? 0 : index); count < context.size()
                    - preTokenCount && count < contextLength && i.hasNext(); ++count) {
                contextLine.append(i.next().getToken()).append(" ");
            }
            contextLine.append(TokenizeLines.CONTEXT_TOKEN + " ");
        }

        // add a number of tokens before the actual line, if specified
        if (preTokenCount > 0) {
            int preTokenIndex = context.size() - preTokenCount;
            for (ListIterator<TokenWrapper> i = context.listIterator(preTokenIndex < 0 ? 0 : preTokenIndex); i.hasNext(); ) {
                contextLine.append(i.next().getToken()).append(" ");
            }
        }

        // add the line itself
        contextLine.append(line);

        // add a number of tokens after the actual line, if specified
        if (postTokenCount > 0) {
            if (lastToken != null && tokenIterator != null) {
                contextLine.append(" ").append(lastToken.getToken());
                int count = 1;
                while (count < postTokenCount && tokenIterator.hasNext()) {
                    ++count;
                    contextLine.append(" ").append(tokenIterator.next().getToken());
                }
            }
        }

        // add the line to the map
        sentenceMap.put(prefixForMap + ":" + parsedLineNumber.first(), contextLine.toString());

        // reuse the StringBuilders
        contextLine.setLength(0);
        line.setLength(0);
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }

    @Override
    public Map<String, String> processItem(Map<String, Set<ComparablePair<Integer, Integer>>> map) {
        createTokenizedLinesOutput(map);
        return sentenceMap;
    }

}
