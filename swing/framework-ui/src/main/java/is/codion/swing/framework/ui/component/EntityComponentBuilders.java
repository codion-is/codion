/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.component.BooleanComboBoxBuilder;
import is.codion.swing.common.ui.component.CheckBoxBuilder;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.component.FormattedTextFieldBuilder;
import is.codion.swing.common.ui.component.TemporalInputPanelBuilder;
import is.codion.swing.common.ui.component.TextAreaBuilder;
import is.codion.swing.common.ui.component.TextFieldBuilder;
import is.codion.swing.common.ui.component.TextInputPanelBuilder;
import is.codion.swing.common.ui.component.ValueListComboBoxBuilder;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link is.codion.swing.common.ui.component.ComponentBuilder}.
 */
public class EntityComponentBuilders {

  private final EntityDefinition entityDefinition;

  /**
   * @param entityDefinition the entity definition
   */
  public EntityComponentBuilders(final EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @return the underlying entity definition
   */
  public final EntityDefinition getEntityDefinition() {
    return entityDefinition;
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.checkBoxBuilder()
            .description(property.getDescription())
            .nullable(property.isNullable())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute) {
    final Property<Boolean> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.booleanComboBoxBuilder(new BooleanComboBoxModel())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @return a builder
   */
  public final ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey, final SwingEntityComboBoxModel comboBoxModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyComboBoxBuilder(comboBoxModel)
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a builder
   */
  public final ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey, final EntitySearchModel searchModel) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeySearchFieldBuilder(searchModel)
            .description(foreignKeyProperty.getDescription() == null ? searchModel.getDescription() : foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param foreignKey the foreign key
   * @return a builder
   */
  public final ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey) {
    final ForeignKeyProperty foreignKeyProperty = entityDefinition.getForeignKeyProperty(foreignKey);

    return new DefaultForeignKeyFieldBuilder()
            .description(foreignKeyProperty.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a ValueListProperty");
    }

    return ComponentBuilders.valueListComboBoxBuilder(((ValueListProperty<T>) property).getValues())
            .description(property.getDescription())
            .nullable(property.isNullable());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param comboBoxModel the combo box model
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.comboBoxBuilder(attribute.getTypeClass(), comboBoxModel)
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a builder
   */
  public final <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute) {
    if (!attribute.isTemporal()) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not Temporal");
    }
    final Property<T> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.temporalInputPanelBuiler(attribute.getTypeClass())
            .description(property.getDescription())
            .dateTimePattern(property.getDateTimePattern());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.textInputPanelBuilder()
            .description(property.getDescription())
            .maximumLength(property.getMaximumLength())
            .caption(property.getCaption());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final TextAreaBuilder textAreaBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);
    if (!attribute.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return ComponentBuilders.textAreaBuilder()
            .description(property.getDescription())
            .maximumLength(property.getMaximumLength());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @param <C> the text field type
   * @param <B> the builder type
   * @return a builder
   */
  public final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textFieldBuilder(final Attribute<T> attribute) {
    final Property<T> property = entityDefinition.getProperty(attribute);

    return (TextFieldBuilder<T, C, B>) ComponentBuilders.textFieldBuilder(attribute.getTypeClass())
            .format(property.getFormat())
            .dateTimePattern(property.getDateTimePattern())
            .maximumLength(property.getMaximumLength())
            .minimumValue(property.getMinimumValue())
            .maximumValue(property.getMaximumValue())
            .maximumFractionDigits(property.getMaximumFractionDigits())
            .description(property.getDescription());
  }

  /**
   * Creates a builder.
   * @param attribute the attribute
   * @return a builder
   */
  public final FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute) {
    final Property<String> property = entityDefinition.getProperty(attribute);

    return ComponentBuilders.formattedTextFieldBuilder()
            .description(property.getDescription());
  }
}
