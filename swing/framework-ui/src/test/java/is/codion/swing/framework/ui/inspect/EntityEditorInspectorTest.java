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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.inspect;

import is.codion.swing.common.ui.inspect.UiInspector;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntityEditorInspectorTest {

	@Test
	void locatedViaServiceLoader() {
		assertTrue(UiInspector.instances().stream().anyMatch(EntityEditorInspector.class::isInstance));
		assertTrue(UiInspector.instances().stream().anyMatch(EntityTableModelInspector.class::isInstance));
	}

	@Test
	void notApplicableWithoutPanel() {
		assertFalse(new EntityEditorInspector().state(new JTextField()).isPresent());
		assertFalse(new EntityTableModelInspector().state(new JTextField()).isPresent());
	}
}
