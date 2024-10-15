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
package is.codion.swing.common.ui.component.table;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultFilterTableCellRendererTest {

	@Test
	void horizontalAlignment() {
		assertEquals(BOOLEAN_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder(Boolean.class).build().horizontalAlignment());
		assertEquals(TEMPORAL_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder(LocalDate.class).build().horizontalAlignment());
		assertEquals(NUMERICAL_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder(Double.class).build().horizontalAlignment());
	}
}
