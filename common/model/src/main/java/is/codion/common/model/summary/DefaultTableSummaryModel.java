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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.summary;

import is.codion.common.model.summary.SummaryModel.SummaryValues;

import java.text.Format;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultTableSummaryModel<C> implements TableSummaryModel<C> {

	private final SummaryValues.Factory<C> summaryValuesFactory;
	private final Map<C, SummaryModel> summaryModels = new HashMap<>();

	DefaultTableSummaryModel(SummaryValues.Factory<C> summaryValuesFactory) {
		this.summaryValuesFactory = requireNonNull(summaryValuesFactory);
	}

	@Override
	public Optional<SummaryModel> summaryModel(C identifier) {
		return Optional.ofNullable(summaryModels.computeIfAbsent(requireNonNull(identifier), k ->
						createSummaryModel(k, NumberFormat.getInstance()).orElse(null)));
	}

	private Optional<SummaryModel> createSummaryModel(C identifier, Format format) {
		return summaryValuesFactory.createSummaryValues(identifier, format)
						.map(SummaryModel::summaryModel);
	}
}
