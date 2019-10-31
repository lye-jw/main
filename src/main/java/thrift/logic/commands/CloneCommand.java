package thrift.logic.commands;

import static java.util.Objects.requireNonNull;
import static thrift.commons.util.CollectionUtil.requireAllNonNull;
import static thrift.model.transaction.TransactionDate.DATE_FORMATTER;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import thrift.commons.core.LogsCenter;
import thrift.commons.core.Messages;
import thrift.commons.core.index.Index;
import thrift.logic.commands.exceptions.CommandException;
import thrift.logic.parser.CliSyntax;
import thrift.model.Model;
import thrift.model.clone.Occurrence;
import thrift.model.tag.Tag;
import thrift.model.transaction.Description;
import thrift.model.transaction.Expense;
import thrift.model.transaction.Income;
import thrift.model.transaction.Remark;
import thrift.model.transaction.Transaction;
import thrift.model.transaction.TransactionDate;
import thrift.model.transaction.Value;
import thrift.ui.TransactionListPanel;

/**
 * Clones a transaction specified by its index in THRIFT.
 */
public class CloneCommand extends ScrollingCommand implements Undoable {

    private static final Logger logger = LogsCenter.getLogger(CloneCommand.class);

    public static final String COMMAND_WORD = "clone";

    public static final String HELP_MESSAGE = COMMAND_WORD
            + ": Clones the transaction specified by its index number used in the displayed transaction list.\n"
            + "Format: "
            + COMMAND_WORD + " " + CliSyntax.PREFIX_INDEX + "INDEX (must be a positive integer)\n"
            + "[" + CliSyntax.PREFIX_OCCURRENCE
            + "OCCURRENCE (FREQUENCY:NUMBER_OF_OCCURRENCES)]"
            + "\n- Valid FREQUENCY values are: \"daily\", \"weekly\", \"monthly\", \"yearly\"."
            + "\n- Valid NUMBER_OF_OCCURRENCES ranges are: 0 - 5 with \"yearly\", 0 - 12 with other frequencies.\n"
            + "If NUMBER_OF_OCCURRENCES is 0, one clone will be created on the same date as original "
            + "transaction as if\nno " + CliSyntax.PREFIX_OCCURRENCE + "OCCURRENCE parameter was input."
            + "\nPossible usage of " + COMMAND_WORD + ": \n"
            + "To clone the transaction at index 8 in the displayed transaction list: "
            + COMMAND_WORD + " " + CliSyntax.PREFIX_INDEX + "8\n"
            + "To clone the transaction at index 8 5 times across next 5 months (including current month): "
            + COMMAND_WORD + " " + CliSyntax.PREFIX_INDEX + "8 " + CliSyntax.PREFIX_OCCURRENCE + "monthly:5";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Clones the transaction specified by its index number used in the displayed transaction list.\n"
            + "Parameters: " + CliSyntax.PREFIX_INDEX + "INDEX (must be a positive integer) "
            + "[" + CliSyntax.PREFIX_OCCURRENCE
            + "OCCURRENCE (FREQUENCY:NUMBER_OF_OCCURRENCES)]"
            + "\n- Valid FREQUENCY values are: \"daily\", \"weekly\", \"monthly\", \"yearly\"."
            + "\n- Valid NUMBER_OF_OCCURRENCES ranges are: 0 - 5 with \"yearly\", 0 - 12 with other frequencies.\n"
            + "If NUMBER_OF_OCCURRENCES is 0, one clone will be created on the same date as original "
            + "transaction as if\nno " + CliSyntax.PREFIX_OCCURRENCE + "OCCURRENCE parameter was input."
            + "\nExample: " + COMMAND_WORD + " " + CliSyntax.PREFIX_INDEX + "1 "
            + CliSyntax.PREFIX_OCCURRENCE + "monthly:5";

    public static final String MESSAGE_CLONE_TRANSACTION_SUCCESS = "Cloned transaction: %1$s";
    public static final String MESSAGE_NUM_CLONED_TRANSACTIONS = "(Cloned %s %d time(s))";

    public static final String UNDO_SUCCESS = "Deleted cloned transaction(s):\n%1$s";
    public static final String REDO_SUCCESS = "Added cloned transaction(s):\n%1$s";

    private final Index targetIndex;
    private final Occurrence occurrence;
    private int frequencyCalendarField;
    private ArrayList<Transaction> clonedTransactionList;

    /**
     * Creates a CloneCommand instance to clone an {@code Expense} or {@code Income}
     *
     * @param targetIndex from the displayed list of the transaction to be cloned
     * @param occurrence representing frequency and number of times cloned items occur
     */
    public CloneCommand(Index targetIndex, Occurrence occurrence) {
        requireNonNull(targetIndex);
        requireNonNull(occurrence);
        assert targetIndex.getZeroBased() >= 0 : "Index of transaction to be cloned is negative";
        assert occurrence.getNumOccurrences() >= 0 : "Number of occurrences for clones not positive integer";
        this.targetIndex = targetIndex;
        this.occurrence = occurrence;
        this.clonedTransactionList = new ArrayList<>();
        this.frequencyCalendarField = 0;
    }

    @Override
    public CommandResult execute(Model model, TransactionListPanel transactionListPanel) throws CommandException {
        requireNonNull(model);
        List<Transaction> lastShownList = model.getFilteredTransactionList();

        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_TRANSACTION_DISPLAYED_INDEX);
        }

        Transaction transactionToClone = lastShownList.get(targetIndex.getZeroBased());
        Date originalDate = transactionToClone.getDate().getDate();

        frequencyCalendarField = occurrence.getFrequencyCalendarField();
        assert (frequencyCalendarField == Calendar.DATE) || (frequencyCalendarField == Calendar.WEEK_OF_YEAR) ||
                (frequencyCalendarField == Calendar.MONTH) || (frequencyCalendarField == Calendar.YEAR)
                : "Frequency of occurrence for clones not converted to a valid Calendar field";

        for (int i = 0; i <= occurrence.getNumOccurrences(); i++) {
            // If cloning multiple times, skip step to clone a transaction on date of original transaction
            if (occurrence.getNumOccurrences() > 0 && i == 0) {
                continue;
            }

            // Produce the date attribute of cloned transaction
            Calendar calendar = Calendar.getInstance();
            if (occurrence.getNumOccurrences() > 0) {
                calendar.setTime(originalDate);
            }
            calendar.add(frequencyCalendarField, i);
            String date = DATE_FORMATTER.format(calendar.getTime());

            Transaction clonedTransaction = createClonedTransaction(transactionToClone, date);
            clonedTransactionList.add(clonedTransaction);
            if (clonedTransaction instanceof Expense) {
                model.addExpense((Expense) clonedTransaction);
            } else {
                assert clonedTransaction instanceof Income : "Transaction to Clone not of type Expense or Income";
                model.addIncome((Income) clonedTransaction);
            }

            // Use null comparison instead of requireNonNull(transactionListPanel) as current JUnit tests are unable to
            // handle JavaFX initialization
            if (transactionListPanel != null && model.isInView(clonedTransaction)) {
                transactionListPanel.getTransactionListView().scrollTo(model.getFilteredTransactionList().size() - 1);
            }

            // For single Clone operation, end Cloning after one iteration
            if (occurrence.getNumOccurrences() == 0) {
                logger.info("CLONED transaction [ " + clonedTransaction + " ] [1 TIME]");
                break;
            }

            logger.info("CLONED transaction [ " + clonedTransaction + " ] [" + i + " TIMES]");
        }

        int actualTimesCloned = (occurrence.getNumOccurrences() == 0) ? 1 : occurrence.getNumOccurrences();
        String actualFrequency = (occurrence.getNumOccurrences() == 0) ? "for today" : occurrence.getFrequency();
        return new CommandResult(String.format(MESSAGE_CLONE_TRANSACTION_SUCCESS, transactionToClone)
                + "\n" + String.format(MESSAGE_NUM_CLONED_TRANSACTIONS, actualFrequency, actualTimesCloned));
    }

    /**
     * Creates a clone of the transaction at {@link #targetIndex} of the displayed list.
     *
     * @param transactionToClone {@link Transaction} that a clone should be created of, with current Date.
     * @param date that the cloned Transaction should contain.
     * @return {@link Expense} or {@link Income} clone of {@code transactionToClone} containing current Date.
     */
    private Transaction createClonedTransaction(Transaction transactionToClone, String date) {

        Description clonedDescription = transactionToClone.getDescription();
        Value clonedValue = transactionToClone.getValue();
        Remark clonedRemark = transactionToClone.getRemark();
        Set<Tag> clonedTags = transactionToClone.getTags();
        TransactionDate currentDate = new TransactionDate(date);
        if (transactionToClone instanceof Expense) {
            return new Expense(clonedDescription, clonedValue, clonedRemark, currentDate, clonedTags);
        } else {
            return new Income(clonedDescription, clonedValue, clonedRemark, currentDate, clonedTags);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof CloneCommand // instanceof handles nulls
                && targetIndex.equals(((CloneCommand) other).targetIndex))
                && occurrence.equals(((CloneCommand) other).occurrence); // state check
    }

    @Override
    public String undo(Model model) {
        requireAllNonNull(model, occurrence);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= occurrence.getNumOccurrences(); i++) {
            Transaction deleteTransaction = model.deleteLastTransaction();
            sb.append(deleteTransaction).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        return String.format(UNDO_SUCCESS, sb.toString());
    }

    @Override
    public String redo(Model model) {
        requireAllNonNull(model, occurrence);
        assert frequencyCalendarField == Calendar.DATE || frequencyCalendarField == Calendar.WEEK_OF_YEAR
                || frequencyCalendarField == Calendar.MONTH || frequencyCalendarField == Calendar.YEAR;
        assert clonedTransactionList.size() > 0;

        StringBuilder sb = new StringBuilder();
        for (Transaction clonedTransaction : clonedTransactionList) {
            if (clonedTransaction instanceof Expense) {
                model.addExpense((Expense) clonedTransaction);
            } else if (clonedTransaction instanceof Income) {
                model.addIncome((Income) clonedTransaction);
            }
            sb.append(clonedTransaction).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        return String.format(REDO_SUCCESS, sb.toString());
    }
}
