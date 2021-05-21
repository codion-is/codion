/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;

import static is.codion.framework.domain.DomainType.domainType;

public final class EntityEditPanels {

  interface DemoMaster {
    EntityType<Entity> TYPE = domainType("domainType").entityType("master");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
  }

  interface Demo {
    EntityType<Entity> TYPE = domainType("domainType").entityType("entityType");
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
    Attribute<String> VALUE_LIST = TYPE.stringAttribute("value_list");
  }

  private static final class EditPanelDemo extends EntityEditPanel {

    public EditPanelDemo(final SwingEntityEditModel editModel) {
      super(editModel);
    }


    private void booleanValue() {
      // tag::booleanValue[]
      JCheckBox checkBox =
              checkBox(Demo.BOOLEAN)
                      .includeCaption(false)
                      .build();

      NullableCheckBox nullableCheckBox =
              (NullableCheckBox) checkBox(Demo.BOOLEAN)
                      .nullable(true)
                      .build();

      JComboBox<Item<Boolean>> comboBox =
              booleanComboBox(Demo.BOOLEAN)
                      .build();
      // end::booleanValue[]
    }

    private void foreignKeyValue() {
      // tag::foreignKeyValue[]
      EntityComboBox comboBox =
              foreignKeyComboBox(Demo.FOREIGN_KEY)
                      .build();

      EntitySearchField searchField =
              foreignKeySearchField(Demo.FOREIGN_KEY)
                      .build();

      //readOnly
      JTextField textField =
              foreignKeyField(Demo.FOREIGN_KEY)
                      .build();
      // end::foreignKeyValue[]
    }

    private void temporalValue() {
      // tag::temporalValue[]
      JTextField textField =
              textField(Demo.LOCAL_DATE)
                      .build();

      TemporalInputPanel<LocalDate> inputPanel =
              temporalInputPanel(Demo.LOCAL_DATE)
                      .build();
      // end::temporalValue[]
    }

    private void numericalValue() {
      // tag::numericalValue[]
      IntegerField integerField =
              (IntegerField) textField(Demo.INTEGER)
                      .build();

      LongField longField =
              (LongField) textField(Demo.LONG)
                      .build();

      DoubleField doubleField =
              (DoubleField) textField(Demo.DOUBLE)
                      .build();

      BigDecimalField bigDecimalField =
              (BigDecimalField) textField(Demo.BIG_DECIMAL)
                      .build();
      // end::numericalValue[]
    }

    private void textValue() {
      // tag::textValue[]
      JTextField textField =
              textField(Demo.TEXT)
                      .build();

      JTextArea textArea =
              textArea(Demo.LONG_TEXT)
                      .rows(5).columns(20)
                      .build();

      TextInputPanel inputPanel =
              textInputPanel(Demo.LONG_TEXT)
                      .build();

      JFormattedTextField formattedField =
              formattedTextField(Demo.FORMATTED_TEXT)
                      .formatMaskString("###:###")
                      .valueContainsLiterals(true)
                      .build();
      // end::textValue[]
    }

    private void valueList() {
      // tag::valueList[]
      SteppedComboBox<Item<String>> comboBox = valueListComboBox(Demo.VALUE_LIST)
              .build();
      // end::valueList[]
    }

    private void panelLabel() {
      // tag::panelLabel[]
      JLabel label = createLabel(Demo.TEXT);

      JPanel inputPanel = createInputPanel(Demo.TEXT);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
