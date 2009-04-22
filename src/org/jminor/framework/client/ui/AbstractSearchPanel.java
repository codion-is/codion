/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.AbstractSearchModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.model.Type;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: Björn Darri
 * Date: 26.12.2007
 * Time: 15:17:20
 */
public abstract class AbstractSearchPanel extends JPanel {

  public final State stAdvancedSearch = new State("AbstractSearchPanel.stAdvancedSearch");

  public final State stTwoSearchFields = new State("AbstractSearchPanel.stTwoSearchFields", false);

  protected static final SearchType[] searchTypes = new SearchType[] {
          SearchType.LIKE, SearchType.NOT_LIKE, SearchType.MAX, SearchType.MIN,
          SearchType.INSIDE, SearchType.OUTSIDE};

  protected static final String[] searchTypeImageNames = new String[] {
          "Equals60x16.gif", "NotEquals60x16.gif", "LessThanOrEquals60x16.gif",
          "LargerThanOrEquals60x16.gif", "Inclusive60x16.gif", "Exclusive60x16.gif"};

  /**
   * The AbstractSearchModel this AbstractSearchPanel represents
   */
  protected final AbstractSearchModel model;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  protected final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  protected final JToggleButton toggleSearchAdvanced;

  /**
   * A JComboBox for selecting the search type
   */
  protected final JComboBox searchTypeCombo;
  protected final BooleanComboBoxModel upperBooleanComboBoxModel;
  protected final BooleanComboBoxModel lowerBooleanComboBoxModel;
  protected final JComponent upperField;
  protected final JComponent lowerField;

  private final boolean includeToggleSearchEnabledBtn;
  private final boolean includeToggleSearchAdvancedBtn;

  public AbstractSearchPanel(final AbstractSearchModel model, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    this.model = model;
    this.includeToggleSearchEnabledBtn = includeActivateBtn;
    this.includeToggleSearchAdvancedBtn = includeToggleAdvBtn;
    this.upperBooleanComboBoxModel = model.getPropertyType() == Type.BOOLEAN ? new BooleanComboBoxModel() : null;
    this.lowerBooleanComboBoxModel = model.getPropertyType() == Type.BOOLEAN ? new BooleanComboBoxModel() : null;
    this.searchTypeCombo = initSearchTypeComboBox();
    this.upperField = getInputField(true);
    this.lowerField = isLowerFieldRequired(model.getPropertyType()) ? getInputField(false) : null;

    this.toggleSearchEnabled = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(model, "searchEnabled", null, model.getSearchStateChangedEvent()));
    toggleSearchEnabled.setIcon(Images.loadImage("Filter16.gif"));
    this.toggleSearchAdvanced = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.evtStateChanged));
    toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    linkComponentsToLockState();
    initUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the search model this panel uses
   */
  public AbstractSearchModel getModel() {
    return this.model;
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public void setAdvancedSearchOn(final boolean value) {
    stAdvancedSearch.setActive(value);
  }

  /**
   * @return true if the advanced search is enabled
   */
  public boolean isAdvancedSearchOn() {
    return stAdvancedSearch.isActive();
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public JComponent getUpperField() {
    return upperField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public JComponent getLowerField() {
    return lowerField;
  }

  /**
   * Binds events to relevant GUI actions
   */
  protected void bindEvents() {
    stAdvancedSearch.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        initializePanel();
        if (toggleSearchAdvanced != null)
          toggleSearchAdvanced.requestFocusInWindow();
        else
          upperField.requestFocusInWindow();
      }
    });
    model.evtSearchTypeChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stTwoSearchFields.setActive(model.getSearchType() == SearchType.INSIDE
                || model.getSearchType() == SearchType.OUTSIDE);
      }
    });
    stTwoSearchFields.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        initializePanel();
        revalidate();
        searchTypeCombo.requestFocusInWindow();
      }
    });
  }

  protected void initializePanel() {
    if (stAdvancedSearch.isActive())
      initAdvancedPanel();
    else
      initSimplePanel();
  }

  /**
   * @return an ItemComboBoxModel containing the available search types
   */
  protected ItemComboBoxModel initSearchTypeModel() {
    final ItemComboBoxModel ret = new ItemComboBoxModel();
    for (int i = 0; i < searchTypes.length; i++)
      if (searchTypeAllowed(searchTypes[i]))
        ret.addElement(new ItemComboBoxModel.IconItem(searchTypes[i],Images.loadImage(searchTypeImageNames[i])));

    return ret;
  }

  /**
   * @param searchType the search type
   * @return true if the given search type is allowed given the underlying property
   */
  protected abstract boolean searchTypeAllowed(final SearchType searchType);

  /**
   * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
   * @return an input field for either the upper or lower bound
   */
  protected abstract JComponent getInputField(final boolean isUpperBound);

  /**
   * @param type the Type
   * @return true if a lower bound field is required given the data type
   */
  protected abstract boolean isLowerFieldRequired(final Type type);

  private JComboBox initSearchTypeComboBox() {
    final JComboBox ret = new SteppedComboBox(initSearchTypeModel());
    ControlProvider.bindItemSelector(ret, model, "searchType", SearchType.class, model.evtSearchTypeChanged);

    return ret;
  }

  private void initUI() {
    setLayout(new FlexibleGridLayout(2,1,1,1,true,false));
    ((FlexibleGridLayout)getLayout()).setFixedRowHeight(new JTextField().getSize().height);

    this.toggleSearchEnabled.setPreferredSize(new Dimension(20,20));
    this.toggleSearchAdvanced.setPreferredSize(new Dimension(20,20));
  }

  private void initSimplePanel() {
    removeAll();
    ((FlexibleGridLayout)getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1,1));
    if (stTwoSearchFields.isActive()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1,2,1,1));
      fieldBase.add(upperField);
      fieldBase.add(lowerField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      basePanel.add(upperField, BorderLayout.CENTER);

    if (includeToggleSearchEnabledBtn)
      basePanel.add(toggleSearchEnabled, BorderLayout.EAST);
    if (includeToggleSearchAdvancedBtn)
      basePanel.add(toggleSearchAdvanced, BorderLayout.WEST);

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
      fieldBase.add(lowerField);
      fieldBase.add(upperField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      inputPanel.add(upperField, BorderLayout.CENTER);

    final JPanel controlPanel = new JPanel(new BorderLayout(1,1));
    controlPanel.add(searchTypeCombo, BorderLayout.CENTER);
    if (includeToggleSearchEnabledBtn)
      controlPanel.add(toggleSearchEnabled, BorderLayout.EAST);
    if (includeToggleSearchAdvancedBtn)
      controlPanel.add(toggleSearchAdvanced, BorderLayout.WEST);

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()*2));

    revalidate();
  }

  private void linkComponentsToLockState() {
    final State stUnlocked = model.stLocked.getReversedState();
    UiUtil.linkToEnabledState(stUnlocked, searchTypeCombo);
    UiUtil.linkToEnabledState(stUnlocked, upperField);
    if (lowerField != null)
      UiUtil.linkToEnabledState(stUnlocked, lowerField);
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchAdvanced);
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchEnabled);
  }
}
