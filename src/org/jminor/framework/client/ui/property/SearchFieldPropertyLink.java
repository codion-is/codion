package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntitySearchField;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SearchFieldPropertyLink extends AbstractEntityPropertyLink {

  private final EntitySearchField searchField;

  public SearchFieldPropertyLink(final EntityModel model, final String propertyID,
                                 final EntitySearchField entitySearchField) {
    super(model, EntityRepository.get().getProperty(model.getEntityID(), propertyID), LinkType.READ_WRITE, null);
    this.searchField = entitySearchField;
    updateUI();
    entitySearchField.evtSelectedEntityChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateProperty();
      }
    });
  }

  protected void updateProperty() {
    final List<Entity> selectedEntities = searchField.getSelectedEntities();
    setPropertyValue(selectedEntities.size() == 0 ? null : selectedEntities.get(0));
  }

  protected void updateUI() {
    final List<Entity> value = new ArrayList<Entity>();
    if (getPropertyValue() != null)
      value.add((Entity) getPropertyValue());
    searchField.setSelectedEntities(value);
  }
}
