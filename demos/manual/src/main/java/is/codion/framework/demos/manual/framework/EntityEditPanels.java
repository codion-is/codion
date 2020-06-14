/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;
import is.codion.swing.framework.ui.EntityLookupField;

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

  interface Demo {
    EntityType TYPE = domainType("domainType").entityType("entityType");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Entity> FOREIGN_KEY = TYPE.entityAttribute("foreign_key");
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
      JCheckBox checkBox = createCheckBox(Demo.BOOLEAN, null, IncludeCaption.NO);

      NullableCheckBox nullableCheckBox = createNullableCheckBox(Demo.BOOLEAN);

      JComboBox<Item<Boolean>> comboBox = createBooleanComboBox(Demo.BOOLEAN);
      // end::booleanValue[]
    }

    private void foreignKeyValue() {
      // tag::foreignKeyValue[]
      EntityComboBox comboBox = createForeignKeyComboBox(Demo.FOREIGN_KEY);

      EntityLookupField lookupField = createForeignKeyLookupField(Demo.FOREIGN_KEY);

      //readOnly
      JTextField textField = createForeignKeyField(Demo.FOREIGN_KEY);
      // end::foreignKeyValue[]
    }

    private void temporalValue() {
      // tag::temporalValue[]
      JTextField textField = createTextField(Demo.LOCAL_DATE);

      TemporalInputPanel<LocalDate> inputPanel = createTemporalInputPanel(Demo.LOCAL_DATE);
      // end::temporalValue[]
    }

    private void numericalValue() {
      // tag::numericalValue[]
      IntegerField integerField = (IntegerField) createTextField(Demo.INTEGER);

      LongField longField = (LongField) createTextField(Demo.LONG);

      DoubleField doubleField = (DoubleField) createTextField(Demo.DOUBLE);

      BigDecimalField bigDecimalField = (BigDecimalField) createTextField(Demo.BIG_DECIMAL);
      // end::numericalValue[]
    }

    private void textValue() {
      // tag::textValue[]
      JTextField textField = createTextField(Demo.TEXT);

      JTextArea textArea = createTextArea(Demo.LONG_TEXT, 5, 20);

      TextInputPanel inputPanel = createTextInputPanel(Demo.LONG_TEXT);

      JFormattedTextField formattedField =
              createMaskedTextField(Demo.FORMATTED_TEXT, "###:###",
                      ValueContainsLiterals.YES);
      // end::textValue[]
    }

    private void valueList() {
      // tag::valueList[]
      SteppedComboBox<Item<String>> comboBox = createValueListComboBox(Demo.VALUE_LIST);
      // end::valueList[]
    }

    private void panelLabel() {
      // tag::panelLabel[]
      JLabel label = createLabel(Demo.TEXT);

      JPanel propertyPanel = createInputPanel(Demo.TEXT);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
