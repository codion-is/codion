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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.framework;

import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditComponentPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComboBoxPanel;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchFieldPanel;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.framework.domain.DomainType.domainType;

public final class EntityEditPanels {

	interface DemoMaster {
		EntityType TYPE = domainType("domainType").entityType("master");
		Column<Integer> ID = TYPE.integerColumn("id");
	}

	interface Demo {
		EntityType TYPE = domainType("domainType").entityType("entityType");
		Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
		Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean");
		Column<Integer> FOREIGN_ATTRIBUTE = TYPE.integerColumn("foreign_id");
		ForeignKey FOREIGN_KEY = TYPE.foreignKey("foreign_key", FOREIGN_ATTRIBUTE, DemoMaster.ID);
		Column<LocalDate> LOCAL_DATE = TYPE.localDateColumn("local_date");
		Column<Integer> INTEGER = TYPE.integerColumn("integer");
		Column<Long> LONG = TYPE.longColumn("long");
		Column<Double> DOUBLE = TYPE.doubleColumn("double");
		Column<BigDecimal> BIG_DECIMAL = TYPE.bigDecimalColumn("big_decimal");
		Column<BigInteger> BIG_INTEGER = TYPE.bigIntegerColumn("big_integer");
		Column<String> TEXT = TYPE.stringColumn("text");
		Column<String> LONG_TEXT = TYPE.stringColumn("long_text");
		Column<String> FORMATTED_TEXT = TYPE.stringColumn("formatted_text");
		Column<String> ITEM_LIST = TYPE.stringColumn("item_list");
	}

	private static final class EditPanelDemo extends EntityEditComponentPanel {

		public EditPanelDemo(SwingEntityEditModel editModel) {
			super(editModel);
		}

		private void booleanValue() {
			// tag::booleanValue[]
			JCheckBox checkBox =
							createCheckBox(Demo.BOOLEAN)
											.build();

			NullableCheckBox nullableCheckBox =
							createNullableCheckBox(Demo.BOOLEAN_NULLABLE)
											.build();

			JComboBox<Item<Boolean>> comboBox =
							createBooleanComboBox(Demo.BOOLEAN_NULLABLE)
											.build();
			// end::booleanValue[]
		}

		private void foreignKeyValue() {
			// tag::foreignKeyValue[]
			EntityComboBox comboBox =
							createComboBox(Demo.FOREIGN_KEY)
											.build();

			// Include add/edit buttons
			EntityComboBoxPanel comboBoxPanel =
							createComboBoxPanel(Demo.FOREIGN_KEY, this::createEditPanel)
											.includeAddButton(true)
											.includeEditButton(true)
											.build();

			EntitySearchField searchField =
							createSearchField(Demo.FOREIGN_KEY)
											.build();

			// Include add/edit buttons
			EntitySearchFieldPanel searchFieldPanel =
							createSearchFieldPanel(Demo.FOREIGN_KEY, this::createEditPanel)
											.includeAddButton(true)
											.includeEditButton(true)
											.build();

			//readOnly
			JTextField textField =
							createTextField(Demo.FOREIGN_KEY)
											.build();
			// end::foreignKeyValue[]
		}

		private void temporalValue() {
			// tag::temporalValue[]
			TemporalField<LocalDateTime> textField =
							(TemporalField<LocalDateTime>) createTextField(Demo.LOCAL_DATE)
											.build();

			TemporalField<LocalDate> localDateField =
							createTemporalField(Demo.LOCAL_DATE)
											.build();

			TemporalFieldPanel<LocalDate> temporalPanel =
							createTemporalFieldPanel(Demo.LOCAL_DATE)
											.build();
			// end::temporalValue[]
		}

		private void numericalValue() {
			// tag::numericalValue[]
			NumberField<Integer> integerField =
							(NumberField<Integer>) createTextField(Demo.INTEGER)
											.build();

			integerField =
							createIntegerField(Demo.INTEGER)
											.build();

			NumberField<Long> longField =
							(NumberField<Long>) createTextField(Demo.LONG)
											.build();

			longField =
							createLongField(Demo.LONG)
											.build();

			NumberField<Double> doubleField =
							(NumberField<Double>) createTextField(Demo.DOUBLE)
											.build();

			doubleField =
							createDoubleField(Demo.DOUBLE)
											.build();

			NumberField<BigDecimal> bigDecimalField =
							(NumberField<BigDecimal>) createTextField(Demo.BIG_DECIMAL)
											.build();

			bigDecimalField =
							createBigDecimalField(Demo.BIG_DECIMAL)
											.build();

			NumberField<BigInteger> bigIntegerField =
							(NumberField<BigInteger>) createTextField(Demo.BIG_DECIMAL)
											.build();

			bigIntegerField =
							createBigIntegerField(Demo.BIG_INTEGER)
											.build();
			// end::numericalValue[]
		}

		private void textValue() {
			// tag::textValue[]
			JTextField textField =
							createTextField(Demo.TEXT)
											.build();

			JFormattedTextField maskedField =
							createMaskedTextField(Demo.FORMATTED_TEXT)
											.mask("###:###")
											.valueContainsLiteralCharacters(true)
											.build();

			JTextArea textArea =
							createTextArea(Demo.LONG_TEXT)
											.rowsColumns(5, 20)
											.build();

			TextFieldPanel inputPanel =
							createTextFieldPanel(Demo.LONG_TEXT)
											.build();
			// end::textValue[]
		}

		private void selectionValue() {
			// tag::selectionValue[]
			DefaultComboBoxModel<String> comboBoxModel =
							new DefaultComboBoxModel<>(new String[] {"One", "Two"});

			JComboBox<String> comboBox =
							createComboBox(Demo.TEXT, comboBoxModel)
											.editable(true)
											.build();
			// end::selectionValue[]
		}

		private void item() {
			// tag::item[]
			JComboBox<Item<String>> comboBox =
							createItemComboBox(Demo.ITEM_LIST)
											.build();
			// end::item[]
		}

		private void panelLabel() {
			// tag::panelLabel[]
			JLabel label = createLabel(Demo.TEXT)
							.build();

			JPanel inputPanel = createInputPanel(Demo.TEXT)
							.label(new JLabel("Label"))
							.build();
			// end::panelLabel[]
		}

		private EntityEditPanel createEditPanel() {
			return null;
		}
	}
}
