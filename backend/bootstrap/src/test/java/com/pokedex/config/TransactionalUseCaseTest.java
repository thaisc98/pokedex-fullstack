package com.pokedex.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Architecture §2.1 "transaction decorator in bootstrap": a use case port is wrapped so every call runs inside one
 * {@link TransactionTemplate} transaction, keeping {@code @Transactional} out of {@code application}.
 */
class TransactionalUseCaseTest {

    /** A one-method port, like every use case port. */
    interface Greeter {
        String greet(String name);
    }

    private final RecordingTransactionManager transactions = new RecordingTransactionManager();

    @Test
    void should_run_the_delegate_inside_a_committed_transaction() {
        Greeter greeter = TransactionalUseCase.wrap(
                Greeter.class,
                name -> {
                    assertThat(transactions.active).as("inside a transaction").isTrue();
                    return "hello " + name;
                },
                new TransactionTemplate(transactions));

        assertThat(greeter.greet("ash")).isEqualTo("hello ash");
        assertThat(transactions.events).containsExactly("begin", "commit");
    }

    @Test
    void should_use_the_template_settings_such_as_read_only() {
        TransactionTemplate readOnly = new TransactionTemplate(transactions);
        readOnly.setReadOnly(true);

        TransactionalUseCase.wrap(Greeter.class, name -> name, readOnly).greet("ash");

        assertThat(transactions.lastDefinition).isNotNull();
        assertThat(transactions.lastDefinition.isReadOnly()).isTrue();
    }

    @Test
    void should_roll_back_and_rethrow_the_same_exception() {
        IllegalStateException failure = new IllegalStateException("boom");
        Greeter greeter = TransactionalUseCase.wrap(
                Greeter.class,
                name -> {
                    throw failure;
                },
                new TransactionTemplate(transactions));

        assertThatThrownBy(() -> greeter.greet("ash")).isSameAs(failure);
        assertThat(transactions.events).containsExactly("begin", "rollback");
    }

    @Test
    void should_answer_object_methods_without_a_transaction() {
        Greeter delegate = name -> name;
        Greeter greeter = TransactionalUseCase.wrap(Greeter.class, delegate, new TransactionTemplate(transactions));

        assertThat(greeter.toString()).contains("Transactional");
        assertThat(greeter).isEqualTo(greeter).hasSameHashCodeAs(greeter);
        assertThat(transactions.events).isEmpty();
    }

    /** Records begin/commit/rollback; no database. */
    static final class RecordingTransactionManager implements PlatformTransactionManager {
        final List<String> events = new ArrayList<>();
        boolean active;

        @Nullable TransactionDefinition lastDefinition;

        @Override
        public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
            lastDefinition = definition;
            active = true;
            events.add("begin");
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            active = false;
            events.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status) {
            active = false;
            events.add("rollback");
        }
    }
}
