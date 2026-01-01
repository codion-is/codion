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
import is.codion.common.utilities.Text;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DefaultTableSummaryModel.
 * Tests caching, factory delegation, and generic type handling.
 */
public class DefaultTableSummaryModelTest {

	private static final String COLUMN_A = "columnA";
	private static final String COLUMN_B = "columnB";
	private static final Integer COLUMN_ID_1 = 1;
	private static final Integer COLUMN_ID_2 = 2;

	@Nested
	@DisplayName("String column identifiers")
	class StringColumnTest {

		private TestFactory<String> factory;
		private DefaultTableSummaryModel<String> tableSummary;

		@BeforeEach
		void setUp() {
			factory = new TestFactory<>();
			tableSummary = new DefaultTableSummaryModel<>(factory);
		}

		@Test
		@DisplayName("Constructor throws NPE for null factory")
		void constructor_nullFactory_throwsNPE() {
			assertThrows(NullPointerException.class, () -> new DefaultTableSummaryModel<String>(null));
		}

		@Test
		@DisplayName("First call to get creates and caches summary model")
		void get_firstCall_createsSummaryModel() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get(COLUMN_A);

			assertTrue(result.isPresent());
			assertEquals(1, factory.callCount);
			assertEquals(COLUMN_A, factory.lastIdentifier);
		}

		@Test
		@DisplayName("Second call with same identifier returns cached instance")
		void get_secondCall_returnsCachedInstance() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> first = tableSummary.get(COLUMN_A);
			Optional<SummaryModel> second = tableSummary.get(COLUMN_A);

			assertSame(first.get(), second.get());
			assertEquals(1, factory.callCount); // Factory called only once
		}

		@Test
		@DisplayName("Different identifiers create different instances")
		void get_differentIdentifiers_createsDifferentInstances() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> resultA = tableSummary.get(COLUMN_A);
			Optional<SummaryModel> resultB = tableSummary.get(COLUMN_B);

			assertTrue(resultA.isPresent());
			assertTrue(resultB.isPresent());
			assertNotSame(resultA.get(), resultB.get());
			assertEquals(2, factory.callCount); // Factory called twice
		}

		@Test
		@DisplayName("Factory returns empty Optional results in empty")
		void get_factoryReturnsEmpty_returnsEmpty() {
			factory.returnValue = Optional.empty();

			Optional<SummaryModel> result = tableSummary.get(COLUMN_A);

			assertFalse(result.isPresent());
			assertEquals(1, factory.callCount);
		}

		@Test
		@DisplayName("Multiple calls when factory returns empty")
		void get_multipleCallsEmptyFactory_alwaysCallsFactory() {
			factory.returnValue = Optional.empty();

			tableSummary.get(COLUMN_A);
			tableSummary.get(COLUMN_A);

			// Should call factory each time since no caching occurs for empty results
			assertEquals(2, factory.callCount);
		}

		@Test
		@DisplayName("Null identifier handling")
		void get_nullIdentifier_throwsNPE() {
			assertThrows(NullPointerException.class, () -> tableSummary.get(null));
		}

		@Test
		@DisplayName("Created summary model uses default NumberFormat")
		void get_createdSummaryModel_usesDefaultNumberFormat() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get(COLUMN_A);
			SummaryModel summaryModel = result.get();

			// Verify the model was created with NumberFormat.getInstance()
			assertNotNull(summaryModel);
			assertEquals(NumberFormat.getInstance(), factory.lastFormat);
		}
	}

	@Nested
	@DisplayName("Integer column identifiers")
	class IntegerColumnTest {

		private TestFactory<Integer> factory;
		private DefaultTableSummaryModel<Integer> tableSummary;

		@BeforeEach
		void setUp() {
			factory = new TestFactory<>();
			tableSummary = new DefaultTableSummaryModel<>(factory);
		}

		@Test
		@DisplayName("Works with Integer column identifiers")
		void get_integerIdentifiers_worksCorrectly() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get(COLUMN_ID_1);

			assertTrue(result.isPresent());
			assertEquals(1, factory.callCount);
			assertEquals(COLUMN_ID_1, factory.lastIdentifier);
		}

		@Test
		@DisplayName("Caching works with Integer identifiers")
		void get_integerIdentifiersCaching_worksCorrectly() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> first = tableSummary.get(COLUMN_ID_1);
			Optional<SummaryModel> second = tableSummary.get(COLUMN_ID_1);

			assertSame(first.get(), second.get());
			assertEquals(1, factory.callCount);
		}

		@Test
		@DisplayName("Different Integer identifiers work independently")
		void get_differentIntegerIdentifiers_workIndependently() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result1 = tableSummary.get(COLUMN_ID_1);
			Optional<SummaryModel> result2 = tableSummary.get(COLUMN_ID_2);

			assertTrue(result1.isPresent());
			assertTrue(result2.isPresent());
			assertNotSame(result1.get(), result2.get());
			assertEquals(2, factory.callCount);
		}
	}

	enum TestColumn {
		PRICE, QUANTITY, TOTAL
	}

	@Nested
	@DisplayName("Enum column identifiers")
	class EnumColumnTest {

		private TestFactory<TestColumn> factory;
		private DefaultTableSummaryModel<TestColumn> tableSummary;

		@BeforeEach
		void setUp() {
			factory = new TestFactory<>();
			tableSummary = new DefaultTableSummaryModel<>(factory);
		}

		@Test
		@DisplayName("Works with enum column identifiers")
		void get_enumIdentifiers_worksCorrectly() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get(TestColumn.PRICE);

			assertTrue(result.isPresent());
			assertEquals(1, factory.callCount);
			assertEquals(TestColumn.PRICE, factory.lastIdentifier);
		}

		@Test
		@DisplayName("All enum values can be used as identifiers")
		void get_allEnumValues_workCorrectly() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> priceResult = tableSummary.get(TestColumn.PRICE);
			Optional<SummaryModel> quantityResult = tableSummary.get(TestColumn.QUANTITY);
			Optional<SummaryModel> totalResult = tableSummary.get(TestColumn.TOTAL);

			assertTrue(priceResult.isPresent());
			assertTrue(quantityResult.isPresent());
			assertTrue(totalResult.isPresent());

			// All should be different instances
			assertNotSame(priceResult.get(), quantityResult.get());
			assertNotSame(quantityResult.get(), totalResult.get());
			assertNotSame(priceResult.get(), totalResult.get());

			assertEquals(3, factory.callCount);
		}
	}

	@Nested
	@DisplayName("Factory error handling")
	class FactoryErrorHandlingTest {

		private TestFactory<String> factory;
		private DefaultTableSummaryModel<String> tableSummary;

		@BeforeEach
		void setUp() {
			factory = new TestFactory<>();
			tableSummary = new DefaultTableSummaryModel<>(factory);
		}

		@Test
		@DisplayName("Factory throwing exception propagates to caller")
		void get_factoryThrowsException_propagatesToCaller() {
			RuntimeException expectedException = new RuntimeException("Factory error");
			factory.throwException = expectedException;

			RuntimeException actualException = assertThrows(RuntimeException.class,
							() -> tableSummary.get(COLUMN_A));

			assertSame(expectedException, actualException);
		}

		@Test
		@DisplayName("Exception on first call does not prevent subsequent calls")
		void get_exceptionThenSuccess_subsequentCallsWork() {
			// First call throws exception
			factory.throwException = new RuntimeException("First call fails");
			assertThrows(RuntimeException.class, () -> tableSummary.get(COLUMN_A));

			// Second call succeeds
			factory.throwException = null;
			factory.returnValue = Optional.of(new TestSummaryValues());
			Optional<SummaryModel> result = tableSummary.get(COLUMN_A);

			assertTrue(result.isPresent());
		}
	}

	@Nested
	@DisplayName("Edge cases")
	class EdgeCasesTest {

		private TestFactory<String> factory;
		private DefaultTableSummaryModel<String> tableSummary;

		@BeforeEach
		void setUp() {
			factory = new TestFactory<>();
			tableSummary = new DefaultTableSummaryModel<>(factory);
		}

		@Test
		@DisplayName("Empty string identifier works")
		void get_emptyStringIdentifier_works() {
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get("");

			assertTrue(result.isPresent());
			assertEquals("", factory.lastIdentifier);
		}

		@Test
		@DisplayName("Very long string identifier works")
		void get_longStringIdentifier_works() {
			String longIdentifier = Text.leftPad("", 1000, 'a');
			factory.returnValue = Optional.of(new TestSummaryValues());

			Optional<SummaryModel> result = tableSummary.get(longIdentifier);

			assertTrue(result.isPresent());
			assertEquals(longIdentifier, factory.lastIdentifier);
		}

		@Test
		@DisplayName("Factory returns null throws NPE")
		void get_factoryReturnsNull_throwsNPE() {
			factory.returnValue = null;

			assertThrows(NullPointerException.class, () -> tableSummary.get(COLUMN_A));
		}
	}

	// Test implementations

	private static class TestFactory<C> implements SummaryValues.Factory<C> {
		Optional<? extends SummaryValues<? extends Number>> returnValue = Optional.empty();
		RuntimeException throwException = null;
		int callCount = 0;
		C lastIdentifier = null;
		Format lastFormat = null;

		@Override
		public <T extends Number> Optional<SummaryValues<T>> createSummaryValues(C identifier, Format format) {
			callCount++;
			lastIdentifier = identifier;
			lastFormat = format;

			if (throwException != null) {
				throw throwException;
			}

			if (returnValue == null) {
				throw new NullPointerException("Factory returned null");
			}

			// Type casting for test purposes - in real scenarios this would be type-safe
			@SuppressWarnings("unchecked")
			Optional<SummaryValues<T>> result = (Optional<SummaryValues<T>>) returnValue;
			return result;
		}
	}

	private static class TestSummaryValues implements SummaryValues<Integer> {
		@Override
		public String format(Object value) {
			return value != null ? value.toString() : "null";
		}

		@Override
		public Collection<Integer> values() {
			return asList(1, 2, 3, 4, 5);
		}

		@Override
		public boolean subset() {
			return false;
		}

		@Override
		public Observer<?> valuesChanged() {
			// Return a simple observer for test purposes
			return Event.event();
		}
	}
}