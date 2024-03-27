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
package is.codion.common.model.table;

import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;

import java.util.Optional;

/**
 * @param <C> the column identifier type
 */
public interface TableSummaryModel<C> {

	/**
	 * Returns the {@link ColumnSummaryModel} associated with {@code columnIdentifier}
	 * @param columnIdentifier the column identifier
	 * @return the ColumnSummaryModel for the column identified by the given identifier, an empty Optional if none is available
	 */
	Optional<ColumnSummaryModel> summaryModel(C columnIdentifier);

	/**
	 * @param summaryModelFactory the summary model factory
	 * @param <C> the column identifier type
	 * @return a new {@link TableSummaryModel} instance
	 */
	static <C> TableSummaryModel<C> tableSummaryModel(SummaryValueProvider.Factory<C> summaryModelFactory) {
		return new DefaultTableSummaryModel<>(summaryModelFactory);
	}
}
