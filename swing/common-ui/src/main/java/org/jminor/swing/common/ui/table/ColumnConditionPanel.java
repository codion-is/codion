/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.DateUtil;
import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.Item;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.Value;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A UI implementation for ColumnConditionModel
 * @param <K> the type of objects used to identify columns
 */
public class ColumnConditionPanel<K> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The ColumnConditionModel this ColumnConditionPanel represents
   */
  private final ColumnConditionModel<K> conditionModel;

  /**
   * The search types allowed in this model
   */
  private final Collection<Condition.Type> conditionTypes;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleEnabledButton;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleAdvancedButton;
  private final JComboBox conditionTypeCombo;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;

  private final Event<K> focusGainedEvent = Events.event();
  private final State advancedConditionState = States.state();

  private JDialog dialog;
  private Point lastDialogPosition;
  private boolean dialogEnabled = false;
  private boolean dialogVisible = false;

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public ColumnConditionPanel(final ColumnConditionModel<K> conditionModel, final boolean includeToggleEnabledButton,
                              final boolean includeToggleAdvancedConditionButton) {
    this(conditionModel, includeToggleEnabledButton, includeToggleAdvancedConditionButton, Condition.Type.values());
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is include
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is include
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<K> conditionModel, final boolean includeToggleEnabledButton,
                              final boolean includeToggleAdvancedConditionButton, final Condition.Type... conditionTypes) {
    this(conditionModel, includeToggleEnabledButton, includeToggleAdvancedConditionButton, new DefaultInputFieldProvider(conditionModel), conditionTypes);
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is include
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is include
   * @param inputFieldProvider the input field provider
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<K> conditionModel, final boolean includeToggleEnabledButton,
                              final boolean includeToggleAdvancedConditionButton, final InputFieldProvider inputFieldProvider,
                              final Condition.Type... conditionTypes) {
    this(conditionModel, includeToggleEnabledButton, includeToggleAdvancedConditionButton, inputFieldProvider.initializeInputField(true),
            inputFieldProvider.initializeInputField(false), conditionTypes);
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param includeToggleEnabledButton if true a button for enabling this condition panel is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   * @param upperBoundField the upper bound input field
   * @param lowerBoundField the lower bound input field
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<K> conditionModel, final boolean includeToggleEnabledButton,
                              final boolean includeToggleAdvancedConditionButton, final JComponent upperBoundField,
                              final JComponent lowerBoundField, final Condition.Type... conditionTypes) {
    Objects.requireNonNull(conditionModel, "conditionModel");
    this.conditionModel = conditionModel;
    this.conditionTypes = conditionTypes == null ? Arrays.asList(Condition.Type.values()) : Arrays.asList(conditionTypes);
    this.conditionTypeCombo = initializeConditionTypeComboBox();
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    if (includeToggleEnabledButton) {
      this.toggleEnabledButton = ControlProvider.createToggleButton(
              Controls.toggleControl(conditionModel, "enabled", null, conditionModel.getEnabledObserver()));
      toggleEnabledButton.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    }
    else {
      this.toggleEnabledButton = null;
    }
    if (includeToggleAdvancedConditionButton) {
      this.toggleAdvancedButton = ControlProvider.createToggleButton(
              Controls.toggleControl(this, "advancedConditionEnabled", null, advancedConditionState.getObserver()));
      toggleAdvancedButton.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    }
    else {
      this.toggleAdvancedButton = null;
    }
    linkComponentsToLockedState();
    initializeUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the condition model this panel uses
   */
  public final ColumnConditionModel<K> getConditionModel() {
    return this.conditionModel;
  }

  /**
   * @return the last screen position
   */
  public final Point getLastDialogPosition() {
    return lastDialogPosition;
  }

  /**
   * @return true if the dialog is enabled
   */
  public final boolean isDialogEnabled() {
    return dialogEnabled;
  }

  /**
   * @return true if the dialog is being shown
   */
  public final boolean isDialogVisible() {
    return dialogVisible;
  }

  /**
   * Displays this condition panel in a dialog
   * @param dialogParent the dialog parent
   * @param position the position
   */
  public final void enableDialog(final Container dialogParent, final Point position) {
    if (!isDialogEnabled()) {
      initializeConditionDialog(dialogParent);
      Point actualPosition = position;
      if (position == null) {
        actualPosition = lastDialogPosition;
      }
      if (actualPosition == null) {
        actualPosition = new Point(0, 0);
      }

      actualPosition.y = actualPosition.y - dialog.getHeight();
      dialog.setLocation(actualPosition);
      dialogEnabled = true;
    }

    showDialog();
  }

  /**
   * Hides the dialog displaying this condition panel
   */
  public final void disableDialog() {
    if (isDialogEnabled()) {
      if (isDialogVisible()) {
        hideDialog();
      }
      lastDialogPosition = dialog.getLocation();
      lastDialogPosition.y = lastDialogPosition.y + dialog.getHeight();
      dialog.dispose();
      dialog = null;
      dialogEnabled = false;
    }
  }

  /**
   * Displays this panel in a dialog
   */
  public final void showDialog() {
    if (isDialogEnabled() && !isDialogVisible()) {
      dialog.setVisible(true);
      upperBoundField.requestFocusInWindow();
      dialogVisible = true;
    }
  }

  /**
   * Hides the dialog showing this panel if visible
   */
  public final void hideDialog() {
    if (isDialogVisible()) {
      dialog.setVisible(false);
      dialogVisible = false;
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public final JDialog getDialog() {
    return dialog;
  }

  /**
   * Requests keyboard focus for this panels input field
   */
  public final void requestInputFocus() {
    if (conditionModel.isLowerBoundRequired()) {
      lowerBoundField.requestFocusInWindow();
    }
    else {
      upperBoundField.requestFocusInWindow();
    }
  }

  /**
   * @param value true if advanced condition should be enabled
   */
  public final void setAdvancedConditionEnabled(final boolean value) {
    advancedConditionState.setActive(value);
  }

  /**
   * @return true if the advanced condition is enabled
   */
  public final boolean isAdvancedConditionEnabled() {
    return advancedConditionState.isActive();
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public final JComponent getUpperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public final JComponent getLowerBoundField() {
    return lowerBoundField;
  }

  /**
   * @param listener a listener notified each time the advanced condition state changes
   */
  public final void addAdvancedConditionListener(final EventListener listener) {
    advancedConditionState.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedConditionListener(final EventListener listener) {
    advancedConditionState.removeListener(listener);
  }

  /**
   * @param listener listener notified when a this condition panels input fields receive focus
   */
  public final void addFocusGainedListener(final EventDataListener<K> listener) {
    focusGainedEvent.addDataListener(listener);
  }

  /**
   * Provides a upper/lower bound input fields for a ColumnConditionPanel
   */
  public interface InputFieldProvider {

    /**
     * @param isUpperBound if true then the returned field should be bound
     * with with upper bound value int he condition model, otherwise the lower bound
     * @return a upper/lower bound input field
     */
    JComponent initializeInputField(final boolean isUpperBound);
  }

  private static final class DefaultInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<?> columnConditionModel;

    private DefaultInputFieldProvider(final ColumnConditionModel<?> columnConditionModel) {
      Objects.requireNonNull(columnConditionModel, "columnConditionModel");
      this.columnConditionModel = columnConditionModel;
    }

    /**
     * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
     * @return an input field for either the upper or lower bound
     */
    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (columnConditionModel.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for boolean values
      }
      final JComponent field = initializeField();
      if (columnConditionModel.getType() == Types.BOOLEAN) {
        createToggleProperty((JCheckBox) field, isUpperBound);
      }
      else {
        createTextProperty(field, isUpperBound);
      }

      return field;
    }

    private JComponent initializeField() {
      switch (columnConditionModel.getType()) {
        case Types.INTEGER:
          return new IntegerField(DEFAULT_FIELD_COLUMNS);
        case Types.DOUBLE:
          return new DoubleField(DEFAULT_FIELD_COLUMNS);
        case Types.BIGINT:
          return new LongField(DEFAULT_FIELD_COLUMNS);
        case Types.BOOLEAN:
          return new JCheckBox();
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.DATE:
          return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) columnConditionModel.getFormat()));
        default:
          return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void createToggleProperty(final JCheckBox checkBox, final boolean upperBound) {
      ValueLinks.toggleValueLink(checkBox.getModel(),
              upperBound ? columnConditionModel.getUpperBoundValue() : columnConditionModel.getLowerBoundValue(), false);
    }

    @SuppressWarnings("unchecked")
    private void createTextProperty(final JComponent component, final boolean upperBound) {
      final Value modelValue = upperBound ? columnConditionModel.getUpperBoundValue() : columnConditionModel.getLowerBoundValue();
      switch (columnConditionModel.getType()) {
        case Types.INTEGER:
          ValueLinks.integerValueLink((IntegerField) component, modelValue, false, false, true);
          break;
        case Types.DOUBLE:
          ValueLinks.doubleValueLink((DoubleField) component, modelValue, false, false, true);
          break;
        case Types.BIGINT:
          ValueLinks.longValueLink((LongField) component, modelValue, false, false, true);
          break;
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.DATE:
          ValueLinks.dateValueLink((JFormattedTextField) component, modelValue, false,
                  (SimpleDateFormat) columnConditionModel.getFormat(), columnConditionModel.getType(), true);
          break;
        default:
          ValueLinks.textValueLink((JTextField) component, modelValue, null, true, false);
      }
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedConditionState.addListener(() -> {
      initializePanel();
      if (toggleAdvancedButton != null) {
        toggleAdvancedButton.requestFocusInWindow();
      }
      else {
        upperBoundField.requestFocusInWindow();
      }
    });
    conditionModel.addLowerBoundRequiredListener(() -> {
      initializePanel();
      revalidate();
      conditionTypeCombo.requestFocusInWindow();
    });
    final FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        focusGainedEvent.fire((K) conditionModel.getColumnIdentifier());
      }
    };
    conditionTypeCombo.addFocusListener(focusGainedListener);
    upperBoundField.addFocusListener(focusGainedListener);
    if (lowerBoundField != null) {
      lowerBoundField.addFocusListener(focusGainedListener);
    }
    if (toggleAdvancedButton != null) {
      toggleAdvancedButton.addFocusListener(focusGainedListener);
    }
    if (toggleEnabledButton != null) {
      toggleEnabledButton.addFocusListener(focusGainedListener);
    }
  }

  private void initializePanel() {
    if (advancedConditionState.isActive()) {
      initializeAdvancedPanel();
    }
    else {
      initializeSimplePanel();
    }
  }

  private JComboBox initializeConditionTypeComboBox() {
    final ItemComboBoxModel<Condition.Type> comboBoxModel = new ItemComboBoxModel<>();
    for (final Condition.Type type : Condition.Type.values()) {
      if (conditionTypes.contains(type)) {
        comboBoxModel.addItem(new Item<>(type, type.getCaption()));
      }
    }
    final JComboBox<Condition.Type> comboBox = new SteppedComboBox(comboBoxModel);
    ValueLinks.selectedItemValueLink(comboBox, conditionModel, "conditionType", Condition.Type.class, (EventObserver) conditionModel.getConditionTypeObserver());
    comboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                    final boolean isSelected, final boolean cellHasFocus) {
        final JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        component.setToolTipText(((Item<Condition.Type>) value).getItem().getDescription());

        return component;
      }
    });

    return comboBox;
  }

  private void initializeUI() {
    final FlexibleGridLayout layout = new FlexibleGridLayout(2, 1, 1, 1, true, false);
    setLayout(layout);
    if (toggleEnabledButton != null) {
      this.toggleEnabledButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    if (toggleAdvancedButton != null) {
      this.toggleAdvancedButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
  }

  private void initializeSimplePanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1, 1));
    if (conditionModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2, 1, 1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      basePanel.add(upperBoundField, BorderLayout.CENTER);
    }

    if (toggleEnabledButton != null) {
      basePanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      basePanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }

    add(basePanel);

    setPreferredSize(new Dimension(getPreferredSize().width, basePanel.getPreferredSize().height));

    revalidate();
  }

  private void initializeAdvancedPanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(2);
    final JPanel inputPanel = new JPanel(new BorderLayout(1, 1));
    if (conditionModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2, 1, 1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      inputPanel.add(upperBoundField, BorderLayout.CENTER);
    }

    final JPanel controlPanel = new JPanel(new BorderLayout(1, 1));
    controlPanel.add(conditionTypeCombo, BorderLayout.CENTER);
    if (toggleEnabledButton != null) {
      controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      controlPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));

    revalidate();
  }

  private void linkComponentsToLockedState() {
    UiUtil.linkToEnabledState(conditionModel.getLockedObserver().getReversedObserver(),
            conditionTypeCombo, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
  }

  private void initializeConditionDialog(final Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null) {
      dialog = new JDialog(dlgParent, conditionModel.getColumnIdentifier().toString(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), conditionModel.getColumnIdentifier().toString(), false);
    }

    final JPanel conditionPanel = new JPanel(new BorderLayout());
    conditionPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(conditionPanel);
    dialog.pack();

    addAdvancedConditionListener(dialog::pack);

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        disableDialog();
      }
    });
  }
}
