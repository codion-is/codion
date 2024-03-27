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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.TrackTableModel;
import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.bigDecimalField;

public final class TrackTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(TrackTablePanel.class.getName());

	public TrackTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.editComponentFactory(Track.MILLISECONDS, new MinutesSecondsComponentFactory(false))
						.tableCellEditorFactory(Track.MILLISECONDS, new MinutesSecondsComponentFactory(true))
						.includeLimitMenu(true));
	}

	@Override
	protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
		return super.createPopupMenuControls(additionalPopupMenuControls)
						.addAt(0, Control.builder(this::raisePriceOfSelected)
										.name(BUNDLE.getString("raise_price") + "...")
										.enabled(tableModel().selectionModel().selectionNotEmpty())
										.build())
						.addSeparatorAt(1);
	}

	private void raisePriceOfSelected() throws DatabaseException {
		TrackTableModel tableModel = tableModel();
		tableModel.raisePriceOfSelected(getAmountFromUser());
	}

	private BigDecimal getAmountFromUser() {
		ComponentValue<BigDecimal, NumberField<BigDecimal>> amountValue =
						bigDecimalField()
										.buildValue();

		return Dialogs.inputDialog(amountValue)
						.owner(this)
						.title(BUNDLE.getString("amount"))
						.inputValidator(Objects::nonNull)
						.show();
	}

	private static final class MinutesSecondsComponentFactory
					extends DefaultEntityComponentFactory<Integer, Attribute<Integer>, MinutesSecondsPanel> {

		private final boolean horizontal;

		private MinutesSecondsComponentFactory(boolean horizontal) {
			this.horizontal = horizontal;
		}

		@Override
		public ComponentValue<Integer, MinutesSecondsPanel> componentValue(Attribute<Integer> attribute,
																																			 SwingEntityEditModel editModel,
																																			 Integer initialValue) {
			MinutesSecondsPanelValue minutesSecondsPanelValue = new MinutesSecondsPanelValue(horizontal);
			minutesSecondsPanelValue.set(initialValue);

			return minutesSecondsPanelValue;
		}
	}
}
