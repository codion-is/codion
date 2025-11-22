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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.demos.chinook.ui.DurationComponentValue.DurationPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.EditComponentFactory;

import javax.swing.JSpinner;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.utilities.Text.rightPad;
import static is.codion.demos.chinook.ui.DurationComponentValue.minutes;
import static is.codion.demos.chinook.ui.DurationComponentValue.seconds;
import static is.codion.swing.common.ui.component.Components.bigDecimalField;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.math.BigDecimal.ZERO;
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
						.editComponentFactory(Track.RATING, new RatingEditComponentFactory())
						// Custom component for editing track durations
						.editComponentFactory(Track.MILLISECONDS, new DurationEditComponentFactory())
						// Custom cell renderer for ratings
						.cellRenderer(Track.RATING, TrackTablePanel::ratingRenderer)
						// Custom cell renderer for track duration (min:sec)
						.cellRenderer(Track.MILLISECONDS, TrackTablePanel::durationRenderer)
						// Custom cell editor for track ratings
						.cellEditor(Track.RATING, ratingEditor(tableModel.entityDefinition()))
						// Custom cell editor for track durations (min:sec:ms)
						.cellEditor(Track.MILLISECONDS, durationEditor())
						// Start editing when the INSERT key is pressed
						.table(table -> table
										.autoStartsEdit(false)
										.startEditing(keyStroke(VK_INSERT)))
						.includeLimitMenu(true));
		// Add a custom control to the top of the table popup menu.
		// Start by clearing the popup menu layout
		configurePopupMenu(layout -> layout.clear()
						// add our custom control
						.control(Control.builder()
										.command(this::raisePriceOfSelected)
										.caption(BUNDLE.getString("raise_price") + "...")
										.enabled(tableModel().selection().empty().not()))
						// and a separator
						.separator()
						// and add all the default controls
						.defaults());
	}

	private void raisePriceOfSelected() {
		TrackTableModel tableModel = (TrackTableModel) tableModel();
		tableModel.raisePriceOfSelected(getAmountFromUser());
	}

	private BigDecimal getAmountFromUser() {
		return Dialogs.input()
						.component(bigDecimalField()
										.nullable(false)
										.minimum(0)
										// Silently prevent invalid input
										.silentValidation(true))
						.owner(this)
						.title(BUNDLE.getString("amount"))
						// The field is not nullable so the amount is never null
						.validator(amount -> amount.compareTo(ZERO) > 0)
						.show();
	}

	private static void durationRenderer(FilterTableCellRenderer.Builder<Entity, Attribute<?>, Integer> renderer) {
		renderer.formatter(milliseconds -> minutes(milliseconds) + " min " + seconds(milliseconds) + " sec")
						.toolTip(milliseconds -> milliseconds + " ms");
	}

	private static FilterTableCellEditor<Integer> durationEditor() {
		return FilterTableCellEditor.builder()
						.component(() -> new DurationComponentValue(true))
						.build();
	}

	private static void ratingRenderer(FilterTableCellRenderer.Builder<Entity, Attribute<?>, Integer> renderer) {
		renderer.formatter(RATINGS::get)
						.toolTip(Object::toString);
	}

	private static FilterTableCellEditor<Integer> ratingEditor(EntityDefinition entityDefinition) {
		return FilterTableCellEditor.builder()
						.component(() -> ratingSpinner(entityDefinition).buildValue())
						.build();
	}

	private static NumberSpinnerBuilder<Integer> ratingSpinner(EntityDefinition entityDefinition) {
		return entityComponents(entityDefinition).integerSpinner(Track.RATING);
	}

	private static final class RatingEditComponentFactory
					implements EditComponentFactory<JSpinner, Integer> {

		@Override
		public ComponentValue<JSpinner, Integer> component(SwingEntityEditModel editModel) {
			return ratingSpinner(editModel.entityDefinition()).buildValue();
		}
	}

	private static final class DurationEditComponentFactory
					implements EditComponentFactory<DurationPanel, Integer> {

		@Override
		public Optional<String> caption(AttributeDefinition<Integer> attributeDefinition) {
			return Optional.empty();
		}

		@Override
		public ComponentValue<DurationPanel, Integer> component(SwingEntityEditModel editModel) {
			return new DurationComponentValue(false);
		}
	}
}
