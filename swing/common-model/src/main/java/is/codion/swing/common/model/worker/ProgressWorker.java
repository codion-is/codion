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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.worker;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * <p>A {@link SwingWorker} implementation. Instances of this class are not reusable.</p>
 * <p>Note that this implementation does <b>NOT</b> coalesce progress reports or intermediate result publishing, but simply pushes
 * those directly to the {@code onProgress} and {@code onPublish} handlers on the Event Dispatch Thread.</p>
 * <p>The {@code onStarted} handler is guaranteed to be called on the Event Dispatch Thread before the background task executes,
 * and the {@code onDone} handler is guaranteed to be called after the background task completes.
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
 *}
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result produced by this {@link ProgressWorker}
 * @see #builder()
 */
public final class ProgressWorker<T, V> extends SwingWorker<T, V> {

	public static final int DEFAULT_MAXIMUM = 100;

	private static final BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	private final Object task;
	private final int maximum;
	private final List<Runnable> onStarted;
	private final List<Runnable> onDone;
	private final List<Consumer<T>> onResult;
	private final List<Consumer<Integer>> onProgress;
	private final List<Consumer<List<V>>> onPublish;
	private final List<Consumer<Exception>> onException;
	private final List<Runnable> onCancelled;
	private final List<Runnable> onInterrupted;

	private ProgressWorker(DefaultBuilder<T, V> builder) {
		this.task = builder.task;
		this.maximum = builder.maximum;
		this.onStarted = builder.onStarted();
		this.onDone = builder.onDone();
		this.onResult = builder.onResult();
		this.onProgress = builder.onProgress();
		this.onPublish = builder.onPublish();
		this.onException = builder.onException();
		this.onCancelled = builder.onCancelled();
		this.onInterrupted = builder.onInterrupted();
	}

	/**
	 * @return a {@link BuilderFactory}
	 */
	public static BuilderFactory builder() {
		return BUILDER_FACTORY;
	}

	@Override
	protected @Nullable T doInBackground() throws Exception {
		runOnStarted();
		if (task instanceof Task) {
			((Task) task).execute();
			return null;
		}
		else if (task instanceof ProgressTask) {
			((ProgressTask<V>) task).execute(new TaskProgressReporter());
			return null;
		}
		else if (task instanceof ResultTask) {
			return ((ResultTask<T>) task).execute();
		}
		else if (task instanceof ProgressResultTask) {
			return ((ProgressResultTask<T, V>) task).execute(new TaskProgressReporter());
		}

		throw new IllegalStateException("Unknown task type: " + task.getClass());
	}

	@Override
	protected void done() {
		onDone.forEach(Runnable::run);
		try {
			T result = get();
			onResult.forEach(c -> c.accept(result));
		}
		catch (CancellationException e) {
			onCancelled.forEach(Runnable::run);
		}
		catch (InterruptedException e) {
			onInterrupted.forEach(Runnable::run);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CancelException) {
				onCancelled.forEach(Runnable::run);
			}
			else if (cause instanceof Exception) {
				onException.forEach(c -> c.accept((Exception) cause));
			}
			else {
				onException.forEach(c -> c.accept(new RuntimeException(cause)));
			}
		}
	}

	private void runOnStarted() throws InterruptedException {
		if (!onStarted.isEmpty()) {
			CountDownLatch startedLatch = new CountDownLatch(1);
			invokeLater(() -> {
				onStarted.forEach(Runnable::run);
				startedLatch.countDown();
			});
			try {
				startedLatch.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
		}
	}

	/**
	 * Provides builders for a given task type.
	 */
	public sealed interface BuilderFactory {

		/**
		 * @param task the task to run
		 * @return a new {@link Builder} instance
		 */
		Builder<?, ?> task(Task task);

		/**
		 * @param task the task to run
		 * @param <T> the worker result typee
		 * @return a new {@link Builder} instance
		 */
		<T> Builder<T, ?> task(ResultTask<T> task);

		/**
		 * @param task the task to run
		 * @param <V> the intermediate result type
		 * @return a new {@link Builder} instance
		 */
		<V> Builder<?, V> task(ProgressTask<V> task);

		/**
		 * @param task the task to run
		 * @param <T> the worker result type
		 * @param <V> the intermediate result type
		 * @return a new {@link Builder} instance
		 */
		<T, V> Builder<T, V> task(ProgressResultTask<T, V> task);
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
		 * @param progressReporter the progress reporter to report a message or progress (0 - maximum()).
		 * @throws Exception in case of an exception
		 */
		void execute(ProgressReporter<V> progressReporter) throws Exception;

		/**
		 * Default {@link #DEFAULT_MAXIMUM} (100)
		 * @return the maximum progress this task will report
		 */
		default int maximum() {
			return DEFAULT_MAXIMUM;
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
		 * @param progressReporter the progress reporter to report a message or progress (0 - maximum()).
		 * @return the task result
		 * @throws Exception in case of an exception
		 */
		T execute(ProgressReporter<V> progressReporter) throws Exception;

		/**
		 * Default {@link #DEFAULT_MAXIMUM} (100)
		 * @return the maximum progress this task will report
		 */
		default int maximum() {
			return DEFAULT_MAXIMUM;
		}
	}

	/**
	 * Reports progress and publishes intermediate results for a ProgressWorker
	 * @param <V> the intermediate result type
	 */
	public interface ProgressReporter<V> {

		/**
		 * @param progress the progress, 0 - maximum.
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
	public sealed interface Builder<T, V> {

		/**
		 * Overrides any maximum progress specified by the task itself.
		 * @param maximum the maximum progress, {@link #DEFAULT_MAXIMUM} (100) by default
		 * @return this builder instance
		 * @see ProgressTask#maximum()
		 * @see ProgressResultTask#maximum()
		 */
		Builder<T, V> maximum(int maximum);

		/**
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
			setProgress(maximum == 0 ? DEFAULT_MAXIMUM : DEFAULT_MAXIMUM * progress / maximum);
			if (!onProgress.isEmpty()) {
				invokeLater(() -> onProgress.forEach(c -> c.accept(progress)));
			}
		}

		@Override
		public void publish(V... chunks) {
			ProgressWorker.this.publish(chunks);
			if (!onPublish.isEmpty()) {
				invokeLater(() -> onPublish.forEach(c -> c.accept(asList(chunks))));
			}
		}
	}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public Builder<?, ?> task(Task task) {
			return new DefaultBuilder<>(task);
		}

		@Override
		public <T> Builder<T, ?> task(ResultTask<T> task) {
			return new DefaultBuilder<>(task);
		}

		@Override
		public <V> Builder<?, V> task(ProgressTask<V> task) {
			return new DefaultBuilder<>(task);
		}

		@Override
		public <T, V> Builder<T, V> task(ProgressResultTask<T, V> task) {
			return new DefaultBuilder<>(task);
		}
	}

	private static final class DefaultBuilder<T, V> implements Builder<T, V> {

		private static final Consumer<Exception> RETHROW_HANDLER = new RethrowHandler();
		private static final Runnable INTERRUPT_CURRENT_ON_INTERRUPTED = new InterruptCurrentOnInterrupted();

		private final Object task;

		private int maximum = DEFAULT_MAXIMUM;
		private @Nullable List<Runnable> onStarted;
		private @Nullable List<Runnable> onDone;
		private @Nullable List<Consumer<T>> onResult;
		private @Nullable List<Consumer<Integer>> onProgress;
		private @Nullable List<Consumer<List<V>>> onPublish;
		private @Nullable List<Consumer<Exception>> onException;
		private @Nullable List<Runnable> onCancelled;
		private @Nullable List<Runnable> onInterrupted;

		private DefaultBuilder(Task task) {
			this.task = requireNonNull(task);
		}

		private DefaultBuilder(ProgressTask<V> progressTask) {
			this.task = requireNonNull(progressTask);
			this.maximum = progressTask.maximum();
		}

		private DefaultBuilder(ResultTask<T> resultTask) {
			this.task = requireNonNull(resultTask);
		}

		private DefaultBuilder(ProgressResultTask<T, V> progressResultTask) {
			this.task = requireNonNull(progressResultTask);
			this.maximum = progressResultTask.maximum();
		}

		@Override
		public Builder<T, V> maximum(int maximum) {
			if (maximum < 0) {
				throw new IllegalArgumentException("Maximum progress must be a positive integer");
			}
			this.maximum = maximum;
			return this;
		}

		@Override
		public Builder<T, V> onStarted(Runnable onStarted) {
			if (this.onStarted == null) {
				this.onStarted = new ArrayList<>(1);
			}
			this.onStarted.add(requireNonNull(onStarted));
			return this;
		}

		@Override
		public Builder<T, V> onDone(Runnable onDone) {
			if (this.onDone == null) {
				this.onDone = new ArrayList<>(1);
			}
			this.onDone.add(requireNonNull(onDone));
			return this;
		}

		@Override
		public Builder<T, V> onResult(Runnable onResult) {
			requireNonNull(onResult);
			return onResult(result -> onResult.run());
		}

		@Override
		public Builder<T, V> onResult(Consumer<T> onResult) {
			if (this.onResult == null) {
				this.onResult = new ArrayList<>(1);
			}
			this.onResult.add(requireNonNull(onResult));
			return this;
		}

		@Override
		public Builder<T, V> onProgress(Consumer<Integer> onProgress) {
			if (this.onProgress == null) {
				this.onProgress = new ArrayList<>(1);
			}
			this.onProgress.add(requireNonNull(onProgress));
			return this;
		}

		@Override
		public Builder<T, V> onPublish(Consumer<List<V>> onPublish) {
			if (this.onPublish == null) {
				this.onPublish = new ArrayList<>(1);
			}
			this.onPublish.add(requireNonNull(onPublish));
			return this;
		}

		@Override
		public Builder<T, V> onException(Consumer<Exception> onException) {
			if (this.onException == null) {
				this.onException = new ArrayList<>(1);
			}
			this.onException.add(requireNonNull(onException));
			return this;
		}

		@Override
		public Builder<T, V> onCancelled(Runnable onCancelled) {
			if (this.onCancelled == null) {
				this.onCancelled = new ArrayList<>(1);
			}
			this.onCancelled.add(requireNonNull(onCancelled));
			return this;
		}

		@Override
		public Builder<T, V> onInterrupted(Runnable onInterrupted) {
			if (this.onInterrupted == null) {
				this.onInterrupted = new ArrayList<>(1);
			}
			this.onInterrupted.add(requireNonNull(onInterrupted));
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

		private List<Runnable> onStarted() {
			return onStarted == null ? emptyList() : onStarted;
		}

		private List<Runnable> onDone() {
			return onDone == null ? emptyList() : onDone;
		}

		private List<Consumer<T>> onResult() {
			return onResult == null ? emptyList() : onResult;
		}

		private List<Consumer<Integer>> onProgress() {
			return onProgress == null ? emptyList() : onProgress;
		}

		private List<Consumer<List<V>>> onPublish() {
			return onPublish == null ? emptyList() : onPublish;
		}

		private List<Consumer<Exception>> onException() {
			return onException == null ? singletonList(RETHROW_HANDLER) : onException;
		}

		private List<Runnable> onCancelled() {
			return onCancelled == null ? emptyList() : onCancelled;
		}

		private List<Runnable> onInterrupted() {
			return onInterrupted == null ? singletonList(INTERRUPT_CURRENT_ON_INTERRUPTED) : onInterrupted;
		}
	}

	private static final class RethrowHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			throw Exceptions.runtime(exception);
		}
	}

	private static final class InterruptCurrentOnInterrupted implements Runnable {

		@Override
		public void run() {
			Thread.currentThread().interrupt();
		}
	}
}