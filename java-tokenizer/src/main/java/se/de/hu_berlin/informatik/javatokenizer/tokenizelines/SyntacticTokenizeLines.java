package se.de.hu_berlin.informatik.javatokenizer.tokenizelines;

import se.de.hu_berlin.informatik.javatokenizer.tokenizer.Tokenizer;
import se.de.hu_berlin.informatik.utils.miscellaneous.ComparablePair;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
public class SyntacticTokenizeLines extends AbstractProcessor<Map<String, Set<ComparablePair<Integer, Integer>>>, Map<String, String>> {

    private final String src_path;
    private final boolean use_context;
    private final boolean startFromMethods;
    private final int order;
    private final boolean use_lookahead;

    //maps trace file lines to sentences
    private final Map<String, String> sentenceMap;

    /**
     * Creates a new {@link SyntacticTokenizeLines} object with the given parameters.
     *
     * @param src_path         is the path to the source folder
     * @param use_context      sets if each sentence should contain a context of previous tokens
     * @param startFromMethods sets if the context (if used) should only go back to the start of a method. (Currently, the implementation
     *                         only goes back to the last opening curly bracket, which doesn't have to be the start of a method.)
     * @param order            the n-gram order (only important for the length of the context)
     * @param use_lookahead    sets if for each line, the next line should also be appended to the sentence
     */
    public SyntacticTokenizeLines(String src_path, boolean use_context, boolean startFromMethods,
                                  int order, boolean use_lookahead) {
        this.sentenceMap = new HashMap<>();
        this.src_path = src_path;
        this.use_context = use_context;
        this.startFromMethods = startFromMethods;
        this.order = order;
        this.use_lookahead = use_lookahead;
    }

    /**
     * Tokenizes the specified lines in all files provided in the given {@link HashMap}
     * and updates the sentence map.
     *
     * @param map              holds source code file paths, each associated with an {@link ArrayList} of line numbers.
     * @param use_context      sets if each sentence should contain a context of previous tokens
     * @param startFromMethods sets if the context (if used) should only go back to the start of a method. (Currently, the implementation
     *                         only goes back to the last opening curly bracket, which doesn't have to be the start of a method.)
     * @param order            the n-gram order (only important for the length of the context)
     * @param use_lookahead    sets if for each line, the next line should also be appended to the sentence
     */
    private void createTokenizedLinesOutput(
            final Map<String, Set<ComparablePair<Integer, Integer>>> map,
            final boolean use_context, final boolean startFromMethods,
            final int order, final boolean use_lookahead) {

        for (Entry<String, Set<ComparablePair<Integer, Integer>>> i : map.entrySet()) {
            createTokenizedLinesOutput(i.getKey(), Paths.get(src_path + File.separator + i.getKey()), i.getValue(), use_context, startFromMethods, order, use_lookahead);
        }
    }

    /**
     * Tokenizes the specified lines in the given file and updates the sentence map.
     *
     * @param prefixForMap     a prefix (path) that completes a trace file line together with a line number
     * @param inputFile        holds the {@link Path} to a Java source code file
     * @param lineNumbers      holds the line numbers of the lines to be tokenized
     * @param use_context      sets if each sentence should contain a context of previous tokens
     * @param startFromMethods sets if the context (if used) should only go back to the start of a method. (Currently, the implementation
     *                         only goes back to the last opening curly bracket, which doesn't have to be the start of a method.)
     * @param order            the n-gram order (only important for the length of the context)
     * @param use_lookahead    sets if for each line, the next line should also be appended to the sentence
     */
    private void createTokenizedLinesOutput(
            String prefixForMap,
            final Path inputFile, final Set<ComparablePair<Integer, Integer>> lineNumbers,
            final boolean use_context, boolean startFromMethods,
            final int order, final boolean use_lookahead) {
        //try opening the file
        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8)) {
            StreamTokenizer st = new StreamTokenizer(reader);
            createTokenizedLinesOutput(prefixForMap, st, lineNumbers, use_context, startFromMethods, order, use_lookahead);
        } catch (IOException x) {
            Log.err(this, "IOexception on file %s. Adding empty strings for corresponding lines.", inputFile);
//			for (int lineNo : lineNumbers) {
//				sentenceMap.put(prefixForMap + ":" + String.valueOf(lineNo), "");
//			}
        }
    }

    /**
     * Tokenizes the specified lines in the file that is the input for the given {@link StreamTokenizer}
     * and updates the sentence map.
     *
     * @param prefixForMap         a prefix (path) that completes a trace file line together with a line number
     * @param inputStreamTokenizer has the input source code file as its input
     * @param lineNumbersSet       holds the line numbers of the lines to be tokenized
     * @param use_context          sets if each sentence should contain a context of previous tokens
     * @param startFromMethods     sets if the context (if used) should only go back to the start of a method. (Currently, the implementation
     *                             only goes back to the last opening curly bracket, which doesn't have to be the start of a method.)
     * @param order                the n-gram order (only important for the length of the context)
     * @param use_lookahead        sets if for each line, the next line should also be appended to the sentence
     * @throws IOException if
     */
    private void createTokenizedLinesOutput(String prefixForMap, final StreamTokenizer inputStreamTokenizer, final Set<ComparablePair<Integer, Integer>> lineNumbersSet,
                                            final boolean use_context, final boolean startFromMethods, final int order, final boolean use_lookahead) throws IOException {

        Tokenizer tokenizer = new Tokenizer(inputStreamTokenizer, true);

        //sort the line numbers
        List<ComparablePair<Integer, Integer>> lineNumbers = asSortedList(lineNumbersSet);

        final int contextLength = order - 1;

        String token;
        List<String> context = new ArrayList<>();
        List<String> nextContext = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder contextLine = new StringBuilder();
        StringBuilder lookAhead = new StringBuilder();

        ComparablePair<Integer, Integer> zeroPair = new ComparablePair<>(-1, -1);

        //parse the first line number
        ComparablePair<Integer, Integer> parsedLineNumber = zeroPair;
        int lineNumber_index = 0;
        try {
            parsedLineNumber = lineNumbers.get(lineNumber_index);
        } catch (Exception e) {
            Log.err(this, "not able to parse line number " + lineNumber_index);
        }

        boolean lastLineNeedsUpdate = false;
        int lastLineNo = 0;
        int ttype = 0;
        while (ttype != StreamTokenizer.TT_EOF) {
            if ((token = tokenizer.getNextToken()) != null) {
                lastLineNo = tokenizer.getLineNo();
                nextContext.add(token);
                if (parsedLineNumber.first() <= lastLineNo && lastLineNo <= parsedLineNumber.second()) {
                    line.append(token).append(" ");
                }
                lookAhead.append(token).append(" ");
            }
            ttype = tokenizer.getTtype();
            if (ttype == StreamTokenizer.TT_EOL || ttype == StreamTokenizer.TT_EOF) {
                if (use_lookahead && lastLineNeedsUpdate) {
                    if (lookAhead.length() > 0) {
                        lookAhead.deleteCharAt(lookAhead.length() - 1);
                        StringBuilder temp = new StringBuilder();
                        temp.append(sentenceMap.get(prefixForMap)).append(":")
                                .append(lineNumbers.get(lineNumber_index - 1).first())
                                .append(" ").append(lookAhead);
                        sentenceMap.put(prefixForMap + ":" + lineNumbers.get(lineNumber_index - 1).first(), temp.toString());
                        lastLineNeedsUpdate = false;
                    }
                }
                if (parsedLineNumber.second() <= lastLineNo && parsedLineNumber != zeroPair) {
                    if (line.length() != 0) {
                        //delete the last space
                        line.deleteCharAt(line.length() - 1);
                    }

                    if (use_context) {
                        int index = context.size() - contextLength;

                        for (ListIterator<String> i = context.listIterator(index < 0 ? 0 : index); i.hasNext(); ) {
                            String temp = i.next();
                            //only use context from the last open curly bracket on
                            //\TODO: context should start from start of methods...
                            if (startFromMethods && temp.compareTo("{") == 0) {
                                contextLine.setLength(0);
                            }
                            contextLine.append(temp).append(" ");
                        }
                        contextLine.append(TokenizeLines.CONTEXT_TOKEN + " ");
                    }
                    contextLine.append(line);

                    //add the line to the map
                    sentenceMap.put(prefixForMap + ":" + lineNumbers.get(lineNumber_index).first(), contextLine.toString());
//					Misc.out(prefixForMap + ":" + String.valueOf(lineNumbers.get(lineNumber_index)) + " -> " + contextLine.toString());
                    lastLineNeedsUpdate = true;
                    //reuse the StringBuilders
                    contextLine.setLength(0);
                    line.setLength(0);

                    try {
                        parsedLineNumber = lineNumbers.get(++lineNumber_index);
                    } catch (Exception e) {
                        parsedLineNumber = zeroPair;
                    }
                }
                if (lastLineNo < parsedLineNumber.first()) {
                    context.addAll(nextContext);
                    nextContext.clear();
                    lookAhead.setLength(0);
                }
            }
        }

        while (parsedLineNumber.first() > lastLineNo) {
            if (line.length() != 0) {
                //delete the last space
                line.deleteCharAt(line.length() - 1);
            }

            if (use_context) {
                int index = context.size() - contextLength;

                for (ListIterator<String> i = context.listIterator(index < 0 ? 0 : index); i.hasNext(); ) {
                    contextLine.append(i.next()).append(" ");
                }
                contextLine.append(TokenizeLines.CONTEXT_TOKEN + " ");
            }
            contextLine.append(line);

            //add the line to the map
            sentenceMap.put(prefixForMap + ":" + lineNumbers.get(lineNumber_index).first(), contextLine.toString());
//			Misc.out(prefixForMap + ":" + String.valueOf(lineNumbers.get(lineNumber_index)) + " -> " + contextLine.toString());

            //reuse the StringBuilders
            contextLine.setLength(0);
            line.setLength(0);

            try {
                parsedLineNumber = lineNumbers.get(++lineNumber_index);
            } catch (Exception e) {
                parsedLineNumber = zeroPair;
            }
        }
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }

    @Override
    public Map<String, String> processItem(Map<String, Set<ComparablePair<Integer, Integer>>> map) {
        createTokenizedLinesOutput(map, use_context, startFromMethods, order, use_lookahead);
        return sentenceMap;
    }

}
