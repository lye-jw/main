package thrift.logic.parser;

import static thrift.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static thrift.commons.core.Messages.MESSAGE_UNKNOWN_COMMAND;
import static thrift.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import thrift.logic.commands.HelpCommand;

public class NoArgumentsCommandParserTest {

    @Test
    public void parse_unknownCommand_throwsParseException() {
        String userInput = "   ";
        NoArgumentsCommandParser invalidParser = new NoArgumentsCommandParser(userInput, null);

        assertParseFailure(invalidParser, userInput, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void parse_commandWithInvalidParam_throwsParseException() {
        String userInput = "help me";
        NoArgumentsCommandParser invalidParser = new NoArgumentsCommandParser(userInput, HelpCommand.MESSAGE_USAGE);
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);

        assertParseFailure(invalidParser, userInput, expectedMessage);
    }
}
