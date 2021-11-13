/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.worker;

import javax.swing.SwingWorker;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A SwingWorker implementation. Note that instances of this class are not reusable.
 * <pre>
 * ProgressWorker.builder(this::performTask)
 *   .onStarted(this::displayDialog)
 *   .onFinished(this::closeDialog)
 *   .onResult(this::handleResult)
 *   .onProgress(this::displayProgress)
 *   .onPublish(this::publishMessage)
 *   .onException(this::displayException)
 *   .execute();
 * </pre>
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result produced by this worker
 * @see #builder(Task)
 * @see #builder(ProgressTask)
 */
public final class ProgressWorker<T, V> extends SwingWorker<T, V> {

  private static final String STATE_PROPERTY = "state";
  private static final String PROGRESS_PROPERTY = "progress";

  private final ProgressTask<T, V> task;
  private final ProgressReporter<V> progressReporter = new DefaultProgressReporter();

  private final Runnable onStarted;
  private final Runnable onFinished;
  private final Consumer<T> onResult;
  private final Consumer<Integer> onProgress;
  private final Consumer<List<V>> onPublish;
  private final Consumer<Throwable> onException;
  private final Runnable onInterrupted;

  private ProgressWorker(final ProgressTask<T, V> task, final Runnable onStarted, final Runnable onFinished,
                         final Consumer<T> onResult, final Consumer<Integer> onProgress, final Consumer<List<V>> onPublish,
                         final Consumer<Throwable> onException, final Runnable onInterrupted) {
    this.task = requireNonNull(task);
    this.onStarted = onStarted;
    this.onFinished = onFinished;
    this.onResult = onResult;
    this.onProgress = onProgress;
    this.onPublish = onPublish;
    this.onException = onException;
    this.onInterrupted = onInterrupted;
    addPropertyChangeListener(this::onPropertyChangeEvent);
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T, ?> builder(final Task<T> task) {
    requireNonNull(task);

    return builder(progressReporter -> task.perform());
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @param <V> the intermediate result type
   * @return a new {@link Builder} instance
   */
  public static <T, V> Builder<T, V> builder(final ProgressTask<T, V> task) {
    return new DefaultBuilder<>(task);
  }

  @Override
  protected T doInBackground() throws Exception {
    return task.perform(progressReporter);
  }

  @Override
  protected void process(final List<V> chunks) {
    handleProcess(chunks);
  }

  @Override
  protected void done() {
    finish();
  }

  private void onPropertyChangeEvent(final PropertyChangeEvent changeEvent) {
    if (STATE_PROPERTY.equals(changeEvent.getPropertyName())) {
      if (StateValue.STARTED.equals(changeEvent.getNewValue())) {
        handleStarted();
      }
    }
    else if (PROGRESS_PROPERTY.equals(changeEvent.getPropertyName())) {
      handleProgress((Integer) changeEvent.getNewValue());
    }
  }

  private void finish() {
    handleFinished();
    try {
      handleResult();
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      handleInterrupted();
    }
    catch (final ExecutionException e) {
      handleException(e);
    }
  }

  private void handleStarted() {
    if (onStarted != null) {
      onStarted.run();
    }
  }

  private void handleProcess(final List<V> chunks) {
    if (onPublish != null) {
      onPublish.accept(chunks);
    }
  }

  private void handleProgress(final Integer progress) {
    if (onProgress != null) {
      onProgress.accept(progress);
    }
  }

  private void handleFinished() {
    if (onFinished != null) {
      onFinished.run();
    }
  }

  private void handleResult() throws InterruptedException, ExecutionException {
    if (onResult != null) {
      onResult.accept(get());
    }
  }

  private void handleInterrupted() {
    if (onInterrupted != null) {
      onInterrupted.run();
    }
  }

  private void handleException(final ExecutionException e) {
    if (onException != null) {
      onException.accept(e.getCause());
    }
    else if (e.getCause() instanceof RuntimeException) {
      throw (RuntimeException) e.getCause();
    }
    else {
      throw new RuntimeException(e.getCause());
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
     * @param onStarted called when the worker starts
     * @return this builder instance
     */
    Builder<T, V> onStarted(Runnable onStarted);

    /**
     * @param onFinished called on the EDT when the task finishes, successfully or not, before the result is processed
     * @return this builder instance
     */
    Builder<T, V> onFinished(Runnable onFinished);

    /**
     * @param onResult called on the EDT when result of a successful run is available
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
     * @param onInterrupted called on the EDT if the worker was interrupted
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

  private final class DefaultProgressReporter implements ProgressReporter<V> {
    @Override
    public void setProgress(final int progress) {
      ProgressWorker.this.setProgress(progress);
    }

    @Override
    public void publish(final V... chunks) {
      ProgressWorker.this.publish(chunks);
    }
  }

  private static final class DefaultBuilder<T, V> implements Builder<T, V> {

    private final ProgressTask<T, V> task;

    private Runnable onStarted;
    private Runnable onFinished;
    private Consumer<T> onResult;
    private Consumer<Integer> onProgress;
    private Consumer<List<V>> onPublish;
    private Consumer<Throwable> onException;
    private Runnable onInterrupted;

    private DefaultBuilder(final ProgressTask<T, V> task) {
      this.task = requireNonNull(task);
    }

    @Override
    public Builder<T, V> onStarted(final Runnable onStarted) {
      this.onStarted = onStarted;
      return this;
    }

    @Override
    public Builder<T, V> onFinished(final Runnable onFinished) {
      this.onFinished = onFinished;
      return this;
    }

    @Override
    public Builder<T, V> onResult(final Consumer<T> onResult) {
      this.onResult = onResult;
      return this;
    }

    @Override
    public Builder<T, V> onProgress(final Consumer<Integer> onProgress) {
      this.onProgress = onProgress;
      return this;
    }

    @Override
    public Builder<T, V> onPublish(final Consumer<List<V>> onPublish) {
      this.onPublish = onPublish;
      return this;
    }

    @Override
    public Builder<T, V> onException(final Consumer<Throwable> onException) {
      this.onException = onException;
      return this;
    }

    @Override
    public Builder<T, V> onInterrupted(final Runnable onInterrupted) {
      this.onInterrupted = onInterrupted;
      return this;
    }

    @Override
    public ProgressWorker<T, V> execute() {
      final ProgressWorker<T, V> worker = build();
      worker.execute();

      return worker;
    }

    @Override
    public ProgressWorker<T, V> build() {
      return new ProgressWorker<>(task, onStarted, onFinished, onResult, onProgress, onPublish, onException, onInterrupted);
    }
  }
}