/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import org.apache.log4j.Logger;
import org.jminor.common.db.DbException;
import org.jminor.common.db.TableStatus;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AggregateState;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.IExceptionHandler;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.printing.JPrinter;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

public abstract class EntityPanel extends EntityBindingFactory implements IExceptionHandler {

  private static final Logger log = Util.getLogger(EntityPanel.class);

  public static final int CONFIRM_TYPE_DELETE = 0;
  public static final int CONFIRM_TYPE_UPDATE = 1;
  public static final int CONFIRM_TYPE_INSERT = 2;

  public static final int ACTION_NONE = -1;
  public static final int ACTION_INSERT = 0;
  public static final int ACTION_UPDATE = 1;
  public static final int ACTION_DELETE = 2;

  public static final int DIALOG = 1;
  public static final int EMBEDDED = 2;
  public static final int HIDDEN = 3;

  public static final int DIVIDER_JUMP = 30;

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

  //Control codes
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String REFRESH = "refresh";
  public static final String CLEAR = "clear";
  public static final String PRINT = "print";
  public static final String MENU_DELETE = "menuDelete";
  public static final String VIEW_DEPENDENCIES = "viewDependencies";
  public static final String UPDATE_SELECTED = "updateSelected";
  public static final String CONFIGURE_QUERY = "configureQuery";

  private static final String NAV_UP = "navigateUp";
  private static final String NAV_DOWN = "navigateDown";
  private static final String NAV_RIGHT = "navigateRight";
  private static final String NAV_LEFT = "navigateLeft";
  private static final String DIV_LEFT = "divLeft";
  private static final String DIV_RIGHT = "divRight";
  private static final String DIV_UP = "divUp";
  private static final String DIV_DOWN = "divDown";

  private final HashMap<String, Control> controlMap = new HashMap<String, Control>();

  private final boolean allowQueryConfiguration;
  private final boolean compactPanel;
  private final boolean specialRendering;
  private final boolean refreshOnInit;
  private final String buttonPlacement;

  private EntityModel model;
  private EntityTablePanel entityTablePanel;

  private JPanel editPanel;
  private JSplitPane horizontalSplitPane;
  private Map<EntityPanelInfo, EntityPanel> detailEntityPanels;
  private JTabbedPane detailTabPane;
  private JPanel compactBase;
  private JDialog detailDialog;
  private JDialog editDialog;

  private JComponent defaultFocusComponent;

  private int editPanelState = HIDDEN;
  private int detailPanelState = HIDDEN;

  private boolean initialized = false;

  public EntityPanel() {
    this(true);
  }

  public EntityPanel(final boolean refreshOnInit) {
    this(refreshOnInit, true);
  }

  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering) {
    this(refreshOnInit, specialRendering, false);
  }

  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons) {
    this(refreshOnInit, specialRendering, horizontalButtons, EMBEDDED);//embedded perhaps not default?
  }

  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState) {
    this(refreshOnInit, specialRendering, horizontalButtons, detailPanelState, true);
  }

  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState, final boolean allowQueryConfiguration) {
    this(refreshOnInit, specialRendering, horizontalButtons, detailPanelState,
            allowQueryConfiguration,  false);
  }

  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState, final boolean allowQueryConfiguration, final boolean compactPanel) {
    if (detailPanelState == DIALOG)
      throw new IllegalArgumentException("EntityPanel constructor only accepts HIDDEN or EMBEDDED as default detail states");

    this.refreshOnInit = refreshOnInit;
    this.allowQueryConfiguration = allowQueryConfiguration;
    this.specialRendering = specialRendering;
    this.buttonPlacement = horizontalButtons ? BorderLayout.SOUTH : BorderLayout.EAST;
    this.detailPanelState = detailPanelState;
    this.compactPanel = compactPanel;
  }

  public EntityPanel setModel(final EntityModel model) {
    if (this.model != null)
      throw new RuntimeException("EntityPanel already has a model: " + this.model);

    this.model = model;
    bindModelEvents();

    return this;
  }

  /** {@inheritDoc} */
  public EntityModel getModel() {
    return this.model;
  }

  /**
   * Initializes this EntityPanel, override to add any specific initialization
   * functionality, to show the search panel for example.
   * Remember to return right away if isInitialized() returns true and to call super.initialize()
   * After this method has finished isInitialized() returns true
   * @see #isInitialized()
   */
  public void initialize() {
    if (initialized)
      return;

    if (model == null)
      throw new RuntimeException("Cannot initialize a EntityPanel without a model, call setModel() first");
    try {
      UiUtil.setWaitCursor(true, this);
      bindEvents();
      addComponentListener(new ComponentAdapter() {
        public void componentHidden(ComponentEvent e) {
          setFilterPanelsVisible(false);
        }
        public void componentShown(ComponentEvent e) {
          setFilterPanelsVisible(true);
        }
      });
      initializeResizing();
      if (FrameworkSettings.get().useKeyboardNavigation)
        initializeNavigation();
      if (FrameworkSettings.get().useFocusActivation) {//todo mind that darn memory leak!! only use for persistent panels?
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
                "focusOwner", new java.beans.PropertyChangeListener() {
          public void propertyChange(final PropertyChangeEvent evt) {
            final Component focusOwner = (Component) evt.getNewValue();
            if (focusOwner != null && isParentPanel(focusOwner))
              model.stActive.setActive(true);
          }
        });
      }
      initializeAssociatedPanels();
      setupControls();
      initializeControlPanels();

      final List<EntityPanelInfo> detailPanelInfos = getDetailPanelInfo();
      this.detailEntityPanels = new LinkedHashMap<EntityPanelInfo, EntityPanel>(detailPanelInfos.size());
      for (final EntityPanelInfo detailPanelInfo : detailPanelInfos) {
        final EntityModel detailModel = model.getDetailModel(detailPanelInfo.getEntityModelClass());
        if (detailModel == null)
          throw new RuntimeException("Detail model of type " + detailPanelInfo.getEntityModelClass()
                  + " not found in model of type " + model.getClass());
        this.detailEntityPanels.put(detailPanelInfo, detailPanelInfo.getInstance(detailModel));
      }
      this.editPanel = initializeEditPanel();
      this.entityTablePanel = model.getTableModel() != null ? initializeEntityTablePanel(specialRendering) : null;
      this.horizontalSplitPane = this.detailEntityPanels.size() > 0 ? initializeLeftRightSplitPane() : null;
      this.detailTabPane = this.detailEntityPanels.size() > 0 ? initializeDetailTabPane() : null;

      bindTableModelEvents();
      bindTablePanelEvents();

      initializeUI();

      setDetailPanelState(detailPanelState);
      setEditPanelState(EMBEDDED);

      if (refreshOnInit)
        model.refresh();//refreshes combo models
      else
        model.refreshComboBoxModels();
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
    finally {
      this.initialized = true;
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * @return Value for property 'initialized'.
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * @return Value for property 'defaultFocusComponent'.
   */
  public JComponent getDefaultFocusComponent() {
    return this.defaultFocusComponent == null ? this : this.defaultFocusComponent;
  }

  public JComponent setDefaultFocusComponent(final JComponent defaultFocusComponent) {
    return this.defaultFocusComponent = defaultFocusComponent;
  }

  /**
   * @return Value for property 'tablePanel'.
   */
  public EntityTablePanel getTablePanel() {
    return this.entityTablePanel;
  }

  /**
   * @return Value for property 'selectedDetailPanel'.
   */
  public EntityPanel getSelectedDetailPanel() {
    return (EntityPanel) this.detailTabPane.getSelectedComponent();
  }

  /**
   * @return Value for property 'active'.
   */
  public boolean isActive() {
    return this.model.stActive.isActive();
  }

  /** {@inheritDoc} */
  public String toString() {
    return this.model.getModelCaption();
  }

  public void toggleDetailPanelState() {
    final int state = getDetailPanelState();
    if (state == DIALOG)
      setDetailPanelState(HIDDEN);
    else if (state == EMBEDDED)
      setDetailPanelState(DIALOG);
    else
      setDetailPanelState(EMBEDDED);
  }

  public void toggleEditPanelState() {
    final int state = getEditPanelState();
    if (state == DIALOG)
      setEditPanelState(HIDDEN);
    else if (state == EMBEDDED)
      setEditPanelState(DIALOG);
    else
      setEditPanelState(EMBEDDED);
  }

  /**
   * @return Value for property 'detailPanelState'.
   */
  public int getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @param state Value to set for property 'detailPanelState'.
   */
  public void setDetailPanelState(final int state) {
    if (detailTabPane == null)
      return;

    if (state != HIDDEN)
      getSelectedDetailPanel().initialize();

    model.setLinkedDetailModel(state != HIDDEN ? getSelectedDetailPanel().getModel() : null);

    detailPanelState = state;
    if (state != DIALOG)
      disposeDetailDialog();

    if (state == EMBEDDED)
      horizontalSplitPane.setRightComponent(detailTabPane);
    else if (state == HIDDEN)
      horizontalSplitPane.setRightComponent(null);
    else
      showDetailDialog();

    revalidate();
  }

  /**
   * @param state Value to set for property 'editPanelState'.
   */
  public void setEditPanelState(final int state) {
    if (editPanel == null)
      return;

    editPanelState = state;
    if (state != DIALOG)
      disposeEditDialog();

    if (state == EMBEDDED) {
      if (compactPanel)
        compactBase.add(editPanel, BorderLayout.NORTH);
      else
        add(editPanel, BorderLayout.NORTH);
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
      if (compactPanel)
        compactBase.remove(editPanel);
      else
        remove(editPanel);
    }
    else
      showEditDialog();

    revalidate();
  }

  /**
   * @return Value for property 'editPanelState'.
   */
  public int getEditPanelState() {
    return editPanelState;
  }

  /**
   * @param val Value to set for property 'filterPanelsVisible'.
   */
  public void setFilterPanelsVisible(final boolean val) {
    if (!initialized)
      return;

    if (model.getTableModel() != null)
      entityTablePanel.setFilterPanelsVisible(val);
    for (final EntityPanel detailEntityPanel : detailEntityPanels.values())
      detailEntityPanel.setFilterPanelsVisible(val);
  }

  public void resizePanel(final int direction) {
    switch(direction) {
      case UP :
        setEditPanelState(HIDDEN);
        break;
      case DOWN :
        setEditPanelState(EMBEDDED);
        break;
      case RIGHT :
        int newPos = horizontalSplitPane.getDividerLocation() + DIVIDER_JUMP;
        if (newPos <= horizontalSplitPane.getMaximumDividerLocation())
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
        break;
      case LEFT :
        newPos = horizontalSplitPane.getDividerLocation() - DIVIDER_JUMP;
        if (newPos >= 0)
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(0);
        break;
    }
  }

  public boolean isParentPanel(final Component component) {
    final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, component);
    if (parent == this)
      return true;

    //is editDialog parent?
    return editDialog != null && SwingUtilities.getWindowAncestor(component) == editDialog;
  }

  /** {@inheritDoc} */
  public void handleException(final Throwable e) {
    handleException(e, this);
  }

  public void handleException(final Throwable e, final JComponent parent) {
    log.error(this, e);
    FrameworkUiUtil.handleException(e, model.getEntityID(), parent);
  }

  //#############################################################################################
  // Begin - control methods, see setupControls
  //#############################################################################################

  public final void handleSave() {
    if ((getModel().getTableModel() != null && getModel().getTableModel().getSelectionModel().isSelectionEmpty())
            || !getModel().isActiveEntityModified()) {//no entity selected or selected entity is unmodified, can only insert
      handleInsert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), -1, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE),
                      FrameworkMessages.get(FrameworkMessages.SAVE), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) //update
        handleUpdate();
      else if (choiceIdx == 1) //insert
        handleInsert();
    }
  }

  /**
   * @return true in case of successful insert, false otherwise
   */
  public final boolean handleInsert() {
    try {
      validateData();
      try {
        UiUtil.setWaitCursor(true, EntityPanel.this);
        model.insert();
      }
      finally {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
      prepareUI(true, true);
      postInsert();
      return true;
    }
    catch (Exception ex) {
      handleException(ex);
    }

    return false;
  }

  public final boolean handleDelete() {
    try {
      if (confirmDelete(model.getTableModel().getSelectedEntities())) {
        try {
          UiUtil.setWaitCursor(true, EntityPanel.this);
          model.delete();
        }
        finally {
          UiUtil.setWaitCursor(false, EntityPanel.this);
        }

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  public final boolean handleUpdate() {
    try {
      validateData();
      if (confirmUpdate(model.getActiveEntityCopy())) {
        try {
          UiUtil.setWaitCursor(true, EntityPanel.this);
          model.update();
        }
        finally {
          UiUtil.setWaitCursor(false, EntityPanel.this);
        }
        prepareUI(true, false);

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  public void updateSelectedEntities() {
    try {
      updateSelectedEntities(getPropertyToUpdate());
    }
    catch (UserCancelException e) {/**/}
  }

  public void updateSelectedEntities(final Property propertyToUpdate) {
    try {
      if (model.getTableModel() == null || model.getTableModel().stSelectionEmpty.isActive())
        return;

      final List<Entity> selectedEntities = model.getTableModel().getSelectedEntities();
      final EntityPropertyEditor editPanel = new EntityPropertyEditor(
              selectedEntities.get(0).getValue(propertyToUpdate.propertyID), propertyToUpdate,
              model.getDbConnectionProvider(),
              getInputManager(propertyToUpdate), selectedEntities.size() > 1);
      UiUtil.showInDialog(this, editPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
              null, editPanel.getOkButton(), editPanel.evtButtonClicked);
      if (editPanel.getButtonValue() == JOptionPane.OK_OPTION) {
        final Object[] oldValues = EntityUtil.setPropertyValue(
                propertyToUpdate.propertyID, editPanel.getValue(), selectedEntities);
        try {
          UiUtil.setWaitCursor(true, this);
          model.update(selectedEntities);
        }
        catch (Exception e) {
          EntityUtil.setPropertyValue(propertyToUpdate.propertyID, oldValues, selectedEntities);
          throw e;
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
      }
    }
    catch(Exception e) {
      handleException(e);
    }
  }

  public void viewSelectionDependencies() {
    new SwingWorker() {//just testing this thingy
      protected Object doInBackground() throws Exception {
        try {
          final Map<String, List<Entity>> dependencies;
          try {
            UiUtil.setWaitCursor(true, EntityPanel.this);
            dependencies = model.getTableModel().getSelectionDependencies();
          }
          finally {
            UiUtil.setWaitCursor(false, EntityPanel.this);
          }
          if (EntityUtil.activeDependencies(dependencies)) {
            showDependenciesDialog(dependencies, model.getDbConnectionProvider(), EntityPanel.this);
          }
          else {
            JOptionPane.showMessageDialog(EntityPanel.this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
                    FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
          }

          return true;
        }
        catch (Exception e) {
          handleException(e);
        }

        return false;
      }
    }.execute();
  }

  public void printTable() throws UserException {
    try {
      if (model.getTableModel() != null)
        JPrinter.print(entityTablePanel.getJTable());

      prepareUI(true, false);
    }
    catch (PrinterException pr) {
      throw new UserException(pr);
    }
  }

  public void configureQuery() {
    getTablePanel().configureQuery();
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  public final void prepareUI(boolean requestDefaultFocus, final boolean clearUI) {
    if (clearUI)
      model.clear();
    if (requestDefaultFocus && !isParentPanel(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner())) {
      if (getEditPanelState() != EMBEDDED && entityTablePanel != null)
        entityTablePanel.requestFocusInWindow();
      else if (defaultFocusComponent != null)
        defaultFocusComponent.requestFocusInWindow();
      else if (getComponentCount() > 0)
        getComponents()[0].requestFocusInWindow();
    }
  }

  public Control getConfigureQueryControl() {
    return ControlFactory.methodControl(this, "configureQuery",
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + "...", null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY));
  }

  public Control getViewDependenciesControl() {
    return ControlFactory.methodControl(this, "viewSelectionDependencies",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...",
            model.getTableModel().stSelectionEmpty.getReversedState(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  public Control getPrintControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return ControlFactory.methodControl(this, "printTable", printCaption, null,
            printCaption, printCaption.charAt(0));
  }

  public Control getDeleteSelectedControl() {
    final String deleteCaption = FrameworkMessages.get(FrameworkMessages.DELETE);
    return ControlFactory.methodControl(this, "handleDelete", deleteCaption,
            new AggregateState(AggregateState.AND,
                    model.getAllowDeleteState(),
                    model.getTableModel().stSelectionEmpty.getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  public Control getRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC);
    return ControlFactory.methodControl(model, "forceRefresh", refreshCaption,
            model.stActive, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  public Control getUpdateSelectedControl() {
    final String updateSelectedCaption = FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED);
    return ControlFactory.methodControl(this, "updateSelectedEntities",
            updateSelectedCaption,
            new AggregateState(AggregateState.AND,
                    model.getAllowUpdateState(),
                    model.getTableModel().stSelectionEmpty.getReversedState()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP), 0,
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  public ControlSet getUpdateSelectedControlSet(final String name) {
    final State enabled = new AggregateState(AggregateState.AND, model.getAllowUpdateState(),
            model.getTableModel().stSelectionEmpty.getReversedState());
    final ControlSet ret = new ControlSet(name, (char) 0, Images.loadImage("Modify16.gif"), enabled);
    ret.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : getUpdateProperties()) {
      ret.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(property.getCaption()) {
        public void actionPerformed(final ActionEvent e) {
          updateSelectedEntities(property);
        }
      }));
    }

    return ret;
  }

  public Control getDeleteControl() {
    final String deleteCaption = FrameworkMessages.get(FrameworkMessages.DELETE);
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return ControlFactory.methodControl(this, "handleDelete", deleteCaption,
            new AggregateState(AggregateState.AND,
                    model.stActive,
                    model.getAllowDeleteState(),
                    model.stEntityActive),//changed from stSelectionEmpty.getReversedState()
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0), null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  public Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return ControlFactory.methodControl(model, "clear", clearCaption,
            model.stActive, FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  public Control getUpdateControl() {
    final String updateCaption = FrameworkMessages.get(FrameworkMessages.UPDATE);
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return ControlFactory.methodControl(this, "handleUpdate", updateCaption,
            new AggregateState(AggregateState.AND,
                    model.stActive,
                    model.getAllowUpdateState(),
                    model.stEntityActive,
                    model.getEntityModifiedState()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0),
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  public Control getInsertControl() {
    final String insertCaption = FrameworkMessages.get(FrameworkMessages.INSERT);
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return ControlFactory.methodControl(this, "handleSave", insertCaption,
            new AggregateState(AggregateState.AND, model.stActive, model.getAllowInsertState()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  public Control getSaveControl() {
    final String insertCaption = FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE);
    final State stInsertUpdate = new AggregateState(AggregateState.OR, model.getAllowInsertState(),
            new AggregateState(AggregateState.AND, model.getAllowUpdateState(), model.getEntityModifiedState()));
    return ControlFactory.methodControl(this, "handleSave", insertCaption,
            new AggregateState(AggregateState.AND, model.stActive, stInsertUpdate),
            FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE_TIP),
            insertCaption.charAt(0), null, Images.loadImage(Images.IMG_PROPERTIES_16));
  }

  public Control getControl(final String controlCode) {
    return controlMap.get(controlCode);
  }

  public static EntityPanel createStaticEntityPanel(final List<Entity> entities) throws UserException {
    if (entities == null || entities.size() == 0)
      throw new UserException("Cannot create an EntityPanel without the entities");

    return createStaticEntityPanel(entities, null);
  }

  public static EntityPanel createStaticEntityPanel(final List<Entity> entities,
                                                    final IEntityDbProvider dbProvider) throws UserException {
    if (entities == null || entities.size() == 0)
      throw new UserException("Cannot create an EntityPanel without the entities");

    return createStaticEntityPanel(entities, dbProvider, entities.get(0).getEntityID(), true);
  }

  public static EntityPanel createStaticEntityPanel(final List<Entity> entities,
                                                    final IEntityDbProvider dbProvider,
                                                    final String entityID) throws UserException {
    return createStaticEntityPanel(entities, dbProvider, entityID, true);
  }

  public static EntityPanel createStaticEntityPanel(final List<Entity> entities, final IEntityDbProvider dbProvider,
                                                    final String entityID, final boolean includePopupMenu) throws UserException {
    final EntityModel model = new EntityModel(entityID, dbProvider, entityID) {
      protected EntityTableModel initializeTableModel() {
        return new EntityTableModel(dbProvider, entityID) {
          protected List<Entity> getAllEntitiesFromDb() throws DbException, UserException {
            return entities;
          }

          protected void setCurrentTableStatus(final TableStatus currentTableStatus) {
            currentTableStatus.setRecordCount(entities.size());
            super.setCurrentTableStatus(currentTableStatus);
          }
        };
      }
    };

    final EntityPanel ret = new EntityPanel(true, false, false, EMBEDDED, false) {
      protected EntityTablePanel initializeEntityTablePanel(final boolean specialRendering) {
        return new EntityTablePanel(model.getTableModel(), getTablePopupControlSet(), false, false) {
          protected JPanel initializeSearchPanel() {
            return null;
          }
          protected JToolBar getRefreshToolbar() {
            return null;
          }
        };
      }
      protected ControlSet getTablePopupControlSet() {
        return includePopupMenu ? super.getTablePopupControlSet() : null;
      }
      protected JPanel initializePropertyPanel() {
        return null;
      }
    };
    ret.setModel(model);
    ret.initialize();

    return ret;
  }

  protected void showPanelTab() {
    final JTabbedPane tp = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
    if (tp != null)
      tp.setSelectedComponent(this);
  }

  //#############################################################################################
  // Begin - initialization methods
  //#############################################################################################

  protected void initializeUI() {
    setLayout(new BorderLayout(5,5));
    if (detailTabPane == null) { //no left right split pane
      add(entityTablePanel, BorderLayout.CENTER);
    }
    else {
      if (compactPanel) {
        compactBase = new JPanel(new BorderLayout(5,5));
        compactBase.add(entityTablePanel, BorderLayout.CENTER);
        horizontalSplitPane.setLeftComponent(compactBase);
      }
      else {
        horizontalSplitPane.setLeftComponent(entityTablePanel);
      }
      horizontalSplitPane.setRightComponent(detailTabPane);
      add(horizontalSplitPane, BorderLayout.CENTER);
    }
  }

  protected JPanel initializeEditPanel() {
    final JPanel propertyPanel = initializePropertyPanel();
    if (propertyPanel == null)
      return null;

    final JPanel editPanel = new JPanel(new BorderLayout(5,5));
    editPanel.setMinimumSize(new Dimension(0,0));
    editPanel.setBorder(BorderFactory.createEtchedBorder());
    final JPanel propertyBase =
            new JPanel(new FlowLayout(buttonPlacement.equals(BorderLayout.SOUTH) ? FlowLayout.CENTER : FlowLayout.LEADING,5,5));
    editPanel.addMouseListener(new ActivationFocusAdapter(propertyBase));
    propertyBase.add(propertyPanel);
    editPanel.add(propertyBase, BorderLayout.CENTER);
    final JComponent actionPanel = FrameworkSettings.get().toolbarActions ?
            initializeActionToolBar() : initializeActionPanel();
    if (actionPanel != null)
      editPanel.add(actionPanel, FrameworkSettings.get().toolbarActions ?
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.NORTH : BorderLayout.WEST) :
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.SOUTH : BorderLayout.EAST));

    return editPanel;
  }

  protected JSplitPane initializeLeftRightSplitPane() {
    final JSplitPane leftRightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    leftRightSplitPane.setBorder(BorderFactory.createEmptyBorder());
    leftRightSplitPane.setOneTouchExpandable(true);
    leftRightSplitPane.setResizeWeight(getDetailSplitPaneResizeWeight());
    leftRightSplitPane.setDividerSize(18);

    return leftRightSplitPane;
  }

  /**
   * @return Value for property 'detailSplitPaneResizeWeight'.
   */
  protected double getDetailSplitPaneResizeWeight() {
    return 0.5;
  }

  protected JPanel initializeActionPanel() {
    JPanel ret;
    if (buttonPlacement.equals(BorderLayout.SOUTH)) {
      ret = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
      ret.add(ControlProvider.createHorizontalButtonPanel(getPanelControlSet()));
    }
    else {
      ret = new JPanel(new BorderLayout(5,5));
      ret.add(ControlProvider.createVerticalButtonPanel(getPanelControlSet()), BorderLayout.NORTH);
    }

    return ret;
  }

  protected JToolBar initializeActionToolBar() {
    return ControlProvider.createToolbar(getPanelControlSet(), JToolBar.VERTICAL);
  }

  protected JTabbedPane initializeDetailTabPane() {
    final JTabbedPane ret = new JTabbedPane();
    ret.setFocusable(false);
    ret.setUI(new BasicTabbedPaneUI() {
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(1,0,0,0);
      }
    });
    for (final EntityPanel detailPanel : detailEntityPanels.values())
      ret.addTab(detailPanel.model.getModelCaption(), detailPanel);

    ret.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        model.setLinkedDetailModel(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().initialize();
      }
    });
    ret.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
          setDetailPanelState(getDetailPanelState() == DIALOG ? EMBEDDED : DIALOG);
        else if (e.getButton() == MouseEvent.BUTTON2)
          setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
      }
    });

    return ret;
  }

  protected abstract JPanel initializePropertyPanel();

  /**
   * @return a list of EntityPanelInfo objects, specifying which detail panels this panel should contain
   */
  protected List<EntityPanelInfo> getDetailPanelInfo() {
    return new ArrayList<EntityPanelInfo>(0);
  }

  /**
   * Called during construction, before controls have been initialized
   */
  protected void initializeAssociatedPanels() {}

  /**
   * Called during construction, after controls have been initialized
   */
  protected void initializeControlPanels() {}

  protected EntityTablePanel initializeEntityTablePanel(final boolean specialRendering) {
    final EntityTablePanel ret = new EntityTablePanel(model.getTableModel(), getTablePopupControlSet(),
            specialRendering, allowQueryConfiguration);
    ret.addSouthPanelButtons(getSouthPanelButtons(ret));
    if (editPanel != null || detailEntityPanels.size() > 0) {
      ret.setDoubleClickAction(new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
          if (editPanel != null) {
            if (getEditPanelState() == HIDDEN)
              setEditPanelState(DIALOG);
            else if (getDetailPanelState() == HIDDEN)
              setDetailPanelState(DIALOG);
          }
        }
      });
    }
    ret.setMinimumSize(new Dimension(0,0));

    return ret;
  }

  protected AbstractButton[] getSouthPanelButtons(final EntityTablePanel tablePanel) {
    final List<AbstractButton> ret = new ArrayList<AbstractButton>();

    AbstractButton tmp = getToggleSummaryPanelButton(tablePanel);
    if (tmp != null)
      ret.add(tmp);
    tmp = getSearchButton(tablePanel);
    if (tmp != null)
      ret.add(tmp);
    tmp = getPrintButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getUpdateButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getDeleteButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getClearSelectionButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getSelectionDownButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getSelectionUpButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getToggleEditPanelButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getToggleDetaiPanelButton();
    if (tmp != null)
      ret.add(tmp);

    return ret.toArray(new AbstractButton[ret.size()]);
  }

  protected void setupControls() {
    if (!model.isReadOnly()) {
      if (model.allowInsert())
        controlMap.put(INSERT, getInsertControl());
      if (model.allowUpdate())
        controlMap.put(UPDATE, getUpdateControl());
      if (model.allowDelete())
        controlMap.put(DELETE, getDeleteControl());
    }
    controlMap.put(CLEAR, getClearControl());
    if (model.getTableModel() != null) {
      if (!model.isReadOnly() && model.allowUpdate() && model.getAllowMultipleUpdate())
        controlMap.put(UPDATE_SELECTED, getUpdateSelectedControl());
      controlMap.put(REFRESH, getRefreshControl());
      if (!model.isReadOnly() && model.allowDelete())
        controlMap.put(MENU_DELETE, getDeleteSelectedControl());
      controlMap.put(PRINT, getPrintControl());
      controlMap.put(VIEW_DEPENDENCIES, getViewDependenciesControl());
      if (allowQueryConfiguration)
        controlMap.put(CONFIGURE_QUERY, getConfigureQueryControl());
    }
  }

  protected ControlSet getTablePopupControlSet() {
    boolean seperatorRequired = false;
    final ControlSet ret = new ControlSet("");
    if (detailEntityPanels.size() > 0) {
      ret.add(getDetailPanelControls(EMBEDDED));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    if (controlMap.containsKey(UPDATE_SELECTED)) {
      ret.add(getUpdateSelectedControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED)));
      seperatorRequired = true;
    }
    if (controlMap.containsKey(MENU_DELETE)) {
      ret.add(controlMap.get(MENU_DELETE));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    if (controlMap.containsKey(VIEW_DEPENDENCIES)) {
      ret.add(controlMap.get(VIEW_DEPENDENCIES));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    final ControlSet printControls = getPrintControls();
    if (printControls != null) {
      ret.add(getPrintControls());
      seperatorRequired = true;
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      if (seperatorRequired)
        ret.addSeparator();
      ret.add(controlMap.get(CONFIGURE_QUERY));
    }

    return ret;
  }

  protected ControlSet getDetailPanelControls(final int status) {
    if (detailEntityPanels.size() == 0)
      return null;

    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels.values()) {
      final EntityModel model = detailPanel.getModel();
      if (model == null)
        throw new RuntimeException("EntityPanel does not have a EntityModel associated with it");
      ret.add(new Control(model.getModelCaption()) {
        public void actionPerformed(ActionEvent e) {
          detailTabPane.setSelectedComponent(detailPanel);
          setDetailPanelState(status);
        }
      });
    }

    return ret;
  }

  //override to provide specific print actions, i.e. reports
  protected ControlSet getPrintControls() {
    return controlMap.containsKey(PRINT) ? new ControlSet(Messages.get(Messages.PRINT), (char) 0, null,
            Images.loadImage("Print16.gif"), controlMap.get(PRINT)) : null;
  }

  /**
   * Override to keep event bindings in one place,
   * this method is called during initialization
   */
  protected void bindEvents() {}

  /**
   * Override to keep table model event bindings in one place,
   * this method is called during initialization
   */
  protected void bindTableModelEvents() {}

  protected void bindTablePanelEvents() {
    if (entityTablePanel == null)
      return;

    if (!getModel().isReadOnly()) {
      entityTablePanel.getJTable().addKeyListener(new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_DELETE && !getModel().getTableModel().stSelectionEmpty.isActive())
            handleDelete();
        }
      });
    }
    getModel().evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        entityTablePanel.getJTable().repaint();
      }
    });
  }

  protected ControlSet getPanelControlSet() {
    final ControlSet ret = new ControlSet("Actions");
    if (controlMap.containsKey(INSERT))
      ret.add(controlMap.get(INSERT));
    if (controlMap.containsKey(UPDATE))
      ret.add(controlMap.get(UPDATE));
    if (controlMap.containsKey(DELETE))
      ret.add(controlMap.get(DELETE));
    if (controlMap.containsKey(CLEAR))
      ret.add(controlMap.get(CLEAR));
    if (controlMap.containsKey(REFRESH))
      ret.add(controlMap.get(REFRESH));

    return ret;
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  /**
   * for overriding, called after a successful insert, after the UI has been initialized for a new entry
   * @throws UserException in case of an exception
   */
  protected void postInsert() throws UserException {}

  /**
   * for overriding, called before insert/update
   * @throws UserException in case of an exception
   * @throws UserCancelException in case the user cancels the action during validation
   */
  protected void validateData() throws UserException, UserCancelException {}

  /**
   * for overriding, to provide specific input components for multi-entity update, see updateSelectedEntities
   * @param property the property for which to get the InputManager
   * @return the InputManager handling input for <code>property</code>
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected EntityPropertyEditor.InputManager getInputManager(final Property property) {
    return null;
  }

  protected boolean confirmDelete(final List<Entity> entities) {
    if (entities == null || entities.size() == 0)
      return false;

    final String[] msgs = getConfirmationMessages(CONFIRM_TYPE_DELETE);
    final int res = JOptionPane.showConfirmDialog(EntityPanel.this,
            msgs[0], msgs[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  protected boolean confirmUpdate(final Object object) {
    if (object == null)
      return false;

    final String[] msgs = getConfirmationMessages(CONFIRM_TYPE_UPDATE);
    final int res = JOptionPane.showConfirmDialog(EntityPanel.this,
            msgs[0], msgs[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  protected String[] getConfirmationMessages(final int type) {
    switch (type) {
      case CONFIRM_TYPE_DELETE :
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
                FrameworkMessages.get(FrameworkMessages.DELETE)};
      case CONFIRM_TYPE_INSERT :
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_INSERT),
                FrameworkMessages.get(FrameworkMessages.INSERT)};
      case CONFIRM_TYPE_UPDATE :
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE),
                FrameworkMessages.get(FrameworkMessages.UPDATE)};
    }

    throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
  }

  protected void showDetailDialog() {
    final Window parent = UiUtil.getParentWindow(this);
    final Dimension parentSize = parent.getSize();
    final Dimension size = getDetailDialogSize(parentSize);
    final Point parentLocation = parent.getLocation();
    final Point location = new Point(parentLocation.x+(parentSize.width-size.width),
            parentLocation.y+(parentSize.height-size.height)-29);
    detailDialog = UiUtil.showInDialog(UiUtil.getParentWindow(EntityPanel.this), detailTabPane, false,
            getModel().getModelCaption() + " - " + FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES), false, true,
            null, size, location, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDetailPanelState(HIDDEN);
      }
    });
  }

  protected Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width/1.5),
            (editPanel != null) ? (int) (parentSize.height/1.5) : parentSize.height-54);
  }

  protected void showEditDialog() {
    final Point location = getLocationOnScreen();
    location.setLocation(location.x+1, location.y + getSize().height-editPanel.getSize().height-98);
    editDialog = UiUtil.showInDialog(UiUtil.getParentWindow(EntityPanel.this), editPanel, false,
            getModel().getModelCaption(), false, true,
            null, null, location, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setEditPanelState(HIDDEN);
      }
    });
    prepareUI(true, false);
  }

  protected void printReportForSelected(final String reportPath, final HashMap reportParams) throws UserException {
    try {
      final JFrame frame = new JFrame(FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER));
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(new JRViewer(initJasperPrintObject(reportPath, reportParams)));
      UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
      UiUtil.centerWindow(frame);
      frame.setVisible(true);
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private JButton getSearchButton(final EntityTablePanel panel) {
    if (!allowQueryConfiguration)
      return null;

    final Control toggleSearch = new Control() {
      public void actionPerformed(ActionEvent e) {
        final JPanel searchPanel = panel.getSearchPanel();
        if (searchPanel instanceof EntityTableSearchPanel) {
          if (panel.isSearchPanelVisible()) {
            if (((EntityTableSearchPanel) searchPanel).isAdvanced())
              panel.setSearchPanelVisible(false);
            else
              ((EntityTableSearchPanel) searchPanel).setAdvanced(true);
          }
          else {
            ((EntityTableSearchPanel) searchPanel).setAdvanced(false);
            panel.setSearchPanelVisible(true);
          }
        }
        else
          panel.setSearchPanelVisible(!panel.isSearchPanelVisible());
      }
    };
    final JButton ret = new JButton(toggleSearch);
    ret.setIcon(Images.loadImage("Filter16.gif"));
    ret.setToolTipText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getUpdateButton() {
    if (model.isReadOnly() || !model.getAllowMultipleUpdate() || !model.allowUpdate())
      return null;

    final ControlSet updateSet = getUpdateSelectedControlSet(null);
    final JPopupMenu menu = ControlProvider.createPopupMenu(updateSet);
    final JButton ret = new JButton(Images.loadImage("Modify16.gif"));
    ret.setToolTipText(updateSet.getDescription());
    UiUtil.linkToEnabledState(updateSet.getEnabledState(), ret);
    ret.addMouseListener(new MouseAdapter() {
      public void mouseReleased(final MouseEvent e) {
        menu.show(ret, e.getX(), e.getY()-menu.getPreferredSize().height);
      }
    });
    ret.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        menu.show(ret, (int) ret.getLocation().getX()+ret.getWidth()/2, (int) ret.getLocation().getY()+ret.getHeight()/2-menu.getPreferredSize().height);
      }
    });
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getDeleteButton() {
    if (model.isReadOnly() || !model.allowDelete())
      return null;

    final Control delete = getDeleteSelectedControl();
    delete.setName(null);
    delete.setIcon(Images.loadImage("Delete16.gif"));
    final JButton ret = ControlProvider.createButton(delete);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getClearSelectionButton() {
    final Control clearSelection = ControlFactory.methodControl(getModel().getTableModel(), "clearSelection", null,
            getModel().getTableModel().stSelectionEmpty.getReversedState(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));
    final JButton ret = ControlProvider.createButton(clearSelection);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getSelectionDownButton() {
    final Control selectionDown = ControlFactory.methodControl(getModel().getTableModel(), "selectionDown",
            Images.loadImage("Down16.gif"));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));
    final JButton ret = ControlProvider.createButton(selectionDown);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getSelectionUpButton() {
    final Control selectionUp = ControlFactory.methodControl(getModel().getTableModel(), "selectionUp",
            Images.loadImage("Up16.gif"));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));
    final JButton ret = ControlProvider.createButton(selectionUp);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getPrintButton() {
    final Control print = getPrintControl();
    print.setName("");
    print.setIcon(Images.loadImage("Print16.gif"));
    final JButton ret = ControlProvider.createButton(print);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private Dimension getSouthButtonSize() {
    return new Dimension(20,20);
  }

  private JButton getToggleEditPanelButton() {
    if (editPanel == null)
      return null;

    final Control toggle = ControlFactory.methodControl(this, "toggleEditPanelState",
            Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));
    final JButton ret = ControlProvider.createButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getToggleDetaiPanelButton() {
    if (detailEntityPanels.size() == 0)
      return null;

    final Control toggle = ControlFactory.methodControl(this, "toggleDetailPanelState",
            Images.loadImage("History16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_DETAIL_TIP));
    final JButton ret = ControlProvider.createButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JToggleButton getToggleSummaryPanelButton(final EntityTablePanel tablePanel) {
    final ToggleBeanPropertyLink toggle = ControlFactory.toggleControl(tablePanel, "summaryPanelVisible", null,
            tablePanel.evtTableSummaryPanelVisibleChanged);
    toggle.setIcon(Images.loadImage("Sum16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_SUMMARY_TIP));
    final JToggleButton ret = ControlProvider.createToggleButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private void disposeEditDialog() {
    if (editDialog != null) {
      editDialog.setVisible(false);
      editDialog.dispose();
      editDialog = null;
    }
  }

  private void disposeDetailDialog() {
    if (detailDialog != null) {
      detailDialog.setVisible(false);
      detailDialog.dispose();
      detailDialog = null;
    }
  }

  private JasperPrint initJasperPrintObject(final String reportPath, final HashMap reportParams)
          throws JRException, IOException {
    InputStream stream = null;
    try {
      if (reportPath.toUpperCase().startsWith("HTTP"))
        return JasperFillManager.fillReport(stream = new URL(reportPath).openStream(),
                reportParams, getModel().getTableModel().getJRDataSource());
      else
        return JasperFillManager.fillReport(reportPath, reportParams, getModel().getTableModel().getJRDataSource());
    }
    finally {
      if (stream != null)
        stream.close();
    }
  }

  private Property getPropertyToUpdate() throws UserCancelException {
    final JComboBox box = new JComboBox(new Vector<Property>(getUpdateProperties()));
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION)
      return (Property) box.getSelectedItem();
    else
      throw new UserCancelException();
  }

  private List<Property> getUpdateProperties() {
    final List<Property> ret = EntityRepository.get().getDatabaseProperties(getModel().getEntityID(), false, false, false);
    final ListIterator<Property> iter = ret.listIterator();
    while(iter.hasNext())
      if (iter.next().hasParentProperty())
        iter.remove();
    Collections.sort(ret, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return propertyOne.toString().compareTo(propertyTwo.toString());
      }
    });

    return ret;
  }

  private void bindModelEvents() {
    model.stActive.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (model.stActive.isActive()) {
          initialize();
          showPanelTab();
          prepareUI(true, false);
        }
      }
    });
    model.evtRefreshStarted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityPanel.this);
      }
    });
    model.evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
    });
  }

  private void initializeResizing() {
    final InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_LEFT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_DOWN);

    actionMap.put(DIV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, EntityPanel.this);
        if (parent != null)
          parent.resizePanel(RIGHT);
      }
    });
    actionMap.put(DIV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, EntityPanel.this);
        if (parent != null)
          parent.resizePanel(LEFT);
      }
    });
    actionMap.put(DIV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        resizePanel(DOWN);
      }
    });
    actionMap.put(DIV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        resizePanel(UP);
      }
    });
  }

  private void initializeNavigation() {
    final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getActionMap();
    final DefaultTreeModel applicationTreeModel = EntityApplicationModel.getApplicationModel().getApplicationTreeModel();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK, true), NAV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK, true), NAV_DOWN);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK, true), NAV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK, true), NAV_LEFT);

    actionMap.put(NAV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(UP, applicationTreeModel);
      }
    });
    actionMap.put(NAV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(DOWN, applicationTreeModel);
      }
    });
    actionMap.put(NAV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(RIGHT, applicationTreeModel);
      }
    });
    actionMap.put(NAV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(LEFT, applicationTreeModel);
      }
    });
  }

  private void navigate(final int direction, final DefaultTreeModel applicationTreeModel) {
    final EntityModel active = getActiveModel();
    if (active == null) //fallback on default if no active panel found
      activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().iterator().next());
    else {
      switch(direction) {
        case UP:
          activateModel(getParent(active));
          break;
        case DOWN:
          if (active.getDetailModels().size() > 0 && active.getLinkedDetailModels().size() > 0)
            activateModel(active.getLinkedDetailModel());
          else
            activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().iterator().next());
          break;
        case LEFT:
          if (!activateModel(getLeftSibling(active, applicationTreeModel))) //wrap around
            activateModel(getRightmostSibling(active, applicationTreeModel));
          break;
        case RIGHT:
          if (!activateModel(getRightSibling(active, applicationTreeModel))) //wrap around
            activateModel(getLeftmostSibling(active, applicationTreeModel));
          break;
      }
    }
  }

  private static boolean activateModel(final EntityModel model) {
    if (model != null)
      model.stActive.setActive(true);

    return model != null;
  }

  private EntityModel getActiveModel() {
    final Enumeration enu = ((DefaultMutableTreeNode) EntityApplicationModel.getApplicationModel().getApplicationTreeModel().getRoot()).breadthFirstEnumeration();
    while (enu.hasMoreElements()) {
      final EntityModel model = (EntityModel) ((DefaultMutableTreeNode) enu.nextElement()).getUserObject();
      if (model != null && model.stActive.isActive())
        return model;
    }

    return null;
  }

  private EntityModel getParent(final EntityModel entityModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) EntityApplicationModel.getApplicationModel().getApplicationTreeModel().getRoot(), entityModel);
    if (path != null) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      if (node.getParent() != null) {
        final EntityModel parent = (EntityModel) ((DefaultMutableTreeNode) node.getParent()).getUserObject();
        if (parent != null)
          return parent;
      }
    }

    return null;
  }

  private static EntityModel getRightSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node.getNextSibling() != null)
      return (EntityModel) node.getNextSibling().getUserObject();

    return null;
  }

  private static EntityModel getRightmostSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();

    return (EntityModel) ((DefaultMutableTreeNode) node.getLastChild()).getUserObject();
  }

  private static EntityModel getLeftSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node.getPreviousSibling() != null)
      return (EntityModel) node.getPreviousSibling().getUserObject();

    return null;
  }

  private static EntityModel getLeftmostSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();

    return (EntityModel) ((DefaultMutableTreeNode) node.getFirstChild()).getUserObject();
  }

  private static TreePath findObject(final DefaultMutableTreeNode root, final Object object) {
    if (object == null)
      return null;

    final Enumeration nodes = root.preorderEnumeration();
    while (nodes.hasMoreElements()) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
      if (node.getUserObject() == object)
        return new TreePath(node.getPath());
    }

    return null;
  }

  private static Container createDependenciesPanel(final Map<String, List<Entity>> dependencies,
                                                  final IEntityDbProvider dbProvider) throws UserException {
    try {
      final JPanel ret = new JPanel(new BorderLayout());
      final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
      tabPane.setUI(new BorderlessTabbedPaneUI());
      for (final Map.Entry<String, List<Entity>> entry : dependencies.entrySet()) {
        final List<Entity> dependantEntities = entry.getValue();
        if (dependantEntities.size() > 0)
          tabPane.addTab(entry.getKey(), createStaticEntityPanel(dependantEntities, dbProvider));
      }
      ret.add(tabPane, BorderLayout.CENTER);

      return ret;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private static void showDependenciesDialog(final Map<String, List<Entity>> dependencies,
                                            final IEntityDbProvider model,
                                            final JComponent dialogParent) throws UserException {
    JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, dialogParent);

      final JOptionPane optionPane = new JOptionPane(createDependenciesPanel(dependencies, model),
              JOptionPane.PLAIN_MESSAGE, JOptionPane.NO_OPTION, null,
              new String[] {Messages.get(Messages.CLOSE)});
      dialog = optionPane.createDialog(dialogParent,
              FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      UiUtil.resizeWindow(dialog, 0.4, new Dimension(800, 400));
      dialog.setLocationRelativeTo(dialogParent);
      dialog.setResizable(true);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }

    dialog.setVisible(true);
  }

  private static class ActivationFocusAdapter extends MouseAdapter {

    private final JComponent target;

    public ActivationFocusAdapter(final JComponent target) {
      this.target = target;
    }

    public void mouseReleased(MouseEvent e) {
      target.requestFocusInWindow();//activates this EntityPanel, see initFocusActivation()
    }
  }

  public static class EntityPanelInfo  implements Comparable {

    private final String caption;
    private final Class<? extends EntityPanel> entityPanelClass;
    private final Class<? extends EntityModel> entityModelClass;

    public EntityPanelInfo(final Class<? extends EntityModel> entityModelClass,
                           final Class<? extends EntityPanel> entityPanelClass) {
      this(null, entityModelClass, entityPanelClass);
    }

    public EntityPanelInfo(final String caption,
                           final Class<? extends EntityModel> entityModelClass,
                           final Class<? extends EntityPanel> entityPanelClass) {
      this.caption = caption == null ? "" : caption;
      this.entityModelClass = entityModelClass;
      this.entityPanelClass = entityPanelClass;
    }

    /**
     * @return Value for property 'caption'.
     */
    public String getCaption() {
      return caption;
    }

    /**
     * @return Value for property 'entityModelClass'.
     */
    public Class<? extends EntityModel> getEntityModelClass() {
      return entityModelClass;
    }

    /**
     * @return Value for property 'entityPanelClass'.
     */
    public Class<? extends EntityPanel> getEntityPanelClass() {
      return entityPanelClass;
    }

    public EntityPanel getInstance(final EntityModel model) throws UserException {
      if (model == null)
        throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
      try {
        return getEntityPanelClass().getConstructor().newInstance().setModel(model);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException ite) {
        if (ite.getCause() instanceof UserException)
          throw (UserException) ite.getCause();

        throw new UserException(ite.getCause());
      }
      catch (Exception e) {
        throw new UserException(e);
      }
    }

    public EntityPanel getInstance(final IEntityDbProvider provider) throws UserException {
      try {
        return getEntityPanelClass().getConstructor().newInstance().setModel(
            getEntityModelClass().getConstructor(IEntityDbProvider.class).newInstance(provider));
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException ite) {
        if (ite.getCause() instanceof UserException)
          throw (UserException) ite.getCause();

        throw new UserException(ite.getCause());
      }
      catch (Exception e) {
        throw new UserException(e);
      }
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
      if(this == obj)
        return true;
      if((obj == null) || (obj.getClass() != this.getClass()))
        return false;

      final EntityPanelInfo panelInfo = (EntityPanelInfo) obj;

      return getCaption().equals(panelInfo.getCaption())
              && getEntityModelClass().equals(panelInfo.getEntityModelClass())
              && getEntityPanelClass().equals(panelInfo.getEntityPanelClass());
    }

    /** {@inheritDoc} */
    public int hashCode() {
      int hash = 7;
      hash = 31 * hash + getCaption().hashCode();
      hash = 31 * hash + getEntityModelClass().hashCode();
      hash = 31 * hash + getEntityPanelClass().hashCode();

      return hash;
    }

    /** {@inheritDoc} */
    public int compareTo(Object o) {
      final String thisCompare = getCaption() == null ? entityPanelClass.getSimpleName() : getCaption();
      final String thatCompare = ((EntityPanelInfo)o).getCaption() == null
              ? ((EntityPanelInfo)o).entityPanelClass.getSimpleName() : ((EntityPanelInfo)o).getCaption();

      return thisCompare.compareTo(thatCompare);
    }
  }
}