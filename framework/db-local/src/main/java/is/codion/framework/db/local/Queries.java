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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.util.List;

final class Queries {

	private static final String WHERE_SPACE_POSTFIX = "WHERE ";
	private static final String NEWLINE = "\n";

	private Queries() {}

	/**
	 * @param tableName the table name
	 * @param columnDefinitions the column definitions used to insert the given entity type
	 * @return a query for inserting
	 */
	static String insertQuery(String tableName, List<ColumnDefinition<?>> columnDefinitions) {
		StringBuilder queryBuilder = new StringBuilder("INSERT ").append("INTO ").append(tableName).append("(");
		StringBuilder columnValues = new StringBuilder(")").append(NEWLINE).append("VALUES(");
		for (int i = 0; i < columnDefinitions.size(); i++) {
			queryBuilder.append(columnDefinitions.get(i).name());
			columnValues.append("?");
			if (i < columnDefinitions.size() - 1) {
				queryBuilder.append(", ");
				columnValues.append(", ");
			}
		}

		return queryBuilder.append(columnValues).append(")").toString();
	}

	/**
	 * @param tableName the table name
	 * @param columnDefinitions the column definitions being updated
	 * @param conditionString the condition string, without the WHERE keyword
	 * @return a query for updating
	 */
	static String updateQuery(String tableName, List<ColumnDefinition<?>> columnDefinitions,
														String conditionString) {
		StringBuilder queryBuilder = new StringBuilder("UPDATE ").append(tableName).append(NEWLINE).append("SET ");
		for (int i = 0; i < columnDefinitions.size(); i++) {
			queryBuilder.append(columnDefinitions.get(i).name()).append(" = ?");
			if (i < columnDefinitions.size() - 1) {
				queryBuilder.append(", ");
			}
		}

		return queryBuilder.append(NEWLINE).append(WHERE_SPACE_POSTFIX).append(conditionString).toString();
	}

	/**
	 * @param tableName the table name
	 * @param conditionString the condition string
	 * @return a query for deleting the entities specified by the given condition
	 */
	static String deleteQuery(String tableName, String conditionString) {
		return "DELETE FROM " + tableName + (conditionString.isEmpty() ? "" : NEWLINE + WHERE_SPACE_POSTFIX + conditionString);
	}
}
