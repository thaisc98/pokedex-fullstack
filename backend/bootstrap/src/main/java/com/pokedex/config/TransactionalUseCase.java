package com.pokedex.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The bootstrap transaction decorator (architecture.md §2.1): wraps a use-case port so every call runs inside one
 * {@link TransactionTemplate} transaction, so {@code application} needs no {@code @Transactional}. A runtime
 * exception rolls back and propagates unchanged. Only for use cases without HTTP or upstream calls.
 */
final class TransactionalUseCase {

    private TransactionalUseCase() {}

    static <T> T wrap(Class<T> port, T delegate, TransactionTemplate transactions) {
        Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(transactions, "transactions");
        InvocationHandler handler = new Handler(port, delegate, transactions);
        return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[] {port}, handler));
    }

    private record Handler(Class<?> port, Object delegate, TransactionTemplate transactions)
            implements InvocationHandler {

        @Override
        public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> args != null && proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> "Transactional[" + port.getSimpleName() + "]";
                };
            }
            return transactions.execute(status -> call(method, args));
        }

        private @Nullable Object call(Method method, @Nullable Object[] args) {
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new UndeclaredThrowableException(cause);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
