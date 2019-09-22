package seedu.address.logic.parser;

import static seedu.address.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {
    private static final String REMARK_PLACEHOLDER = "Test remark";
    private RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_validRemarkPresent_success() {
        String userInput = INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_REMARK + REMARK_PLACEHOLDER;
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(REMARK_PLACEHOLDER));
        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_validEmptyRemark_success() {
        String userInput = INDEX_FIRST_PERSON.getOneBased() + " " + PREFIX_REMARK;
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_invalidRemarkInput_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

        // "remark" not followed by anything
        assertParseFailure(parser, RemarkCommand.COMMAND_WORD, expectedMessage);

        // "remark INDEX REMARK" without "r/" prefix
        assertParseFailure(parser, RemarkCommand.COMMAND_WORD + INDEX_FIRST_PERSON.getOneBased() + " "
                + REMARK_PLACEHOLDER, expectedMessage);

        // "remark r/REMARK" without index
        assertParseFailure(parser, RemarkCommand.COMMAND_WORD + PREFIX_REMARK + " " + REMARK_PLACEHOLDER,
                expectedMessage);
    }
}
