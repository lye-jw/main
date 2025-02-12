package thrift.model.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static thrift.testutil.Assert.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import thrift.commons.core.index.Index;
import thrift.logic.commands.CommandTestUtil;
import thrift.model.transaction.exceptions.TransactionNotFoundException;
import thrift.testutil.ExpenseBuilder;
import thrift.testutil.TypicalTransactions;

public class TransactionListTest {

    private final TransactionList transactionList = new TransactionList();

    @Test
    public void contains_nullTransaction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> transactionList.contains(null));
    }

    @Test
    public void contains_transactionNotInList_returnsFalse() {
        assertFalse(transactionList.contains(TypicalTransactions.LAKSA));
    }

    @Test
    public void contains_transactionInList_returnsTrue() {
        transactionList.add(TypicalTransactions.LAKSA);
        assertTrue(transactionList.contains(TypicalTransactions.LAKSA));
    }

    @Test
    public void add_nullTransaction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> transactionList.add(null));
    }

    @Test
    public void setTransaction_nullTargetTransaction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, ()
            -> transactionList.setTransaction(null, TypicalTransactions.LAKSA));
    }

    @Test
    public void setTransaction_nullUpdatedTransaction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, ()
            -> transactionList.setTransaction(TypicalTransactions.LAKSA, null));
    }

    @Test
    public void setTransaction_targetTransactionNotInList_throwsTransactionNotFoundException() {
        assertThrows(TransactionNotFoundException.class, () -> transactionList
                .setTransaction(TypicalTransactions.LAKSA, TypicalTransactions.LAKSA));
    }

    @Test
    public void setTransaction_updatedTransactionIsSameTransaction_success() {
        transactionList.add(TypicalTransactions.LAKSA);
        transactionList.setTransaction(TypicalTransactions.LAKSA, TypicalTransactions.LAKSA);
        TransactionList expectedTransactionList = new TransactionList();
        expectedTransactionList.add(TypicalTransactions.LAKSA);
        assertEquals(expectedTransactionList, transactionList);
    }

    @Test
    public void setTransaction_updatedTransactionHasSameIdentity_success() {
        transactionList.add(TypicalTransactions.LAKSA);
        Expense updatedExpense = new ExpenseBuilder(TypicalTransactions.LAKSA)
                .withDescription(CommandTestUtil.VALID_DESCRIPTION_AIRPODS)
                .withTags(CommandTestUtil.VALID_TAG_ACCESSORY)
                .build();
        transactionList.setTransaction(TypicalTransactions.LAKSA, updatedExpense);
        TransactionList expectedTransactionList = new TransactionList();
        expectedTransactionList.add(updatedExpense);
        assertEquals(expectedTransactionList, transactionList);
    }

    @Test
    public void setTransaction_updatedTransactionHasDifferentIdentity_success() {
        transactionList.add(TypicalTransactions.LAKSA);
        transactionList.setTransaction(TypicalTransactions.LAKSA, TypicalTransactions.PENANG_LAKSA);
        TransactionList expectedTransactionList = new TransactionList();
        expectedTransactionList.add(TypicalTransactions.PENANG_LAKSA);
        assertEquals(expectedTransactionList, transactionList);
    }

    @Test
    public void setTransactionWithIndex_nullIndex_throwsNullPointerException() {
        assertThrows(NullPointerException.class, ()
            -> transactionList.setTransactionWithIndex(null, TypicalTransactions.LAKSA));
    }

    @Test
    public void setTransactionWithIndex_nullUpdatedTransaction_throwsNullPointerException() {
        transactionList.add(TypicalTransactions.LAKSA);
        assertThrows(NullPointerException.class, ()
            -> transactionList.setTransactionWithIndex(Index.fromZeroBased(0), null));
    }

    @Test
    public void setTransactionWithIndex_invalidIndex_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> transactionList
                .setTransactionWithIndex(Index.fromOneBased(0), TypicalTransactions.LAKSA));
    }

    @Test
    public void remove_nullTransaction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> transactionList.remove(null));
    }

    @Test
    public void remove_transactionDoesNotExist_throwsTransactionNotFoundException() {
        assertThrows(TransactionNotFoundException.class, () -> transactionList.remove(TypicalTransactions.LAKSA));
    }

    @Test
    public void remove_existingTransaction_removesTransaction() {
        transactionList.add(TypicalTransactions.LAKSA);
        transactionList.remove(TypicalTransactions.LAKSA);
        TransactionList expectedTransactionList = new TransactionList();
        assertEquals(expectedTransactionList, transactionList);
    }

    @Test
    public void setTransactions_nullTransactionList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> transactionList.setTransactions((TransactionList) null));
    }

    @Test
    public void setTransactions_transactionList_replacesOwnListWithProvidedTransactionList() {
        transactionList.add(TypicalTransactions.LAKSA);
        TransactionList expectedTransactionList = new TransactionList();
        expectedTransactionList.add(TypicalTransactions.PENANG_LAKSA);
        transactionList.setTransactions(expectedTransactionList);
        assertEquals(expectedTransactionList, transactionList);
    }

    @Test
    public void setTransactions_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> transactionList.setTransactions((List<Transaction>) null));
    }

    @Test
    public void setTransactions_list_replacesOwnListWithProvidedList() {
        transactionList.add(TypicalTransactions.LAKSA);
        List<Transaction> transactionList = Collections.singletonList(TypicalTransactions.PENANG_LAKSA);
        this.transactionList.setTransactions(transactionList);
        TransactionList expectedTransactionList = new TransactionList();
        expectedTransactionList.add(TypicalTransactions.PENANG_LAKSA);
        assertEquals(expectedTransactionList, this.transactionList);
    }

    @Test
    public void asUnmodifiableObservableList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, ()
            -> transactionList.asUnmodifiableObservableList().remove(0));
    }
}
