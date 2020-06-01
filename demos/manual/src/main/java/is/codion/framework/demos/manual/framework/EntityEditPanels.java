/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.framework.domain.property.Identity;
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

import static is.codion.framework.domain.property.Attributes.*;

public final class EntityEditPanels {

  private static final class Domain {
    static final Identity ENTITY_ID = Identity.identity("entityId");
    static final Attribute<Boolean> BOOLEAN_ATTRIBUTE = booleanAttribute("boolean", ENTITY_ID);
    static final EntityAttribute FOREIGN_KEY_ATTRIBUTE = entityAttribute("foreign_key", ENTITY_ID);
    static final Attribute<LocalDate> LOCAL_DATE_ATTRIBUTE = localDateAttribute("local_date", ENTITY_ID);
    static final Attribute<Integer> INTEGER_ATTRIBUTE = integerAttribute("integer", ENTITY_ID);
    static final Attribute<Long> LONG_ATTRIBUTE = longAttribute("long", ENTITY_ID);
    static final Attribute<Double> DOUBLE_ATTRIBUTE = doubleAttribute("double", ENTITY_ID);
    static final Attribute<BigDecimal> BIG_DECIMAL_ATTRIBUTE = bigDecimalAttribute("big_decimal", ENTITY_ID);
    static final Attribute<String> TEXT_ATTRIBUTE = stringAttribute("text", ENTITY_ID);
    static final Attribute<String> LONG_TEXT_ATTRIBUTE = stringAttribute("long_text", ENTITY_ID);
    static final Attribute<String> FORMATTED_TEXT_ATTRIBUTE = stringAttribute("formatted_text", ENTITY_ID);
    static final Attribute<String> VALUE_LIST_ATTRIBUTE = stringAttribute("value_list", ENTITY_ID);
  }

  private static final class EditPanelDemo extends EntityEditPanel {

    public EditPanelDemo(final SwingEntityEditModel editModel) {
      super(editModel);
    }


    private void booleanValue() {
      // tag::booleanValue[]
      JCheckBox checkBox = createCheckBox(Domain.BOOLEAN_ATTRIBUTE, null, IncludeCaption.NO);

      NullableCheckBox nullableCheckBox = createNullableCheckBox(Domain.BOOLEAN_ATTRIBUTE);

      JComboBox comboBox = createBooleanComboBox(Domain.BOOLEAN_ATTRIBUTE);
      // end::booleanValue[]
    }

    private void foreignKeyValue() {
      // tag::foreignKeyValue[]
      EntityComboBox comboBox = createForeignKeyComboBox(Domain.FOREIGN_KEY_ATTRIBUTE);

      EntityLookupField lookupField = createForeignKeyLookupField(Domain.FOREIGN_KEY_ATTRIBUTE);

      //readOnly
      JTextField textField = createForeignKeyField(Domain.FOREIGN_KEY_ATTRIBUTE);
      // end::foreignKeyValue[]
    }

    private void temporalValue() {
      // tag::temporalValue[]
      JTextField textField = createTextField(Domain.LOCAL_DATE_ATTRIBUTE);

      TemporalInputPanel inputPanel = createTemporalInputPanel(Domain.LOCAL_DATE_ATTRIBUTE);
      // end::temporalValue[]
    }

    private void numericalValue() {
      // tag::numericalValue[]
      IntegerField integerField = (IntegerField) createTextField(Domain.INTEGER_ATTRIBUTE);

      LongField longField = (LongField) createTextField(Domain.LONG_ATTRIBUTE);

      DecimalField doubleField = (DecimalField) createTextField(Domain.DOUBLE_ATTRIBUTE);

      DecimalField bigDecimalField = (DecimalField) createTextField(Domain.BIG_DECIMAL_ATTRIBUTE);
      // end::numericalValue[]
    }

    private void textValue() {
      // tag::textValue[]
      JTextField textField = createTextField(Domain.TEXT_ATTRIBUTE);

      JTextArea textArea = createTextArea(Domain.LONG_TEXT_ATTRIBUTE, 5, 20);

      TextInputPanel inputPanel = createTextInputPanel(Domain.LONG_TEXT_ATTRIBUTE);

      JFormattedTextField formattedField = (JFormattedTextField)
              createTextField(Domain.FORMATTED_TEXT_ATTRIBUTE, UpdateOn.KEYSTROKE, "###:###");
      // end::textValue[]
    }

    private void valueList() {
      // tag::valueList[]
      SteppedComboBox comboBox = createValueListComboBox(Domain.VALUE_LIST_ATTRIBUTE);
      // end::valueList[]
    }

    private void panelLabel() {
      // tag::panelLabel[]
      JLabel label = createLabel(Domain.TEXT_ATTRIBUTE);

      JPanel propertyPanel = createPropertyPanel(Domain.TEXT_ATTRIBUTE);
      // end::panelLabel[]
    }

    @Override
    protected void initializeUI() {}
  }
}
