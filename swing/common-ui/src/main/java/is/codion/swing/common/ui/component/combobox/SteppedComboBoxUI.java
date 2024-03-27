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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Dimension;
import java.awt.Rectangle;

import static java.util.Objects.requireNonNull;

/**
 * A JComboBox UI which automatically sets the popup width according to the largest value in the combo box.
 * Slightly modified, automatic popup size according to getDisplaySize().
 * @author Nobuo Tamemasa (originally)
 */
final class SteppedComboBoxUI extends MetalComboBoxUI {

	private int popupWidth = 0;

	SteppedComboBoxUI(JComboBox<?> comboBox, int popupWidth) {
		requireNonNull(comboBox).setUI(this);
		this.popupWidth = popupWidth;
	}

	@Override
	protected ComboPopup createPopup() {
		return new SteppedComboBoxPopup(comboBox, this);
	}

	private static final class SteppedComboBoxPopup extends BasicComboPopup {

		private final SteppedComboBoxUI comboBoxUI;

		private SteppedComboBoxPopup(JComboBox comboBox, SteppedComboBoxUI comboBoxUI) {
			super(comboBox);
			this.comboBoxUI = comboBoxUI;
			getAccessibleContext().setAccessibleParent(comboBox);
		}

		@Override
		public void setVisible(boolean visible) {
			if (visible) {
				Dimension popupSize = popupSize(comboBox);
				popupSize.setSize(popupSize.width, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
				Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height, popupSize.width, popupSize.height);
				scroller.setMaximumSize(popupBounds.getSize());
				scroller.setPreferredSize(popupBounds.getSize());
				scroller.setMinimumSize(popupBounds.getSize());
				getList().invalidate();
				int selectedIndex = comboBox.getSelectedIndex();
				if (selectedIndex == -1) {
					getList().clearSelection();
				}
				else {
					getList().setSelectedIndex(selectedIndex);
				}
				getList().ensureIndexIsVisible(getList().getSelectedIndex());
				setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
			}

			super.setVisible(visible);
		}

		private Dimension popupSize(JComboBox<?> comboBox) {
			Dimension displaySize = comboBoxUI.getDisplaySize();
			Dimension size = comboBox.getSize();

			return new Dimension(Math.max(size.width, comboBoxUI.popupWidth <= 0 ? displaySize.width :
							comboBoxUI.popupWidth), size.height);
		}
	}
}
