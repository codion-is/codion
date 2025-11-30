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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.worker;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.value.Value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public final class ProgressWorkerTest {

	@Test
	void test() throws Exception {
		Value<Integer> progressValue = Value.nullable();
		Value<String> messageValue = Value.nullable();

		List<Integer> stateChanges = new ArrayList<>();

		ProgressWorker.ProgressResultTask<Integer, String> task = progressReporter -> {
			Thread.sleep(100);
			progressReporter.report(100);
			progressReporter.publish("Done");

			return 42;
		};
		ProgressWorker.builder()
						.task(task)
						.onStarted(() -> stateChanges.add(0))
						.onProgress(progressValue::set)
						.onPublish(chunks -> messageValue.set(chunks.get(0)))
						.onDone(() -> {
							stateChanges.add(1);
							assertEquals(100, progressValue.get());
							assertEquals("Done", messageValue.get());
						})
						.onResult(result -> {
							stateChanges.add(2);
							assertEquals(42, result);
							assertEquals(3, stateChanges.size());
							//sanity check the order of state changes
							for (int i = 0; i < stateChanges.size(); i++) {
								assertEquals(Integer.valueOf(i), stateChanges.get(i));
							}
						})
						.onCancelled(() -> {})
						.onInterrupted(() -> {})
						.onException(exception -> {})
						.execute()
						.get();
	}

	@Test
	void progressWorkerException() throws Exception {
		AtomicBoolean onStartedCalled = new AtomicBoolean();
		AtomicBoolean onDoneCalled = new AtomicBoolean();
		AtomicBoolean onExceptionCalled = new AtomicBoolean();
		AtomicReference<Exception> caughtException = new AtomicReference<>();

		CountDownLatch latch = new CountDownLatch(1);

		Exception testException = new RuntimeException("Test exception");

		ProgressWorker.ResultTask<Integer> task = () -> {
			throw testException;
		};

		ProgressWorker<Integer, ?> worker = ProgressWorker.builder()
						.task(task)
						.onStarted(() -> onStartedCalled.set(true))
						.onDone(() -> onDoneCalled.set(true))
						.onException(exception -> {
							onExceptionCalled.set(true);
							caughtException.set(exception);
							latch.countDown();
						})
						.execute();

		assertTrue(latch.await(5, TimeUnit.SECONDS));

		assertTrue(onStartedCalled.get());
		assertTrue(onDoneCalled.get());
		assertTrue(onExceptionCalled.get());
		assertEquals(testException, caughtException.get());

		// onResult should not be called when exception occurs
		assertThrows(Exception.class, worker::get);
	}

	@Test
	void progressWorkerCancellation() throws Exception {
		CountDownLatch taskStartedLatch = new CountDownLatch(1);
		CountDownLatch cancelledLatch = new CountDownLatch(1);

		AtomicBoolean onCancelledCalled = new AtomicBoolean();
		AtomicBoolean onDoneCalled = new AtomicBoolean();
		AtomicBoolean onResultCalled = new AtomicBoolean();

		ProgressWorker.Task task = () -> {
			taskStartedLatch.countDown();
			// Sleep long enough to allow cancellation
			Thread.sleep(1000);
		};

		ProgressWorker<?, ?> worker = ProgressWorker.builder()
						.task(task)
						.onDone(() -> onDoneCalled.set(true))
						.onCancelled(() -> {
							onCancelledCalled.set(true);
							cancelledLatch.countDown();
						})
						.onResult(result -> onResultCalled.set(true))
						.execute();

		// Wait for task to start
		assertTrue(taskStartedLatch.await(5, TimeUnit.SECONDS));

		// Cancel the worker
		worker.cancel(true);

		// Wait for cancellation to be processed
		assertTrue(cancelledLatch.await(5, TimeUnit.SECONDS));

		assertTrue(onDoneCalled.get());
		assertTrue(onCancelledCalled.get());
		assertFalse(onResultCalled.get());
		assertTrue(worker.isCancelled());
	}

	@Test
	void progressWorkerCancelException() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean onCancelledCalled = new AtomicBoolean();

		// Test CancelException handling
		ProgressWorker.ResultTask<Integer> task = () -> {
			throw new CancelException();
		};

		ProgressWorker.builder()
						.task(task)
						.onCancelled(() -> {
							onCancelledCalled.set(true);
							latch.countDown();
						})
						.execute();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertTrue(onCancelledCalled.get());
	}

	@Test
	void progressWorkerInterruption() throws Exception {
		CountDownLatch taskStartedLatch = new CountDownLatch(1);
		CountDownLatch completionLatch = new CountDownLatch(1);

		AtomicBoolean onInterruptedCalled = new AtomicBoolean();
		AtomicBoolean onCancelledCalled = new AtomicBoolean();
		AtomicBoolean onDoneCalled = new AtomicBoolean();

		ProgressWorker.Task task = () -> {
			taskStartedLatch.countDown();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
		};

		ProgressWorker<?, ?> worker = ProgressWorker.builder()
						.task(task)
						.onDone(() -> onDoneCalled.set(true))
						.onInterrupted(() -> {
							onInterruptedCalled.set(true);
							completionLatch.countDown();
						})
						.onCancelled(() -> {
							onCancelledCalled.set(true);
							completionLatch.countDown();
						})
						.execute();

		// Wait for task to start
		assertTrue(taskStartedLatch.await(5, TimeUnit.SECONDS));

		// Cancel with interruption
		worker.cancel(true);

		// Wait for either interruption or cancellation to be processed
		assertTrue(completionLatch.await(5, TimeUnit.SECONDS));

		assertTrue(onDoneCalled.get());
		// Either onInterrupted or onCancelled should be called, depending on SwingWorker implementation
		assertTrue(onInterruptedCalled.get() || onCancelledCalled.get());
	}

	@Test
	void progressWorkerMultipleCallbacks() throws Exception {
		Value<Integer> progressValue = Value.nullable();
		List<String> publishedMessages = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);

		ProgressWorker.ProgressResultTask<String, String> task = progressReporter -> {
			progressReporter.report(25);
			progressReporter.publish("25% complete");
			Thread.sleep(50);

			progressReporter.report(50);
			progressReporter.publish("50% complete");
			Thread.sleep(50);

			progressReporter.report(75);
			progressReporter.publish("75% complete");
			Thread.sleep(50);

			progressReporter.report(100);
			progressReporter.publish("100% complete");

			return "Task completed";
		};

		ProgressWorker.builder()
						.task(task)
						.onProgress(progressValue::set)
						.onPublish(publishedMessages::addAll)
						.onResult(result -> {
							assertEquals("Task completed", result);
							assertEquals(100, progressValue.get());
							// Due to SwingWorker behavior, we should have received all messages
							assertTrue(publishedMessages.contains("100% complete"));
							latch.countDown();
						})
						.execute();

		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	@Test
	void progressWorkerResultTask() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> resultRef = new AtomicReference<>();

		ProgressWorker.ResultTask<String> task = () -> {
			Thread.sleep(50);
			return "Result from task";
		};

		ProgressWorker.builder()
						.task(task)
						.onResult(result -> {
							resultRef.set(result);
							latch.countDown();
						})
						.execute();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals("Result from task", resultRef.get());
	}

	@Test
	void progressWorkerVoidTask() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean taskExecuted = new AtomicBoolean();

		ProgressWorker.Task task = () -> {
			Thread.sleep(50);
			taskExecuted.set(true);
		};

		ProgressWorker.builder()
						.task(task)
						.onDone(latch::countDown)
						.execute();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertTrue(taskExecuted.get());
	}

	@Test
	void progressWorkerProgressTask() throws Exception {
		List<Integer> progressReports = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);

		ProgressWorker.ProgressTask<String> task = progressReporter -> {
			for (int i = 0; i <= 100; i += 25) {
				progressReporter.report(i);
				progressReporter.publish("Progress: " + i + "%");
				Thread.sleep(20);
			}
		};

		ProgressWorker.builder()
						.task(task)
						.onProgress(progressReports::add)
						.onDone(latch::countDown)
						.execute();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertFalse(progressReports.isEmpty());
		assertTrue(progressReports.contains(100));
	}
}
