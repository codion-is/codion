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
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.slider.SliderBuilder;
import is.codion.swing.common.ui.component.spinner.ItemSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.ListSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextInputPanel;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.component.EntityComponentValidators.addFormattedValidator;
import static is.codion.swing.framework.ui.component.EntityComponentValidators.addValidator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;

/**
 * A base class for entity edit panels, providing components for editing entities.
 */
public class EntityEditComponentPanel extends JPanel {

  /**
   * Specifies whether label text should be underlined to indicate that the associated value is modified<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see #MODIFIED_INDICATOR_UNDERLINE_STYLE
   */
  public static final PropertyValue<Boolean> USE_MODIFIED_INDICATOR =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditComponentPanel.useModifiedIndicator", true);

  /**
   * The type of underline to use to indicate a modified value<br>
   * Value type: Integer<br>
   * Default value: {@link TextAttribute#UNDERLINE_LOW_DOTTED}<br>
   * Valid values: {@link TextAttribute}.UNDERLINE_*
   * @see #USE_MODIFIED_INDICATOR
   */
  public static final PropertyValue<Integer> MODIFIED_INDICATOR_UNDERLINE_STYLE =
          Configuration.integerValue("is.codion.swing.framework.ui.EntityEditComponentPanel.modifiedIndicatorUnderlineStyle", TextAttribute.UNDERLINE_LOW_DOTTED);

  /**
   * The edit model these edit components are associated with
   */
  private final SwingEntityEditModel editModel;

  /**
   * The input component builder factory
   */
  private final EntityComponents entityComponents;

  /**
   * Input components mapped to their respective attributes
   */
  private final Map<Attribute<?>, JComponent> components = new HashMap<>();

  /**
   * Input component builders mapped to their respective attributes
   */
  private final Map<Attribute<?>, ComponentBuilder<?, ?, ?>> componentBuilders = new HashMap<>();

  /**
   * Attributes that should be excluded when presenting the component selection
   */
  private final Set<Attribute<?>> excludeFromSelection = new HashSet<>();

  /**
   * Holds the last focused input component
   */
  private final Value<JComponent> focusedInputComponent = Value.value();

  /**
   * The component that should receive focus when the UI is initialized/cleared
   */
  private JComponent initialFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is initialized/cleared
   */
  private Attribute<?> initialFocusAttribute;

  /**
   * The component that should receive focus when the UI is initialized after insert
   */
  private JComponent afterInsertFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is initialized after insert
   */
  private Attribute<?> afterInsertFocusAttribute;

  /**
   * Specifies whether components created by this edit component panel should transfer focus on enter.
   */
  private boolean transferFocusOnEnter = true;

  /**
   * The default number of text field columns
   */
  private int defaultTextFieldColumns = TextFieldBuilder.DEFAULT_TEXT_FIELD_COLUMNS.get();

  /**
   * Specifies whether to use a modified indicator on component labels
   */
  private boolean useModifiedIndicator = USE_MODIFIED_INDICATOR.get();

  /**
   * Instantiates a new EntityEditComponentPanel
   * @param editModel the edit model
   */
  protected EntityEditComponentPanel(SwingEntityEditModel editModel) {
    this(editModel, new EntityComponents(editModel.entityDefinition()));
  }

  /**
   * Instantiates a new EntityEditComponentPanel
   * @param editModel the edit model
   * @param entityComponents the entity components to use when creating components
   * @throws IllegalArgumentException in case the edit model and entity components entityTypes don't match
   */
  protected EntityEditComponentPanel(SwingEntityEditModel editModel, EntityComponents entityComponents) {
    this.editModel = requireNonNull(editModel, "editModel");
    this.entityComponents = requireNonNull(entityComponents, "entityComponents");
    if (!editModel.entityType().equals(entityComponents.entityDefinition().entityType())) {
      throw new IllegalArgumentException("Entity type mismatch: " + editModel.entityType() + ", " + entityComponents.entityDefinition().entityType());
    }
    addFocusedComponentListener();
  }

  /**
   * @param <T> the edit model type
   * @return the edit model this panel is based on
   */
  public final <T extends SwingEntityEditModel> T editModel() {
    return (T) editModel;
  }

  /**
   * @return an unmodifiable view of the attributes that have associated components.
   */
  public final Collection<Attribute<?>> componentAttributes() {
    Set<Attribute<?>> attributes = new HashSet<>(components.keySet());
    attributes.addAll(componentBuilders.keySet());

    return Collections.unmodifiableCollection(attributes);
  }

  /**
   * @param attribute the attribute
   * @return the component associated with the given attribute
   * @throws IllegalArgumentException in case no component or component builder has been associated with the given attribute
   */
  public final JComponent component(Attribute<?> attribute) {
    JComponent component = getComponentInternal(attribute);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with attribute: " + attribute);
    }

    return component;
  }

  /**
   * @param component the component
   * @param <T> the attribute type
   * @return the attribute the given component is associated with
   * @throws IllegalArgumentException in case no attribute is associated with the given component
   */
  public final <T> Attribute<T> attribute(JComponent component) {
    requireNonNull(component);
    return (Attribute<T>) components.entrySet().stream()
            .filter(entry -> entry.getValue() == component)
            .findFirst()
            .map(Map.Entry::getKey)
            .orElseThrow(() -> new IllegalArgumentException("No attribute associated with this component"));
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via {@link #setInitialFocusAttribute(Attribute)}
   * @param initialFocusComponent the component
   * @return the component
   */
  public final JComponent setInitialFocusComponent(JComponent initialFocusComponent) {
    this.initialFocusComponent = requireNonNull(initialFocusComponent);
    return initialFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param attribute the component attribute
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusAttribute(Attribute<?> attribute) {
    this.initialFocusAttribute = requireNonNull(attribute);
  }

  /**
   * Sets the component that should receive the focus after an insert has been performed.
   * Overrides the value set via {@link #setAfterInsertFocusAttribute(Attribute)}
   * @param afterInsertFocusComponent the component
   * @return the component
   */
  public final JComponent setAfterInsertFocusComponent(JComponent afterInsertFocusComponent) {
    this.afterInsertFocusComponent = requireNonNull(afterInsertFocusComponent);
    return afterInsertFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the focus after an insert is performed in this edit panel.
   * This is overridden by setAfterInsertFocusComponent().
   * @param attribute the component attribute
   * @see #setAfterInsertFocusComponent(JComponent)
   */
  public final void setAfterInsertFocusAttribute(Attribute<?> attribute) {
    this.afterInsertFocusAttribute = requireNonNull(attribute);
  }

  /**
   * Sets the initial focus, if an initial focus component or component attribute
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus.
   * Note that if this panel is not visible then calling this method has no effect.
   * @see #isVisible()
   * @see #setInitialFocusAttribute(Attribute)
   * @see #setInitialFocusComponent(JComponent)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestFocus(initialFocusComponent());
    }
  }

  /**
   * Request focus for the component associated with the given attribute.
   * If no component is associated with the attribute calling this method has no effect.
   * @param attribute the attribute of the component to select
   */
  public final void requestComponentFocus(Attribute<?> attribute) {
    JComponent component = getComponentInternal(attribute);
    if (component != null) {
      focusableComponent(component).requestFocus();
    }
  }

  /**
   * Displays a dialog allowing the user the select an input component which should receive the keyboard focus,
   * if only one input component is available then that component is selected automatically.
   * @see #excludeComponentsFromSelection(Attribute[])
   * @see #requestComponentFocus(Attribute)
   */
  public final void selectInputComponent() {
    Entities entities = editModel().entities();
    List<AttributeDefinition<?>> attributeDefinitions = selectComponentAttributes().stream()
            .map(attribute -> entities.definition(attribute.entityType()).attributes().definition(attribute))
            .sorted(AttributeDefinition.definitionComparator())
            .collect(Collectors.toList());
    Optional<AttributeDefinition<?>> optionalAttribute = attributeDefinitions.size() == 1 ? Optional.of(attributeDefinitions.iterator().next()) :
            Dialogs.selectionDialog(attributeDefinitions)
                    .owner(this)
                    .title(FrameworkMessages.selectInputField())
                    .selectSingle();
    optionalAttribute.ifPresent(attributeDefinition -> requestComponentFocus(attributeDefinition.attribute()));
  }

  /**
   * @return an unmodifiable view of the attributes to present when selecting an input component in this panel,
   * this returns all (non-excluded) attributes that have an associated component in this panel
   * that is enabled, displayable, visible and focusable.
   * @see #excludeComponentsFromSelection(Attribute[])
   * @see #setComponent(Attribute, JComponent)
   */
  public final Collection<Attribute<?>> selectComponentAttributes() {
    return components.keySet().stream()
            .filter(attribute -> !excludeFromSelection.contains(attribute))
            .filter(attribute -> isComponentSelectable(getComponentInternal(attribute)))
            .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableCollection));
  }

  /**
   * Specifies that the given attributes should be excluded when presenting a component selection list.
   * @param attributes the attributes to exclude from selection
   * @see #selectInputComponent()
   */
  public final void excludeComponentsFromSelection(Attribute<?>... attributes) {
    for (Attribute<?> attribute : requireNonNull(attributes)) {
      excludeFromSelection.add(requireNonNull(attribute));
    }
  }

  /**
   * Handles the given exception, simply displays the error message to the user by default.
   * @param exception the exception to handle
   * @see #displayException(Throwable)
   */
  protected void onException(Throwable exception) {
    displayException(exception);
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to display
   */
  protected final void displayException(Throwable exception) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      focusOwner = EntityEditComponentPanel.this;
    }
    Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
  }

  /**
   * If set to true then component labels will indicate that the value is modified.
   * This applies to all components created by this edit component panel as well as
   * components set via {@link #setComponent(Attribute, JComponent)} as long
   * as the component has a JLabel associated with its 'labeledBy' client property.
   * Note that this has no effect on components that have already been created.
   * @param useModifiedIndicator the new value
   * @see #USE_MODIFIED_INDICATOR
   * @see JLabel#setLabelFor(Component)
   */
  protected final void setUseModifiedIndicator(boolean useModifiedIndicator) {
    this.useModifiedIndicator = useModifiedIndicator;
  }

  /**
   * If set to true then components created subsequently will transfer focus on enter, otherwise not.
   * Note that this has no effect on components that have already been created.
   * @param transferFocusOnEnter the new value
   * @see ComponentBuilder#TRANSFER_FOCUS_ON_ENTER
   */
  protected final void setTransferFocusOnEnter(boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
  }

  /**
   * Sets the default number of text field columns
   * @param defaultTextFieldColumns the default text field columns
   * @see #createTextField(Attribute)
   * @see #createForeignKeySearchField(ForeignKey)
   * @see #createTextInputPanel(Attribute)
   */
  protected final void setDefaultTextFieldColumns(int defaultTextFieldColumns) {
    this.defaultTextFieldColumns = defaultTextFieldColumns;
  }

  /**
   * Associates the given input component with the given attribute.
   * @param attribute the attribute
   * @param component the input component
   */
  protected final void setComponent(Attribute<?> attribute, JComponent component) {
    components.put(requireNonNull(attribute), requireNonNull(component));
    if (useModifiedIndicator && attribute.entityType().equals(editModel.entityType())) {
      editModel.modified(attribute).addDataListener(new ModifiedIndicator(component));
    }
  }

  /**
   * Adds a panel for the given attribute to this panel
   * @param attribute the attribute
   * @see #createInputPanel(Attribute)
   */
  protected final void addInputPanel(Attribute<?> attribute) {
    add(createInputPanel(attribute));
  }

  /**
   * Adds a panel for the given attribute to this panel using the given layout constraints
   * @param attribute the attribute
   * @param constraints the layout constraints
   * @see #createInputPanel(Attribute)
   */
  protected final void addInputPanel(Attribute<?> attribute, Object constraints) {
    add(createInputPanel(attribute), constraints);
  }

  /**
   * Adds a panel for the given attribute to this panel
   * @param attribute the attribute
   * @param inputComponent a component bound to {@code attribute}
   * @see #createInputPanel(Attribute, JComponent)
   */
  protected final void addInputPanel(Attribute<?> attribute, JComponent inputComponent) {
    add(createInputPanel(attribute, inputComponent));
  }

  /**
   * Adds a panel for the given attribute to this panel using the given layout constraints
   * @param attribute the attribute
   * @param inputComponent a component bound to {@code attribute}
   * @param constraints the layout constraints
   * @see #createInputPanel(Attribute, JComponent)
   */
  protected final void addInputPanel(Attribute<?> attribute, JComponent inputComponent, Object constraints) {
    add(createInputPanel(attribute, inputComponent), constraints);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption defined for {@code attribute}.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param attribute the attribute from which definition to retrieve the label caption
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given attribute
   */
  protected final JPanel createInputPanel(Attribute<?> attribute) {
    return createInputPanel(attribute, component(attribute));
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption defined for {@code attribute}.
   * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
   * @param attribute the attribute from which definition to retrieve the label caption
   * @param inputComponent a component bound to the value of {@code attribute}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent) {
    return createInputPanel(attribute, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption defined for on {@code attribute}.
   * @param attribute the attribute from which definition to retrieve the label caption
   * @param inputComponent a component bound to the value of {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent,
                                          String labelBorderLayoutConstraints) {
    return createInputPanel(attribute, inputComponent, labelBorderLayoutConstraints, SwingConstants.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of {@code attribute}.
   * @param attribute the attribute from which definition to retrieve the label caption
   * @param inputComponent a component bound to the value of {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @param labelAlignment the label alignment
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent,
                                          String labelBorderLayoutConstraints, int labelAlignment) {
    return createInputPanel(createLabel(attribute).horizontalAlignment(labelAlignment).build(), inputComponent, labelBorderLayoutConstraints);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent} with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(JComponent labelComponent, JComponent inputComponent) {
    return createInputPanel(labelComponent, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel with a BorderLayout, with the {@code inputComponent} at {@link BorderLayout#CENTER}
   * and the {@code labelComponent} at a specified location.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(JComponent labelComponent, JComponent inputComponent,
                                          String labelBorderLayoutConstraints) {
    requireNonNull(labelComponent, "labelComponent");
    requireNonNull(inputComponent, "inputComponent");
    if (labelComponent instanceof JLabel) {
      // we need to be a bit clever here, since the input component argument isn't necessarily the actual input
      // component, f.ex. this could be a text area wrapped in a scroll pane or a combobox on a panel with
      // a new instance button, we assume that this input component at least contains the actual component
      components.values().stream()
              .filter(comp -> sameOrParentOf(inputComponent, comp))
              .findAny()
              .ifPresent(component -> setLabelForComponent((JLabel) labelComponent, component));
    }
    return Components.panel(borderLayout())
            .add(inputComponent, BorderLayout.CENTER)
            .add(labelComponent, labelBorderLayoutConstraints)
            .build();
  }

  /**
   * Creates a builder for text areas.
   * @param attribute the attribute for which to build a text area
   * @return a text area builder
   */
  protected final TextAreaBuilder createTextArea(Attribute<String> attribute) {
    return setComponentBuilder(attribute, entityComponents.textArea(attribute)
            .onBuild(textArea -> addValidator(attribute, textArea, editModel())));
  }

  /**
   * Creates a builder for text input panels.
   * @param attribute the attribute for which to build a text input panel
   * @return a text input panel builder
   */
  protected final TextInputPanel.Builder createTextInputPanel(Attribute<String> attribute) {
    return setComponentBuilder(attribute, entityComponents.textInputPanel(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(inputPanel -> addValidator(attribute, inputPanel.textField(), editModel())));
  }

  /**
   * Creates a builder for temporal input panels.
   * @param attribute the attribute for which to build a temporal input panel
   * @param <T> the temporal type
   * @return a text area builder
   */
  protected final <T extends Temporal> TemporalInputPanel.Builder<T> createTemporalInputPanel(Attribute<T> attribute) {
    return setComponentBuilder(attribute, entityComponents.temporalInputPanel(attribute)
            .onBuild(inputPanel -> addFormattedValidator(attribute, inputPanel.temporalField(), editModel())));
  }

  /**
   * Creates a builder for temporal input panels.
   * @param attribute the attribute for which to build a temporal input panel
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return a text area builder
   */
  protected final <T extends Temporal> TemporalInputPanel.Builder<T> createTemporalInputPanel(Attribute<T> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.temporalInputPanel(attribute, dateTimePattern)
            .onBuild(inputPanel -> addFormattedValidator(attribute, inputPanel.temporalField(), editModel())));
  }

  /**
   * Creates a builder for text fields.
   * @param attribute the attribute for which to build a text field
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @return a text field builder
   */
  protected final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> createTextField(Attribute<T> attribute) {
    return setComponentBuilder(attribute, (TextFieldBuilder<T, C, B>) entityComponents.textField(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @return a local time field builder
   */
  protected final TemporalField.Builder<LocalTime> createLocalTimeField(Attribute<LocalTime> attribute) {
    return setComponentBuilder(attribute, entityComponents.localTimeField(attribute)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param dateTimePattern the date time pattern
   * @return a local time field builder
   */
  protected final TemporalField.Builder<LocalTime> createLocalTimeField(Attribute<LocalTime> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.localTimeField(attribute, dateTimePattern)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @return a local date field builder
   */
  protected final TemporalField.Builder<LocalDate> createLocalDateField(Attribute<LocalDate> attribute) {
    return setComponentBuilder(attribute, entityComponents.localDateField(attribute)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param dateTimePattern the date time pattern
   * @return a local date field builder
   */
  protected final TemporalField.Builder<LocalDate> createLocalDateField(Attribute<LocalDate> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.localDateField(attribute, dateTimePattern)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @return a local date time field builder
   */
  protected final TemporalField.Builder<LocalDateTime> createLocalDateTimeField(Attribute<LocalDateTime> attribute) {
    return setComponentBuilder(attribute, entityComponents.localDateTimeField(attribute)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param dateTimePattern the date time pattern
   * @return a local date time field builder
   */
  protected final TemporalField.Builder<LocalDateTime> createLocalDateTimeField(Attribute<LocalDateTime> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.localDateTimeField(attribute, dateTimePattern)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @return an offset date time field builder
   */
  protected final TemporalField.Builder<OffsetDateTime> createOffsetDateTimeField(Attribute<OffsetDateTime> attribute) {
    return setComponentBuilder(attribute, entityComponents.offsetDateTimeField(attribute)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param dateTimePattern the date time pattern
   * @return an offset date time field builder
   */
  protected final TemporalField.Builder<OffsetDateTime> createOffsetDateTimeField(Attribute<OffsetDateTime> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.offsetDateTimeField(attribute, dateTimePattern)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param <T> the temporal type
   * @return an offset date time field builder
   */
  protected final <T extends Temporal> TemporalField.Builder<T> createTemporalField(Attribute<T> attribute) {
    return setComponentBuilder(attribute, entityComponents.temporalField(attribute)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for temporal fields.
   * @param attribute the attribute for which to build a temporal field
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return an offset date time field builder
   */
  protected final <T extends Temporal> TemporalField.Builder<T> createTemporalField(Attribute<T> attribute, String dateTimePattern) {
    return setComponentBuilder(attribute, entityComponents.temporalField(attribute, dateTimePattern)
            .onBuild(field -> addFormattedValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for a slider
   * @param attribute the attribute
   * @return a slider builder
   */
  protected final SliderBuilder createSlider(Attribute<Integer> attribute) {
    return setComponentBuilder(attribute, entityComponents.slider(attribute));
  }

  /**
   * Creates a builder for a spinner
   * @param attribute the attribute
   * @return a spinner builder
   */
  protected final NumberSpinnerBuilder<Integer> createIntegerSpinner(Attribute<Integer> attribute) {
    return setComponentBuilder(attribute, entityComponents.integerSpinner(attribute));
  }

  /**
   * Creates a builder for a spinner
   * @param attribute the attribute
   * @return a spinner builder
   */
  protected final NumberSpinnerBuilder<Double> createDoubleSpinner(Attribute<Double> attribute) {
    return setComponentBuilder(attribute, entityComponents.doubleSpinner(attribute));
  }

  /**
   * Creates a builder for a list spinner
   * @param attribute the attribute
   * @param spinnerListModel the spinner model
   * @param <T> the value type
   * @return a spinner builder
   */
  protected final <T> ListSpinnerBuilder<T> createListSpinner(Attribute<T> attribute, SpinnerListModel spinnerListModel) {
    return setComponentBuilder(attribute, entityComponents.listSpinner(attribute, spinnerListModel));
  }

  /**
   * Creates a builder for a list spinner
   * @param attribute the attribute
   * @param <T> the value type
   * @return a spinner builder
   */
  protected final <T> ItemSpinnerBuilder<T> createItemSpinner(Attribute<T> attribute) {
    return setComponentBuilder(attribute, entityComponents.itemSpinner(attribute));
  }

  /**
   * Creates a builder for integer fields.
   * @param attribute the attribute for which to build a text field
   * @return a integer field builder
   */
  protected final NumberField.Builder<Integer> createIntegerField(Attribute<Integer> attribute) {
    return setComponentBuilder(attribute, entityComponents.integerField(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(field -> addValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for long fields.
   * @param attribute the attribute for which to build a text field
   * @return a long field builder
   */
  protected final NumberField.Builder<Long> createLongField(Attribute<Long> attribute) {
    return setComponentBuilder(attribute, entityComponents.longField(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(field -> addValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for double fields.
   * @param attribute the attribute for which to build a text field
   * @return a double field builder
   */
  protected final NumberField.Builder<Double> createDoubleField(Attribute<Double> attribute) {
    return setComponentBuilder(attribute, entityComponents.doubleField(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(field -> addValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for big decimal fields.
   * @param attribute the attribute for which to build a text field
   * @return a big decimal field builder
   */
  protected final NumberField.Builder<BigDecimal> createBigDecimalField(Attribute<BigDecimal> attribute) {
    return setComponentBuilder(attribute, entityComponents.bigDecimalField(attribute)
            .columns(defaultTextFieldColumns)
            .onBuild(field -> addValidator(attribute, field, editModel())));
  }

  /**
   * Creates a builder for formatted text fields.
   * @param attribute the attribute for which to build a formatted text field
   * @return a formatted text field builder
   */
  protected final MaskedTextFieldBuilder createMaskedTextField(Attribute<String> attribute) {
    return setComponentBuilder(attribute, entityComponents.maskedTextField(attribute)
            .onBuild(textField -> addFormattedValidator(attribute, textField, editModel())));
  }

  /**
   * Creates a builder for check boxes. If {@link CheckBoxBuilder#nullable(boolean)} is set to true,
   * a {@link NullableCheckBox} is built.
   * @param attribute the attribute for which to build a check-box
   * @return a check-box builder
   */
  protected final CheckBoxBuilder createCheckBox(Attribute<Boolean> attribute) {
    return setComponentBuilder(attribute, entityComponents.checkBox(attribute));
  }

  /**
   * Creates a builder for boolean combo boxes.
   * @param attribute the attribute for which to build boolean combo box
   * @return a boolean combo box builder
   */
  protected ItemComboBoxBuilder<Boolean> createBooleanComboBox(Attribute<Boolean> attribute) {
    return setComponentBuilder(attribute, entityComponents.booleanComboBox(attribute));
  }

  /**
   * Creates a builder for combo boxes.
   * @param attribute the attribute for which to build combo box
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a combo box builder
   */
  protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> createComboBox(Attribute<T> attribute, ComboBoxModel<T> comboBoxModel) {
    return setComponentBuilder(attribute, entityComponents.comboBox(attribute, comboBoxModel));
  }

  /**
   * Creates a builder for value item list combo boxes.
   * @param attribute the attribute for which to build a value list combo box
   * @param <T> the value type
   * @return a value item list combo box builder
   */
  protected final <T> ItemComboBoxBuilder<T> createItemComboBox(Attribute<T> attribute) {
    return setComponentBuilder(attribute, entityComponents.itemComboBox(attribute));
  }

  /**
   * Creates a builder for a combo boxe, containing the values of the given column.
   * @param column the column for which to build a combo box
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a combo box builder
   */
  protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> createComboBox(Column<T> column) {
    FilteredComboBoxModel<T> comboBoxModel = editModel().comboBoxModel(column);
    comboBoxModel.refresher().addRefreshFailedListener(this::onException);

    return (ComboBoxBuilder<T, C, B>) setComponentBuilder(column, entityComponents.comboBox(column, comboBoxModel)
            .onSetVisible(EntityEditComponentPanel::refreshIfCleared));
  }

  /**
   * Creates a builder for foreign key combo boxes.
   * @param foreignKey the foreign key for which to build a combo box
   * @param <B> the builder type
   * @return a foreign key combo box builder
   */
  protected final <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> createForeignKeyComboBox(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = editModel().foreignKeyComboBoxModel(foreignKey);
    comboBoxModel.refresher().addRefreshFailedListener(this::onException);

    return (ComboBoxBuilder<Entity, EntityComboBox, B>) setComponentBuilder(foreignKey, entityComponents.foreignKeyComboBox(foreignKey, comboBoxModel)
            .onSetVisible(EntityEditComponentPanel::refreshIfCleared));
  }

  /**
   * Creates a builder for foreign key search fields.
   * @param foreignKey the foreign key for which to build a search field
   * @return a foreign key search field builder
   */
  protected final EntitySearchField.Builder createForeignKeySearchField(ForeignKey foreignKey) {
    return setComponentBuilder(foreignKey, entityComponents.foreignKeySearchField(foreignKey,
                    editModel().foreignKeySearchModel(foreignKey))
            .columns(defaultTextFieldColumns));
  }

  /**
   * Creates a builder for a read-only, non-focusable foreign key text field.
   * @param foreignKey the foreign key for which to build a text field
   * @param <B> the builder type
   * @return a foreign key text field builder
   */
  protected final <B extends TextFieldBuilder<Entity, JTextField, B>> TextFieldBuilder<Entity, JTextField, B> createForeignKeyTextField(ForeignKey foreignKey) {
    return setComponentBuilder(foreignKey, entityComponents.foreignKeyTextField(foreignKey));
  }

  /**
   * Creates a builder for a read-only foreign key label.
   * @param foreignKey the foreign key for which to build a label
   * @return a foreign key label builder
   */
  protected final LabelBuilder<Entity> createForeignKeyLabel(ForeignKey foreignKey) {
    return setComponentBuilder(foreignKey, entityComponents.foreignKeyLabel(foreignKey));
  }

  /**
   * Creates a builder for a label using the caption and mnemonic associated with {@code attribute}. If an input component exists
   * for the given attribute the label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param attribute the attribute from which to retrieve the caption
   * @param <T> the attribute type
   * @return a label builder for the given attribute
   */
  protected final <T> LabelBuilder<T> createLabel(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = editModel().entities()
            .definition(requireNonNull(attribute).entityType()).attributes().definition(attribute);
    return (LabelBuilder<T>) Components.label(attributeDefinition.caption())
            .displayedMnemonic(attributeDefinition.mnemonic() == null ? 0 : attributeDefinition.mnemonic())
            .labelFor(getComponentInternal(attribute));
  }

  /**
   * @return the component that should get the initial focus when the UI is initialized
   */
  protected JComponent initialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusAttribute != null) {
      return getComponentInternal(initialFocusAttribute);
    }

    return null;
  }

  /**
   * @return the component that should receive the focus when the UI is initialized after insert
   */
  protected JComponent afterInsertFocusComponent() {
    if (afterInsertFocusComponent != null) {
      return afterInsertFocusComponent;
    }

    if (afterInsertFocusAttribute != null) {
      return getComponentInternal(afterInsertFocusAttribute);
    }

    return initialFocusComponent();
  }

  protected final void requestFocusAfterInsert() {
    requestFocus(afterInsertFocusComponent());
  }

  protected final void requestFocusAfterUpdate() {
    requestFocus(focusedInputComponent.optional().orElse(initialFocusComponent()));
  }

  private <T, B extends ComponentBuilder<T, ?, ?>> B setComponentBuilder(Attribute<T> attribute, B componentBuilder) {
    requireNonNull(attribute);
    requireNonNull(componentBuilder);
    if (componentBuilders.containsKey(attribute)) {
      throw new IllegalStateException("ComponentBuilder has already been set for attribute: " + attribute);
    }
    componentBuilders.put(attribute, componentBuilder
            .transferFocusOnEnter(transferFocusOnEnter)
            .linkedValue(editModel().value(attribute))
            .onBuild(new OnComponentBuilt<>(attribute)));

    return componentBuilder;
  }

  private void requestFocus(JComponent component) {
    if (component != null && component.isFocusable()) {
      component.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  private JComponent getComponentInternal(Attribute<?> attribute) {
    ComponentBuilder<?, ?, ?> componentBuilder = componentBuilders.get(requireNonNull(attribute));
    if (componentBuilder != null) {
      componentBuilder.build();
    }

    return components.get(attribute);
  }

  private void addFocusedComponentListener() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addPropertyChangeListener("focusOwner", new FocusedInputComponentListener());
  }

  /**
   * Returns true if this component can be selected, that is,
   * if it is non-null, displayable, visible, focusable and enabled.
   * @param component the component
   * @return true if this component can be selected
   */
  private static boolean isComponentSelectable(JComponent component) {
    return component != null &&
            component.isDisplayable() &&
            component.isVisible() &&
            isFocusable(component) &&
            component.isEnabled();
  }

  private static boolean isFocusable(JComponent component) {
    if (component instanceof JSpinner) {
      return ((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField().isFocusable();
    }

    return component.isFocusable();
  }

  private static JComponent focusableComponent(JComponent component) {
    if (component instanceof JSpinner) {
      return ((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField();
    }

    return component;
  }

  private static JLabel setLabelForComponent(JLabel label, JComponent component) {
    if (component != null && label.getLabelFor() != component) {
      label.setLabelFor(component);
    }

    return label;
  }

  private static void refreshIfCleared(JComboBox<?> comboBox) {
    ComboBoxModel<?> model = comboBox.getModel();
    if (model instanceof FilteredComboBoxModel) {
      FilteredComboBoxModel<?> comboBoxModel = (FilteredComboBoxModel<?>) model;
      if (comboBoxModel.isCleared()) {
        comboBoxModel.refresh();
      }
    }
  }

  private static boolean sameOrParentOf(JComponent parent, JComponent component) {
    if (parent == component) {
      return true;
    }

    return Arrays.stream(parent.getComponents()).anyMatch(childComponent ->
            childComponent instanceof JComponent &&
                    sameOrParentOf((JComponent) childComponent, component));
  }

  private final class OnComponentBuilt<C extends JComponent> implements Consumer<C> {

    private final Attribute<?> attribute;

    private OnComponentBuilt(Attribute<?> attribute) {
      this.attribute = attribute;
    }

    @Override
    public void accept(C component) {
      componentBuilders.remove(attribute);
      setComponent(attribute, component);
    }
  }

  private final class FocusedInputComponentListener implements PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      Component component = (Component) event.getNewValue();
      if (component instanceof JComponent && isInputComponent((JComponent) component)) {
        focusedInputComponent.set((JComponent) component);
      }
    }

    private boolean isInputComponent(JComponent component) {
      for (JComponent inputComponent : components.values()) {
        if (sameOrParentOf(inputComponent, component)) {
          return true;
        }
      }

      return false;
    }
  }

  private static final class ModifiedIndicator implements Consumer<Boolean> {

    private static final String LABELED_BY_PROPERTY = "labeledBy";
    private static final Integer UNDERLINE_STYLE = MODIFIED_INDICATOR_UNDERLINE_STYLE.get();

    private final JComponent component;

    private ModifiedIndicator(JComponent component) {
      this.component = component;
    }

    @Override
    public void accept(Boolean modified) {
      JLabel label = (JLabel) component.getClientProperty(LABELED_BY_PROPERTY);
      if (label != null) {
        if (SwingUtilities.isEventDispatchThread()) {
          setModifiedIndicator(label, modified);
        }
        else {
          SwingUtilities.invokeLater(() -> setModifiedIndicator(label, modified));
        }
      }
    }

    private static void setModifiedIndicator(JLabel label, boolean modified) {
      Font font = label.getFont();
      Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
      attributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, modified ? UNDERLINE_STYLE : null);
      label.setFont(font.deriveFont(attributes));
    }
  }
}
