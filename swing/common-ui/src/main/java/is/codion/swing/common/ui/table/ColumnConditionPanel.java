/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for ColumnConditionModel
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 */
public final class ColumnConditionPanel<C, T> extends JPanel {

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
  private final JPanel buttonPanel = new JPanel();
  private final JPanel controlPanel = new JPanel(new BorderLayout());
  private final JPanel inputPanel = new JPanel(new BorderLayout());

  private final Event<C> focusGainedEvent = Event.event();
  private final State advancedConditionState = State.state();

  private JDialog dialog;

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton) {
    this(conditionModel, toggleAdvancedButton, new DefaultBoundFieldFactory<>(conditionModel));
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldFactory the input field factory
   * @throws IllegalArgumentException in case operators is empty
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final BoundFieldFactory boundFieldFactory) {
    requireNonNull(conditionModel, "conditionModel");
    requireNonNull(boundFieldFactory, "boundFieldFactory");
    this.conditionModel = conditionModel;
    final boolean modelLocked = conditionModel.isLocked();
    conditionModel.setLocked(false);//otherwise, the validator checking the locked state kicks in during value linking
    this.equalField = boundFieldFactory.createEqualField();
    this.upperBoundField = boundFieldFactory.createUpperBoundField();
    this.lowerBoundField = boundFieldFactory.createLowerBoundField();
    this.operatorCombo = initializeOperatorComboBox(conditionModel.getOperators());
    this.toggleEnabledButton = radioButton(conditionModel.getEnabledState())
            .horizontalAlignment(CENTER)
            .build();
    this.toggleAdvancedButton = toggleAdvancedButton == ToggleAdvancedButton.YES ? toggleButton(advancedConditionState)
            .caption("...")
            .build() : null;
    conditionModel.setLocked(modelLocked);
    initializeUI();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(toggleEnabledButton, toggleAdvancedButton, operatorCombo,
            equalField, lowerBoundField, upperBoundField, controlPanel, inputPanel);
  }

  /**
   * @return the condition model this panel uses
   */
  public ColumnConditionModel<C, T> getModel() {
    return this.conditionModel;
  }

  /**
   * @return true if the dialog is enabled
   */
  public boolean isDialogEnabled() {
    return dialog != null;
  }

  /**
   * @return true if the dialog is being shown
   */
  public boolean isDialogVisible() {
    return dialog != null && dialog.isVisible();
  }

  /**
   * Displays this condition panel in a dialog
   * @param dialogParent the dialog parent
   * @param title the dialog title
   */
  public void enableDialog(final Container dialogParent, final String title) {
    if (!isDialogEnabled()) {
      initializeConditionDialog(dialogParent, title);
    }
  }

  /**
   * Displays this panel in a dialog
   * @param position the location, used if specified
   */
  public void showDialog(final Point position) {
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
  public void hideDialog() {
    if (isDialogVisible()) {
      dialog.setVisible(false);
      dialog.dispose();
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public JDialog getDialog() {
    return dialog;
  }

  /**
   * Requests keyboard focus for this panels input field
   */
  public void requestInputFocus() {
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
  public void setAdvanced(final boolean advanced) {
    advancedConditionState.set(advanced);
  }

  /**
   * @return true if the advanced condition is enabled
   */
  public boolean isAdvanced() {
    return advancedConditionState.get();
  }

  /**
   * @return the condition operator combo box
   */
  public JComboBox<Operator> getOperatorComboBox() {
    return operatorCombo;
  }

  /**
   * @return the JComponent used to specify the equal value
   */
  public JComponent getEqualField() {
    return equalField;
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public JComponent getUpperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public JComponent getLowerBoundField() {
    return lowerBoundField;
  }

  /**
   * @param listener a listener notified each time the advanced condition state changes
   */
  public void addAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.removeDataListener(listener);
  }

  /**
   * @param listener listener notified when this condition panels input fields receive focus
   */
  public void addFocusGainedListener(final EventDataListener<C> listener) {
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
        return checkBox((Value<Boolean>) value)
                .nullable(true)
                .horizontalAlignment(CENTER)
                .buildComponentValue().getComponent();
      }
      if (typeClass.equals(Integer.class)) {
        return integerField((Value<Integer>) value)
                .format(columnConditionModel.getFormat())
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(Double.class)) {
        return doubleField((Value<Double>) value)
                .format(columnConditionModel.getFormat())
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(BigDecimal.class)) {
        return bigDecimalField((Value<BigDecimal>) value)
                .format(columnConditionModel.getFormat())
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(Long.class)) {
        return longField((Value<Long>) value)
                .format(columnConditionModel.getFormat())
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalTime.class)) {
        return localTimeField(columnConditionModel.getDateTimePattern(), (Value<LocalTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalDate.class)) {
        return localDateField(columnConditionModel.getDateTimePattern(), (Value<LocalDate>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(LocalDateTime.class)) {
        return localDateTimeField(columnConditionModel.getDateTimePattern(), (Value<LocalDateTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(OffsetDateTime.class)) {
        return offsetDateTimeField(columnConditionModel.getDateTimePattern(), (Value<OffsetDateTime>) value)
                .buildComponentValue().getComponent();
      }
      else if (typeClass.equals(String.class)) {
        return textField((Value<String>) value)
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
    toggleEnabledButton.addFocusListener(focusGainedListener);
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
    repaint();
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
    setupButtonPanel();
    inputPanel.add(buttonPanel, BorderLayout.EAST);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
  }

  private void setAdvanced() {
    setupButtonPanel();
    controlPanel.add(buttonPanel, BorderLayout.EAST);
    add(controlPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
  }

  private void setupButtonPanel() {
    buttonPanel.setLayout(new GridLayout(1, toggleAdvancedButton == null ? 1 : 2));
    if (toggleAdvancedButton != null) {
      buttonPanel.add(toggleAdvancedButton);
      buttonPanel.add(toggleEnabledButton);
    }
    else {
      buttonPanel.add(toggleEnabledButton);
    }
  }

  private SteppedComboBox<Operator> initializeOperatorComboBox(final List<Operator> operators) {
    return Components.comboBox(new DefaultComboBoxModel<>(operators.toArray(new Operator[0])),
                    conditionModel.getOperatorValue())
            .completionMode(Completion.Mode.NONE)
            .renderer(new OperatorComboBoxRenderer())
            .font(UIManager.getFont("ComboBox.font").deriveFont(OPERATOR_FONT_SIZE))
            .mouseWheelScrolling(true)
            .componentOrientation(ComponentOrientation.RIGHT_TO_LEFT)
            .maximumRowCount(operators.size())
            .onBuild(comboBox -> addComponentListener(new OperatorBoxPopupWidthListener()))
            .build();
  }

  private void initializeUI() {
    Utilities.linkToEnabledState(conditionModel.getLockedObserver().getReversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
    setLayout(new BorderLayout());
    controlPanel.setBorder(BorderFactory.createEmptyBorder(2, 1, 1, 0));
    inputPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 0));
    toggleEnabledButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    if (toggleAdvancedButton != null) {
      toggleAdvancedButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
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

  private final class OperatorBoxPopupWidthListener extends ComponentAdapter {

    @Override
    public void componentResized(final ComponentEvent e) {
      operatorCombo.setPopupWidth(getWidth() - 1);
    }
  }

  private static final class OperatorComboBoxRenderer implements ListCellRenderer<Operator> {

    private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

    private OperatorComboBoxRenderer() {
      listCellRenderer.setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends Operator> list, final Operator value,
                                                  final int index, final boolean isSelected, final boolean cellHasFocus) {
      return listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }
}
