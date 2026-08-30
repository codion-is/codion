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
package is.codion.common.model.filter;

import is.codion.common.model.filter.FilterModel.Refresher;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultRefresherTest {

	@Test
	void delayDefaultsToTheConfiguredValue() {
		assertEquals(0, Refresher.builder().items(Collections::emptyList).build().delay().getOrThrow());

		FilterModel.REFRESH_DELAY.set(250);
		try {
			assertEquals(250, Refresher.builder().items(Collections::emptyList).build().delay().getOrThrow());
		}
		finally {
			FilterModel.REFRESH_DELAY.set(0);
		}
	}

	@Test
	void delayRejectsNegative() {
		Refresher<String> refresher = Refresher.<String>builder().items(Collections::emptyList).build();
		assertThrows(IllegalArgumentException.class, () -> refresher.delay().set(-1));
		assertEquals(0, refresher.delay().getOrThrow());
		//null reverts to no delay rather than failing
		refresher.delay().clear();
		assertEquals(0, refresher.delay().getOrThrow());
	}

	@Test
	void syncRefreshIgnoresTheDelay() {
		//a synchronous refresh exists to be immediate and predictable, so it does not wait
		AtomicInteger supplierCalls = new AtomicInteger();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							supplierCalls.incrementAndGet();

							return asList("a", "b");
						})
						.build();
		refresher.async().set(false);
		refresher.delay().set(60_000);

		refresher.refresh(null);
		assertEquals(1, supplierCalls.get());
		assertFalse(refresher.active().is());
	}

	@Test
	void syncRefreshResultConsumerExceptionPropagatesNotRoutedToOnException() {
		//on the sync path a result consumer throwing must propagate to the caller, as it does on the
		//async path, rather than being caught and reported as a refresh failure via onException
		AtomicInteger onExceptionCalls = new AtomicInteger();
		RuntimeException consumerException = new RuntimeException("boom");
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> asList("a", "b"))
						.onResult(result -> {
							throw consumerException;
						})
						.onException(exception -> onExceptionCalls.incrementAndGet())
						.build();
		refresher.async().set(false);

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> refresher.refresh(null));
		assertSame(consumerException, thrown);
		assertEquals(0, onExceptionCalls.get());
		assertFalse(refresher.active().is());
	}

	@Test
	void syncRefreshItemsSupplierExceptionRoutedToOnException() {
		//an exception from the refresh itself (the items supplier) is a refresh failure and must reach onException
		AtomicInteger onExceptionCalls = new AtomicInteger();
		Refresher<String> refresher = Refresher.<String>builder()
						.items(() -> {
							throw new RuntimeException("refresh failed");
						})
						.onException(exception -> onExceptionCalls.incrementAndGet())
						.build();
		refresher.async().set(false);

		assertDoesNotThrow(() -> refresher.refresh(null));
		assertEquals(1, onExceptionCalls.get());
		assertFalse(refresher.active().is());
	}
}
