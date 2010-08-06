/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;

public final class EntityTableSearchSimplePanel extends JPanel implements EntityTableSearchPanel {

  private final EntityTableSearchModel searchModel;
  private final Refreshable refreshable;
  private final Collection<Property.ColumnProperty> searchProperties;

  private JTextField searchField;
  private JButton searchButton;

  public EntityTableSearchSimplePanel(final EntityTableSearchModel searchModel, final Refreshable refreshable) {
    this.searchModel = searchModel;
    this.refreshable = refreshable;
    this.searchProperties = Entities.getSearchProperties(searchModel.getEntityID());
    initUI();
  }

  public EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  public ControlSet getControls() {
    return null;
  }

  public void setSearchTest(final String txt) {
    searchField.setText(txt);
  }

  public void performSearch() {
    searchButton.doClick();
  }

  private void initUI() {
    searchField = new JTextField();
    final Action action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        performSimpleSearch(searchField.getText(), searchProperties);
      }
    };
    searchButton = new JButton(action);

    searchField.addActionListener(action);
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    add(searchField, BorderLayout.CENTER);
    add(searchButton, BorderLayout.EAST);
  }

  private void performSimpleSearch(final String searchText, final Collection<Property.ColumnProperty> searchProperties) {
    final Conjunction conjunction = searchModel.getSearchConjunction();
    try {
      searchModel.clearPropertySearchModels();
      searchModel.setSearchConjunction(Conjunction.OR);
      if (!searchText.isEmpty()) {
        final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
        final String searchTextWithWildcards = wildcard + searchText + wildcard;
        for (final Property searchProperty : searchProperties) {
          final PropertySearchModel propertySearchModel = searchModel.getPropertySearchModel(searchProperty.getPropertyID());
          propertySearchModel.setCaseSensitive(false);
          propertySearchModel.setUpperBound(searchTextWithWildcards);
          propertySearchModel.setSearchType(SearchType.LIKE);
          propertySearchModel.setSearchEnabled(true);
        }
      }

      refreshable.refresh();
    }
    finally {
      searchModel.setSearchConjunction(conjunction);
    }
  }
}
