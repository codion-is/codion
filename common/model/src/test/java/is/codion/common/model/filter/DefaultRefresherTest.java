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

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultRefresherTest {

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
						.async(false)
						.build();

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
						.async(false)
						.build();

		assertDoesNotThrow(() -> refresher.refresh(null));
		assertEquals(1, onExceptionCalls.get());
		assertFalse(refresher.active().is());
	}
}
