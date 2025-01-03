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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import java.util.function.Consumer;

final class AddListSelectionListener implements Consumer<ListSelectionListener> {

	private final JList<?> list;

	AddListSelectionListener(JList<?> list) {
		this.list = list;
	}

	@Override
	public void accept(ListSelectionListener listener) {
		list.addListSelectionListener(listener);
	}
}
