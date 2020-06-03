/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.DecimalField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.UpdateOn;
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

import static is.codion.framework.domain.entity.Entities.type;

public final class EntityEditPanels {

  interface Demo {
    EntityType TYPE = type("entityType");
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

      JComboBox comboBox = createBooleanComboBox(Demo.BOOLEAN);
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

      TemporalInputPanel inputPanel = createTemporalInputPanel(Demo.LOCAL_DATE);
      // end::temporalValue[]
    }

    private void numericalValue() {
      // tag::numericalValue[]
      IntegerField integerField = (IntegerField) createTextField(Demo.INTEGER);

      LongField longField = (LongField) createTextField(Demo.LONG);

      DecimalField doubleField = (DecimalField) createTextField(Demo.DOUBLE);

      DecimalField bigDecimalField = (DecimalField) createTextField(Demo.BIG_DECIMAL);
      // end::numericalValue[]
    }

    private void textValue() {
      // tag::textValue[]
      JTextField textField = createTextField(Demo.TEXT);

      JTextArea textArea = createTextArea(Demo.LONG_TEXT, 5, 20);

      TextInputPanel inputPanel = createTextInputPanel(Demo.LONG_TEXT);

      JFormattedTextField formattedField = (JFormattedTextField)
              createTextField(Demo.FORMATTED_TEXT, UpdateOn.KEYSTROKE, "###:###");
      // end::textValue[]
    }

    private void valueList() {
      // tag::valueList[]
      SteppedComboBox comboBox = createValueListComboBox(Demo.VALUE_LIST);
      // end::valueList[]
    }

    private void panelLabel() {
      // tag::panelLabel[]
      JLabel label = createLabel(Demo.TEXT);

      JPanel propertyPanel = createPropertyPanel(Demo.TEXT);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
