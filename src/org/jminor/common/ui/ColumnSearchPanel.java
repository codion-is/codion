/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.DateBeanValueLink;
import org.jminor.common.ui.control.DoubleBeanValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.common.ui.control.TimestampBeanValueLink;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;

/**
 * A UI implementation for ColumnSearchModel
 */
public class ColumnSearchPanel<K> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The SearchModel this AbstractSearchPanel represents
   */
  private final ColumnSearchModel<K> model;

  /**
   * The search types allowed in this model
   */
  private final Collection<SearchType> searchTypes;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleSearchAdvanced;
  private final State stIsDialogActive = States.state();

  private final State stIsDialogShowing = States.state();
  private JDialog dialog;

  private Point lastPosition;
  /**
   * A JComboBox for selecting the search type
   */
  private final JComboBox searchTypeCombo;
  private final JComponent upperBoundField;

  private final JComponent lowerBoundField;
  private final State stAdvancedSearch = States.state();

  private final State stTwoSearchFields = States.state();
  private final boolean includeToggleSearchEnabledBtn;
  private final boolean includeToggleSearchAdvancedBtn;

  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    this(searchModel, includeActivateBtn, includeToggleAdvBtn, SearchType.values());
  }

  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn,
                           final SearchType... searchTypes) {
    this(searchModel, includeActivateBtn, includeToggleAdvBtn, new DefaultInputFieldProvider<K>(searchModel), searchTypes);
  }

  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn,
                           final InputFieldProvider inputFieldProvider, final SearchType... searchTypes) {
    this(searchModel, includeActivateBtn, includeToggleAdvBtn, inputFieldProvider.initializeInputField(true),
            inputFieldProvider.initializeInputField(false), searchTypes);
  }

  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn,
                           final JComponent upperBoundField, final JComponent lowerBoundField,
                           final SearchType... searchTypes) {
    Util.rejectNullValue(searchModel, "searchModel");
    this.model = searchModel;
    this.includeToggleSearchEnabledBtn = includeActivateBtn;
    this.includeToggleSearchAdvancedBtn = includeToggleAdvBtn;
    this.searchTypes = searchTypes == null ? Arrays.asList(SearchType.values()) : Arrays.asList(searchTypes);
    this.searchTypeCombo = initSearchTypeComboBox();
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    this.toggleSearchEnabled = ControlProvider.createToggleButton(
            Controls.toggleControl(searchModel, "enabled", null, searchModel.getEnabledObserver()));
    toggleSearchEnabled.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    this.toggleSearchAdvanced = ControlProvider.createToggleButton(
            Controls.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.getObserver()));
    toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    linkComponentsToLockedState();
    initUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the search model this panel uses
   */
  public final ColumnSearchModel<K> getModel() {
    return this.model;
  }

  /**
   * @return the last screen position
   */
  public final Point getLastPosition() {
    return lastPosition;
  }

  /**
   * @return true if the dialog is active
   */
  public final boolean isDialogActive() {
    return stIsDialogActive.isActive();
  }

  /**
   * @return true if the dialog is being shown
   */
  public final boolean isDialogShowing() {
    return stIsDialogShowing.isActive();
  }

  public final void activateDialog(final Container dialogParent, final Point position) {
    if (!isDialogActive()) {
      initSearchDlg(dialogParent);
      Point actualPosition = position;
      if (position == null) {
        actualPosition = lastPosition;
      }
      if (actualPosition == null) {
        actualPosition = new Point(0, 0);
      }

      actualPosition.y = actualPosition.y - dialog.getHeight();
      dialog.setLocation(actualPosition);
      stIsDialogActive.setActive(true);
    }

    showDialog();
  }

  public final void inactivateDialog() {
    if (isDialogActive()) {
      if (isDialogShowing()) {
        hideDialog();
      }
      lastPosition = dialog.getLocation();
      lastPosition.y = lastPosition.y + dialog.getHeight();
      dialog.dispose();
      dialog = null;

      stIsDialogActive.setActive(false);
    }
  }

  public final void showDialog() {
    if (isDialogActive() && !isDialogShowing()) {
      dialog.setVisible(true);
      upperBoundField.requestFocusInWindow();
      stIsDialogShowing.setActive(true);
    }
  }

  public final void hideDialog() {
    if (isDialogShowing()) {
      dialog.setVisible(false);
      stIsDialogShowing.setActive(false);
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public final JDialog getDialog() {
    return dialog;
  }

  public final StateObserver getDialogActiveState() {
    return stIsDialogActive.getObserver();
  }

  public final StateObserver getDialogShowingState() {
    return stIsDialogShowing.getObserver();
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public final void setAdvancedSearchOn(final boolean value) {
    stAdvancedSearch.setActive(value);
  }

  /**
   * @return true if the advanced search is enabled
   */
  public final boolean isAdvancedSearchOn() {
    return stAdvancedSearch.isActive();
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

  public final StateObserver getAdvancedSearchState() {
    return stAdvancedSearch.getObserver();
  }

  public final StateObserver getTwoSearchFieldsState() {
    return stTwoSearchFields.getObserver();
  }

  public interface InputFieldProvider<K> {

    ColumnSearchModel<K> getSearchModel();

    JComponent initializeInputField(final boolean isUpperBound);
  }

  private static final class DefaultInputFieldProvider<K> implements InputFieldProvider<K> {

    private final ColumnSearchModel<K> model;

    private DefaultInputFieldProvider(final ColumnSearchModel<K> model) {
      this.model = model;
    }

    public ColumnSearchModel<K> getSearchModel() {
      return model;
    }

    /**
     * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
     * @return an input field for either the upper or lower bound
     */
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initField();
      if (model.getType() == Types.BOOLEAN) {
        createToggleProperty((JCheckBox) field, isUpperBound);
      }
      else {
        createTextProperty(field, isUpperBound);
      }

      if (field instanceof JTextField) {//enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            model.setEnabled(!model.isEnabled());
          }
        });
      }
      field.setToolTipText(isUpperBound ? "a" : "b");

      return field;
    }

    private JComponent initField() {
      if (model.getType() == Types.DATE || model.getType() == Types.TIMESTAMP) {
        return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) model.getFormat()));
      }
      else if (model.getType() == Types.DOUBLE) {
        return new DoubleField(DEFAULT_FIELD_COLUMNS);
      }
      else if (model.getType() == Types.INTEGER) {
        return new IntField(DEFAULT_FIELD_COLUMNS);
      }
      else if (model.getType() == Types.BOOLEAN) {
        return new JCheckBox();
      }
      else {
        return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
      new ToggleBeanValueLink(checkBox.getModel(), model,
              isUpperBound ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
              isUpperBound ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
    }

    private TextBeanValueLink createTextProperty(final JComponent component, final boolean isUpper) {
      if (model.getType() == Types.INTEGER) {
        return new IntBeanValueLink((IntField) component, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      if (model.getType() == Types.DOUBLE) {
        return new DoubleBeanValueLink((DoubleField) component, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      if (model.getType() == Types.DATE) {
        return new DateBeanValueLink((JFormattedTextField) component, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(),
                LinkType.READ_WRITE, (DateFormat) model.getFormat());
      }
      if (model.getType() == Types.TIMESTAMP) {
        return new TimestampBeanValueLink((JFormattedTextField) component, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(),
                LinkType.READ_WRITE, (DateFormat) model.getFormat());
      }

      return new TextBeanValueLink((JTextField) component, model,
              isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
              String.class, isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    stAdvancedSearch.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        initializePanel();
        if (toggleSearchAdvanced != null) {
          toggleSearchAdvanced.requestFocusInWindow();
        }
        else {
          upperBoundField.requestFocusInWindow();
        }
      }
    });
    model.addSearchTypeListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stTwoSearchFields.setActive(model.getSearchType() == SearchType.WITHIN_RANGE
                || model.getSearchType() == SearchType.OUTSIDE_RANGE);
      }
    });
    stTwoSearchFields.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        initializePanel();
        revalidate();
        searchTypeCombo.requestFocusInWindow();
      }
    });
  }

  private void initializePanel() {
    if (stAdvancedSearch.isActive()) {
      initAdvancedPanel();
    }
    else {
      initSimplePanel();
    }
  }

  private JComboBox initSearchTypeComboBox() {
    final ItemComboBoxModel comboBoxModel = new ItemComboBoxModel();
    for (final SearchType type : SearchType.values()) {
      if (searchTypes.contains(type)) {
        comboBoxModel.addElement(new ItemComboBoxModel.IconItem<SearchType>(type, Images.loadImage(type.getImageName())));
      }
    }
    final JComboBox comboBox = new SteppedComboBox(comboBoxModel);
    ControlProvider.bindItemSelector(comboBox, model, "searchType", SearchType.class, model.getSearchTypeObserver());

    return comboBox;
  }

  private void initUI() {
    setLayout(new FlexibleGridLayout(2,1,1,1,true,false));
    ((FlexibleGridLayout)getLayout()).setFixedRowHeight(new JTextField().getSize().height);

    this.toggleSearchEnabled.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    this.toggleSearchAdvanced.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
  }

  private void initSimplePanel() {
    removeAll();
    ((FlexibleGridLayout)getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1,1));
    if (stTwoSearchFields.isActive()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1,2,1,1));
      fieldBase.add(upperBoundField);
      fieldBase.add(lowerBoundField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      basePanel.add(upperBoundField, BorderLayout.CENTER);
    }

    if (includeToggleSearchEnabledBtn) {
      basePanel.add(toggleSearchEnabled, BorderLayout.EAST);
    }
    if (includeToggleSearchAdvancedBtn) {
      basePanel.add(toggleSearchAdvanced, BorderLayout.WEST);
    }

    add(basePanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));

    revalidate();
  }

  private void initAdvancedPanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(2);
    final JPanel inputPanel = new JPanel(new BorderLayout(1,1));
    if (stTwoSearchFields.isActive()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1,2,1,1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      inputPanel.add(upperBoundField, BorderLayout.CENTER);
    }

    final JPanel controlPanel = new JPanel(new BorderLayout(1,1));
    controlPanel.add(searchTypeCombo, BorderLayout.CENTER);
    if (includeToggleSearchEnabledBtn) {
      controlPanel.add(toggleSearchEnabled, BorderLayout.EAST);
    }
    if (includeToggleSearchAdvancedBtn) {
      controlPanel.add(toggleSearchAdvanced, BorderLayout.WEST);
    }

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight() * 2));

    revalidate();
  }

  private void linkComponentsToLockedState() {
    final StateObserver stUnlocked = model.getLockedState().getReversedState();
    UiUtil.linkToEnabledState(stUnlocked, searchTypeCombo);
    UiUtil.linkToEnabledState(stUnlocked, upperBoundField);
    if (lowerBoundField != null) {
      UiUtil.linkToEnabledState(stUnlocked, lowerBoundField);
    }
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchAdvanced);
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchEnabled);
  }

  private void initSearchDlg(final Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null) {
      dialog = new JDialog(dlgParent, model.getColumnIdentifier().toString(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), model.getColumnIdentifier().toString(), false);
    }

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(searchPanel);
    dialog.pack();

    getAdvancedSearchState().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        dialog.pack();
      }
    });

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        inactivateDialog();
      }
    });
  }
}
