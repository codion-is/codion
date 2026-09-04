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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.summary;

import is.codion.common.model.summary.SummaryModel.SummaryValues;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;

import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultTableSummaryModelTest {

	@Test
	void nullFactory() {
		assertThrows(NullPointerException.class, () -> new DefaultTableSummaryModel<String>(null));
	}

	@Test
	void nullIdentifier() {
		DefaultTableSummaryModel<String> summaries = new DefaultTableSummaryModel<>(new TestFactory());
		assertThrows(NullPointerException.class, () -> summaries.get(null));
	}

	@Test
	void createdOnceAndCached() {
		TestFactory factory = new TestFactory();
		DefaultTableSummaryModel<String> summaries = new DefaultTableSummaryModel<>(factory);
		Optional<SummaryModel> summary = summaries.get("a");
		assertTrue(summary.isPresent());
		assertSame(summary.get(), summaries.get("a").get());
		assertEquals(1, factory.calls);
		assertEquals("a", factory.identifier);
		assertEquals(NumberFormat.getInstance(), factory.format);
	}

	@Test
	void oneModelPerIdentifier() {
		TestFactory factory = new TestFactory();
		DefaultTableSummaryModel<String> summaries = new DefaultTableSummaryModel<>(factory);
		assertNotSame(summaries.get("a").get(), summaries.get("b").get());
		assertEquals(2, factory.calls);
	}

	@Test
	void absenceCached() {
		TestFactory factory = new TestFactory();
		factory.available = false;
		DefaultTableSummaryModel<String> summaries = new DefaultTableSummaryModel<>(factory);
		assertFalse(summaries.get("a").isPresent());
		assertFalse(summaries.get("a").isPresent());
		assertEquals(1, factory.calls);
	}

	@Test
	void failedCreationNotCached() {
		TestFactory factory = new TestFactory();
		factory.exception = new IllegalStateException();
		DefaultTableSummaryModel<String> summaries = new DefaultTableSummaryModel<>(factory);
		assertThrows(IllegalStateException.class, () -> summaries.get("a"));
		factory.exception = null;
		assertTrue(summaries.get("a").isPresent());
		assertEquals(2, factory.calls);
	}

	private static final class TestFactory implements SummaryValues.Factory<String> {

		boolean available = true;
		RuntimeException exception;
		int calls;
		String identifier;
		Format format;

		@Override
		public <T extends Number> Optional<SummaryValues<T>> create(String identifier, Format format) {
			calls++;
			this.identifier = identifier;
			this.format = format;
			if (exception != null) {
				throw exception;
			}

			return available ? Optional.of(new TestSummaryValues<>()) : Optional.empty();
		}
	}

	private static final class TestSummaryValues<T extends Number> implements SummaryValues<T> {

		private final Event<Object> valuesChanged = Event.event();

		@Override
		public String format(Object value) {
			return String.valueOf(value);
		}

		@Override
		public Collection<T> values() {
			return emptyList();
		}

		@Override
		public boolean subset() {
			return false;
		}

		@Override
		public Observer<?> valuesChanged() {
			return valuesChanged.observer();
		}
	}
}