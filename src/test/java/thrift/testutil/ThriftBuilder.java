package thrift.testutil;

import thrift.model.Thrift;
import thrift.model.transaction.Transaction;

/**
 * A utility class to help with building Thrift objects.
 * Example usage: <br>
 *     {@code Thrift ab = new ThriftBuilder().withPerson("John", "Doe").build();}
 */
public class ThriftBuilder {

    private Thrift thrift;

    public ThriftBuilder() {
        thrift = new Thrift();
    }

    public ThriftBuilder(Thrift thrift) {
        this.thrift = thrift;
    }

    /**
     * Adds a new {@code Transaction} to the {@code Thrift} that we are building.
     */
    public ThriftBuilder withTransaction(Transaction transaction) {
        thrift.addTransaction(transaction);
        return this;
    }

    public Thrift build() {
        return thrift;
    }
}
