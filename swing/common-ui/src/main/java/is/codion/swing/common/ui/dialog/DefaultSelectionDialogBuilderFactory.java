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
package is.codion.swing.common.ui.dialog;

import java.util.Collection;

final class DefaultSelectionDialogBuilderFactory implements SelectionDialogBuilderFactory {

	static final SelectionDialogBuilderFactory INSTANCE = new DefaultSelectionDialogBuilderFactory();

	private DefaultSelectionDialogBuilderFactory() {}

	@Override
	public FileSelectionDialogBuilder files() {
		return new DefaultFileSelectionDialogBuilder();
	}

	@Override
	public <T> ListSelectionDialogBuilder<T> list(Collection<T> values) {
		return new DefaultListSelectionDialogBuilder<>(values);
	}

	@Override
	public <T> ComboBoxSelectionDialogBuilder<T> comboBox(Collection<T> values) {
		return new DefaultComboBoxSelectionDialogBuilder<>(values);
	}

	@Override
	public LookAndFeelSelectionDialogBuilder lookAndFeel() {
		return new DefaultLookAndFeelSelectionDialogBuilder();
	}

	@Override
	public ScalingSelectionDialogBuilder scaling() {
		return new DefaultScalingSelectionDialogBuilder();
	}
}
