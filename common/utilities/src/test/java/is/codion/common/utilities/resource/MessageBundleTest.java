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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.resource;

import org.junit.jupiter.api.Test;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MessageBundleTest {

	private static final String TEST_ONE = "test_one";
	private static final String TEST_TWO = "test_two";

	private final ResourceBundle resourceBundle = getBundle(MessageBundleTest.class.getName());
	private final MessageBundle messageBundle = messageBundle(MessageBundleTest.class, resourceBundle);

	@Test
	void getString() {
		assertEquals("Test one", resourceBundle.getString(TEST_ONE));
		assertEquals("Test two", resourceBundle.getString(TEST_TWO));
		assertEquals("Test one override", messageBundle.getString(TEST_ONE));
		assertEquals("Test two override", messageBundle.getString(TEST_TWO));
	}

	@Test
	void missingResource() {
		assertThrows(MissingResourceException.class, () -> resourceBundle.getString("missing_resource"));
	}
}
