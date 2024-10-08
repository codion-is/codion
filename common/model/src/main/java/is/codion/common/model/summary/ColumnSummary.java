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
 * Copyright (c) 2015 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.summary;

import is.codion.common.model.summary.SummaryModel.SummaryValues;
import is.codion.common.resource.MessageBundle;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalDouble;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * The summary types available to the default summary model
 */
public enum ColumnSummary implements SummaryModel.Summary {
	/**
	 * No summary
	 */
	NONE {
		@Override
		public String toString() {
			return MESSAGES.getString("none");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			return "";
		}
	},
	/**
	 * The sum of the supplied values
	 */
	SUM {
		@Override
		public String toString() {
			return MESSAGES.getString("sum");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			Collection<T> values = summaryValues.values();
			if (!values.isEmpty()) {
				return addSubsetIndicator(summaryValues.format(values.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue).sum()), summaryValues.subset());
			}

			return "";
		}
	},
	/**
	 * The average of the supplied values
	 */
	AVERAGE {
		@Override
		public String toString() {
			return MESSAGES.getString("average");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			Collection<T> values = summaryValues.values();
			if (!values.isEmpty()) {
				OptionalDouble average = values.stream()
								.mapToDouble(value -> value == null ? 0d : value.doubleValue())
								.average();
				if (average.isPresent()) {
					return addSubsetIndicator(summaryValues.format(average.getAsDouble()), summaryValues.subset());
				}
			}

			return "";
		}
	},
	/**
	 * The minimum of the supplied values
	 */
	MINIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("minimum");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			Collection<T> values = summaryValues.values();
			if (!values.isEmpty()) {
				OptionalDouble min = values.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.min();
				if (min.isPresent()) {
					return addSubsetIndicator(summaryValues.format(min.getAsDouble()), summaryValues.subset());
				}
			}

			return "";
		}
	},
	/**
	 * The maximum of the supplied values
	 */
	MAXIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("maximum");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			Collection<T> values = summaryValues.values();
			if (!values.isEmpty()) {
				OptionalDouble max = values.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.max();
				if (max.isPresent()) {
					return addSubsetIndicator(summaryValues.format(max.getAsDouble()), summaryValues.subset());
				}
			}

			return "";
		}
	},
	/**
	 * The minimum and maximum of the supplied values
	 */
	MINIMUM_MAXIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("minimum_and_maximum");
		}

		@Override
		public <T extends Number> String summary(SummaryValues<T> summaryValues) {
			Collection<T> values = summaryValues.values();
			if (!values.isEmpty()) {
				OptionalDouble min = values.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.min();
				OptionalDouble max = values.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.max();
				if (min.isPresent() && max.isPresent()) {
					return addSubsetIndicator(summaryValues.format(min.getAsDouble()) + "/" + summaryValues.format(max.getAsDouble()), summaryValues.subset());
				}
			}

			return "";
		}
	};

	private static final MessageBundle MESSAGES = messageBundle(ColumnSummary.class, getBundle(ColumnSummary.class.getName()));

	private static String addSubsetIndicator(String text, boolean subset) {
		return text.isEmpty() ? text : text + (subset ? "*" : "");
	}
}
