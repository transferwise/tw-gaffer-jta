package com.transferwise.common.gaffer;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import com.transferwise.common.baseutils.meters.cache.TagsSet;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultMetricsTemplate implements MetricsTemplate {

  public static final String GAUGE_LIBRARY_INFO = "tw.library.info";

  public static final String COUNTER_TRANSACTION_BEGIN = "gaffer.transaction.begin";
  public static final String COUNTER_TRANSACTION_SUSPEND = "gaffer.transaction.suspend";
  public static final String COUNTER_TRANSACTION_RESUME = "gaffer.transaction.resume";
  public static final String COUNTER_TRANSACTION_ABANDON = "gaffer.transaction.abandon";
  public static final String COUNTER_TRANSACTION_ABANDON_TRACK = "gaffer.transaction.abandon.track";
  public static final String COUNTER_TRANSACTION_COMMIT = "gaffer.transaction.commit";
  public static final String COUNTER_TRANSACTION_ROLLBACK = "gaffer.transaction.rollback";
  public static final String COUNTER_TRANSACTION_ROLLBACK_FAILURE = "gaffer.transaction.rollback.failure";
  public static final String COUNTER_TRANSACTION_HEURISTIC_COMMIT = "gaffer.transaction.heuristic.commit";
  public static final String GAUGE_ACTIVE_TRANSACTIONS = "gaffer.transactions.active";
  public static final String GAUGE_SUSPENDED_TRANSACTIONS = "gaffer.transactions.suspended";
  public static final String COUNTER_CONNECTION_GET = "gaffer.connection.get";
  public static final String COUNTER_CONNECTION_REUSE = "gaffer.connection.reuse";
  public static final String COUNTER_CONNECTION_CLOSE = "gaffer.connection.close";
  public static final String COUNTER_AUTOCOMMIT_SWITCH = "gaffer.connection.autocommit.switch";

  private final IMeterCache meterCache;
  private final AtomicLong activeTransactionsCount = new AtomicLong();
  private final AtomicLong suspendedTransactionsCount = new AtomicLong();

  @PostConstruct
  @Override
  public void init() {
    String version = this.getClass().getPackage().getImplementationVersion();
    if (version == null) {
      version = "Unknown";
    }

    Gauge.builder(GAUGE_LIBRARY_INFO, () -> 1d).tags("version", version, "library", "tw-gaffer-jta")
        .description("Provides metadata about the library, for example the version.")
        .register(meterCache.getMeterRegistry());

    Gauge.builder(GAUGE_ACTIVE_TRANSACTIONS, activeTransactionsCount::get)
        .register(meterCache.getMeterRegistry());

    Gauge.builder(GAUGE_SUSPENDED_TRANSACTIONS, suspendedTransactionsCount::get)
        .register(meterCache.getMeterRegistry());
  }

  @Override
  public void registerTransactionBeginning() {
    meterCache.counter(COUNTER_TRANSACTION_BEGIN, TagsSet.empty())
        .increment();

    activeTransactionsCount.incrementAndGet();
  }

  @Override
  public void registerTransactionSuspending() {
    meterCache.counter(COUNTER_TRANSACTION_SUSPEND, TagsSet.empty())
        .increment();

    activeTransactionsCount.decrementAndGet();
    suspendedTransactionsCount.incrementAndGet();
  }

  @Override
  public void registerTransactionResuming() {
    meterCache.counter(COUNTER_TRANSACTION_RESUME, TagsSet.empty())
        .increment();
    activeTransactionsCount.incrementAndGet();
    suspendedTransactionsCount.decrementAndGet();
  }

  @Override
  public void registerTransactionAbandoning() {
    meterCache.counter(COUNTER_TRANSACTION_ABANDON, TagsSet.empty())
        .increment();
  }

  @Override
  public void registerTransactionAbandoningTracking() {
    meterCache.counter(COUNTER_TRANSACTION_ABANDON_TRACK, TagsSet.empty())
        .increment();
  }

  @Override
  public void registerTransactionCommit(boolean wasSuspended) {
    meterCache.counter(COUNTER_TRANSACTION_COMMIT, TagsSet.of(getSuspendedTag(wasSuspended)))
        .increment();
    activeTransactionsCount.decrementAndGet();
  }

  @Override
  public void registerTransactionRollback(boolean wasSuspended) {
    meterCache.counter(COUNTER_TRANSACTION_ROLLBACK, TagsSet.of(getSuspendedTag(wasSuspended)))
        .increment();
    activeTransactionsCount.decrementAndGet();
  }

  @Override
  public void registerTransactionRollbackFailure(boolean wasSuspended) {
    meterCache.counter(COUNTER_TRANSACTION_ROLLBACK_FAILURE, TagsSet.of(getSuspendedTag(wasSuspended)))
        .increment();

    activeTransactionsCount.decrementAndGet();
  }

  @Override
  public void registerHeuristicTransactionCommit() {
    meterCache.counter(COUNTER_TRANSACTION_HEURISTIC_COMMIT, TagsSet.empty())
        .increment();
  }

  @Override
  public void registerConnectionGet(Tag dataSourceName, boolean transactional) {
    meterCache.counter(COUNTER_CONNECTION_GET, TagsSet.of(
            dataSourceName,
            transactionalTag(transactional)
        ))
        .increment();
  }

  @Override
  public void registerConnectionReuse(Tag dataSourceName, boolean transactional) {
    meterCache.counter(COUNTER_CONNECTION_REUSE, TagsSet.of(
            dataSourceName,
            transactionalTag(transactional)
        ))
        .increment();
  }

  @Override
  public void registerConnectionClose(Tag dataSourceName, boolean transactional) {
    meterCache.counter(COUNTER_CONNECTION_CLOSE, TagsSet.of(
            dataSourceName,
            transactionalTag(transactional)
        ))
        .increment();
  }

  @Override
  public void registerAutoCommitSwitch(Tag dataSourceName, boolean atAcquire,
      boolean transactional, boolean setAutoCommit) {
    meterCache.counter(COUNTER_AUTOCOMMIT_SWITCH, TagsSet.of(
            dataSourceName,
            transactionalTag(transactional),
            atAcquireTag(atAcquire),
            autoCommitTag(setAutoCommit)
        ))
        .increment();
  }

  @Override
  public Tag createDataSourceNameTag(String dataSourceName) {
    return Tag.of("dataSource", dataSourceName);
  }

  private static final Tag SUSPENDED_TAG_TRUE = Tag.of("suspended", "true");
  private static final Tag SUSPENDED_TAG_FALSE = Tag.of("suspended", "false");

  protected Tag getSuspendedTag(boolean suspended) {
    return suspended ? SUSPENDED_TAG_TRUE : SUSPENDED_TAG_FALSE;
  }

  private static final Tag AUTOCOMMIT_TAG_TRUE = Tag.of("autoCommit", "true");
  private static final Tag AUTOCOMMIT_TAG_FALSE = Tag.of("autoCommit", "false");

  protected Tag autoCommitTag(boolean autoCommit) {
    return autoCommit ? AUTOCOMMIT_TAG_TRUE : AUTOCOMMIT_TAG_FALSE;
  }

  private static final Tag TRANSACTIONAL_TAG_TRUE = Tag.of("transactional", "true");
  private static final Tag TRANSACTIONAL_TAG_FALSE = Tag.of("transactional", "false");

  protected Tag transactionalTag(boolean transactional) {
    return transactional ? TRANSACTIONAL_TAG_TRUE : TRANSACTIONAL_TAG_FALSE;
  }

  private static final Tag AT_ACQUIRE_TAG_TRUE = Tag.of("atAcquire", "true");
  private static final Tag AT_ACQUIRE_TAG_FALSE = Tag.of("atAcquire", "false");

  protected Tag atAcquireTag(boolean atAcquire) {
    return atAcquire ? AT_ACQUIRE_TAG_TRUE : AT_ACQUIRE_TAG_FALSE;
  }
}
