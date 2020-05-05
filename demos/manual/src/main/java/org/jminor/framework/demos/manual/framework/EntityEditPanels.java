/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.framework;

import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.TextInputPanel;
import org.jminor.swing.common.ui.time.TemporalInputPanel;
import org.jminor.swing.common.ui.value.UpdateOn;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityInputComponents.IncludeCaption;
import org.jminor.swing.framework.ui.EntityLookupField;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class EntityEditPanels {

  private static final class Domain {
    static final String T_MASTER = "master";
    static final String T_DETAIL = "detail";
    static final String BOOLEAN_PROPERTY = "boolean";
    static final String FOREIGN_KEY_PROPERTY = "foreign_key";
    static final String LOCAL_DATE_PROPERTY = "local_date";
    static final String INTEGER_PROPERTY = "integer";
    static final String LONG_PROPERTY = "long";
    static final String DOUBLE_PROPERTY = "double";
    static final String BIG_DECIMAL_PROPERTY = "big_decimal";
    static final String TEXT_PROPERTY = "text";
    static final String LONG_TEXT_PROPERTY = "long_text";
    static final String FORMATTED_TEXT_PROPERTY = "formatted_text";
    static final String VALUE_LIST_PROPERTY = "value_list";
  }

  private static final class EditPanelDemo extends EntityEditPanel {

    public EditPanelDemo(final SwingEntityEditModel editModel) {
      super(editModel);
    }


    private void booleanValue() {
      // tag::booleanValue[]
      JCheckBox checkBox = createCheckBox(Domain.BOOLEAN_PROPERTY, null, IncludeCaption.NO);

      NullableCheckBox nullableCheckBox = createNullableCheckBox(Domain.BOOLEAN_PROPERTY);

      JComboBox comboBox = createBooleanComboBox(Domain.BOOLEAN_PROPERTY);
      // end::booleanValue[]
    }

    private void foreignKeyValue() {
      // tag::foreignKeyValue[]
      EntityComboBox comboBox = createForeignKeyComboBox(Domain.FOREIGN_KEY_PROPERTY);

      EntityLookupField lookupField = createForeignKeyLookupField(Domain.FOREIGN_KEY_PROPERTY);

      //readOnly
      JTextField textField = createForeignKeyField(Domain.FOREIGN_KEY_PROPERTY);
      // end::foreignKeyValue[]
    }

    private void temporalValue() {
      // tag::temporalValue[]
      JTextField textField = createTextField(Domain.LOCAL_DATE_PROPERTY);

      TemporalInputPanel inputPanel = createTemporalInputPanel(Domain.LOCAL_DATE_PROPERTY);
      // end::temporalValue[]
    }

    private void numericalValue() {
      // tag::numericalValue[]
      IntegerField integerField = (IntegerField) createTextField(Domain.INTEGER_PROPERTY);

      LongField longField = (LongField) createTextField(Domain.LONG_PROPERTY);

      DecimalField doubleField = (DecimalField) createTextField(Domain.DOUBLE_PROPERTY);

      DecimalField bigDecimalField = (DecimalField) createTextField(Domain.BIG_DECIMAL_PROPERTY);
      // end::numericalValue[]
    }

    private void textValue() {
      // tag::textValue[]
      JTextField textField = createTextField(Domain.TEXT_PROPERTY);

      JTextArea textArea = createTextArea(Domain.LONG_TEXT_PROPERTY, 5, 20);

      TextInputPanel inputPanel = createTextInputPanel(Domain.LONG_TEXT_PROPERTY);

      JFormattedTextField formattedField = (JFormattedTextField)
              createTextField(Domain.FORMATTED_TEXT_PROPERTY, UpdateOn.KEYSTROKE, "###:###");
      // end::textValue[]
    }

    private void valueList() {
      // tag::valueList[]
      SteppedComboBox comboBox = createValueListComboBox(Domain.VALUE_LIST_PROPERTY);
      // end::valueList[]
    }

    private void panelLabel() {
      // tag::panelLabel[]
      JLabel label = createLabel(Domain.TEXT_PROPERTY);

      JPanel propertyPanel = createPropertyPanel(Domain.TEXT_PROPERTY);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
