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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainToStringTest {

	@Test
	void underscoreToCamelCase() {
		assertEquals("", DomainToString.underscoreToCamelCase(""));
		assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("noOfSpeakers"));
		assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("no_of_speakers"));
		assertEquals("noOfSpeakers", DomainToString.underscoreToCamelCase("No_OF_speakeRS"));
		assertEquals("helloWorld", DomainToString.underscoreToCamelCase("hello_World"));
		assertEquals("", DomainToString.underscoreToCamelCase("_"));
		assertEquals("aB", DomainToString.underscoreToCamelCase("a_b"));
		assertEquals("aB", DomainToString.underscoreToCamelCase("a_b_"));
		assertEquals("aBC", DomainToString.underscoreToCamelCase("a_b_c"));
		assertEquals("aBaC", DomainToString.underscoreToCamelCase("a_ba_c"));
		assertEquals("a", DomainToString.underscoreToCamelCase("a__"));
		assertEquals("a", DomainToString.underscoreToCamelCase("__a"));
		assertEquals("a", DomainToString.underscoreToCamelCase("__A"));
	}
}
