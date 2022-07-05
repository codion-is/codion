/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.worker;

import javax.swing.SwingWorker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A SwingWorker implementation. Note that instances of this class are not reusable.
 * <pre>
 * ProgressWorker.builder(this::performTask)
 *   .onStarted(this::displayDialog)
 *   .onDone(this::closeDialog)
 *   .onResult(this::handleResult)
 *   .onProgress(this::displayProgress)
 *   .onPublish(this::publishMessage)
 *   .onCancelled(this::displayCancelledMessage)
 *   .onException(this::displayException)
 *   .execute();
 * </pre>
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result produced by this worker
 * @see #builder(Task)
 * @see #builder(ProgressTask)
 */
public final class ProgressWorker<T, V> extends SwingWorker<T, V> {

  private static final String PROGRESS_PROPERTY = "progress";
  private static final String STATE_PROPERTY = "state";

  private final ProgressTask<T, V> task;
  private final Runnable onStarted;
  private final Runnable onDone;
  private final Consumer<T> onResult;
  private final Consumer<Integer> onProgress;
  private final Consumer<List<V>> onPublish;
  private final Consumer<Throwable> onException;
  private final Runnable onCancelled;
  private final Runnable onInterrupted;

  private boolean onDoneRun = false;

  private ProgressWorker(DefaultBuilder<T, V> builder) {
    this.task = builder.task;
    this.onStarted = builder.onStarted;
    this.onDone = builder.onDone;
    this.onResult = builder.onResult;
    this.onProgress = builder.onProgress;
    this.onPublish = builder.onPublish;
    this.onException = builder.onException;
    this.onCancelled = builder.onCancelled;
    this.onInterrupted = builder.onInterrupted;
    getPropertyChangeSupport().addPropertyChangeListener(PROGRESS_PROPERTY, new ProgressListener());
    getPropertyChangeSupport().addPropertyChangeListener(STATE_PROPERTY, new StateListener());
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T, ?> builder(Task<T> task) {
    requireNonNull(task);

    return builder(progressReporter -> task.perform());
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @param <V> the intermediate result type
   * @return a new {@link Builder} instance
   */
  public static <T, V> Builder<T, V> builder(ProgressTask<T, V> task) {
    return new DefaultBuilder<>(task);
  }

  @Override
  protected T doInBackground() throws Exception {
    return task.perform(new TaskProgressReporter());
  }

  @Override
  protected void process(List<V> chunks) {
    onPublish.accept(chunks);
  }

  @Override
  protected void done() {
    runOnDone();
    try {
      onResult.accept(get());
    }
    catch (CancellationException e) {
      onCancelled.run();
    }
    catch (InterruptedException e) {
      onInterrupted.run();
    }
    catch (ExecutionException e) {
      onException.accept(e.getCause());
    }
  }

  private void runOnDone() {
    if (!onDoneRun) {
      onDone.run();
      onDoneRun = true;
    }
  }

  private final class StateListener implements PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      if (changeEvent.getNewValue() == StateValue.STARTED) {
        onStarted.run();
        if (isDone()) {
          runOnDone();
        }
      }
    }
  }

  private final class ProgressListener implements PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      onProgress.accept((Integer) changeEvent.getNewValue());
    }
  }

  /**
   * A background task.
   * @param <T> the task result type
   */
  public interface Task<T> {

    /**
     * Performs the task.
     * @return the task result
     * @throws Exception in case of an exception
     */
    T perform() throws Exception;
  }

  /**
   * A progress aware background task.
   * @param <T> the task result type
   * @param <V> the intermediate result type
   */
  public interface ProgressTask<T, V> {

    /**
     * Performs the task.
     * @param progressReporter the progress reporter to report a message or progress (0 - 100).
     * @return the task result
     * @throws Exception in case of an exception
     */
    T perform(ProgressReporter<V> progressReporter) throws Exception;
  }

  /**
   * Reports progress for a ProgressWorker
   * @param <V> the intermediate result type
   */
  public interface ProgressReporter<V> {

    /**
     * @param progress the progress, 0 - 100.
     */
    void setProgress(int progress);

    /**
     * @param chunks the chunks to publish
     */
    void publish(V... chunks);
  }

  /**
   * Builds a {@link ProgressWorker} instance.
   * @param <T> the worker result type
   * @param <V> the intermediate result type
   */
  public interface Builder<T, V> {

    /**
     * @param onStarted called on the EDT before background processing is started
     * @return this builder instance
     */
    Builder<T, V> onStarted(Runnable onStarted);

    /**
     * @param onDone called on the EDT when the task is done running, successfully or not, before the result is processed
     * @return this builder instance
     */
    Builder<T, V> onDone(Runnable onDone);

    /**
     * @param onResult called on the EDT when the result of a successful run is available
     * @return this builder instance
     */
    Builder<T, V> onResult(Consumer<T> onResult);

    /**
     * @param onProgress called on the EDT when progress is reported
     * @return this builder instance
     */
    Builder<T, V> onProgress(Consumer<Integer> onProgress);

    /**
     * @param onPublish called on the EDT when chunks are available for publishing
     * @return this builder instance
     */
    Builder<T, V> onPublish(Consumer<List<V>> onPublish);

    /**
     * @param onException called on the EDT if an exception occurred
     * @return this builder instance
     */
    Builder<T, V> onException(Consumer<Throwable> onException);

    /**
     * @param onCancelled called on the EDT if the background task was cancelled
     * @return this builder instance
     */
    Builder<T, V> onCancelled(Runnable onCancelled);

    /**
     * @param onInterrupted called on the EDT if the background task was interrupted
     * @return this builder instance
     */
    Builder<T, V> onInterrupted(Runnable onInterrupted);

    /**
     * Builds and executes a new {@link ProgressWorker} based on this builder
     * @return a {@link ProgressWorker} based on this builder
     */
    ProgressWorker<T, V> execute();

    /**
     * @return a {@link ProgressWorker} based on this builder
     */
    ProgressWorker<T, V> build();
  }

  private final class TaskProgressReporter implements ProgressReporter<V> {
    @Override
    public void setProgress(int progress) {
      ProgressWorker.this.setProgress(progress);
    }

    @Override
    public void publish(V... chunks) {
      ProgressWorker.this.publish(chunks);
    }
  }

  private static final class DefaultBuilder<T, V> implements Builder<T, V> {

    private static final Runnable EMPTY_RUNNABLE = new EmptyRunnable();
    private static final Consumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();
    private static final Consumer<Throwable> RETHROW_ON_THROWABLE = new RethrowOnThrowable();
    private static final Runnable INTERRUPT_CURRENT_ON_INTERRUPTED = new InterruptCurrentOnInterrupted();

    private final ProgressTask<T, V> task;

    private Runnable onStarted = EMPTY_RUNNABLE;
    private Runnable onDone = EMPTY_RUNNABLE;
    private Consumer<T> onResult = (Consumer<T>) EMPTY_CONSUMER;
    private Consumer<Integer> onProgress = (Consumer<Integer>) EMPTY_CONSUMER;
    private Consumer<List<V>> onPublish = (Consumer<List<V>>) EMPTY_CONSUMER;
    private Consumer<Throwable> onException = RETHROW_ON_THROWABLE;
    private Runnable onCancelled = EMPTY_RUNNABLE;
    private Runnable onInterrupted = INTERRUPT_CURRENT_ON_INTERRUPTED;

    private DefaultBuilder(ProgressTask<T, V> task) {
      this.task = requireNonNull(task);
    }

    @Override
    public Builder<T, V> onStarted(Runnable onStarted) {
      this.onStarted = requireNonNull(onStarted);
      return this;
    }

    @Override
    public Builder<T, V> onDone(Runnable onDone) {
      this.onDone = requireNonNull(onDone);
      return this;
    }

    @Override
    public Builder<T, V> onResult(Consumer<T> onResult) {
      this.onResult = requireNonNull(onResult);
      return this;
    }

    @Override
    public Builder<T, V> onProgress(Consumer<Integer> onProgress) {
      this.onProgress = requireNonNull(onProgress);
      return this;
    }

    @Override
    public Builder<T, V> onPublish(Consumer<List<V>> onPublish) {
      this.onPublish = requireNonNull(onPublish);
      return this;
    }

    @Override
    public Builder<T, V> onException(Consumer<Throwable> onException) {
      this.onException = requireNonNull(onException);
      return this;
    }

    @Override
    public Builder<T, V> onCancelled(Runnable onCancelled) {
      this.onCancelled = requireNonNull(onCancelled);
      return this;
    }

    @Override
    public Builder<T, V> onInterrupted(Runnable onInterrupted) {
      this.onInterrupted = requireNonNull(onInterrupted);
      return this;
    }

    @Override
    public ProgressWorker<T, V> execute() {
      ProgressWorker<T, V> worker = build();
      worker.execute();

      return worker;
    }

    @Override
    public ProgressWorker<T, V> build() {
      return new ProgressWorker<>(this);
    }
  }

  private static final class EmptyRunnable implements Runnable {

    @Override
    public void run() {}
  }

  private static final class EmptyConsumer<T> implements Consumer<T> {

    @Override
    public void accept(T result) {}
  }

  private static final class RethrowOnThrowable implements Consumer<Throwable> {

    @Override
    public void accept(Throwable exception) {
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      }

      throw new RuntimeException(exception);
    }
  }

  private static final class InterruptCurrentOnInterrupted implements Runnable {

    @Override
    public void run() {
      Thread.currentThread().interrupt();
    }
  }
}