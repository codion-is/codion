package is.codion.swing.framework.ui;

import is.codion.common.Conjunction;
import is.codion.common.event.EventListener;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.component.ComponentValues;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A simple table condition panel, presenting a single text field for condition input.
 */
public final class EntityTableSimpleConditionPanel extends AbstractEntityTableConditionPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableSimpleConditionPanel.class.getName());

  private final JTextField simpleSearchTextField;
  private final EventListener onSearchListener;
  private final Control searchControl;

  /**
   * Instantiates a new EntityTableSimpleConditionPanel
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param onSearchListener notified when this condition panel triggers a search
   */
  public EntityTableSimpleConditionPanel(final EntityTableConditionModel tableConditionModel,
                                         final SwingFilteredTableColumnModel<?> columnModel,
                                         final EventListener onSearchListener) {
    super(tableConditionModel, columnModel.getAllColumns());
    this.searchControl = Control.builder(this::performSimpleSearch)
            .caption(FrameworkMessages.get(FrameworkMessages.SEARCH))
            .build();
    this.simpleSearchTextField = Components.textField(tableConditionModel.getSimpleConditionStringValue())
            .columns(12)
            .action(searchControl)
            .build();
    this.onSearchListener = requireNonNull(onSearchListener);
    setLayout(Layouts.borderLayout());
    add(initializeSimpleConditionPanel(tableConditionModel), BorderLayout.CENTER);
  }

  /**
   * @return the search field
   */
  public JTextField getSimpleSearchTextField() {
    return simpleSearchTextField;
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param searchText the search text
   */
  public void setSearchText(final String searchText) {
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

  private JPanel initializeSimpleConditionPanel(final EntityTableConditionModel conditionModel) {
    JButton simpleSearchButton = searchControl.createButton();
    JPanel panel = new JPanel(Layouts.borderLayout());
    ComponentValues.textComponent(simpleSearchTextField).link(conditionModel.getSimpleConditionStringValue());
    panel.setBorder(BorderFactory.createTitledBorder(MESSAGES.getString("condition")));
    panel.add(simpleSearchTextField, BorderLayout.WEST);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private void performSimpleSearch() {
    Conjunction previousConjunction = getTableConditionModel().getConjunction();
    try {
      getTableConditionModel().setConjunction(Conjunction.OR);
      onSearchListener.onEvent();
    }
    finally {
      getTableConditionModel().setConjunction(previousConjunction);
    }
  }
}
