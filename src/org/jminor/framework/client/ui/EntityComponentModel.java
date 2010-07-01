package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.lang.reflect.InvocationTargetException;

/**
 * User: Björn Darri
 * Date: 1.7.2010
 * Time: 20:22:59
 */
public class EntityComponentModel extends EntityPanelProvider {

  private final String entityID;
  private final Class<? extends EntityEditPanel> editPanelClass;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditModel> editModelClass = DefaultEntityEditModel.class;
  private Class<? extends EntityTableModel> tableModelClass = DefaultEntityTableModel.class;


  public EntityComponentModel(final String entityID,
                              final Class<? extends EntityEditPanel> editPanelClass) {
    this(entityID, entityID, editPanelClass);
  }

  public EntityComponentModel(final String entityID, final String caption,
                              final Class<? extends EntityEditPanel> editPanelClass) {
    super(caption);
    this.entityID = entityID;
    this.editPanelClass = editPanelClass;
  }

  public String getEntityID() {
    return entityID;
  }

  public EntityComponentModel setEditModelClass(Class<? extends EntityEditModel> editModelClass) {
    this.editModelClass = editModelClass;
    return this;
  }

  public EntityComponentModel setTableModelClass(Class<? extends EntityTableModel> tableModelClass) {
    this.tableModelClass = tableModelClass;
    return this;
  }

  public EntityComponentModel setTablePanelClass(Class<? extends EntityTablePanel> tablePanelClass) {
    this.tablePanelClass = tablePanelClass;
    return this;
  }

  public Class<? extends EntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  public Class<? extends EntityEditPanel> getEditPanelClass() {
    return editPanelClass;
  }

  public Class<? extends EntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  public Class<? extends EntityTablePanel> getTablePanelClass() {
    return tablePanelClass;
  }

  public EntityPanel createEntityPanel(final EntityDbProvider dbProvider) {
    try {
      final EntityEditModel editModel = createEditModel(dbProvider);
      final EntityTableModel tableModel = createTableModel(dbProvider);
      final EntityModel model = createModel(editModel, tableModel);
      final EntityEditPanel editPanel = createEditPanel(editModel);
      final EntityTablePanel tablePanel;
      if (model.containsTableModel()) {
        tablePanel = createTablePanel(model.getTableModel());
      }
      else {
        tablePanel = null;
      }
      return createEntityPanel(model, getCaption(), editPanel, tablePanel);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ite.getCause();
      }

      throw new RuntimeException(ite.getCause());
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityPanel createEntityPanel(EntityModel model, final String caption, EntityEditPanel editPanel, EntityTablePanel tablePanel) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getPanelClass().getConstructor(EntityModel.class, String.class, EntityEditPanel.class, EntityTablePanel.class)
            .newInstance(model, caption, editPanel, tablePanel);
  }

  private EntityTablePanel createTablePanel(EntityTableModel tableModel) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getTablePanelClass().getConstructor(EntityTableModel.class).newInstance(tableModel);
  }

  private EntityEditPanel createEditPanel(EntityEditModel editModel) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getEditPanelClass().getConstructor(EntityEditModel.class).newInstance(editModel);
  }

  private EntityModel createModel(EntityEditModel editModel, EntityTableModel tableModel) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getModelClass().getConstructor(EntityEditModel.class, EntityTableModel.class).newInstance(editModel, tableModel);
  }

  private EntityTableModel createTableModel(final EntityDbProvider dbProvider) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getTableModelClass().getConstructor(String.class, EntityDbProvider.class)
            .newInstance(entityID, dbProvider);
  }

  private EntityEditModel createEditModel(EntityDbProvider dbProvider) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    return getEditModelClass().getConstructor(String.class, EntityDbProvider.class).newInstance(entityID, dbProvider);
  }
}
