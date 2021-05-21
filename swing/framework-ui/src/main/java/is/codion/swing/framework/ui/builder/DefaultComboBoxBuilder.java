/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.function.Consumer;

final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>> implements ComboBoxBuilder<T> {

  private final ComboBoxModel<T> comboBoxModel;
  private boolean editable = false;

  DefaultComboBoxBuilder(final Property<T> attribute, final Value<T> value,
                         final ComboBoxModel<T> comboBoxModel) {
    super(attribute, value);
    this.comboBoxModel = comboBoxModel;
  }

  @Override
  public ComboBoxBuilder<T> preferredHeight(final int preferredHeight) {
    return (ComboBoxBuilder<T>) super.preferredHeight(preferredHeight);
  }

  @Override
  public ComboBoxBuilder<T> preferredWidth(final int preferredWidth) {
    return (ComboBoxBuilder<T>) super.preferredWidth(preferredWidth);
  }

  @Override
  public ComboBoxBuilder<T> preferredSize(final Dimension preferredSize) {
    return (ComboBoxBuilder<T>) super.preferredSize(preferredSize);
  }

  @Override
  public ComboBoxBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (ComboBoxBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public ComboBoxBuilder<T> enabledState(final StateObserver enabledState) {
    return (ComboBoxBuilder<T>) super.enabledState(enabledState);
  }

  @Override
  public ComboBoxBuilder<T> onBuild(final Consumer<SteppedComboBox<T>> onBuild) {
    return (ComboBoxBuilder<T>) super.onBuild(onBuild);
  }

  @Override
  public ComboBoxBuilder<T> editable(final boolean editable) {
    this.editable = editable;
    return this;
  }

  @Override
  public SteppedComboBox<T> build() {
    final SteppedComboBox<T> comboBox = createComboBox();
    setPreferredSize(comboBox);
    onBuild(comboBox);
    comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  private SteppedComboBox<T> createComboBox() {
    final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
    if (editable && !property.getAttribute().isString()) {
      throw new IllegalArgumentException("Editable attribute ComboBox is only implemented for String properties");
    }
    comboBox.setEditable(editable);
    ComponentValues.comboBox(comboBox).link(value);

    return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
  }

  static void addComboBoxCompletion(final JComboBox<?> comboBox) {
    final String completionMode = EntityInputComponents.COMBO_BOX_COMPLETION_MODE.get();
    switch (completionMode) {
      case Completion.COMPLETION_MODE_NONE:
        break;
      case Completion.COMPLETION_MODE_AUTOCOMPLETE:
        Completion.autoComplete(comboBox);
        break;
      case Completion.COMPLETION_MODE_MAXIMUM_MATCH:
        Completion.maximumMatch(comboBox);
        break;
      default:
        throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
    }
  }
}
