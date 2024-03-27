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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.common.demo;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import static is.codion.common.item.Item.item;
import static is.codion.common.value.Value.value;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.itemComboBoxModel;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.util.Arrays.asList;

/*
// tag::demoModelImport[]
import static is.codion.common.item.Item.item;
import static is.codion.common.value.Value.value;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.itemComboBoxModel;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.util.Arrays.asList;
// end::demoModelImport[]
*/
// tag::demoModel[]

public final class ApplicationModel {

	private final Value<String> shortStringValue = value();
	private final Value<String> longStringValue = value();
	private final Value<String> textValue = value();
	private final Value<LocalDate> localDateValue = value();
	private final Value<LocalDateTime> localDateTimeValue = value();
	private final Value<String> formattedStringValue = value();
	private final Value<Integer> integerValue = value();
	private final Value<Double> doubleValue = value();
	private final Value<Boolean> booleanValue = value();
	private final Value<Boolean> booleanSelectionValue = value();
	private final Value<Integer> integerItemValue = value();
	private final Value<String> stringSelectionValue = value();
	private final Value<Integer> integerSlideValue = value();
	private final Value<Integer> integerSpinValue = value();
	private final Value<Integer> integerSelectionValue = value();
	private final Value<String> itemSpinValue = value();
	private final ValueSet<String> stringListValue = valueSet();
	private final Value<String> messageValue = value();

	private final Collection<Value<?>> values = asList(
					shortStringValue,
					longStringValue,
					textValue,
					localDateValue,
					localDateTimeValue,
					formattedStringValue,
					integerValue,
					doubleValue,
					booleanValue,
					booleanSelectionValue,
					integerItemValue,
					stringSelectionValue,
					integerSlideValue,
					integerSpinValue,
					integerSelectionValue,
					itemSpinValue,
					stringListValue
	);

	public ApplicationModel() {
		setDefaultUncaughtExceptionHandler(this::exceptionHandler);
		values.forEach(value -> value.addDataListener(this::setMessage));
	}

	public void clear() {
		values.forEach(value -> value.set(null));
	}

	public Value<String> shortStringValue() {
		return shortStringValue;
	}

	public Value<String> longStringValue() {
		return longStringValue;
	}

	public Value<String> textValue() {
		return textValue;
	}

	public Value<LocalDate> localDateValue() {
		return localDateValue;
	}

	public Value<LocalDateTime> localDateTimeValue() {
		return localDateTimeValue;
	}

	public Value<Integer> integerValue() {
		return integerValue;
	}

	public Value<Double> doubleValue() {
		return doubleValue;
	}

	public Value<String> formattedStringValue() {
		return formattedStringValue;
	}

	public Value<Boolean> booleanValue() {
		return booleanValue;
	}

	public Value<Boolean> booleanSelectionValue() {
		return booleanSelectionValue;
	}

	public Value<Integer> integerItemValue() {
		return integerItemValue;
	}

	public Value<Integer> integerSlideValue() {
		return integerSlideValue;
	}

	public Value<Integer> integerSpinValue() {
		return integerSpinValue;
	}

	public Value<Integer> integerSelectionValue() {
		return integerSelectionValue;
	}

	public Value<String> itemSpinnerValue() {
		return itemSpinValue;
	}

	public Value<String> stringSelectionValue() {
		return stringSelectionValue;
	}

	public ValueSet<String> stringListValueSet() {
		return stringListValue;
	}

	public ValueObserver<String> message() {
		return messageValue.observer();
	}

	public ComboBoxModel<String> createStringComboBoxModel() {
		return new DefaultComboBoxModel<>(new String[] {"Hello", "Everybody", "How", "Are", "You"});
	}

	public ItemComboBoxModel<Integer> createIntegerItemComboBoxModel() {
		return itemComboBoxModel(asList(
						item(1, "One"), item(2, "Two"), item(3, "Three"),
						item(4, "Four"), item(5, "Five"), item(6, "Six"),
						item(7, "Seven"), item(8, "Eight"), item(9, "Nine")
		));
	}

	public DefaultBoundedRangeModel createIntegerSliderModel() {
		return new DefaultBoundedRangeModel(0, 0, 0, 100);
	}

	public SpinnerNumberModel createIntegerSpinnerModel() {
		return new SpinnerNumberModel(0, 0, 100, 10);
	}

	public ComboBoxModel<Integer> createIntegerComboBoxModel() {
		return new DefaultComboBoxModel<>(new Integer[] {101, 202, 303, 404});
	}

	public SpinnerListModel createItemSpinnerModel() {
		return new SpinnerListModel(Arrays.asList(
						item("Hello"), item("Everybody"),
						item("How"), item("Are"), item("You")
		));
	}

	public ListModel<String> createStringListModel() {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Here");
		listModel.addElement("Are");
		listModel.addElement("A");
		listModel.addElement("Few");
		listModel.addElement("Elements");
		listModel.addElement("To");
		listModel.addElement("Select");
		listModel.addElement("From");

		return listModel;
	}

	private void exceptionHandler(Thread thread, Throwable exception) {
		messageValue.set(exception.getMessage());
	}

	private <T> void setMessage(T value) {
		messageValue.set(value == null ? " " : value.toString());
	}
}
// end::demoModel[]