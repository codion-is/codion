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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.summary;

import is.codion.common.event.Event;
import is.codion.common.model.summary.ColumnSummaryModel.SummaryValues;
import is.codion.common.observer.Observer;

import org.junit.jupiter.api.Test;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultColumnSummaryModelTest {

	private final Format numberFormat = NumberFormat.getInstance();

	final ColumnSummaryModel testIntModel = new DefaultColumnSummaryModel<>(new SummaryValues<Integer>() {
		@Override
		public String format(Object value) {return numberFormat.format(value);}

		@Override
		public Collection<Integer> values() {
			return asList(1, 2, 3, null, 4, 5);
		}

		@Override
		public boolean subset() {
			return false;
		}

		@Override
		public Observer<?> valuesChanged() {
			return Event.event().observer();
		}
	});

	final ColumnSummaryModel testDoubleModel = new DefaultColumnSummaryModel<>(new SummaryValues<Double>() {
		@Override
		public String format(Object value) {return numberFormat.format(value);}

		@Override
		public Collection<Double> values() {
			return asList(1.1, 2.2, 3.3, null, 4.4, 5.5);
		}

		@Override
		public boolean subset() {
			return false;
		}

		@Override
		public Observer<?> valuesChanged() {
			return Event.event().observer();
		}
	});

	@Test
	void test() {
		testIntModel.summary().set(ColumnSummary.SUM);
		assertEquals(ColumnSummary.SUM, testIntModel.summary().get());
		assertFalse(testIntModel.summaries().isEmpty());
	}

	@Test
	void intSum() {
		testIntModel.summary().set(ColumnSummary.SUM);
		assertEquals("15", testIntModel.summaryText().get());
	}

	@Test
	void intAverage() {
		testIntModel.summary().set(ColumnSummary.AVERAGE);
		assertEquals(numberFormat.format(2.5), testIntModel.summaryText().get());
	}

	@Test
	void intMininum() {
		testIntModel.summary().set(ColumnSummary.MINIMUM);
		assertEquals("1", testIntModel.summaryText().get());
	}

	@Test
	void intMaximum() {
		testIntModel.summary().set(ColumnSummary.MAXIMUM);
		assertEquals("5", testIntModel.summaryText().get());
	}

	@Test
	void intMininumMaximum() {
		testIntModel.summary().set(ColumnSummary.MINIMUM_MAXIMUM);
		assertEquals("1/5", testIntModel.summaryText().get());
	}

	@Test
	void doubleSum() {
		testDoubleModel.summary().set(ColumnSummary.SUM);
		assertEquals(numberFormat.format(16.5), testDoubleModel.summaryText().get());
	}

	@Test
	void doubleAverage() {
		testDoubleModel.summary().set(ColumnSummary.AVERAGE);
		assertEquals(numberFormat.format(2.75), testDoubleModel.summaryText().get());
	}

	@Test
	void doubleMininum() {
		testDoubleModel.summary().set(ColumnSummary.MINIMUM);
		assertEquals(numberFormat.format(1.1), testDoubleModel.summaryText().get());
	}

	@Test
	void doubleMaximum() {
		testDoubleModel.summary().set(ColumnSummary.MAXIMUM);
		assertEquals(numberFormat.format(5.5), testDoubleModel.summaryText().get());
	}

	@Test
	void doubleMininumMaximum() {
		testDoubleModel.summary().set(ColumnSummary.MINIMUM_MAXIMUM);
		assertEquals(numberFormat.format(1.1) + "/" + numberFormat.format(5.5), testDoubleModel.summaryText().get());
	}

	@Test
	void locked() {
		testDoubleModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> testDoubleModel.summary().set(ColumnSummary.MINIMUM_MAXIMUM));
	}
}
