/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.worker;

import is.codion.common.model.CancelException;

import javax.swing.SwingWorker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * <p>A {@link SwingWorker} implementation. Instances of this class are not reusable.</p>
 * <p>Note that this implementation does <b>NOT</b> coalesce progress reports or intermediate result publishing, but simply pushes
 * those directly to the {@code onProgress} and {@code onPublish} handlers on the Event Dispatch Thread.</p>
 * <p>Note that the {@code onStarted} handler is NOT called in case the background task finishes
 * 	before the {@link javax.swing.SwingWorker.StateValue#STARTED} change event is fired.
 * {@snippet :
 * ProgressWorker.builder(this::performTask)
 *   .onStarted(this::displayDialog)
 *   .onDone(this::closeDialog)
 *   .onResult(this::handleResult)
 *   .onProgress(this::displayProgress)
 *   .onPublish(this::publishMessage)
 *   .onCancelled(this::displayCancelledMessage)
 *   .onException(this::displayException)
 *   .execute();
 * }
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result produced by this {@link ProgressWorker}
 * @see #builder(ResultTask)
 * @see #builder(ProgressResultTask)
 */
public final class ProgressWorker<T, V> extends SwingWorker<T, V> {

	public static final int DEFAULT_MAXIMUM_PROGRESS = 100;

	private static final String STATE_PROPERTY = "state";

	private final Object task;
	private final int maximumProgress;
	private final Runnable onStarted;
	private final Runnable onDone;
	private final Consumer<T> onResult;
	private final Consumer<Integer> onProgress;
	private final Consumer<List<V>> onPublish;
	private final Consumer<Exception> onException;
	private final Runnable onCancelled;
	private final Runnable onInterrupted;

	private boolean onDoneRun = false;

	private ProgressWorker(DefaultBuilder<T, V> builder) {
		this.task = builder.task;
		this.maximumProgress = builder.maximumProgress;
		this.onStarted = builder.onStarted;
		this.onDone = builder.onDone;
		this.onResult = builder.onResult;
		this.onProgress = builder.onProgress;
		this.onPublish = builder.onPublish;
		this.onException = builder.onException;
		this.onCancelled = builder.onCancelled;
		this.onInterrupted = builder.onInterrupted;
		getPropertyChangeSupport().addPropertyChangeListener(STATE_PROPERTY, new StateListener());
	}

	/**
	 * @param task the task to run
	 * @return a new {@link Builder} instance
	 */
	public static Builder<?, ?> builder(Task task) {
		return new DefaultBuilder<>(task);
	}

	/**
	 * @param task the task to run
	 * @param <V> the intermediate result type
	 * @return a new {@link Builder} instance
	 */
	public static <V> Builder<?, V> builder(ProgressTask<V> task) {
		return new DefaultBuilder<>(task);
	}

	/**
	 * @param task the task to run
	 * @param <T> the worker result type
	 * @return a new {@link Builder} instance
	 */
	public static <T> Builder<T, ?> builder(ResultTask<T> task) {
		return new DefaultBuilder<>(task);
	}

	/**
	 * @param task the task to run
	 * @param <T> the worker result type
	 * @param <V> the intermediate result type
	 * @return a new {@link Builder} instance
	 */
	public static <T, V> Builder<T, V> builder(ProgressResultTask<T, V> task) {
		return new DefaultBuilder<>(task);
	}

	@Override
	protected T doInBackground() throws Exception {
		if (task instanceof Task) {
			((Task) task).execute();
		}
		else if (task instanceof ProgressTask) {
			((ProgressTask<V>) task).execute(new TaskProgressReporter());
		}
		else if (task instanceof ResultTask) {
			return ((ResultTask<T>) task).execute();
		}
		else if (task instanceof ProgressResultTask) {
			return ((ProgressResultTask<T, V>) task).execute(new TaskProgressReporter());
		}

		return null;
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
			Throwable cause = e.getCause();
			if (cause instanceof CancelException) {
				onCancelled.run();
			}
			else if (cause instanceof Exception) {
				onException.accept((Exception) cause);
			}
			else {
				onException.accept(new RuntimeException(cause));
			}
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
				if (isDone()) {
					runOnDone();
				}
				else {
					onStarted.run();
				}
			}
		}
	}

	/**
	 * A background task.
	 */
	public interface Task {

		/**
		 * Executes the task.
		 * @throws Exception in case of an exception
		 */
		void execute() throws Exception;
	}

	/**
	 * A background task producing a result.
	 * @param <T> the task result type
	 */
	public interface ResultTask<T> {

		/**
		 * Executes the task.
		 * @return the task result
		 * @throws Exception in case of an exception
		 */
		T execute() throws Exception;
	}

	/**
	 * A progress aware background task.
	 * @param <V> the intermediate result type
	 */
	public interface ProgressTask<V> {

		/**
		 * Executes the task.
		 * @param progressReporter the progress reporter to report a message or progress (0 - maximumProgress).
		 * @throws Exception in case of an exception
		 */
		void execute(ProgressReporter<V> progressReporter) throws Exception;

		/**
		 * Default {@link #DEFAULT_MAXIMUM_PROGRESS} (100)
		 * @return the maximum progress this task will report
		 */
		default int maximumProgress() {
			return DEFAULT_MAXIMUM_PROGRESS;
		}
	}

	/**
	 * A progress aware background task producing a result.
	 * @param <T> the task result type
	 * @param <V> the intermediate result type
	 */
	public interface ProgressResultTask<T, V> {

		/**
		 * Executes the task.
		 * @param progressReporter the progress reporter to report a message or progress (0 - maximumProgress).
		 * @return the task result
		 * @throws Exception in case of an exception
		 */
		T execute(ProgressReporter<V> progressReporter) throws Exception;

		/**
		 * Default {@link #DEFAULT_MAXIMUM_PROGRESS} (100)
		 * @return the maximum progress this task will report
		 */
		default int maximumProgress() {
			return DEFAULT_MAXIMUM_PROGRESS;
		}
	}

	/**
	 * Reports progress and publishes intermediate results for a ProgressWorker
	 * @param <V> the intermediate result type
	 */
	public interface ProgressReporter<V> {

		/**
		 * @param progress the progress, 0 - maximumProgress.
		 */
		void report(int progress);

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
		 * Overrides any maximumProgress specified by the task itself.
		 * @param maximumProgress the maximum progress, {@link #DEFAULT_MAXIMUM_PROGRESS} (100) by default
		 * @return this builder instance
		 * @see ProgressTask#maximumProgress()
		 * @see ProgressResultTask#maximumProgress()
		 */
		Builder<T, V> maximumProgress(int maximumProgress);

		/**
		 * Note that this handler does not get called in case the background task finishes
		 * before the {@link javax.swing.SwingWorker.StateValue#STARTED} change event is fired.
		 * @param onStarted called on the EDT before background processing is started
		 * @return this builder instance
		 */
		Builder<T, V> onStarted(Runnable onStarted);

		/**
		 * @param onDone called on the Event Dispatch Thread when the task is done running, successfully or not, before the result is processed
		 * @return this builder instance
		 */
		Builder<T, V> onDone(Runnable onDone);

		/**
		 * @param onResult called on the Event Dispatch Thread when the result of a successful run is available
		 * @return this builder instance
		 */
		Builder<T, V> onResult(Runnable onResult);

		/**
		 * @param onResult called on the Event Dispatch Thread when the result of a successful run is available
		 * @return this builder instance
		 */
		Builder<T, V> onResult(Consumer<T> onResult);

		/**
		 * @param onProgress called on the Event Dispatch Thread when progress is reported
		 * @return this builder instance
		 */
		Builder<T, V> onProgress(Consumer<Integer> onProgress);

		/**
		 * @param onPublish called on the Event Dispatch Thread when chunks are available for publishing
		 * @return this builder instance
		 */
		Builder<T, V> onPublish(Consumer<List<V>> onPublish);

		/**
		 * @param onException called on the Event Dispatch Thread if an exception occurred
		 * @return this builder instance
		 */
		Builder<T, V> onException(Consumer<Exception> onException);

		/**
		 * Called in case the background task is cancelled via {@link SwingWorker#cancel(boolean)}
		 * or if it throws a {@link CancelException}
		 * @param onCancelled called on the Event Dispatch Thread if the background task was cancelled
		 * @return this builder instance
		 */
		Builder<T, V> onCancelled(Runnable onCancelled);

		/**
		 * @param onInterrupted called on the Event Dispatch Thread if the background task was interrupted
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
		public void report(int progress) {
			setProgress(maximumProgress == 0 ? 100 : 100 * progress / maximumProgress);
			if (onProgress != DefaultBuilder.EMPTY_CONSUMER) {
				invokeLater(() -> onProgress.accept(progress));
			}
		}

		@Override
		public void publish(V... chunks) {
			ProgressWorker.this.publish(chunks);
			if (onPublish != DefaultBuilder.EMPTY_CONSUMER) {
				invokeLater(() -> onPublish.accept(asList(chunks)));
			}
		}
	}

	private static final class DefaultBuilder<T, V> implements Builder<T, V> {

		private static final Runnable EMPTY_RUNNABLE = new EmptyRunnable();
		private static final Consumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();
		private static final Consumer<Exception> RETHROW_HANDLER = new RethrowHandler();
		private static final Runnable INTERRUPT_CURRENT_ON_INTERRUPTED = new InterruptCurrentOnInterrupted();

		private final Object task;

		private int maximumProgress = DEFAULT_MAXIMUM_PROGRESS;
		private Runnable onStarted = EMPTY_RUNNABLE;
		private Runnable onDone = EMPTY_RUNNABLE;
		private Consumer<T> onResult = (Consumer<T>) EMPTY_CONSUMER;
		private Consumer<Integer> onProgress = (Consumer<Integer>) EMPTY_CONSUMER;
		private Consumer<List<V>> onPublish = (Consumer<List<V>>) EMPTY_CONSUMER;
		private Consumer<Exception> onException = RETHROW_HANDLER;
		private Runnable onCancelled = EMPTY_RUNNABLE;
		private Runnable onInterrupted = INTERRUPT_CURRENT_ON_INTERRUPTED;

		private DefaultBuilder(Task task) {
			this.task = requireNonNull(task);
		}

		private DefaultBuilder(ProgressTask<V> progressTask) {
			this.task = requireNonNull(progressTask);
			this.maximumProgress = progressTask.maximumProgress();
		}

		private DefaultBuilder(ResultTask<T> resultTask) {
			this.task = requireNonNull(resultTask);
		}

		private DefaultBuilder(ProgressResultTask<T, V> progressResultTask) {
			this.task = requireNonNull(progressResultTask);
			this.maximumProgress = progressResultTask.maximumProgress();
		}

		@Override
		public Builder<T, V> maximumProgress(int maximumProgress) {
			if (maximumProgress < 0) {
				throw new IllegalArgumentException("Maximum progress must be a positive integer");
			}
			this.maximumProgress = maximumProgress;
			return this;
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
		public Builder<T, V> onResult(Runnable onResult) {
			requireNonNull(onResult);
			return onResult(result -> onResult.run());
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
		public Builder<T, V> onException(Consumer<Exception> onException) {
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

	private static final class RethrowHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
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