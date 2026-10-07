# Q5 stock reservation: why an atomic conditional update

Each order line runs `update products set stock = stock - :qty where id = :id and stock >= :qty` inside one transaction. One row updated means reserved; zero means missing (404) or short (409). Any failure rolls back every earlier line, so an order is all-or-nothing.

| | Atomic conditional update (chosen) | Pessimistic lock (`SELECT ... FOR UPDATE`) | Optimistic lock (`@Version` + retry) |
|---|---|---|---|
| Correctness | The database checks and decrements in one statement under the row lock, so stock cannot go negative. | Correct: read, check and write while holding the row lock. | Correct, but losers get `OptimisticLockException` and must retry, or they fail even when stock is left. |
| Throughput | One round trip per line, and the lock is held only from the update to commit. | Two round trips per line, with the lock held across the read, the Java check and the write. | No locks, but under a hot product most attempts conflict and retry, wasting work. |
| Complexity | One repository method. The 409 message needs one extra read only on failure. | A `@Lock(PESSIMISTIC_WRITE)` finder plus entity logic, and the lock timeout needs handling. | A version column, a retry loop with backoff, and a retry budget to tune. |

Deadlocks are avoided by merging duplicate product ids and updating rows in ascending id order.

Idempotency: `(customer_email, idempotency_key)` is unique. A replay returns the stored order with 200. A concurrent duplicate fails on insert, its transaction (including its stock reservation) rolls back, and the caller then reads and returns the winner's order in a new transaction. A request hash (SHA-256 of the canonical items) turns key reuse with a different body into a 409.

Cancel uses a guarded `update orders set status = 'CANCELLED' where id = :id and status = 'PLACED'`. Only one concurrent cancel can update the row, so stock is restored once, without a `@Version` column or retry handling.
