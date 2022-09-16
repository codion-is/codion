/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Conjunction;
import is.codion.common.event.EventListener;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A simple table condition panel, presenting a single text field for condition input.
 * For instances use the {@link #entityTableSimpleConditionPanel(EntityTableConditionModel, FilteredTableColumnModel, EventListener)} factory method.
 * @see #entityTableSimpleConditionPanel(EntityTableConditionModel, FilteredTableColumnModel, EventListener)
 */
public final class EntityTableSimpleConditionPanel extends AbstractEntityTableConditionPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableSimpleConditionPanel.class.getName());

  private final JTextField simpleSearchTextField;
  private final EventListener onSearchListener;
  private final Control searchControl;

  private EntityTableSimpleConditionPanel(EntityTableConditionModel tableConditionModel,
                                          FilteredTableColumnModel<?> columnModel,
                                          EventListener onSearchListener) {
    super(tableConditionModel, columnModel.columns());
    this.searchControl = Control.builder(this::performSimpleSearch)
            .caption(FrameworkMessages.search())
            .build();
    this.simpleSearchTextField = Components.textField(tableConditionModel.simpleConditionStringValue())
            .columns(12)
            .action(searchControl)
            .build();
    this.onSearchListener = requireNonNull(onSearchListener);
    setLayout(Layouts.borderLayout());
    add(createSimpleConditionPanel(), BorderLayout.CENTER);
  }

  /**
   * @return the search field
   */
  public JTextField simpleSearchTextField() {
    return simpleSearchTextField;
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param searchText the search text
   */
  public void setSearchText(String searchText) {
    simpleSearchTextField.setText(searchText);
  }

  @Override
  public void selectConditionPanel() {
    simpleSearchTextField.requestFocusInWindow();
  }

  /**
   * Performs the search, notifying the search listener.
   */
  public void performSearch() {
    performSimpleSearch();
  }

  /**
   * Instantiates a new {@link EntityTableSimpleConditionPanel}
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param onSearchListener notified when this condition panel triggers a search
   * @return a new {@link EntityTableSimpleConditionPanel}
   */
  public static EntityTableSimpleConditionPanel entityTableSimpleConditionPanel(EntityTableConditionModel tableConditionModel,
                                                                                FilteredTableColumnModel<?> columnModel,
                                                                                EventListener onSearchListener) {
    return new EntityTableSimpleConditionPanel(tableConditionModel, columnModel, onSearchListener);
  }

  private JPanel createSimpleConditionPanel() {
    return Components.panel(Layouts.borderLayout())
            .border(BorderFactory.createTitledBorder(MESSAGES.getString("condition")))
            .add(simpleSearchTextField, BorderLayout.WEST)
            .add(searchControl.createButton(), BorderLayout.EAST)
            .build();
  }

  private void performSimpleSearch() {
    Conjunction previousConjunction = tableConditionModel().getConjunction();
    try {
      tableConditionModel().setConjunction(Conjunction.OR);
      onSearchListener.onEvent();
    }
    finally {
      tableConditionModel().setConjunction(previousConjunction);
    }
  }
}
