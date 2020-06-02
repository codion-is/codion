/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.framework;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityId;
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

import static is.codion.framework.domain.entity.Entities.entityId;

public final class EntityEditPanels {

  private static final class Domain {
    static final EntityId ENTITY_ID = entityId("entityId");
    static final Attribute<Boolean> BOOLEAN_ATTRIBUTE = ENTITY_ID.booleanAttribute("boolean");
    static final Attribute<Entity> FOREIGN_KEY_ATTRIBUTE = ENTITY_ID.entityAttribute("foreign_key");
    static final Attribute<LocalDate> LOCAL_DATE_ATTRIBUTE = ENTITY_ID.localDateAttribute("local_date");
    static final Attribute<Integer> INTEGER_ATTRIBUTE = ENTITY_ID.integerAttribute("integer");
    static final Attribute<Long> LONG_ATTRIBUTE = ENTITY_ID.longAttribute("long");
    static final Attribute<Double> DOUBLE_ATTRIBUTE = ENTITY_ID.doubleAttribute("double");
    static final Attribute<BigDecimal> BIG_DECIMAL_ATTRIBUTE = ENTITY_ID.bigDecimalAttribute("big_decimal");
    static final Attribute<String> TEXT_ATTRIBUTE = ENTITY_ID.stringAttribute("text");
    static final Attribute<String> LONG_TEXT_ATTRIBUTE = ENTITY_ID.stringAttribute("long_text");
    static final Attribute<String> FORMATTED_TEXT_ATTRIBUTE = ENTITY_ID.stringAttribute("formatted_text");
    static final Attribute<String> VALUE_LIST_ATTRIBUTE = ENTITY_ID.stringAttribute("value_list");
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
