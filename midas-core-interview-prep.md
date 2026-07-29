# Midas Core — Project Overview & Interview Prep Guide

## Project Overview

Midas Core is a Spring Boot microservice that simulates a simplified financial
transaction processor. Over the course of building it, you touched nearly every
layer of a typical backend service:

1. **Ingestion** — a Kafka consumer (`KafkaConsumer`) listens on a configurable
   topic and deserializes incoming JSON messages into `Transaction` objects.
2. **Validation & business logic** — `DatabaseConduit` validates each
   transaction (sender/recipient exist, sender has sufficient balance) before
   doing anything else.
3. **External integration** — valid transactions are posted to an external
   "incentive" REST API via `RestTemplate`, and the response affects how
   balances are adjusted.
4. **Persistence** — validated transactions and balance changes are written to
   an H2 in-memory database via Spring Data JPA, with a `TransactionRecord`
   entity holding `@ManyToOne` relationships to `UserRecord` (sender/recipient).
5. **Exposure** — a REST controller (`BalanceController`) exposes a `/balance`
   endpoint so external clients can query a user's current balance.
6. **Testing** — the whole pipeline is exercised through Spring Boot tests using
   an embedded/test Kafka broker, so nothing needs a real running broker
   locally to verify correctness.

This is a compact but realistic slice of what a "trade processing" or
"payments" backend team builds in practice: **event-driven ingestion → business
rules → external service call → persistence → query API**. That shape shows up
constantly in fintech, e-commerce order processing, and any system that reacts
to a stream of events.

---

## What To Master For Interviews

### 1. Spring Boot fundamentals
- **Dependency injection & bean lifecycle** — constructor injection (what you
  used throughout: `DatabaseConduit(UserRepository, TransactionRepository,
  RestTemplate)`) vs field injection, and *why* constructor injection is
  preferred (immutability, testability, avoiding circular dependency surprises).
- **`@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`** — know
  the semantic difference even though they're functionally similar
  stereotypes; `@Repository` also enables exception translation.
- **Auto-configuration** — be able to explain *why* `RestTemplate` needed a
  manual `@Bean` but `JpaRepository`/`CrudRepository` implementations didn't
  (Spring Boot auto-configures Spring Data repositories from interfaces, but
  deliberately stopped auto-configuring a `RestTemplate` bean directly — you
  build it from `RestTemplateBuilder` instead).
- **Profiles & externalized configuration** — `application.yml`, property
  placeholders (`${general.kafka-topic}`), and how Spring resolves them.

### 2. Apache Kafka (this is likely to be a deep-dive topic)
- **Core concepts**: topics, partitions, offsets, consumer groups, brokers.
  Be able to explain *why* partitions enable parallelism and how consumer
  groups divide work.
- **Producer/consumer serialization** — you hit this directly: mismatched
  `StringSerializer` vs actual object type, then the `JsonDeserializer`
  "setter vs properties" conflict. Interviewers love asking about
  serialization pitfalls because they reveal whether you've actually run into
  them, not just read about Kafka.
- **`spring-kafka` abstractions**: `KafkaTemplate` (producer side),
  `@KafkaListener` (consumer side), `ConcurrentKafkaListenerContainerFactory`,
  and how they wrap the raw `kafka-clients` API.
- **Delivery semantics**: at-most-once vs at-least-once vs exactly-once, and
  where offset commits fit in. You didn't need to reason about this deeply for
  this project, but it's one of the most common Kafka interview questions.
- **Testing Kafka code**: `@EmbeddedKafka` vs Testcontainers — know the
  tradeoffs (embedded = fast, in-JVM, not 100% identical to real broker
  behavior; Testcontainers = a real broker in Docker, slower, more faithful).

### 3. JPA / Hibernate & relational modeling
- **Entity relationships**: `@ManyToOne`, `@OneToMany`, `@JoinColumn`,
  `optional = false`/`nullable = false` — you used this directly for
  `TransactionRecord`'s sender/recipient relationships. Be ready to explain
  *why* the hint pushed you toward a **separate** `TransactionRecord` entity
  rather than annotating the existing `Transaction` DTO (separation of your
  wire/transport model from your persistence model — a classic and important
  distinction interviewers probe).
- **`ddl-auto` strategies**: `none`, `validate`, `update`, `create`,
  `create-drop` — know when each is appropriate (never `update`/`create` in
  real production databases; migrations tools like Flyway/Liquibase are the
  real answer there).
- **N+1 query problem** — not something this project surfaced directly, but a
  near-guaranteed interview question once `@ManyToOne`/`@OneToMany` comes up.
- **Repository abstractions**: `CrudRepository` vs `JpaRepository` (the latter
  adds batching, flushing, and paging/sorting support) and Spring Data's
  method-name query derivation (`findByName`, as you added).

### 4. REST API design
- **`@RestController`, `@GetMapping`, `@RequestParam`** — you used all three
  for `BalanceController`. Know the difference between `@RequestParam` (query
  string), `@PathVariable` (URL path segment), and `@RequestBody` (JSON body) —
  and when each is the right choice.
- **HTTP semantics** — why `/balance` should be GET-only (idempotent, safe,
  cacheable) rather than POST.
- **Client-side HTTP calls**: `RestTemplate` (what you used — now in
  maintenance mode) vs the newer `RestClient`/`WebClient` — worth being able
  to at least mention the shift, since interviewers may ask "would you use
  `RestTemplate` today?"

### 5. Testing strategy
- **`@SpringBootTest`** — full application context tests, what they're good
  for and why they're slower than slice tests.
- **Test slices** — `@DataJpaTest`, `@WebMvcTest` — narrower, faster
  alternatives you didn't need here but should know exist.
- **Embedded/test infrastructure** — the pattern of swapping a real Kafka
  broker/database for a lightweight or in-memory equivalent during tests, and
  the tradeoff (speed and simplicity vs fidelity to production behavior).

### 6. Build tooling
- **Maven fundamentals**: `dependencyManagement` vs `dependencies`, BOM
  imports, the "nearest declaration wins" resolution rule you ran into
  directly with the `kafka.version` property collision — a genuinely good
  interview anecdote about a subtle transitive dependency bug you diagnosed
  and fixed yourself.

---

## Strong Interview Talking Points From This Project

These are real, specific stories worth having ready — interviewers consistently
rate "tell me about a bug you diagnosed" answers higher when they're this
concrete:

1. **The `kafka.version` property collision** — a custom Maven property name
   accidentally shadowed an internal property Spring Boot's dependency
   management used to pin `kafka-clients`, causing Maven to search for
   nonexistent artifact versions. Good story about transitive dependency
   management and property scoping in Maven.
2. **The `JsonDeserializer` "setters vs properties" conflict** — configuring
   the same deserializer both via YAML properties and (indirectly) via
   Spring's generic-type-driven construction tripped an internal assertion.
   Good story about understanding a framework's internals deeply enough to
   fix a genuinely confusing runtime error, not just copy-paste a fix.
3. **Why `TransactionRecord` is a separate entity from `Transaction`** — good
   story about DTO/entity separation and why coupling your wire format to
   your persistence schema is a design smell.
4. **Sender-not-debited-but-recipient-credited incentive logic** — a small
   but real business-rule nuance (asymmetric balance adjustment) that's a good
   example of translating a written spec into precise code, including the
   validate-before-external-call ordering (never call an external paid API
   for a transaction you're about to reject anyway).

---

## Suggested Study Order (if starting from your current level)

1. **Kafka deep dive** — this is the highest-leverage area if the target role
   is Kafka-heavy. Understand partitioning strategy, consumer group
   rebalancing, and delivery guarantees beyond what this project required.
2. **JPA relationship mapping + N+1 problem** — round out what you built with
   the standard follow-up questions interviewers ask once they see
   `@ManyToOne`.
3. **Spring transaction management (`@Transactional`)** — notably *absent*
   from this project. `processTransaction` makes multiple sequential saves
   (`sender`, `recipient`, `TransactionRecord`) without a transaction boundary
   — worth understanding how you'd wrap that in `@Transactional` for atomicity,
   and being ready to discuss it if asked "how would you make this more
   robust?"
3. **System design framing** — be ready to zoom out and describe this project
   at a whiteboard level: "event-driven ingestion, idempotency concerns,
   how would this scale, what happens on partial failure between the
   incentive API call and the DB write" — that gap (a call succeeds but the
   DB write fails, or vice versa) is a great distributed-systems discussion
   point given your embedded/IoT and distributed systems background.
