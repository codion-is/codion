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

import is.codion.common.state.ObservableState;
import is.codion.framework.model.EntityEditModel.EditorValue;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

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

final class DurationComponentValue extends AbstractComponentValue<Integer, DurationComponentValue.DurationPanel> {

	DurationComponentValue() {
		this(new DurationPanel(false, null));
	}

	DurationComponentValue(EditorValue<Integer> millisecondsValue) {
		this(new DurationPanel(false, millisecondsValue.valid()));
		link(millisecondsValue);
	}

	DurationComponentValue(boolean cellEditor) {
		this(new DurationPanel(cellEditor, null));
	}

	DurationComponentValue(DurationPanel panel) {
		super(panel);
		component().minutesField.observable().addListener(this::notifyListeners);
		component().secondsField.observable().addListener(this::notifyListeners);
		component().millisecondsField.observable().addListener(this::notifyListeners);
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

		final NumberField<Integer> minutesField;
		final NumberField<Integer> secondsField;
		final NumberField<Integer> millisecondsField;

		private DurationPanel(boolean cellEditor, ObservableState valid) {
			super(borderLayout());
			minutesField = integerField()
							.transferFocusOnEnter(true)
							.selectAllOnFocusGained(true)
							.validIndicator(valid)
							.columns(2)
							.build();
			secondsField = integerField()
							.valueRange(0, 59)
							.transferFocusOnEnter(true)
							.selectAllOnFocusGained(true)
							.silentValidation(true)
							.validIndicator(valid)
							.columns(2)
							.build();
			millisecondsField = integerField()
							.valueRange(0, 999)
							.transferFocusOnEnter(!cellEditor)
							.selectAllOnFocusGained(true)
							.silentValidation(true)
							.validIndicator(valid)
							.columns(3)
							.build();
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

		private void initializeCellEditor() {
			add(flexibleGridLayoutPanel(1, 3)
							.add(minutesField)
							.add(secondsField)
							.add(millisecondsField)
							.build(), BorderLayout.CENTER);
		}

		private void initializeInputPanel() {
			add(borderLayoutPanel()
							.northComponent(gridLayoutPanel(1, 3)
											.add(new JLabel(BUNDLE.getString("min")))
											.add(new JLabel(BUNDLE.getString("sec")))
											.add(new JLabel(BUNDLE.getString("ms")))
											.build())
							.centerComponent(gridLayoutPanel(1, 2)
											.add(minutesField)
											.add(secondsField)
											.add(millisecondsField)
											.build())
							.build());
		}
	}
}
