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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.common;

import org.junit.jupiter.api.Test;

public final class InputControlsTest {

	@Test
	void test() {
		InputControls.basics();
		InputControls.checkBox();
		InputControls.nullableCheckBox();
		InputControls.booleanComboBox();
		InputControls.stringField();
		InputControls.characterField();
		InputControls.textArea();
		InputControls.integerField();
		InputControls.longField();
		InputControls.doubleField();
		InputControls.bigDecimalField();
		InputControls.localTime();
		InputControls.localDate();
		InputControls.localDateTime();
		InputControls.selectionComboBox();
		InputControls.filterComboBoxModel();
		InputControls.comboBoxCompletion();
	}
}
