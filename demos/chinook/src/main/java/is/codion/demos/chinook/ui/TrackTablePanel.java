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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.demos.chinook.ui.DurationComponentValue.DurationPanel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.EntityComponentFactory;

import javax.swing.JSpinner;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.Text.rightPad;
import static is.codion.demos.chinook.ui.DurationComponentValue.minutes;
import static is.codion.demos.chinook.ui.DurationComponentValue.seconds;
import static is.codion.swing.common.ui.component.Components.bigDecimalField;
import static is.codion.swing.common.ui.component.table.FilterTableCellEditor.filterTableCellEditor;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.ResourceBundle.getBundle;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;

public final class TrackTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = getBundle(TrackTablePanel.class.getName());

	static final Map<Integer, String> RATINGS = rangeClosed(1, 10)
					.mapToObj(ranking -> rightPad("", ranking, '*'))
					.collect(toMap(String::length, identity()));

	public TrackTablePanel(TrackTableModel tableModel) {
		super(tableModel, config -> config
						// Custom component for editing track ratings
						.editComponentFactory(Track.RATING, new RatingComponentFactory())
						// Custom component for editing track durations
						.editComponentFactory(Track.MILLISECONDS, new DurationComponentFactory(tableModel))
						// Custom cell renderer for ratings
						.cellRenderer(Track.RATING, ratingRenderer(tableModel))
						// Custom cell renderer for track duration (min:sec)
						.cellRenderer(Track.MILLISECONDS, durationRenderer(tableModel))
						// Custom cell editor for track ratings
						.cellEditor(Track.RATING, ratingEditor(tableModel.entityDefinition()))
						// Custom cell editor for track durations (min:sec:ms)
						.cellEditor(Track.MILLISECONDS, durationEditor())
						.includeLimitMenu(true));
		// Add a custom control to the top of the table popup menu.
		// Start by clearing the popup menu layout
		configurePopupMenu(layout -> layout.clear()
						// add our custom control
						.control(Control.builder()
										.command(this::raisePriceOfSelected)
										.name(BUNDLE.getString("raise_price") + "...")
										.enabled(tableModel().selection().empty().not()))
						// and a separator
						.separator()
						// and add all the default controls
						.defaults());
	}

	private void raisePriceOfSelected() {
		TrackTableModel tableModel = tableModel();
		tableModel.raisePriceOfSelected(getAmountFromUser());
	}

	private BigDecimal getAmountFromUser() {
		ComponentValue<BigDecimal, NumberField<BigDecimal>> amountValue =
						bigDecimalField()
										.nullable(false)
										.minimumValue(0)
										.buildValue();

		return Dialogs.inputDialog(amountValue)
						.owner(this)
						.title(BUNDLE.getString("amount"))
						.validator(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
						.show();
	}

	private static FilterTableCellRenderer<Integer> durationRenderer(SwingEntityTableModel tableModel) {
		return EntityTableCellRenderer.builder(Track.MILLISECONDS, tableModel)
						.string(milliseconds -> minutes(milliseconds) + " min " + seconds(milliseconds) + " sec")
						.toolTipData(true)
						.build();
	}

	private static FilterTableCellEditor<Integer> durationEditor() {
		return filterTableCellEditor(() -> new DurationComponentValue(true));
	}

	private static FilterTableCellRenderer<Integer> ratingRenderer(SwingEntityTableModel tableModel) {
		return EntityTableCellRenderer.builder(Track.RATING, tableModel)
						.string(RATINGS::get)
						.toolTipData(true)
						.build();
	}

	private static FilterTableCellEditor<Integer> ratingEditor(EntityDefinition entityDefinition) {
		return filterTableCellEditor(() -> ratingSpinner(entityDefinition).buildValue());
	}

	private static NumberSpinnerBuilder<Integer> ratingSpinner(EntityDefinition entityDefinition) {
		return entityComponents(entityDefinition).integerSpinner(Track.RATING);
	}

	private static final class RatingComponentFactory
					implements EntityComponentFactory<Integer, JSpinner> {

		@Override
		public ComponentValue<Integer, JSpinner> componentValue(SwingEntityEditModel editModel,
																														Integer value) {
			return ratingSpinner(editModel.entityDefinition())
							.value(value)
							.buildValue();
		}
	}

	private static final class DurationComponentFactory
					implements EntityComponentFactory<Integer, DurationPanel> {

		private final String caption;

		private DurationComponentFactory(TrackTableModel tableModel) {
			this.caption = tableModel.entityDefinition().attributes().definition(Track.MILLISECONDS).caption();
		}

		@Override
		public Optional<String> caption() {
			return Optional.of(caption);
		}

		@Override
		public ComponentValue<Integer, DurationPanel> componentValue(SwingEntityEditModel editModel, Integer value) {
			DurationComponentValue durationValue = new DurationComponentValue();
			durationValue.set(value);

			return durationValue;
		}
	}
}
