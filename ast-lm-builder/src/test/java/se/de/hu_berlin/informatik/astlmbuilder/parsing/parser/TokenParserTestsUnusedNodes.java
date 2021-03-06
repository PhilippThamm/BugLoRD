package se.de.hu_berlin.informatik.astlmbuilder.parsing.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import junit.framework.TestCase;
import se.de.hu_berlin.informatik.astlmbuilder.mapping.keywords.KeyWordConstants;
import se.de.hu_berlin.informatik.astlmbuilder.mapping.keywords.KeyWordConstantsShort;
import se.de.hu_berlin.informatik.astlmbuilder.mapping.mapper.IBasicNodeMapper;
import se.de.hu_berlin.informatik.astlmbuilder.mapping.mapper.Node2AbstractionMapper;
import se.de.hu_berlin.informatik.astlmbuilder.parsing.InfoWrapperBuilder;
import se.de.hu_berlin.informatik.astlmbuilder.parsing.InformationWrapper;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;

/**
 * This class stores all tests that should currently not be executed because we create no nodes
 * for those node types
 *
 * @author Roy Lieck
 */
public class TokenParserTestsUnusedNodes extends TestCase {
    private static final int testDepth = 3;

    final ITokenParser t_parser_long = new SimpleTokenParser(new KeyWordConstants());
    final IBasicNodeMapper<String> mapper_long = new Node2AbstractionMapper.Builder(new KeyWordConstants())
            .usesStringAbstraction()
//			.usesVariableNameAbstraction()
//			.usesPrivateMethodAbstraction()
//			.usesClassNameAbstraction()
//			.usesMethodNameAbstraction()
//			.usesGenericTypeNameAbstraction()
            .build();

    final ITokenParser t_parser_short = new SimpleTokenParser(new KeyWordConstantsShort());
    final IBasicNodeMapper<String> mapper_short = new Node2AbstractionMapper.Builder(new KeyWordConstantsShort())
            .usesStringAbstraction()
//			.usesVariableNameAbstraction()
//			.usesPrivateMethodAbstraction()
//			.usesClassNameAbstraction()
//			.usesMethodNameAbstraction()
//			.usesGenericTypeNameAbstraction()
            .build();

    public void testTokenParserBlockCommentParent() {
        testTokenParserBlockComment(mapper_short, t_parser_short);
        testTokenParserBlockComment(mapper_long, t_parser_long);
    }

    private void testTokenParserBlockComment(IBasicNodeMapper<String> mapper, ITokenParser parser) {

        //String content
        Node node = new BlockComment(
                "JustAContentString?");

        //using the mapper here instead of fixed tokens spares us from fixing the tests when we change the mapping or the keywords around
        String token = mapper.getMappingForNode(node, null, testDepth, false, null);
        Log.out(this, token); //output the token for debugging purposes

        InformationWrapper info = InfoWrapperBuilder.buildInfoWrapperForNode(node); // This may be filled with data later on

        // we are not parsing block comments even though we may create tokens with them
        // this is why the test for it ends before the parsing
        Node parsedNode = parser.parseNodeFromToken(token, info);

        assertTrue(parsedNode instanceof BlockComment);

        BlockComment castedNode = (BlockComment) parsedNode;

        assertNotNull(castedNode.getContent());
        assertEquals("JustAContentString?", castedNode.getContent());

    }

    public void testTokenParserLineCommentParent() {
        testTokenParserLineComment(mapper_short, t_parser_short);
        testTokenParserLineComment(mapper_long, t_parser_long);
    }

    private void testTokenParserLineComment(IBasicNodeMapper<String> mapper, ITokenParser parser) {

        //String content
        Node node = new LineComment(
                "LineComment");

        //using the mapper here instead of fixed tokens spares us from fixing the tests when we change the mapping or the keywords around
        String token = mapper.getMappingForNode(node, null, testDepth, false, null);
        Log.out(this, token); //output the token for debugging purposes

        InformationWrapper info = InfoWrapperBuilder.buildInfoWrapperForNode(node); // This may be filled with data later on

        Node parsedNode = parser.parseNodeFromToken(token, info);

        assertTrue(parsedNode instanceof LineComment);

        LineComment castedNode = (LineComment) parsedNode;

        assertNotNull(castedNode.getContent());
        assertEquals("LineComment", castedNode.getContent());
    }

    public void testTokenParserJavadocCommentParent() {
        testTokenParserJavadocComment(mapper_short, t_parser_short);
        testTokenParserJavadocComment(mapper_long, t_parser_long);
    }

    private void testTokenParserJavadocComment(IBasicNodeMapper<String> mapper, ITokenParser parser) {

        //String content
        Node node = new JavadocComment(
                "JavadocComment");

        //using the mapper here instead of fixed tokens spares us from fixing the tests when we change the mapping or the keywords around
        String token = mapper.getMappingForNode(node, null, testDepth, false, null);
        Log.out(this, token); //output the token for debugging purposes

        InformationWrapper info = InfoWrapperBuilder.buildInfoWrapperForNode(node); // This may be filled with data later on

        Node parsedNode = parser.parseNodeFromToken(token, info);

        assertTrue(parsedNode instanceof JavadocComment);

        JavadocComment castedNode = (JavadocComment) parsedNode;

        assertNotNull(castedNode.getContent());
        assertEquals("JavadocComment", castedNode.getContent());
    }

}
