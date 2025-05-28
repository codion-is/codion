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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SelectQueryInspectorTest {

	@Test
	public void testInsertFormatting() {
		String sql = "INSERT INTO world.country (code, name, continent, region, surfacearea, indepyear) VALUES ('ARG', 'Argentina', 'South America', 'South America', 2780400, 1816)";

		String formatted = SelectQueryInspector.BasicFormatterImpl.format(sql);

		String expected = "INSERT \n" +
						"INTO\n" +
						"    world.country\n" +
						"    (code, name, continent, region, surfacearea,\n" +
						"\t indepyear) \n" +
						"VALUES\n" +
						"    ('ARG', 'Argentina', 'South America', 'South America', 2780400,\n" +
						"\t 1816)";

		assertEquals(expected, formatted, "INSERT formatting should break lines every 5 items");

		// Verify specific line breaks are present
		assertTrue(formatted.contains("surfacearea,\n"), "Should break after 5th column");
		assertTrue(formatted.contains("2780400,\n"), "Should break after 5th value");
	}

	@Test
	public void testSimpleSelect() {
		String sql = "SELECT code, name FROM world.country WHERE code = 'ARG'";
		String formatted = SelectQueryInspector.BasicFormatterImpl.format(sql);

		// This should work with existing formatter
		assertTrue(formatted.contains("SELECT"), "Should contain SELECT");
		assertTrue(formatted.contains("FROM"), "Should contain FROM");
	}
}