/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
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
import java.text.Format;
import java.text.SimpleDateFormat;

/**
 * An abstract panel for showing search/filter configuration.<br>
 * User: Bjorn Darri<br>
 * Date: 26.12.2007<br>
 * Time: 15:17:20<br>
 */
public abstract class AbstractSearchPanel<K> extends JPanel {

  private static final SearchType[] SEARCH_TYPES = {
          SearchType.LIKE, SearchType.NOT_LIKE, SearchType.AT_LEAST,
          SearchType.AT_MOST, SearchType.WITHIN_RANGE, SearchType.OUTSIDE_RANGE};

  private static final String[] SEARCH_TYPE_IMAGES = {
          "Equals60x16.gif", "NotEquals60x16.gif", "LessThanOrEquals60x16.gif",
          "LargerThanOrEquals60x16.gif", "Inclusive60x16.gif", "Exclusive60x16.gif"};

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The SearchModel this AbstractSearchPanel represents
   */
  private final SearchModel<K> model;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleSearchAdvanced;
  private final State stIsDialogActive = new State();

  private final State stIsDialogShowing = new State();
  private JDialog dialog;

  private Point lastPosition;
  /**
   * A JComboBox for selecting the search type
   */
  private final JComboBox searchTypeCombo;
  private final JComponent upperBoundField;

  private final JComponent lowerBoundField;
  private final State stAdvancedSearch = new State();

  private final State stTwoSearchFields = new State();
  private final boolean includeToggleSearchEnabledBtn;
  private final boolean includeToggleSearchAdvancedBtn;

  public AbstractSearchPanel(final SearchModel<K> searchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    Util.rejectNullValue(searchModel, "searchModel");
    this.model = searchModel;
    this.includeToggleSearchEnabledBtn = includeActivateBtn;
    this.includeToggleSearchAdvancedBtn = includeToggleAdvBtn;
    this.searchTypeCombo = initSearchTypeComboBox();
    this.upperBoundField = getInputField(true);
    this.lowerBoundField = isLowerBoundFieldRequired(searchModel.getSearchKey()) ? getInputField(false) : null;

    this.toggleSearchEnabled = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(searchModel, "searchEnabled", null, searchModel.eventEnabledChanged()));
    toggleSearchEnabled.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    this.toggleSearchAdvanced = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.eventStateChanged()));
    toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    linkComponentsToLockedState();
    initUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the search model this panel uses
   */
  public final SearchModel<K> getModel() {
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
      getUpperBoundField().requestFocusInWindow();
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

  public final State stateIsDialogActive() {
    return stIsDialogActive.getLinkedState();
  }

  public final State stateIsDialogShowing() {
    return stIsDialogShowing.getLinkedState();
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

  public final State stateAdvancedSearch() {
    return stAdvancedSearch.getLinkedState();
  }

  public final State stateTwoSearchFields() {
    return stTwoSearchFields.getLinkedState();
  }

  /**
   * @param searchType the search type
   * @return true if the given search type is allowed given the underlying property
   */
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return true;
  }

  /**
   * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
   * @return an input field for either the upper or lower bound
   */
  protected JComponent getInputField(final boolean isUpperBound) {
    final DateFormat format = getDateFormat();
    final JComponent field = initField(format);
    if (getModel().getType() == Types.BOOLEAN) {
      createToggleProperty((JCheckBox) field, isUpperBound);
    }
    else {
      createTextProperty(field, isUpperBound, format);
    }

    if (field instanceof JTextField) {//enter button toggles the filter on/off
      ((JTextField) field).addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          getModel().setSearchEnabled(!getModel().isSearchEnabled());
        }
      });
    }
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  /**
   * @param property the Property
   * @return true if a lower bound field is required given the property
   */
  protected abstract boolean isLowerBoundFieldRequired(final K property);

  /**
   * @return the Format object to use when formatting input, is any
   */
  protected abstract DateFormat getDateFormat();

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    stAdvancedSearch.eventStateChanged().addListener(new ActionListener() {
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
    model.eventSearchTypeChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stTwoSearchFields.setActive(model.getSearchType() == SearchType.WITHIN_RANGE
                || model.getSearchType() == SearchType.OUTSIDE_RANGE);
      }
    });
    stTwoSearchFields.eventStateChanged().addListener(new ActionListener() {
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

  /**
   * @return an ItemComboBoxModel containing the available search types
   */
  private ItemComboBoxModel initSearchTypeModel() {
    final ItemComboBoxModel comboBoxModel = new ItemComboBoxModel();
    for (int i = 0; i < SEARCH_TYPES.length; i++) {
      if (searchTypeAllowed(SEARCH_TYPES[i])) {
        comboBoxModel.addElement(new ItemComboBoxModel.IconItem<SearchType>(SEARCH_TYPES[i], Images.loadImage(SEARCH_TYPE_IMAGES[i])));
      }
    }

    return comboBoxModel;
  }

  private JComboBox initSearchTypeComboBox() {
    final JComboBox comboBox = new SteppedComboBox(initSearchTypeModel());
    ControlProvider.bindItemSelector(comboBox, model, "searchType", SearchType.class, model.eventSearchTypeChanged());

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
    ((FlexibleGridLayout)getLayout()).setRows(2);
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
    final State stUnlocked = model.stateLocked().getReversedState();
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
      dialog = new JDialog(dlgParent, getModel().getSearchKey().toString(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), getModel().getSearchKey().toString(), false);
    }

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(searchPanel);
    dialog.pack();

    stateAdvancedSearch().eventStateChanged().addListener(new ActionListener() {
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

  private JComponent initField(final Format format) {
    if (getModel().getType() == Types.DATE || getModel().getType() == Types.TIMESTAMP) {
      return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) format));
    }
    else if (getModel().getType() == Types.DOUBLE) {
      return new DoubleField(4);
    }
    else if (getModel().getType() == Types.INTEGER) {
      return new IntField(4);
    }
    else if (getModel().getType() == Types.BOOLEAN) {
      return new JCheckBox();
    }
    else {
      return new JTextField(4);
    }
  }

  private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
    new ToggleBeanValueLink(checkBox.getModel(), getModel(),
            isUpperBound ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
            isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
  }

  private TextBeanValueLink createTextProperty(final JComponent component, final boolean isUpper, final DateFormat format) {
    if (getModel().getType() == Types.INTEGER) {
      return new IntBeanValueLink((IntField) component, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    if (getModel().getType() == Types.DOUBLE) {
      return new DoubleBeanValueLink((DoubleField) component, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    if (getModel().getType() == Types.DATE) {
      return new DateBeanValueLink((JFormattedTextField) component, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(),
              LinkType.READ_WRITE, format);
    }
    if (getModel().getType() == Types.TIMESTAMP) {
      return new TimestampBeanValueLink((JFormattedTextField) component, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(),
              LinkType.READ_WRITE, format);
    }

    return new TextBeanValueLink((JTextField) component, getModel(),
            isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
            String.class, isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
  }
}
