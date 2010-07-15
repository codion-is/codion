/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.EntityRepository;
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
import java.util.ArrayList;
import java.util.Collection;

public class EntityTableSearchSimplePanel extends JPanel implements EntityTableSearchPanel {

  private final EntityTableSearchModel searchModel;
  private final Refreshable refreshable;
  private final Collection<Property> searchProperties;

  public EntityTableSearchSimplePanel(final EntityTableSearchModel searchModel, final Refreshable refreshable) {
    this.searchModel = searchModel;
    this.refreshable = refreshable;
    this.searchProperties = getSearchableProperties();
    initUI();
  }

  public EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  public ControlSet getControls() {
    return null;
  }

  public Collection<Property> getSearchProperties() {
    return searchProperties;
  }

  private void initUI() {
    final JTextField searchField = new JTextField();
    final Action action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        performSimpleSearch(searchField.getText(), searchProperties);
      }
    };

    searchField.addActionListener(action);
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    add(searchField, BorderLayout.CENTER);
    add(new JButton(action), BorderLayout.EAST);
  }

  private void performSimpleSearch(final String searchText, final Collection<Property> searchProperties) {
    final CriteriaSet.Conjunction conjunction = searchModel.getSearchConjunction();
    try {
      searchModel.clearPropertySearchModels();
      searchModel.setSearchConjunction(CriteriaSet.Conjunction.OR);
      if (searchText.length() > 0) {
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

  private Collection<Property> getSearchableProperties() {
    final Collection<Property> searchableProperties = new ArrayList<Property>();
    final Collection<String> defaultSearchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(searchModel.getEntityID());
    if (defaultSearchPropertyIDs.size() > 0) {
      for (final String propertyID : defaultSearchPropertyIDs) {
        searchableProperties.add(EntityRepository.getProperty(searchModel.getEntityID(), propertyID));
      }
    }
    else {
      for (final Property property : EntityRepository.getDatabaseProperties(searchModel.getEntityID())) {
        if (property.isString() && !property.isHidden()) {
          searchableProperties.add(property);
        }
      }
    }
    if (searchableProperties.size() == 0) {
      throw new RuntimeException("Unable to create a simple search panel for entity: "
              + searchModel.getEntityID() + ", no STRING based properties found");
    }

    return searchableProperties;
  }
}
