package se.de.hu_berlin.informatik.astlmbuilder.mapping.keywords;

/**
 * Interface to get keywords for token generation
 */
public interface IKeyWordProvider<T> extends IBasicKeyWords {

    // order is important here, as we use the ordinals as short keywords
    public enum KeyWords {
        BLOCK_STMT,
        WHILE_STMT,
        TRY_STMT,
        THROW_STMT,
        SYNCHRONIZED_STMT,
        SWITCH_STMT,
        SWITCH_ENTRY_STMT,
        RETURN_STMT,
        LABELED_STMT,
        IF_STMT,
        FOR_STMT,
        FOR_EACH_STMT,
        EXPRESSION_STMT,
        EXPL_CONSTR_INVOC_STMT,
        DO_STMT,
        CONTINUE_STMT,
        CATCH_CLAUSE_STMT,
        VARIABLE_DECLARATION_EXPRESSION,
        TYPE_EXPRESSION,
        SUPER_EXPRESSION,
        NULL_LITERAL_EXPRESSION,
        METHOD_REFERENCE_EXPRESSION,
        LAMBDA_EXPRESSION,
        INSTANCEOF_EXPRESSION,
        FIELD_ACCESS_EXPRESSION,
        CONDITIONAL_EXPRESSION,
        CLASS_EXPRESSION,
        CAST_EXPRESSION,
        ASSIGN_EXPRESSION,
        ARRAY_INIT_EXPRESSION,
        ARRAY_CREATE_EXPRESSION,
        ARRAY_ACCESS_EXPRESSION,
        NAME,
        SIMPLE_NAME,
        LOCAL_CLASS_DECLARATION_STMT,
        ARRAY_TYPE,
        ARRAY_CREATION_LEVEL,
        BREAK,
        PARAMETER,
        ENCLOSED_EXPRESSION,
        ASSERT_STMT,
        MEMBER_VALUE_PAIR,
        TYPE_PRIMITIVE,
        TYPE_UNION,
        TYPE_INTERSECTION,
        TYPE_PAR,
        TYPE_WILDCARD,
        TYPE_VOID,
        TYPE_UNKNOWN,
        CLASS_OR_INTERFACE_TYPE,
        BINARY_EXPRESSION,
        UNARY_EXPRESSION,
        METHOD_CALL_EXPRESSION,
        NAME_EXPRESSION,
        INTEGER_LITERAL_EXPRESSION,
        DOUBLE_LITERAL_EXPRESSION,
        STRING_LITERAL_EXPRESSION,
        BOOLEAN_LITERAL_EXPRESSION,
        CHAR_LITERAL_EXPRESSION,
        LONG_LITERAL_EXPRESSION,
        THIS_EXPRESSION,
        OBJ_CREATE_EXPRESSION,
        MARKER_ANNOTATION_EXPRESSION,
        NORMAL_ANNOTATION_EXPRESSION,
        SINGLE_MEMBER_ANNOTATION_EXPRESSION,
        EMPTY_STMT,
        COMPILATION_UNIT,
        CONSTRUCTOR_DECLARATION,
        INITIALIZER_DECLARATION,
        ENUM_CONSTANT_DECLARATION,
        VARIABLE_DECLARATOR,
        ENUM_DECLARATION,
        ANNOTATION_DECLARATION,
        ANNOTATION_MEMBER_DECLARATION,
        CLASS_OR_INTERFACE_DECLARATION,
        METHOD_DECLARATION,
        PACKAGE_DECLARATION,
        IMPORT_DECLARATION,
        FIELD_DECLARATION,

        MODULE_DECLARATION,
        MODULE_EXPORTS_STMT,
        MODULE_OPENS_STMT,
        MODULE_USES_STMT,
        MODULE_PROVIDES_STMT,
        MODULE_REQUIRES_STMT,

        LINE_COMMENT,
        BLOCK_COMMENT,
        JAVADOC_COMMENT,

        UNKNOWN,

        NULL,
        NULL_LIST,
        EMPTY_LIST,

//		CLOSING_MDEC,
//		CLOSING_CNSTR,
//		CLOSING_IF,
//		CLOSING_WHILE,
//		CLOSING_FOR,
//		CLOSING_TRY,
//		CLOSING_CATCH,
//		CLOSING_FOR_EACH,
//		CLOSING_DO,
//		CLOSING_SWITCH,
//		CLOSING_ENCLOSED,
//		CLOSING_BLOCK_STMT,
//		CLOSING_EXPRESSION_STMT,
//		CLOSING_COMPILATION_UNIT,
    }

    public KeyWords StringToKeyWord(String token) throws IllegalArgumentException;

    public T getKeyWord(KeyWords keyWord);

//	public T getClosingKeyWord(KeyWords keyWord);

    public T markAsClosing(T mapping);

    public boolean isMarkedAsClosing(T mapping);

}
