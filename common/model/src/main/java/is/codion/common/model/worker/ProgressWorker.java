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
package is.codion.common.model.worker;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * <p>Executes a task on a background thread, invoking its lifecycle handlers on the UI thread as resolved by
 * {@link Dispatcher}. Instances of this class are not reusable.</p>
 * <p>Each execution runs on its own thread from a shared pool of daemon threads, created as needed and reaped
 * when idle, so {@code N} simultaneous executions use {@code N} threads.</p>
 * <p>Note that this implementation does <b>NOT</b> coalesce progress reports or intermediate result publishing, but simply pushes
 * those directly to the {@code onProgress} and {@code onPublish} handlers on the UI thread.</p>
 * <p>The {@code onStarted} handlers are guaranteed to be called on the UI thread before the background task executes,
 * and the {@code onDone} handlers are guaranteed to be called after the background task completes.
 * <p>All handler types support multiple handlers, which are called in the order they were added.
 * <p>There are two ways to use this class:
 * <ul>
 * <li><b>Builder-only</b>: pass a task and wire handlers via the builder.
 * <li><b>Handler interfaces</b>: implement one of the handler interfaces ({@link TaskHandler}, {@link ResultTaskHandler},
 * {@link ProgressTaskHandler}, {@link ProgressResultTaskHandler}) to encapsulate both the background task and its
 * handlers in a single class. Handler interface methods are wired automatically and called first.
 * Additional handlers can then be added via the builder and are called after.
 * </ul>
 * <p>On successful completion, handlers are called in this order:
 * {@code onDone} &rarr; {@code onSuccess} &rarr; {@code onResult(T)} (result-producing tasks only).
 * <p>Builder based usage example:
 * {@snippet :
 * ProgressWorker.builder(this::performTask)
 *   .onStarted(this::displayDialog)
 *   .onDone(this::closeDialog)
 *   .onSuccess(this::handleSuccess)
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
 * @see Handler
 */
public final class ProgressWorker<T, V> {

	public static final int DEFAULT_MAXIMUM = 100;

	private static final BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new DaemonThreadFactory());

	private final Object task;
	private final List<Runnable> onStarted;
	private final List<Runnable> onDone;
	private final List<Consumer<T>> onResult;
	private final List<Consumer<Integer>> onProgress;
	private final List<Consumer<List<V>>> onPublish;
	private final List<Consumer<Exception>> onException;
	private final List<Runnable> onCancelled;
	private final List<Runnable> onInterrupted;
	private final Dispatcher dispatcher;

	private Executor uiExecutor = Runnable::run;
	private final FutureTask<T> future = new FutureTask<>(this::doInBackground) {
		@Override
		protected void done() {
			uiExecutor.execute(ProgressWorker.this::finished);
		}
	};

	private ProgressWorker(DefaultBuilder<T, V> builder) {
		this.task = builder.task;
		this.onStarted = builder.onStarted();
		this.onDone = builder.onDone();
		this.onResult = builder.onResult();
		this.onProgress = builder.onProgress();
		this.onPublish = builder.onPublish();
		this.onException = builder.onException();
		this.onCancelled = builder.onCancelled();
		this.onInterrupted = builder.onInterrupted();
		this.dispatcher = builder.dispatcher == null ? Dispatcher.instance() : builder.dispatcher;
	}

	/**
	 * @return a {@link BuilderFactory}
	 */
	public static BuilderFactory builder() {
		return BUILDER_FACTORY;
	}

	/**
	 * Executes this worker, running the task on a background thread and its handlers on the UI thread.
	 * <p>Must be called on the UI thread, since the {@link Dispatcher} handler executor is resolved for the
	 * current context at this point (relevant for platforms with a per-context UI thread).
	 */
	public void execute() {
		uiExecutor = dispatcher.executor();
		EXECUTOR.execute(future);
	}

	/**
	 * Attempts to cancel the background task.
	 * @param mayInterruptIfRunning true if the thread executing the task should be interrupted
	 * @return false if the task could not be cancelled, typically because it has already completed
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	/**
	 * @return true if this task was cancelled before it completed
	 */
	public boolean isCancelled() {
		return future.isCancelled();
	}

	/**
	 * @return true if this task has completed, whether normally, via an exception or cancellation
	 */
	public boolean isDone() {
		return future.isDone();
	}

	/**
	 * Waits if necessary for the task to complete, and returns its result.
	 * @return the task result
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 * @throws ExecutionException if the task threw an exception
	 * @throws CancellationException if the task was cancelled
	 */
	public @Nullable T get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	private @Nullable T doInBackground() throws Exception {
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

	private void finished() {
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
			AtomicReference<RuntimeException> exception = new AtomicReference<>();
			CountDownLatch startedLatch = new CountDownLatch(1);
			uiExecutor.execute(() -> {
				try {
					onStarted.forEach(Runnable::run);
				}
				catch (RuntimeException e) {
					exception.set(e);
				}
				finally {
					startedLatch.countDown();
				}
			});
			try {
				startedLatch.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
			if (exception.get() != null) {
				throw exception.get();
			}
		}
	}

	/**
	 * <p>Provides builders for a given task type.
	 * <p>If the task also implements the corresponding handler interface (e.g. {@link TaskHandler},
	 * {@link ResultTaskHandler}), the handler methods are automatically wired first.
	 * Additional handlers can then be added via the returned {@link Builder},
	 * and are called after the handler interface methods, in the order they were added.
	 */
	public sealed interface BuilderFactory {

		/**
		 * @param task the task to run, if it implements {@link TaskHandler} its handler methods are wired automatically
		 * @return a new {@link Builder} instance
		 */
		Builder<?, ?> task(Task task);

		/**
		 * @param task the task to run, if it implements {@link ResultTaskHandler} its handler methods are wired automatically
		 * @param <T> the worker result type
		 * @return a new {@link Builder} instance
		 */
		<T> Builder<T, ?> task(ResultTask<T> task);

		/**
		 * @param task the task to run, if it implements {@link ProgressTaskHandler} its handler methods are wired automatically
		 * @param <V> the intermediate result type
		 * @return a new {@link Builder} instance
		 */
		<V> Builder<?, V> task(ProgressTask<V> task);

		/**
		 * @param task the task to run, if it implements {@link ProgressResultTaskHandler} its handler methods are wired automatically
		 * @param <T> the worker result type
		 * @param <V> the intermediate result type
		 * @return a new {@link Builder} instance
		 */
		<T, V> Builder<T, V> task(ProgressResultTask<T, V> task);
	}

	/**
	 * A background task.
	 */
	@FunctionalInterface
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
	@FunctionalInterface
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
	@FunctionalInterface
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
	@FunctionalInterface
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
	 * <p>Provides default handler methods for the {@link ProgressWorker} lifecycle.
	 * <p>All handler methods are called on the UI thread.
	 * <p>Combined with a task interface (via {@link TaskHandler}, {@link ResultTaskHandler}, etc.),
	 * this allows a single class to encapsulate both the background task and its handlers.
	 * Handler methods from this interface are automatically wired when the task is passed to
	 * {@link BuilderFactory#task(Task)}, and called first. Any handlers added via the {@link Builder}
	 * are called after, in the order they were added.
	 * @see TaskHandler
	 * @see ResultTaskHandler
	 * @see ProgressTaskHandler
	 * @see ProgressResultTaskHandler
	 */
	public interface Handler {

		/**
		 * Called on the UI thread before the background task executes.
		 */
		default void onStarted() {}

		/**
		 * Called on the UI thread when the task is done, successfully or not, before the result is processed.
		 */
		default void onDone() {}

		/**
		 * Called on the UI thread after a successful task execution,
		 * before {@link ResultTaskHandler#onResult(Object)} or
		 * {@link ProgressResultTaskHandler#onResult(Object)} for result-producing tasks.
		 */
		default void onSuccess() {}

		/**
		 * Called on the UI thread if an exception occurred during the background task.
		 * <p>The default implementation rethrows the exception as a {@link RuntimeException}.
		 * Override to handle the exception, for example by displaying it.
		 * @param exception the exception
		 */
		default void onException(Exception exception) {
			throw Exceptions.runtime(exception);
		}

		/**
		 * Called on the UI thread if the background task was cancelled.
		 */
		default void onCancelled() {}

		/**
		 * Called on the UI thread if the background task was interrupted.
		 * <p>The default implementation simply calls {@code Thread.currentThread().interrupt()}.
		 */
		default void onInterrupted() {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * A {@link Task} combined with {@link Handler}, for encapsulating a background task and its handlers in a single class.
	 * @see BuilderFactory#task(Task)
	 */
	@FunctionalInterface
	public interface TaskHandler extends Task, Handler {}

	/**
	 * A {@link ResultTask} combined with {@link Handler}, for encapsulating a result-producing background task
	 * and its handlers in a single class.
	 * @param <T> the task result type
	 * @see BuilderFactory#task(ResultTask)
	 */
	@FunctionalInterface
	public interface ResultTaskHandler<T> extends ResultTask<T>, Handler {

		/**
		 * Called on the UI thread after a successful execution,
		 * after {@link Handler#onSuccess()}.
		 * @param result the task result
		 */
		default void onResult(T result) {}
	}

	/**
	 * Extends {@link Handler} with progress and publish handlers.
	 * @param <V> the intermediate result type
	 * @see ProgressTaskHandler
	 * @see ProgressResultTaskHandler
	 */
	public interface ProgressHandler<V> extends Handler {

		/**
		 * Called on the UI thread when progress is reported.
		 * @param progress the progress value
		 */
		default void onProgress(int progress) {}

		/**
		 * Called on the UI thread when intermediate results are available.
		 * @param chunks the published chunks
		 */
		default void onPublish(List<V> chunks) {}
	}

	/**
	 * A {@link ProgressTask} combined with {@link ProgressHandler}, for encapsulating a progress-aware
	 * background task and its handlers in a single class.
	 * @param <V> the intermediate result type
	 * @see BuilderFactory#task(ProgressTask)
	 */
	@FunctionalInterface
	public interface ProgressTaskHandler<V> extends ProgressTask<V>, ProgressHandler<V> {}

	/**
	 * A {@link ProgressResultTask} combined with {@link ProgressHandler}, for encapsulating a progress-aware,
	 * result-producing background task and its handlers in a single class.
	 * @param <T> the task result type
	 * @param <V> the intermediate result type
	 * @see BuilderFactory#task(ProgressResultTask)
	 */
	@FunctionalInterface
	public interface ProgressResultTaskHandler<T, V> extends ProgressResultTask<T, V>, ProgressHandler<V> {

		/**
		 * Called on the UI thread after a successful execution,
		 * after {@link Handler#onSuccess()}.
		 * @param result the task result
		 */
		default void onResult(T result) {}
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
	 * <p>Builds a {@link ProgressWorker} instance.
	 * <p>Each handler method can be called multiple times to add multiple handlers.
	 * All handlers are called in the order they were added. When using a task that implements
	 * a handler interface (e.g. {@link TaskHandler}), those handler methods are added first,
	 * and any handlers added via this builder are called after.
	 * @param <T> the worker result type
	 * @param <V> the intermediate result type
	 */
	public sealed interface Builder<T, V> {

		/**
		 * Adds a handler called on the UI thread before background processing is started.
		 * @param onStarted the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onStarted(Runnable onStarted);

		/**
		 * Adds a handler called on the UI thread when the task is done running,
		 * successfully or not, before the result is processed.
		 * @param onDone the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onDone(Runnable onDone);

		/**
		 * Adds a handler called on the UI thread after a successful task run,
		 * before any {@link #onResult(Consumer)} handlers.
		 * @param onSuccess the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onSuccess(Runnable onSuccess);

		/**
		 * Adds a handler called on the UI thread when the result of a successful run is available,
		 * after any {@link #onSuccess(Runnable)} handlers.
		 * @param onResult the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onResult(Consumer<T> onResult);

		/**
		 * Adds a handler called on the UI thread when progress is reported.
		 * @param onProgress the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onProgress(Consumer<Integer> onProgress);

		/**
		 * Adds a handler called on the UI thread when chunks are available for publishing.
		 * @param onPublish the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onPublish(Consumer<List<V>> onPublish);

		/**
		 * Adds a handler called on the UI thread if an exception occurred.
		 * @param onException the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onException(Consumer<Exception> onException);

		/**
		 * Adds a handler called on the UI thread if the background task is cancelled
		 * via {@link ProgressWorker#cancel(boolean)} or if it throws a {@link CancelException}.
		 * @param onCancelled the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onCancelled(Runnable onCancelled);

		/**
		 * Adds a handler called on the UI thread if the background task was interrupted.
		 * @param onInterrupted the handler to add
		 * @return this builder instance
		 */
		Builder<T, V> onInterrupted(Runnable onInterrupted);

		/**
		 * Overrides the {@link Dispatcher} used to run the handlers, {@link Dispatcher#instance()} by default.
		 * <p>Provided primarily for testing, {@link Dispatcher#SYNCHRONOUS} runs the handlers synchronously
		 * on the calling thread.
		 * @param dispatcher the dispatcher to use
		 * @return this builder instance
		 */
		Builder<T, V> dispatcher(Dispatcher dispatcher);

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
			if (!onProgress.isEmpty()) {
				uiExecutor.execute(() -> onProgress.forEach(c -> c.accept(progress)));
			}
		}

		@Override
		public void publish(V... chunks) {
			if (!onPublish.isEmpty()) {
				uiExecutor.execute(() -> onPublish.forEach(c -> c.accept(asList(chunks))));
			}
		}
	}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public Builder<?, ?> task(Task task) {
			DefaultBuilder<?, ?> builder = new DefaultBuilder<>(task);
			if (task instanceof TaskHandler) {
				TaskHandler handler = (TaskHandler) task;
				builder.onStarted(handler::onStarted)
								.onDone(handler::onDone)
								.onSuccess(handler::onSuccess)
								.onException(handler::onException)
								.onCancelled(handler::onCancelled)
								.onInterrupted(handler::onInterrupted);
			}

			return builder;
		}

		@Override
		public <T> Builder<T, ?> task(ResultTask<T> task) {
			DefaultBuilder<T, ?> builder = new DefaultBuilder<>(task);
			if (task instanceof ResultTaskHandler) {
				ResultTaskHandler<T> handler = (ResultTaskHandler<T>) task;
				builder.onStarted(handler::onStarted)
								.onDone(handler::onDone)
								.onSuccess(handler::onSuccess)
								.onResult(handler::onResult)
								.onException(handler::onException)
								.onCancelled(handler::onCancelled)
								.onInterrupted(handler::onInterrupted);
			}

			return builder;
		}

		@Override
		public <V> Builder<?, V> task(ProgressTask<V> task) {
			DefaultBuilder<?, V> builder = new DefaultBuilder<>(task);
			if (task instanceof ProgressTaskHandler) {
				ProgressTaskHandler<V> handler = (ProgressTaskHandler<V>) task;
				builder.onStarted(handler::onStarted)
								.onProgress(handler::onProgress)
								.onPublish(handler::onPublish)
								.onDone(handler::onDone)
								.onSuccess(handler::onSuccess)
								.onException(handler::onException)
								.onCancelled(handler::onCancelled)
								.onInterrupted(handler::onInterrupted);
			}

			return builder;
		}

		@Override
		public <T, V> Builder<T, V> task(ProgressResultTask<T, V> task) {
			DefaultBuilder<T, V> builder = new DefaultBuilder<>(task);
			if (task instanceof ProgressResultTaskHandler) {
				ProgressResultTaskHandler<T, V> handler = (ProgressResultTaskHandler<T, V>) task;
				builder.onStarted(handler::onStarted)
								.onProgress(handler::onProgress)
								.onPublish(handler::onPublish)
								.onDone(handler::onDone)
								.onSuccess(handler::onSuccess)
								.onResult(handler::onResult)
								.onException(handler::onException)
								.onCancelled(handler::onCancelled)
								.onInterrupted(handler::onInterrupted);
			}

			return builder;
		}
	}

	private static final class DefaultBuilder<T, V> implements Builder<T, V> {

		private static final Consumer<Exception> RETHROW_HANDLER = new RethrowHandler();
		private static final Runnable INTERRUPT_CURRENT_ON_INTERRUPTED = new InterruptCurrentOnInterrupted();

		private final Object task;

		private @Nullable List<Runnable> onStarted;
		private @Nullable List<Runnable> onDone;
		private @Nullable List<Consumer<T>> onResult;
		private @Nullable List<Consumer<Integer>> onProgress;
		private @Nullable List<Consumer<List<V>>> onPublish;
		private @Nullable List<Consumer<Exception>> onException;
		private @Nullable List<Runnable> onCancelled;
		private @Nullable List<Runnable> onInterrupted;
		private @Nullable Dispatcher dispatcher;

		private DefaultBuilder(Task task) {
			this.task = requireNonNull(task);
		}

		private DefaultBuilder(ProgressTask<V> progressTask) {
			this.task = requireNonNull(progressTask);
		}

		private DefaultBuilder(ResultTask<T> resultTask) {
			this.task = requireNonNull(resultTask);
		}

		private DefaultBuilder(ProgressResultTask<T, V> progressResultTask) {
			this.task = requireNonNull(progressResultTask);
		}

		@Override
		public Builder<T, V> onStarted(Runnable onStarted) {
			this.onStarted = initialize(this.onStarted);
			this.onStarted.add(requireNonNull(onStarted));
			return this;
		}

		@Override
		public Builder<T, V> onDone(Runnable onDone) {
			this.onDone = initialize(this.onDone);
			this.onDone.add(requireNonNull(onDone));
			return this;
		}

		@Override
		public Builder<T, V> onSuccess(Runnable onSuccess) {
			requireNonNull(onSuccess);
			return onResult(result -> onSuccess.run());
		}

		@Override
		public Builder<T, V> onResult(Consumer<T> onResult) {
			this.onResult = initialize(this.onResult);
			this.onResult.add(requireNonNull(onResult));
			return this;
		}

		@Override
		public Builder<T, V> onProgress(Consumer<Integer> onProgress) {
			this.onProgress = initialize(this.onProgress);
			this.onProgress.add(requireNonNull(onProgress));
			return this;
		}

		@Override
		public Builder<T, V> onPublish(Consumer<List<V>> onPublish) {
			this.onPublish = initialize(this.onPublish);
			this.onPublish.add(requireNonNull(onPublish));
			return this;
		}

		@Override
		public Builder<T, V> onException(Consumer<Exception> onException) {
			this.onException = initialize(this.onException);
			this.onException.add(requireNonNull(onException));
			return this;
		}

		@Override
		public Builder<T, V> onCancelled(Runnable onCancelled) {
			this.onCancelled = initialize(this.onCancelled);
			this.onCancelled.add(requireNonNull(onCancelled));
			return this;
		}

		@Override
		public Builder<T, V> onInterrupted(Runnable onInterrupted) {
			this.onInterrupted = initialize(this.onInterrupted);
			this.onInterrupted.add(requireNonNull(onInterrupted));
			return this;
		}

		@Override
		public Builder<T, V> dispatcher(Dispatcher dispatcher) {
			this.dispatcher = requireNonNull(dispatcher);
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

		private static <B> List<B> initialize(@Nullable List<B> list) {
			return list == null ? new ArrayList<>(1) : list;
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

	private static final class DaemonThreadFactory implements ThreadFactory {

		private final AtomicLong counter = new AtomicLong();

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "ProgressWorker-" + counter.incrementAndGet());
			thread.setDaemon(true);

			return thread;
		}
	}
}