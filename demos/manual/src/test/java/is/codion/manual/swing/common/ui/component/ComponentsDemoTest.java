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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui.component;

import org.junit.jupiter.api.Test;

public final class ComponentsDemoTest {

	@Test
	void test() {
		ComponentsDemo.basics();
		ComponentsDemo.checkBox();
		ComponentsDemo.nullableCheckBox();
		ComponentsDemo.booleanComboBox();
		ComponentsDemo.stringField();
		ComponentsDemo.characterField();
		ComponentsDemo.textArea();
		ComponentsDemo.integerField();
		ComponentsDemo.longField();
		ComponentsDemo.bigIntegerField();
		ComponentsDemo.doubleField();
		ComponentsDemo.bigDecimalField();
		ComponentsDemo.localTime();
		ComponentsDemo.localDate();
		ComponentsDemo.localDateTime();
		ComponentsDemo.selectionComboBox();
		ComponentsDemo.filterComboBoxModel();
		ComponentsDemo.comboBoxCompletion();
		ComponentsDemo.customTextFields();
	}
}
