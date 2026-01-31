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
package is.codion.demos.chinook.ui;

import is.codion.common.reactive.state.ObservableState;
import is.codion.demos.chinook.ui.DurationPanelBuilder.DurationPanel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicator;
import is.codion.swing.common.ui.component.indicator.ValidIndicator;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.util.ResourceBundle.getBundle;

final class DurationPanelBuilder extends AbstractComponentValueBuilder<DurationPanel, Integer, DurationPanelBuilder> {

	private boolean cellEditor = false;

	DurationPanelBuilder cellEditor(boolean cellEditor) {
		this.cellEditor = cellEditor;
		return this;
	}

	@Override
	protected DurationPanel createComponent() {
		return new DurationPanel(cellEditor);
	}

	@Override
	protected ComponentValue<DurationPanel, Integer> createValue(DurationPanel component) {
		return new DurationComponentValue(component);
	}

	@Override
	protected void enable(TransferFocusOnEnter transferFocusOnEnter, DurationPanel component) {
		transferFocusOnEnter.enable(component.minutesField);
		transferFocusOnEnter.enable(component.secondsField);
		// If the component is being used as a table cell editor, the focus jumps outside
		// the table on ENTER on the last field instead of just stopping the table editing
		if (!cellEditor) {
			transferFocusOnEnter.enable(component.millisecondsField);
		}
	}

	@Override
	protected void enable(ValidIndicator validIndicator, DurationPanel component, ObservableState valid) {
		validIndicator.enable(component.minutesField, valid);
		validIndicator.enable(component.secondsField, valid);
		validIndicator.enable(component.millisecondsField, valid);
	}

	@Override
	protected void enable(ModifiedIndicator modifiedIndicator, DurationPanel component, ObservableState modified) {
		modifiedIndicator.enable(component.minutesField, modified);
		modifiedIndicator.enable(component.secondsField, modified);
		modifiedIndicator.enable(component.millisecondsField, modified);
	}

	static Integer minutes(Integer milliseconds) {
		if (milliseconds == null) {
			return null;
		}

		return (int) ofMillis(milliseconds).toMinutes();
	}

	static Integer seconds(Integer milliseconds) {
		if (milliseconds == null) {
			return null;
		}

		return (int) ofMillis(milliseconds)
						.minusMinutes(ofMillis(milliseconds).toMinutes())
						.getSeconds();
	}

	static Integer milliseconds(Integer milliseconds) {
		if (milliseconds == null) {
			return null;
		}

		return (int) ofMillis(milliseconds)
						.minusSeconds(ofMillis(milliseconds).toSeconds())
						.toMillis();
	}

	static final class DurationPanel extends JPanel {

		private static final ResourceBundle BUNDLE = getBundle(DurationPanel.class.getName());

		private final JLabel minLabel = Components.label(BUNDLE.getString("min")).build();
		private final JLabel secLabel = Components.label(BUNDLE.getString("sec")).build();
		private final JLabel msLabel = Components.label(BUNDLE.getString("ms")).build();

		private final NumberField<Integer> minutesField = integerField()
						.selectAllOnFocusGained(true)
						.label(minLabel)
						.columns(2)
						.build();
		private final NumberField<Integer> secondsField = integerField()
						.range(0, 59)
						.selectAllOnFocusGained(true)
						.silentValidation(true)
						.label(secLabel)
						.columns(2)
						.build();
		private final NumberField<Integer> millisecondsField = integerField()
						.range(0, 999)
						.selectAllOnFocusGained(true)
						.silentValidation(true)
						.label(msLabel)
						.columns(3)
						.build();

		private DurationPanel(boolean cellEditor) {
			super(borderLayout());
			if (cellEditor) {
				initializeCellEditor();
			}
			else {
				initializeInputPanel();
			}
			addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					minutesField.requestFocusInWindow();
				}
			});
		}

		static void configureCellEditor(FilterTableCellEditor<DurationPanel, Integer> cellEditor) {
			cellEditor.componentValue().component().millisecondsField.addActionListener(e -> cellEditor.stopCellEditing());
		}

		private void initializeCellEditor() {
			add(flexibleGridLayoutPanel(1, 0)
							.add(minutesField)
							.add(secondsField)
							.add(millisecondsField)
							.build(), BorderLayout.CENTER);
		}

		private void initializeInputPanel() {
			add(borderLayoutPanel()
							.north(gridLayoutPanel(1, 0)
											.add(minLabel)
											.add(secLabel)
											.add(msLabel))
							.center(gridLayoutPanel(1, 0)
											.add(minutesField)
											.add(secondsField)
											.add(millisecondsField))
							.build());
		}
	}

	private static final class DurationComponentValue extends AbstractComponentValue<DurationPanel, Integer> {

		private DurationComponentValue(DurationPanel panel) {
			super(panel);
			component().minutesField.observable().addListener(this::notifyObserver);
			component().secondsField.observable().addListener(this::notifyObserver);
			component().millisecondsField.observable().addListener(this::notifyObserver);
		}

		@Override
		protected Integer getComponentValue() {
			Integer minutes = component().minutesField.get();
			Integer seconds = component().secondsField.get();
			Integer milliseconds = component().millisecondsField.get();
			if (minutes == null && seconds == null && milliseconds == null) {
				return null;
			}

			return (int) ofMinutes(component().minutesField.optional().orElse(0))
							.plusSeconds(component().secondsField.optional().orElse(0))
							.plusMillis(component().millisecondsField.optional().orElse(0))
							.toMillis();
		}

		@Override
		protected void setComponentValue(Integer milliseconds) {
			component().minutesField.set(minutes(milliseconds));
			component().secondsField.set(seconds(milliseconds));
			component().millisecondsField.set(milliseconds(milliseconds));
		}
	}
}
