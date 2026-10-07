# Q5 stock reservation: why an atomic conditional update

Each order line runs `update products set stock = stock - :qty where id = :id and stock >= :qty` inside one transaction. One row updated means reserved; zero means missing (404) or short (409). Any failure rolls back every earlier line, so an order is all-or-nothing.

| | Atomic conditional update (chosen) | Pessimistic lock (`SELECT ... FOR UPDATE`) | Optimistic lock (`@Version` + retry) |
|---|---|---|---|
| Correctness | The database checks and decrements in one statement under the row lock, so stock cannot go negative. | Correct: read, check and write while holding the row lock. | Correct, but losers get `OptimisticLockException` and must retry, or they fail even when stock is left. |
| Throughput | One round trip per line, and the lock is held only from the update to commit. | Two round trips per line, with the lock held across the read, the Java check and the write. | No locks, but under a hot product most attempts conflict and retry, wasting work. |
| Complexity | One repository method. The 409 message needs one extra read only on failure. | A `@Lock(PESSIMISTIC_WRITE)` finder plus entity logic, and the lock timeout needs handling. | A version column, a retry loop with backoff, and a retry budget to tune. |

Deadlocks are avoided by merging duplicate product ids and updating rows in ascending id order.

Idempotency: `(customer_email, idempotency_key)` is unique, and the order row is inserted and flushed *before* any stock is reserved, so the key is claimed first. A concurrent request with the same key blocks on the unique index until the first transaction finishes and never touches stock: if the first commits, its insert fails and it returns the stored order with 200 (even when that order took the last unit); if the first rolls back, it proceeds as a fresh order. Only a violation of `uk_orders_customer_idempotency_key` is treated as a retry race; any other integrity error is rethrown. A request hash (SHA-256 of the canonical items) turns key reuse with a different body into a 409.

Cancel uses a guarded `update orders set status = 'CANCELLED' where id = :id and status = 'PLACED'`. Only one concurrent cancel can update the row, so stock is restored once, without a `@Version` column or retry handling.
