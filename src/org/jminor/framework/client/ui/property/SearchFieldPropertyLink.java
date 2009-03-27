package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntitySearchField;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for linking an EntitySearchField to a EntityModel entity property value
 */
public class SearchFieldPropertyLink extends AbstractEntityPropertyLink {

  private final EntitySearchField searchField;

  /**
   * Instantiates a new SearchFieldPropertyLink
   * @param entityModel the EntityModel instance
   * @param propertyID the ID of the property to link
   * @param entitySearchField the search field to link
   */
  public SearchFieldPropertyLink(final EntityModel entityModel, final String propertyID,
                                 final EntitySearchField entitySearchField) {
    this(entityModel, EntityRepository.get().getEntityProperty(entityModel.getEntityID(), propertyID), entitySearchField);
  }

  /**
   * Instantiates a new SearchFieldPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link
   * @param entitySearchField the search field to link
   */
  public SearchFieldPropertyLink(final EntityModel entityModel, final Property.EntityProperty property,
                                 final EntitySearchField entitySearchField) {
    super(entityModel, property, LinkType.READ_WRITE);
    this.searchField = entitySearchField;
    updateUI();
    entitySearchField.evtSelectedEntityChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateModel();
      }
    });
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    final List<Entity> selectedEntities = searchField.getSelectedEntities();
    return selectedEntities.size() == 0 ? null : selectedEntities.get(0);
  }

  /** {@inheritDoc} */
  protected void setUIPropertyValue(final Object propertyValue) {
    final List<Entity> value = new ArrayList<Entity>();
    if (getModelPropertyValue() != null)
      value.add((Entity) propertyValue);
    searchField.setSelectedEntities(value);
  }
}
