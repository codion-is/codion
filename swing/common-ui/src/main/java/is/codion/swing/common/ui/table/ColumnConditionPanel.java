/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.component.ComponentBuilders.*;
import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for ColumnConditionModel
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 */
public class ColumnConditionPanel<C, T> extends JPanel {

  /**
   * Specifies whether a condition panel should include
   * a button for toggling advanced mode.
   */
  public enum ToggleAdvancedButton {
    /**
     * Include a button for toggling advancded mode.
     */
    YES,
    /**
     * Don't include a button for toggling advancded mode.
     */
    NO
  }

  private static final float OPERATOR_FONT_SIZE = 18f;

  private final ColumnConditionModel<C, T> conditionModel;
  private final JToggleButton toggleEnabledButton;
  private final JToggleButton toggleAdvancedButton;
  private final SteppedComboBox<Operator> operatorCombo;
  private final JComponent equalField;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;
  private final JPanel controlPanel = new JPanel(new BorderLayout());
  private final JPanel inputPanel = new JPanel(new BorderLayout());

  private final Event<C> focusGainedEvent = Event.event();
  private final State advancedConditionState = State.state();

  private JDialog dialog;

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory and all available Operators.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton) {
    this(conditionModel, toggleAdvancedButton, Arrays.asList(Operator.values()));
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param operators the operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final List<Operator> operators) {
    this(conditionModel, toggleAdvancedButton, new DefaultBoundFieldFactory<>(conditionModel), operators);
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldFactory the input field factory
   * @param operators the search operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final BoundFieldFactory boundFieldFactory, final List<Operator> operators) {
    requireNonNull(conditionModel, "conditionModel");
    if (requireNonNull(operators, "operators").isEmpty()) {
      throw new IllegalArgumentException("One or more operators must be specified");
    }
    this.conditionModel = conditionModel;
    final boolean modelLocked = conditionModel.isLocked();
    conditionModel.setLocked(false);//otherwise, the validator checking the locked state kicks in during value linking
    this.equalField = boundFieldFactory.createEqualField();
    this.upperBoundField = boundFieldFactory.createUpperBoundField();
    this.lowerBoundField = boundFieldFactory.createLowerBoundField();
    this.operatorCombo = initializeOperatorComboBox(operators);
    this.toggleEnabledButton = ToggleControl.builder(conditionModel.getEnabledState())
            .icon(icons().filter())
            .build().createToggleButton();
    this.toggleAdvancedButton = toggleAdvancedButton == ToggleAdvancedButton.YES ? ToggleControl.builder(advancedConditionState)
            .icon(icons().configure())
            .build().createToggleButton() : null;
    conditionModel.setLocked(modelLocked);
    initializeUI();
    bindEvents();
  }

  @Override
  public final void updateUI() {
    super.updateUI();
    Components.updateUI(toggleEnabledButton, toggleAdvancedButton, operatorCombo,
            equalField, lowerBoundField, upperBoundField, controlPanel, inputPanel);
  }

  /**
   * @return the condition model this panel uses
   */
  public final ColumnConditionModel<C, T> getModel() {
    return this.conditionModel;
  }

  /**
   * @return true if the dialog is enabled
   */
  public final boolean isDialogEnabled() {
    return dialog != null;
  }

  /**
   * @return true if the dialog is being shown
   */
  public final boolean isDialogVisible() {
    return dialog != null && dialog.isVisible();
  }

  /**
   * Displays this condition panel in a dialog
   * @param dialogParent the dialog parent
   * @param title the dialog title
   */
  public final void enableDialog(final Container dialogParent, final String title) {
    if (!isDialogEnabled()) {
      initializeConditionDialog(dialogParent, title);
    }
  }

  /**
   * Displays this panel in a dialog
   * @param position the location, used if specified
   */
  public final void showDialog(final Point position) {
    if (!isDialogEnabled()) {
      throw new IllegalStateException("Dialog has not been enabled for this condition panel");
    }
    if (!isDialogVisible()) {
      if (position != null) {
        final Point adjustedPosition = position;
        adjustedPosition.y = adjustedPosition.y - dialog.getHeight();
        dialog.setLocation(adjustedPosition);
      }
      dialog.setVisible(true);
      requestInputFocus();
    }
  }

  /**
   * Hides the dialog showing this panel if visible
   */
  public final void hideDialog() {
    if (isDialogVisible()) {
      dialog.setVisible(false);
      dialog.dispose();
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
    switch (conditionModel.getOperator()) {
      case EQUAL:
      case NOT_EQUAL:
        equalField.requestFocusInWindow();
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        lowerBoundField.requestFocusInWindow();
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
        upperBoundField.requestFocusInWindow();
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
  }

  /**
   * @param advanced true if advanced condition should be enabled
   */
  public final void setAdvanced(final boolean advanced) {
    advancedConditionState.set(advanced);
  }

  /**
   * @return true if the advanced condition is enabled
   */
  public final boolean isAdvanced() {
    return advancedConditionState.get();
  }

  /**
   * @return the condition operator combo box
   */
  public final JComboBox<Operator> getOperatorComboBox() {
    return operatorCombo;
  }

  /**
   * @return the JComponent used to specify the equal value
   */
  public final JComponent getEqualField() {
    return equalField;
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
  public final void addAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.removeDataListener(listener);
  }

  /**
   * @param listener listener notified when this condition panels input fields receive focus
   */
  public final void addFocusGainedListener(final EventDataListener<C> listener) {
    focusGainedEvent.addDataListener(listener);
  }

  /**
   * Provides an equal, upper and lower bound input fields for a ColumnConditionPanel
   */
  public interface BoundFieldFactory {

    /**
     * @return the equal value field
     */
    JComponent createEqualField();

    /**
     * @return an upper bound input field
     */
    JComponent createUpperBoundField();

    /**
     * @return a lower bound input field
     */
    JComponent createLowerBoundField();
  }

  private static final class DefaultBoundFieldFactory<T> implements BoundFieldFactory {

    private final ColumnConditionModel<?, T> columnConditionModel;

    private DefaultBoundFieldFactory(final ColumnConditionModel<?, T> columnConditionModel) {
      this.columnConditionModel = requireNonNull(columnConditionModel, "columnConditionModel");
    }

    public JComponent createEqualField() {
      return createField(columnConditionModel.getEqualValueSet().value());
    }

    @Override
    public JComponent createUpperBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return createField(columnConditionModel.getUpperBoundValue());
    }

    @Override
    public JComponent createLowerBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return createField(columnConditionModel.getLowerBoundValue());
    }

    private JComponent createField(final Value<?> value) {
      final Class<?> typeClass = columnConditionModel.getTypeClass();
      if (typeClass.equals(Boolean.class)) {
        return checkBox()
                .nullable(true)
                .horizontalAlignment(CENTER)
                .linkedValue((Value<Boolean>) value)
                .buildComponentValue().getComponent();
      }
      if (typeClass.equals(Integer.class)) {
        return integerField()
                .format(columnConditionModel.getFormat())
                .linkedValue((Value<Integer>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(Double.class)) {
        return doubleField()
                .format(columnConditionModel.getFormat())
                .linkedValue((Value<Double>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(BigDecimal.class)) {
        return bigDecimalField()
                .format(columnConditionModel.getFormat())
                .linkedValue((Value<BigDecimal>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(Long.class)) {
        return longField()
                .format(columnConditionModel.getFormat())
                .linkedValue((Value<Long>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalTime.class)) {
        return localTimeField(columnConditionModel.getDateTimePattern())
                .linkedValue((Value<LocalTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalDate.class)) {
        return localDateField(columnConditionModel.getDateTimePattern())
                .linkedValue((Value<LocalDate>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalDateTime.class)) {
        return localDateTimeField(columnConditionModel.getDateTimePattern())
                .linkedValue((Value<LocalDateTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(OffsetDateTime.class)) {
        return offsetDateTimeField(columnConditionModel.getDateTimePattern())
                .linkedValue((Value<OffsetDateTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(String.class)) {
        return textField()
                .linkedValue((Value<String>) value)
                .buildComponentValue().getComponent();
      }

      throw new IllegalArgumentException("Unsupported type: " + typeClass);
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedConditionState.addDataListener(this::onAdvancedChange);
    conditionModel.getOperatorValue().addDataListener(this::onOperatorChanged);
    final FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.getColumnIdentifier());
        }
      }
    };
    operatorCombo.addFocusListener(focusGainedListener);
    if (equalField != null) {
      equalField.addFocusListener(focusGainedListener);
    }
    if (upperBoundField != null) {
      upperBoundField.addFocusListener(focusGainedListener);
    }
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

  private void onOperatorChanged(final Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
        singleValuePanel(equalField);
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
        singleValuePanel(lowerBoundField);
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
        singleValuePanel(upperBoundField);
        break;
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        rangePanel(lowerBoundField, upperBoundField);
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
    revalidate();
  }

  private void onAdvancedChange(final boolean advanced) {
    if (advanced) {
      setAdvanced();
    }
    else {
      setSimple();
    }
  }

  private void setSimple() {
    remove(controlPanel);
    if (toggleEnabledButton != null) {
      controlPanel.remove(toggleEnabledButton);
      inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      controlPanel.remove(toggleAdvancedButton);
      inputPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
  }

  private void setAdvanced() {
    if (toggleEnabledButton != null) {
      inputPanel.remove(toggleEnabledButton);
      controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      inputPanel.remove(toggleAdvancedButton);
      controlPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }
    add(controlPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
  }

  private SteppedComboBox<Operator> initializeOperatorComboBox(final List<Operator> operators) {
    final DefaultComboBoxModel<Operator> comboBoxModel = new DefaultComboBoxModel<>();
    Arrays.stream(Operator.values()).filter(operators::contains).forEach(comboBoxModel::addElement);
    final SteppedComboBox<Operator> comboBox = new SteppedComboBox<>(comboBoxModel);
    Components.setPreferredHeight(comboBox, TextFields.getPreferredTextFieldHeight());
    ComponentValues.comboBox(comboBox).link(conditionModel.getOperatorValue());
    comboBox.setRenderer(new OperatorComboBoxRenderer());
    comboBox.setFont(comboBox.getFont().deriveFont(OPERATOR_FONT_SIZE));

    return comboBox;
  }

  private void initializeUI() {
    Components.linkToEnabledState(conditionModel.getLockedObserver().getReversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
    setLayout(new BorderLayout());
    if (toggleEnabledButton != null) {
      this.toggleEnabledButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    }
    if (toggleAdvancedButton != null) {
      this.toggleAdvancedButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    }
    controlPanel.add(operatorCombo, BorderLayout.CENTER);
    onOperatorChanged(conditionModel.getOperator());
    onAdvancedChange(advancedConditionState.get());
  }

  private void initializeConditionDialog(final Container parent, final String title) {
    if (dialog != null) {
      return;
    }

    final JDialog dialogParent = Windows.getParentDialog(parent);
    if (dialogParent != null) {
      dialog = new JDialog(dialogParent, title, false);
    }
    else {
      dialog = new JDialog(Windows.getParentFrame(parent), title, false);
    }

    dialog.getContentPane().add(this);
    dialog.pack();

    addAdvancedListener(advanced -> dialog.pack());
  }

  private void singleValuePanel(final JComponent boundField) {
    inputPanel.removeAll();
    inputPanel.add(boundField, BorderLayout.CENTER);
  }

  private void rangePanel(final JComponent lowerBoundField, final JComponent upperBoundField) {
    final JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(lowerBoundField);
    panel.add(upperBoundField);
    inputPanel.removeAll();
    inputPanel.add(panel, BorderLayout.CENTER);
  }

  private static final class OperatorComboBoxRenderer extends BasicComboBoxRenderer {

    private OperatorComboBoxRenderer() {
      setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                  final boolean isSelected, final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setToolTipText(((Operator) value).getDescription());

      return this;
    }
  }
}
