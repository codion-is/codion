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
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.resource.MessageBundle;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalDouble;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * The summary types available to the default summary model
 */
public enum ColumnSummary implements ColumnSummaryModel.Summary {
	NONE {
		@Override
		public String toString() {
			return MESSAGES.getString("none");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			return "";
		}
	}, SUM {
		@Override
		public String toString() {
			return MESSAGES.getString("sum");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			Collection<T> summaryValues = valueProvider.values();
			if (!summaryValues.isEmpty()) {
				return addSubsetIndicator(valueProvider.format(summaryValues.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue).sum()), valueProvider.subset());
			}

			return "";
		}
	}, AVERAGE {
		@Override
		public String toString() {
			return MESSAGES.getString("average");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			Collection<T> summaryValues = valueProvider.values();
			if (!summaryValues.isEmpty()) {
				OptionalDouble average = summaryValues.stream()
								.mapToDouble(value -> value == null ? 0d : value.doubleValue())
								.average();
				if (average.isPresent()) {
					return addSubsetIndicator(valueProvider.format(average.getAsDouble()), valueProvider.subset());
				}
			}

			return "";
		}
	}, MINIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("minimum");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			Collection<T> summaryValues = valueProvider.values();
			if (!summaryValues.isEmpty()) {
				OptionalDouble min = summaryValues.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.min();
				if (min.isPresent()) {
					return addSubsetIndicator(valueProvider.format(min.getAsDouble()), valueProvider.subset());
				}
			}

			return "";
		}
	}, MAXIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("maximum");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			Collection<T> summaryValues = valueProvider.values();
			if (!summaryValues.isEmpty()) {
				OptionalDouble max = summaryValues.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.max();
				if (max.isPresent()) {
					return addSubsetIndicator(valueProvider.format(max.getAsDouble()), valueProvider.subset());
				}
			}

			return "";
		}
	}, MINIMUM_MAXIMUM {
		@Override
		public String toString() {
			return MESSAGES.getString("minimum_and_maximum");
		}

		@Override
		public <T extends Number> String summary(SummaryValueProvider<T> valueProvider) {
			Collection<T> summaryValues = valueProvider.values();
			if (!summaryValues.isEmpty()) {
				OptionalDouble min = summaryValues.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.min();
				OptionalDouble max = summaryValues.stream()
								.filter(Objects::nonNull)
								.mapToDouble(Number::doubleValue)
								.max();
				if (min.isPresent() && max.isPresent()) {
					return addSubsetIndicator(valueProvider.format(min.getAsDouble()) + "/" + valueProvider.format(max.getAsDouble()), valueProvider.subset());
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
