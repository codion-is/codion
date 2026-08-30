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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.filter;

import is.codion.common.model.filter.FilterModel.Refresher;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The asynchronous refresh path only runs where a dispatch context is bound, which in this build means
 * here, where SwingDispatcher is on the classpath - common-model's own tests resolve
 * Dispatcher.SYNCHRONOUS and take the synchronous path.
 */
public final class RefreshDelayTest {

	@Test
	void burstResultsInOneFetch() throws Exception {
		AtomicInteger fetches = new AtomicInteger();
		CountDownLatch refreshed = new CountDownLatch(1);
		AtomicReference<Collection<String>> result = new AtomicReference<>();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							fetches.incrementAndGet();

							return singletonList("a");
						})
						.onResult(items -> {
							result.set(items);
							refreshed.countDown();
						})
						.build();
		refresher.delay().set(100);

		SwingUtilities.invokeAndWait(() -> {
			//ten refreshes well inside the window, each replacing the one waiting
			for (int i = 0; i < 10; i++) {
				refresher.refresh(null);
			}
			//under way from the moment the first was asked for, not only once fetching
			assertTrue(refresher.active().is());
		});

		assertTrue(refreshed.await(10, SECONDS));
		assertEquals(singletonList("a"), result.get());
		assertEquals(1, fetches.get());
		SwingUtilities.invokeAndWait(() -> assertFalse(refresher.active().is()));
	}

	@Test
	void refreshWithoutDelayIsUnchanged() throws Exception {
		AtomicInteger fetches = new AtomicInteger();
		CountDownLatch refreshed = new CountDownLatch(1);
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							fetches.incrementAndGet();

							return List.of("a");
						})
						.onResult(items -> refreshed.countDown())
						.build();
		assertEquals(0, refresher.delay().getOrThrow());

		//no wait, so active only goes on once the worker starts, as it always has
		SwingUtilities.invokeAndWait(() -> refresher.refresh(null));

		assertTrue(refreshed.await(10, SECONDS));
		assertEquals(1, fetches.get());
	}

	@Test
	void aScheduledRefreshCancelledTooLateDoesNotStart() throws Exception {
		//cancelling the schedule is the optimisation, the identity check is the correctness: a scheduled
		//refresh the scheduler already picked up hands its start to the dispatch context, where it may
		//arrive behind a refresh that replaced it. Forced here by holding the dispatch thread past the
		//wait, so the start is queued before the second refresh replaces the first
		AtomicInteger fetches = new AtomicInteger();
		CountDownLatch refreshed = new CountDownLatch(1);
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							fetches.incrementAndGet();

							return singletonList("a");
						})
						.onResult(items -> refreshed.countDown())
						.build();
		refresher.delay().set(1);

		SwingUtilities.invokeAndWait(() -> {
			refresher.refresh(null);
			try {
				Thread.sleep(100);//the scheduler fires and queues its start behind this
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			refresher.refresh(null);//replaces it, too late to cancel
		});

		assertTrue(refreshed.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {});//drain anything still queued
		assertEquals(1, fetches.get());
	}

	@Test
	void syncRefreshSupersedesAWaitingOne() throws Exception {
		AtomicInteger fetches = new AtomicInteger();
		AtomicInteger delayedCallbacks = new AtomicInteger();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							fetches.incrementAndGet();

							return singletonList("a");
						})
						.build();
		refresher.delay().set(500);

		SwingUtilities.invokeAndWait(() -> refresher.refresh(items -> delayedCallbacks.incrementAndGet()));
		//no dispatch context bound on this thread, so this refresh is synchronous - and it supersedes
		//the one waiting, rather than leaving it to fetch and deliver after it
		refresher.refresh(null);
		assertEquals(1, fetches.get());

		Thread.sleep(1_000);//long past the wait
		SwingUtilities.invokeAndWait(() -> {});//drain anything that should not be there
		assertEquals(1, fetches.get());
		assertEquals(0, delayedCallbacks.get());
		SwingUtilities.invokeAndWait(() -> assertFalse(refresher.active().is()));
	}

	@Test
	void syncRefreshSupersedesAFetchInFlight() throws Exception {
		//the no-delay half: the asynchronous fetch is already under way when the synchronous one runs,
		//so cancelling the schedule can not help - the task identity has the final say
		CountDownLatch fetching = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean firstFetch = new AtomicBoolean(true);
		AtomicInteger asyncCallbacks = new AtomicInteger();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							if (firstFetch.getAndSet(false)) {
								fetching.countDown();
								try {
									release.await(10, SECONDS);
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}

							return singletonList("a");
						})
						.build();

		SwingUtilities.invokeAndWait(() -> refresher.refresh(items -> asyncCallbacks.incrementAndGet()));
		assertTrue(fetching.await(10, SECONDS));
		refresher.refresh(null);//synchronous, supersedes the fetch in flight
		release.countDown();
		Thread.sleep(200);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(0, asyncCallbacks.get());
	}

	@Test
	void theLastCallbackWins() throws Exception {
		//a refresh superseded while waiting invokes no callbacks, as one superseded while fetching does not
		CountDownLatch refreshed = new CountDownLatch(1);
		AtomicInteger firstCallbacks = new AtomicInteger();
		AtomicInteger lastCallbacks = new AtomicInteger();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> singletonList("a"))
						.build();
		refresher.delay().set(100);

		SwingUtilities.invokeAndWait(() -> {
			refresher.refresh(items -> firstCallbacks.incrementAndGet());
			refresher.refresh(items -> {
				lastCallbacks.incrementAndGet();
				refreshed.countDown();
			});
		});

		assertTrue(refreshed.await(10, SECONDS));
		assertEquals(0, firstCallbacks.get());
		assertEquals(1, lastCallbacks.get());
	}
}
