/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.framework.domain.DomainType.domainType;

public final class EntityEditPanels {

  interface DemoMaster {
    EntityType TYPE = domainType("domainType").entityType("master");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
  }

  interface Demo {
    EntityType TYPE = domainType("domainType").entityType("entityType");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Integer> FOREIGN_ATTRIBUTE = TYPE.integerAttribute("foreign_id");
    ForeignKey FOREIGN_KEY = TYPE.foreignKey("foreign_key", FOREIGN_ATTRIBUTE, DemoMaster.ID);
    Attribute<LocalDate> LOCAL_DATE = TYPE.localDateAttribute("local_date");
    Attribute<Integer> INTEGER = TYPE.integerAttribute("integer");
    Attribute<Long> LONG = TYPE.longAttribute("long");
    Attribute<Double> DOUBLE = TYPE.doubleAttribute("double");
    Attribute<BigDecimal> BIG_DECIMAL = TYPE.bigDecimalAttribute("big_decimal");
    Attribute<String> TEXT = TYPE.stringAttribute("text");
    Attribute<String> LONG_TEXT = TYPE.stringAttribute("long_text");
    Attribute<String> FORMATTED_TEXT = TYPE.stringAttribute("formatted_text");
    Attribute<String> ITEM_LIST = TYPE.stringAttribute("item_list");
  }

  private static final class EditPanelDemo extends EntityEditPanel {

    public EditPanelDemo(SwingEntityEditModel editModel) {
      super(editModel);
    }

    private void booleanValue() {
      // tag::booleanValue[]
      JCheckBox checkBox =
              createCheckBox(Demo.BOOLEAN)
                      .build();

      NullableCheckBox nullableCheckBox =
              (NullableCheckBox) createCheckBox(Demo.BOOLEAN)
                      .nullable(true)
                      .build();

      JComboBox<Item<Boolean>> comboBox =
              createBooleanComboBox(Demo.BOOLEAN)
                      .build();
      // end::booleanValue[]
    }

    private void foreignKeyValue() {
      // tag::foreignKeyValue[]
      EntityComboBox comboBox =
              createForeignKeyComboBox(Demo.FOREIGN_KEY)
                      .build();

      EntitySearchField searchField =
              createForeignKeySearchField(Demo.FOREIGN_KEY)
                      .build();

      //readOnly
      JLabel label =
              createForeignKeyLabel(Demo.FOREIGN_KEY)
                      .build();
      // end::foreignKeyValue[]
    }

    private void temporalValue() {
      // tag::temporalValue[]
      TemporalField<LocalDateTime> textField =
              (TemporalField<LocalDateTime>) createTextField(Demo.LOCAL_DATE)
                      .build();

      TemporalField<LocalDate> localDateField =
              createLocalDateField(Demo.LOCAL_DATE)
                      .build();

      TemporalInputPanel<LocalDate> inputPanel =
              createTemporalInputPanel(Demo.LOCAL_DATE)
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

      TextInputPanel inputPanel =
              createTextInputPanel(Demo.LONG_TEXT)
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

      JPanel inputPanel = createInputPanel(Demo.TEXT);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
