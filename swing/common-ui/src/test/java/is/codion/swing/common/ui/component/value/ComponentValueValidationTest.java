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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.value;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests component value validation integration and error handling.
 * Note: Validation happens at multiple levels - during component creation,
 * document updates, and value setting. These tests focus on the value-level validation.
 */
public final class ComponentValueValidationTest {

	@Test
	void validationPreventsSetting() {
		Value.Validator<Integer> rangeValidator = value -> {
			if (value != null && (value < 0 || value > 100)) {
				throw new IllegalArgumentException("Value must be between 0 and 100");
			}
		};

		// For number fields, we need to use value() to set initial value before validation
		ComponentValue<NumberField<Integer>, Integer> numberValue = Components.integerField()
						.value(50)
						.validator(rangeValidator)
						.buildValue();

		// Initial value is set
		assertEquals(50, numberValue.get());
		assertEquals("50", numberValue.component().getText());

		// Valid values work through the component value
		numberValue.set(75);
		assertEquals(75, numberValue.get());
		assertEquals("75", numberValue.component().getText());

		// Invalid values throw
		assertThrows(IllegalArgumentException.class, () -> numberValue.set(150));
		// Value unchanged after failed validation
		assertEquals(75, numberValue.get());
		assertEquals("75", numberValue.component().getText());

		// For NumberField, validation happens at document level
		// so we can't even type invalid values
		numberValue.component().setText("25");
		assertEquals(25, numberValue.get());
	}

	@Test
	void validationWithLinkedValues() {
		Value<Integer> minValue = Value.nullable(0);
		Value<Integer> maxValue = Value.nullable(100);

		// Max must be greater than min
		Value.Validator<Integer> maxValidator = value -> {
			Integer min = minValue.get();
			if (value != null && min != null && value <= min) {
				throw new IllegalArgumentException("Max must be greater than min");
			}
		};

		ComponentValue<NumberField<Integer>, Integer> minField = Components.integerField()
						.link(minValue)
						.buildValue();

		ComponentValue<NumberField<Integer>, Integer> maxField = Components.integerField()
						.link(maxValue)
						.validator(maxValidator)
						.buildValue();

		// Set valid values
		minField.set(10);
		maxField.set(20);
		assertEquals(10, minValue.get());
		assertEquals(20, maxValue.get());

		// Try to set max <= min
		assertThrows(IllegalArgumentException.class, () -> maxField.set(5));
		assertEquals(20, maxValue.get()); // Unchanged

		// Change min, max validation still applies
		minField.set(30);
		assertThrows(IllegalArgumentException.class, () -> maxField.set(25));
		assertEquals(20, maxValue.get()); // Still unchanged
	}

	@Test
	void validationErrorHandling() {
		AtomicInteger successCount = new AtomicInteger();
		Value.Validator<String> emailValidator = value -> {
			if (value != null && !value.isEmpty() && !value.contains("@")) {
				throw new IllegalArgumentException("Invalid email");
			}
		};

		ComponentValue<JTextField, String> emailField = Components.stringField()
						.validator(emailValidator)
						.buildValue();

		// Track successful and failed validations
		emailField.addConsumer(value -> successCount.incrementAndGet());

		// Valid email
		emailField.set("test@example.com");
		assertEquals(1, successCount.get());
		assertEquals("test@example.com", emailField.get());

		// Invalid email throws
		assertThrows(IllegalArgumentException.class, () -> emailField.set("invalid"));
		assertEquals(1, successCount.get()); // No additional success
		assertEquals("test@example.com", emailField.get()); // Value unchanged

		// Empty is valid (but string fields treat empty as null)
		emailField.set("");
		assertEquals(2, successCount.get());
		// Empty string is treated as null in string fields
		assertNull(emailField.get());
		assertEquals("", emailField.component().getText());

		// Null is valid (but since value is already null from empty string, no change event)
		emailField.set(null);
		assertEquals(2, successCount.get()); // No change event since already null
		assertNull(emailField.get());
		assertEquals("", emailField.component().getText());
	}

	@Test
	void nullValueValidation() {
		// For integer fields, we need to test with a non-null value first
		// since the validator runs during component creation
		ComponentValue<NumberField<Integer>, Integer> field = Components.integerField()
						.value(50)
						.range(0, 100)
						.buildValue();

		// Add validator after creation to avoid initial validation
		Value.Validator<Integer> notNullValidator = value -> {
			if (value == null) {
				throw new IllegalArgumentException("Value cannot be null");
			}
		};

		// Now add the validator
		field.addValidator(notNullValidator);

		// Initial value is set
		assertEquals(50, field.get());

		// Can't set null
		assertThrows(IllegalArgumentException.class, () -> field.set(null));
		assertEquals(50, field.get()); // Value unchanged

		// Can set other valid values
		field.set(75);
		assertEquals(75, field.get());
	}

	@Test
	void validatorExceptionTypes() {
		// Test that different exception types are properly propagated
		ComponentValue<JTextField, String> field = Components.stringField()
						.validator(value -> {
							if ("runtime".equals(value)) {
								throw new RuntimeException("Runtime error");
							}
							if ("illegal".equals(value)) {
								throw new IllegalArgumentException("Illegal argument");
							}
							if ("state".equals(value)) {
								throw new IllegalStateException("Illegal state");
							}
						})
						.buildValue();

		// Different exception types are preserved
		assertThrows(RuntimeException.class, () -> field.set("runtime"));
		assertThrows(IllegalArgumentException.class, () -> field.set("illegal"));
		assertThrows(IllegalStateException.class, () -> field.set("state"));

		// Valid value works
		assertDoesNotThrow(() -> field.set("valid"));
		assertEquals("valid", field.get());
	}

	@Test
	void rejectedChangeRestored() throws Exception {
		List<Throwable> rethrown = new ArrayList<>();
		Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> rethrown.add(exception));
		try {
			State state = State.state();
			state.addValidator(value -> {
				if (value) {
					throw new IllegalArgumentException("rejected");
				}
			});
			AtomicInteger notified = new AtomicInteger();
			state.addListener(notified::incrementAndGet);
			ComponentValue<JToggleButton, Boolean> toggleButtonValue = Components.toggleButton()
							.link(state)
							.buildValue();
			AtomicInteger componentValueNotified = new AtomicInteger();
			toggleButtonValue.addListener(componentValueNotified::incrementAndGet);
			JToggleButton toggleButton = toggleButtonValue.component();
			List<Integer> itemEvents = new ArrayList<>();
			toggleButton.addItemListener(event -> itemEvents.add(event.getStateChange()));
			JCheckBox checkBox = Components.checkBox()
							.link(state)
							.build();
			Value<String> item = Value.nullable("a");
			item.addValidator(value -> {
				if ("x".equals(value)) {
					throw new IllegalArgumentException("rejected");
				}
			});
			JComboBox<String> comboBox = Components.comboBox()
							.model(new DefaultComboBoxModel<>(new String[] {"a", "b", "x"}))
							.link(item)
							.build();
			Value<Integer> number = Value.nullable(4);
			number.addValidator(value -> {
				if (value != null && value == 5) {
					throw new IllegalArgumentException("rejected");
				}
			});
			JSpinner spinner = Components.integerSpinner()
							.link(number)
							.build();
			SwingUtilities.invokeAndWait(() -> {
				toggleButton.doClick();
				// not notified, the button pressed no longer
				assertFalse(state.is());
				assertFalse(toggleButton.getModel().isPressed());
				checkBox.doClick();
				comboBox.setSelectedItem("x");
				spinner.setValue(5);
			});
			// restored once the event making the change has completed
			SwingUtilities.invokeAndWait(() -> {});
			assertFalse(toggleButton.isSelected());
			assertFalse(checkBox.isSelected());
			assertFalse(state.is());
			assertEquals(0, notified.get());
			assertEquals(0, componentValueNotified.get());
			// in order, the change and the restore
			assertEquals(asList(ItemEvent.SELECTED, ItemEvent.DESELECTED), itemEvents);
			assertEquals("a", comboBox.getSelectedItem());
			assertEquals("a", item.get());
			assertEquals(4, spinner.getValue());
			assertEquals(4, number.get());
			// rethrown on the Event Dispatch Thread
			assertEquals(4, rethrown.size());
			rethrown.forEach(exception -> assertInstanceOf(IllegalArgumentException.class, exception));
			// a valid change after a rejected one
			SwingUtilities.invokeAndWait(() -> comboBox.setSelectedItem("b"));
			assertEquals("b", item.get());
		}
		finally {
			Thread.setDefaultUncaughtExceptionHandler(handler);
		}
	}

	@Test
	void rejectedChangeOwnValidator() throws Exception {
		List<Throwable> rethrown = new ArrayList<>();
		Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> rethrown.add(exception));
		try {
			// a change made in an unlinked component, validated by its own validators
			ComponentValue<JCheckBox, Boolean> checkBoxValue = Components.checkBox()
							.validator(value -> {
								if (value) {
									throw new IllegalArgumentException("rejected");
								}
							})
							.buildValue();
			// a component value of its own, never set, its initial value restored
			SpinnerValue spinnerValue = new SpinnerValue(new JSpinner(new SpinnerNumberModel(4, 0, 10, 1)));
			spinnerValue.addValidator(value -> {
				if (value != null && value == 5) {
					throw new IllegalArgumentException("rejected");
				}
			});
			SwingUtilities.invokeAndWait(() -> {
				checkBoxValue.component().doClick();
				spinnerValue.component().setValue(5);
			});
			SwingUtilities.invokeAndWait(() -> {});
			assertFalse(checkBoxValue.component().isSelected());
			assertFalse(checkBoxValue.getOrThrow());
			assertEquals(4, spinnerValue.component().getValue());
			assertEquals(4, spinnerValue.get());
			assertEquals(2, rethrown.size());
		}
		finally {
			Thread.setDefaultUncaughtExceptionHandler(handler);
		}
	}

	@Test
	void textChangesNotValidatedOnNotify() throws Exception {
		// edited as text, passing through intermediate values, an edit in progress is not restored
		Value<LocalDate> date = Value.nullable(LocalDate.of(2026, 10, 8));
		date.addValidator(value -> {
			if (value == null) {
				throw new IllegalArgumentException("required");
			}
		});
		ComponentValue<TemporalField<LocalDate>, LocalDate> dateValue = Components.localDateField()
						.dateTimePattern("dd.MM.yyyy")
						.link(date)
						.buildValue();
		TemporalField<LocalDate> field = dateValue.component();
		SwingUtilities.invokeAndWait(() -> {
			// deleting the last digit, the field holding no date, which the linked value rejects
			field.select(field.getText().length() - 1, field.getText().length());
			assertThrows(IllegalArgumentException.class, () -> field.replaceSelection(""));
		});
		SwingUtilities.invokeAndWait(() -> {});
		// not restored
		assertNull(field.get());
		SwingUtilities.invokeAndWait(() -> field.replaceSelection("7"));
		assertEquals(LocalDate.of(2027, 10, 8), date.get());
	}

	private static final class SpinnerValue extends AbstractComponentValue<JSpinner, Integer> {

		private SpinnerValue(JSpinner spinner) {
			super(spinner);
			spinner.addChangeListener(event -> notifyObserver());
		}

		@Override
		protected Integer getComponentValue() {
			return (Integer) component().getValue();
		}

		@Override
		protected void setComponentValue(@Nullable Integer value) {
			component().setValue(value == null ? 0 : value);
		}
	}
}
