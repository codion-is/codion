/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.DbException;
import org.jminor.common.db.TableStatus;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.AbstractSearchModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.FrameworkModelUtil;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.ui.property.CheckBoxPropertyLink;
import org.jminor.framework.client.ui.property.ComboBoxPropertyLink;
import org.jminor.framework.client.ui.property.DateTextPropertyLink;
import org.jminor.framework.client.ui.property.DoubleTextPropertyLink;
import org.jminor.framework.client.ui.property.IntTextPropertyLink;
import org.jminor.framework.client.ui.property.TextPropertyLink;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyChangeEvent;
import org.jminor.framework.model.PropertyListener;
import org.jminor.framework.model.Type;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import org.apache.log4j.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class FrameworkUiUtil {

  public final static char FILTER_INDICATOR = '*';

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

  private static final String NAV_UP = "navigateUp";
  private static final String NAV_DOWN = "navigateDown";
  private static final String NAV_RIGHT = "navigateRight";
  private static final String NAV_LEFT = "navigateLeft";

  private static final String DIV_LEFT = "divLeft";
  private static final String DIV_RIGHT = "divRight";
  private static final String DIV_UP = "divUp";
  private static final String DIV_DOWN = "divDown";

  public static final Dimension DIMENSION18x18 = new Dimension(18,18);

  private FrameworkUiUtil() {}

  public static void previewReport(final JasperPrint jp, final Container dialogParent) {
    JRViewer viewer = new JRViewer(jp);
    JDialog dlg = new JDialog(UiUtil.getParentWindow(dialogParent), Dialog.ModalityType.APPLICATION_MODAL);
    dlg.getContentPane().add(viewer);
    dlg.pack();
    dlg.setLocationRelativeTo(dialogParent);
    UiUtil.centerWindow(dlg);
    dlg.setVisible(true);
  }

  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[] {Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG});
    model.setSelectedItem(Util.getLoggingLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    Util.setLoggingLevel((Level) model.getSelectedItem());
  }

  public static Container createDependenciesPanel(final Map<String, List<Entity>> dependencies,
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

    final EntityPanel ret = new EntityPanel(true, false, false, EntityPanel.EMBEDDED, false) {
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

  public static void showDependenciesDialog(final Map<String, List<Entity>> dependencies,
                                            final IEntityDbProvider model,
                                            final JComponent dialogParent) throws UserException {
    JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, dialogParent);

      final JOptionPane optionPane = new JOptionPane(FrameworkUiUtil.createDependenciesPanel(dependencies, model),
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

  public static void toggleTableSearchPanel(final ActionEvent e, final JTable entityTable,
                                            final List<PropertyFilterPanel> columnFilterPanels) {
    final Point lp = entityTable.getTableHeader().getLocationOnScreen();
    final Point p = (Point) e.getSource();
    final Point pos = new Point((int) (lp.getX() + p.getX()), (int) (lp.getY() - p.getY()));
    final int col = entityTable.getColumnModel().getColumnIndexAtX((int) p.getX());
    toggleFilterPanel(pos, columnFilterPanels.get(col), entityTable);
  }

  public static List<PropertyFilterPanel> initializeFilterPanels(final TableColumnModel columnModel, final JTableHeader header,
                                                                 final List<AbstractSearchModel> searchModels,
                                                                 final boolean includeActivate, final boolean includeToggleAdv) {
    final List<PropertyFilterPanel> columnFilterPanels = new ArrayList<PropertyFilterPanel>(searchModels.size());
    for (final AbstractSearchModel searchModel : searchModels)
      columnFilterPanels.add(initializeFilterPanel(
              columnModel.getColumn(((PropertyFilterModel) searchModel).getColumnIndex()), header,
              (PropertyFilterModel) searchModel, includeActivate, includeToggleAdv));

    return columnFilterPanels;
  }

  /**
   * Initializes a JFrame according to the given parameters
   * @param mainApplicationPanel the JPanel to add to the center of the JFrame
   * @param applicationIcon the frame icon
   * @param title the title string for the JFrame
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param northToolBar true if a toolbar should be included
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximed then it's preferredSize is set to this value
   * @return an initialized, but non-visible JFrame
   * @throws UserException in case of a user exception
   */
  public static JFrame prepareFrame(final EntityApplicationPanel mainApplicationPanel, final ImageIcon applicationIcon,
                                    final String title, final boolean maximize, final boolean northToolBar,
                                    final boolean showMenuBar, final Dimension size) throws UserException {

    final JFrame frame = UiUtil.createFrame(applicationIcon != null ? applicationIcon.getImage() : null);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          mainApplicationPanel.exit();
        }
        catch(UserCancelException uc) {/**/}
      }
    });

    frame.setTitle(title);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(mainApplicationPanel, BorderLayout.CENTER);
    if (showMenuBar) {
      frame.setJMenuBar(createMenuBar(mainApplicationPanel));
    }
    if (northToolBar) {
      final JToolBar toolbar = mainApplicationPanel.getNorthToolBar();
      if (toolbar != null)
        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
    }
    if (size != null) {
      frame.setSize(size);
    }
    else {
      frame.pack();
      UiUtil.setSizeWithinScreenBounds(frame);
    }
    UiUtil.centerWindow(frame);
    if (maximize)
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

    return frame;
  }

  private static JMenuBar createMenuBar(final EntityApplicationPanel mainApplicationPanel) throws UserException {
    final List<ControlSet> menuControlSets = new ArrayList<ControlSet>();
    menuControlSets.add(mainApplicationPanel.getFileControlSet());
    menuControlSets.add(mainApplicationPanel.getViewControlSet());
    menuControlSets.add(mainApplicationPanel.getToolsControlSet());
    final ControlSet supportModelcontrolSet = mainApplicationPanel.getSupportModelControlSet();
    if (supportModelcontrolSet != null)
      menuControlSets.add(supportModelcontrolSet);
    final ControlSet[] additionalMenus = mainApplicationPanel.getAdditionalMenuControlSet();
    if (additionalMenus != null && additionalMenus.length > 0)
      menuControlSets.addAll(Arrays.asList(additionalMenus));

    menuControlSets.add(mainApplicationPanel.getHelpControlSet());

    return ControlProvider.createMenuBar(menuControlSets.toArray(new ControlSet[menuControlSets.size()]));
  }

  private static void toggleFilterPanel(final Point position, final PropertyFilterPanel columnFilterPanel,
                                        final Container parent) {
    if (columnFilterPanel.isDialogActive())
      columnFilterPanel.inactivateDialog();
    else
      columnFilterPanel.activateDialog(parent, position);
  }

  private static PropertyFilterPanel initializeFilterPanel(final TableColumn tableColumn, final JTableHeader header,
                                                           final PropertyFilterModel searchModel,
                                                           final boolean includeActivate, final boolean includeToggleAdv) {
    final PropertyFilterPanel ret = new PropertyFilterPanel(searchModel, includeActivate, includeToggleAdv);
    ret.getModel().evtSearchStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (ret.getModel().isSearchEnabled())
          addFilterIndicator(tableColumn);
        else
          removeFilterIndicator(tableColumn);

        header.repaint();
      }
    });
    if (ret.getModel().isSearchEnabled())
      addFilterIndicator(tableColumn);

    return ret;
  }

  private static void addFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() > 0)
      if (val.charAt(0) != FILTER_INDICATOR)
        val = FILTER_INDICATOR + val;

    column.setHeaderValue(val);
  }

  private static void removeFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() > 0 && val.charAt(0) == FILTER_INDICATOR)
      val = val.substring(1);

    column.setHeaderValue(val);
  }

  public static void initializeResizing(final EntityPanel panel) {
    final InputMap inputMap = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = panel.getActionMap();
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
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class,  panel);
        if (parent != null)
          parent.resizePanel(RIGHT);
      }
    });
    actionMap.put(DIV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class,  panel);
        if (parent != null)
          parent.resizePanel(LEFT);
      }
    });
    actionMap.put(DIV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        panel.resizePanel(DOWN);
      }
    });
    actionMap.put(DIV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        panel.resizePanel(UP);
      }
    });
  }

  public static void initializeNavigation(final InputMap inputMap, final ActionMap actionMap) {
    initializeNavigation(inputMap, actionMap, EntityApplicationModel.getApplicationModel().getApplicationTreeModel());
  }

  public static void initializeNavigation(final InputMap inputMap, final ActionMap actionMap,
                                          final DefaultTreeModel applicationTreeModel) {
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

  private static void navigate(final int direction, final DefaultTreeModel applicationTreeModel) {
    final EntityModel active = getActivePanel(applicationTreeModel);
    if (active == null) //fallback on default if no active panel found
      activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().values().iterator().next());
    else {
      switch(direction) {
        case UP:
          activateModel(getParent(active, applicationTreeModel));
          break;
        case DOWN:
          if (active.getDetailModels().size() > 0 && active.getLinkedDetailModels().size() > 0)
            activateModel(active.getLinkedDetailModel());
          else
            activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().values().iterator().next());
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

  public static EntityModel getActivePanel(final DefaultTreeModel applicationTreeModel) {
    final Enumeration enu = ((DefaultMutableTreeNode) applicationTreeModel.getRoot()).breadthFirstEnumeration();
    while (enu.hasMoreElements()) {
      final EntityModel model = (EntityModel) ((DefaultMutableTreeNode) enu.nextElement()).getUserObject();
      if (model != null && model.stActive.isActive())
        return model;
    }

    return null;
  }

  private static EntityModel getParent(final EntityModel panel, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), panel);
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

  public static void showEntityPanelDialog(final EntityPanelInfo appInfo,
                                           final IEntityDbProvider dbProvider, final JPanel owner) throws UserException {
    showEntityPanelDialog(appInfo, dbProvider, owner, false);
  }

  public static void showEntityPanelDialog(final EntityPanelInfo appInfo,
                                           final IEntityDbProvider dbProvider, final JPanel owner,
                                           final boolean modalDialog) throws UserException {
    final JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, owner);
      final EntityPanel entityPanel = appInfo.getInstance(dbProvider);
      entityPanel.initialize();
      final DefaultTreeModel applicationTree = FrameworkModelUtil.createApplicationTree(Arrays.asList(entityPanel.getModel()));
      initializeNavigation(entityPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
              entityPanel.getActionMap(), applicationTree);
      initializeResizing(entityPanel);

      dialog = new JDialog(UiUtil.getParentWindow(owner), appInfo.getCaption());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityPanel, BorderLayout.CENTER);
      final Action closeAction = new AbstractAction(Messages.get(Messages.CLOSE)) {
        public void actionPerformed(ActionEvent e) {
          dialog.dispose();
        }
      };
      final JButton btnClose = new JButton(closeAction);
      btnClose.setMnemonic('L');
      dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
      dialog.getRootPane().getActionMap().put("close", closeAction);
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(btnClose);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
      if (entityPanel.usePreferredSize())
        dialog.pack();
      else
        UiUtil.resizeWindow(dialog, 0.5, new Dimension(800, 400));
      dialog.setLocationRelativeTo(owner);
      if (modalDialog)
        dialog.setModal(true);
      dialog.setResizable(true);
    }
    finally {
      UiUtil.setWaitCursor(false, owner);
    }
    dialog.setVisible(true);
  }

  public static void handleException(final Throwable exception,
                                     final String entityID, final JComponent dialogParent) {
    if (exception instanceof UserCancelException)
      return;
    if (exception instanceof DbException)
      handleDbException((DbException) exception, entityID, dialogParent);
    else if (exception instanceof JRException && exception.getCause() != null)
      handleException(exception.getCause(), entityID, dialogParent);
    else if (exception instanceof UserException && exception.getCause() instanceof DbException)
      handleDbException((DbException) exception.getCause(), entityID, dialogParent);
    else {
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), getMessageTitle(exception), exception.getMessage(), exception);
    }
  }

  private static String getMessageTitle(final Throwable e) {
    if (e instanceof FileNotFoundException)
      return FrameworkMessages.get(FrameworkMessages.UNABLE_TO_OPEN_FILE);

    return Messages.get(Messages.EXCEPTION);
  }

  public static void handleDbException(final DbException dbException,
                                       final String entityID, final JComponent dialogParent) {
    if (dbException.isDeleteException()) {
      JOptionPane.showMessageDialog(dialogParent, dbException.getTableDependencyInfo(),
              FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND), JOptionPane.ERROR_MESSAGE);
    }
    else if (dbException.isInsertNullValueException()) {
      String columnName = dbException.getNullErrorColumnName().toLowerCase();
      if (entityID != null) {
        if (EntityRepository.get().hasProperty(entityID, columnName)) {
          final Property property = EntityRepository.get().getProperty(entityID, columnName);
          if (property.getCaption() != null)
            columnName = property.getCaption();
        }
      }

      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION),
              FrameworkMessages.get(FrameworkMessages.VALUE_MISSING) + ": " + columnName, dbException);
    }
    else {
      String errMsg = dbException.getORAErrorMessage();
      if (errMsg == null || errMsg.length() == 0) {
        if (dbException.getCause() == null)
          errMsg = trimMessage(dbException);
        else
          errMsg = trimMessage(dbException.getCause());
      }
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION), errMsg, dbException);
    }
  }

  private static String trimMessage(final Throwable e) {
    final String msg = e.getMessage();
    if (msg.length() > 50)
      return msg.substring(0, 50) + "...";

    return msg;
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle) throws UserCancelException {
    return selectEntities(lookupModel, owner, singleSelection, dialogTitle, null, false);
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle,
                                            final Dimension preferredSize, final boolean simpleSearchPanel) throws UserCancelException {
    final ArrayList<Entity> selected = new ArrayList<Entity>();
    final JDialog dialog = new JDialog(owner, dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        final List<Entity> entities = lookupModel.getSelectedEntities();
        for (final Entity entity : entities)
          selected.add(entity);
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(ActionEvent e) {
        selected.add(null);//hack to indicate cancel
        dialog.dispose();
      }
    };
    final Action searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(ActionEvent e) {
        try {
          lookupModel.forceRefresh();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    };

    final EntityTablePanel entityPanel = new EntityTablePanel(lookupModel, null, false) {
      protected void bindEvents() {
        super.bindEvents();
        evtTableDoubleClick.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (!getTableModel().getSelectionModel().isSelectionEmpty())
              okAction.actionPerformed(e);
          }
        });
      }
      protected JPanel initializeSearchPanel() {
        return simpleSearchPanel ? initializeSimpleSearchPanel() : initializeAdvancedSearchPanel();
      }
    };
    entityPanel.setSearchPanelVisible(true);
    if (singleSelection)
      entityPanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    final JButton btnClose  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
    final JButton btnSearch = new JButton(searchAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    dialog.getRootPane().getActionMap().put("cancel", cancelAction);
    entityPanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    btnClose.setMnemonic('L');
    btnCancel.setMnemonic('H');
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null)
      entityPanel.setPreferredSize(preferredSize);
    dialog.add(entityPanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
    buttonPanel.add(btnSearch);
    buttonPanel.add(btnClose);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnClose);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    if (selected.size() == 1 && selected.contains(null))
      throw new UserCancelException();
    else
      return selected;
  }

  public static JDialog showInDialog(final Window owner, final JComponent panel, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction) {
    return showInDialog(owner, panel, modal, title, includeButtonPanel,disposeOnOk, okAction, null);
  }

  public static JDialog showInDialog(final Window owner, final JComponent panel, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction,
                                     final Dimension size) {
    return showInDialog(owner, panel, modal, title, includeButtonPanel,disposeOnOk, okAction, size, null, null);
  }

  public static JDialog showInDialog(final Window owner, final JComponent panel, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction,
                                     final Dimension size, final Point location, final Action closeAction) {
    final JDialog dialog = new JDialog(owner, title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (closeAction != null) {
      dialog.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          closeAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }
    final Action ok = new AbstractAction(
            okAction != null ? (String) okAction.getValue(Action.NAME) : Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        if (okAction != null)
          okAction.actionPerformed(e);
        if (disposeOnOk) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      }
    };
    if (includeButtonPanel) {
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
      final JButton okButton = new JButton(ok);
      buttonPanel.add(okButton);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
    }
    dialog.add(panel, BorderLayout.CENTER);
    if (size == null)
      dialog.pack();
    else
      dialog.setSize(size);
    if (location == null)
      dialog.setLocationRelativeTo(owner);
    else
      dialog.setLocation(location);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  public static JDialog showInDialog(final Container owner, final JComponent component, final boolean modal, final String title,
                                     final Dimension size, final JButton defaultButton, final Event closeEvent) {
    final JDialog dialog = new JDialog(UiUtil.getParentWindow(owner), title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (closeEvent != null) {
      closeEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          dialog.dispose();
        }
      });
    }
    if (defaultButton != null)
      dialog.getRootPane().setDefaultButton(defaultButton);
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

    dialog.add(component, BorderLayout.CENTER);
    if (size == null)
      dialog.pack();
    else
      dialog.setSize(size);

    dialog.setLocationRelativeTo(owner);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  public static UiUtil.DateInputPanel createDateChooserPanel(final Date initialValue, final AbstractDateMaskFormat maskFormat) {
    final JFormattedTextField txtField = UiUtil.createFormattedField(maskFormat.getDateMask());
    txtField.setText(maskFormat.format(initialValue == null ? new Date() : initialValue));

    return new UiUtil.DateInputPanel(txtField, maskFormat, new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final Date d = UiUtil.getDateFromUser(initialValue, FrameworkMessages.get(FrameworkMessages.SELECT_DATE), txtField);
          final String dString = maskFormat.format(d);
          txtField.setText(dString);
        }
        catch (UserCancelException e1) {/**/}
      }
    }, null);
  }

  public static JTextField createDateChooserField(final Date initialValue, final JComponent parent) {
    final JTextField txtField =
            new JTextField(ShortDashDateFormat.get().format(initialValue == null ? new Date() : initialValue));
    txtField.setEditable(false);
    txtField.addMouseListener(new MouseAdapter() {
      public void mouseClicked(final MouseEvent e) {
        try {
          final Date d = UiUtil.getDateFromUser(initialValue, FrameworkMessages.get(FrameworkMessages.SELECT_DATE), parent);
          txtField.setText(ShortDashDateFormat.get().format(d));
        }
        catch (UserCancelException e1) {/**/}
      }
    });

    return txtField;
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel) {
    return createCheckBox(property, entityModel, null);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel,
                                         final State enabledState) {
    return createCheckBox(property, entityModel, enabledState, true);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel,
                                         final State enabledState, final boolean includeCaption) {
    final JCheckBox ret = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    if (!includeCaption)
      ret.setToolTipText(property.getCaption());
    UiUtil.linkToEnabledState(enabledState, ret);
    new CheckBoxPropertyLink(entityModel, property, ret.getModel());

    return ret;
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityModel entityModel) {
    return createBooleanComboBox(property, entityModel, null);
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityModel entityModel,
                                                      final State enabledState) {
    final SteppedComboBox box = createComboBox(property, entityModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(40);

    return box;
  }

  public static EntityComboBox createEntityComboBox(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final EntityPanelInfo appInfo,
                                                    final boolean newButtonFocusable) {
    return createEntityComboBox(property, entityModel, appInfo, newButtonFocusable, null);
  }

  public static EntityComboBox createEntityComboBox(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final EntityPanelInfo appInfo,
                                                    final boolean newButtonFocusable, final State enabledState) {
    final EntityComboBoxModel boxModel = entityModel.getEntityComboBoxModel(property);
    if (boxModel == null)
      throw new RuntimeException("No EntityComboBoxModel found in model: " + entityModel + " for property: " + property);
    final EntityComboBox ret = new EntityComboBox(boxModel, appInfo, newButtonFocusable);
    UiUtil.linkToEnabledState(enabledState, ret);
    new ComboBoxPropertyLink(entityModel, property, null, ret);
    MaximumMatch.enable(ret);

    return ret;
  }

  public static JPanel createEntityFieldPanel(final Property property, final EntityModel model,
                                              final EntityTableModel lookupModel) {
    final JPanel ret = new JPanel(new BorderLayout(5,5));
    final JTextField txt = createEntityField(property, model);
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final List<Entity> selected = FrameworkUiUtil.selectEntities(lookupModel,
                  UiUtil.getParentWindow(ret), true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false);
          model.uiSetValue(property, selected.size() > 0 ? selected.get(0) : null);
        }
        catch (UserCancelException e1) {
          //
        }
      }
    });
    btn.setPreferredSize(FrameworkUiUtil.DIMENSION18x18);

    ret.add(txt, BorderLayout.CENTER);
    ret.add(btn, BorderLayout.EAST);

    return ret;
  }

  public static JTextField createEntityField(final Property property, final EntityModel model) {
    final JTextField txt = new JTextField();
    txt.setEditable(false);
    model.getPropertyChangeEvent(property).addListener(new PropertyListener() {
      protected void propertyChanged(final PropertyChangeEvent e) {
        txt.setText(e.getNewValue() == null ? "" : e.getNewValue().toString());
      }
    });

    return txt;
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityModel entityModel,
                                               final ComboBoxModel model, final State enabledState) {
    return createComboBox(property, entityModel, model, enabledState, false);
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityModel entityModel,
                                               final ComboBoxModel model, final State enabledState,
                                               final boolean editable) {
    final SteppedComboBox ret = new SteppedComboBox(model);
    ret.setEditable(editable);
    UiUtil.linkToEnabledState(enabledState, ret);
    new ComboBoxPropertyLink(entityModel, property, null, ret);

    return ret;
  }

  public static UiUtil.DateInputPanel createDateFieldPanel(final Property property, final EntityModel entityModel,
                                                           final AbstractDateMaskFormat dateMaskFormat,
                                                           final LinkType linkType,
                                                           final boolean includeButton) {
    return createDateFieldPanel(property, entityModel, dateMaskFormat, linkType, includeButton, null);
  }

  public static UiUtil.DateInputPanel createDateFieldPanel(final Property property, final EntityModel entityModel,
                                                           final AbstractDateMaskFormat dateMaskFormat,
                                                           final LinkType linkType,
                                                           final boolean includeButton, final State enabledState) {
    if (property.getPropertyType() != Type.SHORT_DATE && property.getPropertyType() != Type.LONG_DATE)
      throw new IllegalArgumentException("Property " + property + " is not a date property");

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, entityModel, linkType,
            dateMaskFormat.getDateMask(), true, dateMaskFormat, enabledState);

    return new UiUtil.DateInputPanel(field, dateMaskFormat, includeButton ? new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final Date currentValue = (Date) entityModel.getValue(property);
          entityModel.uiSetValue(property, UiUtil.getDateFromUser(
                  EntityUtil.isValueNull(property.getPropertyType(), currentValue) ? null : currentValue,
                  FrameworkMessages.get(FrameworkMessages.SELECT_DATE), field));
        }
        catch (UserCancelException e1) {
          //
        }
      }
    } : null, enabledState);
  }

  public static JTextArea createTextArea(final Property property, final EntityModel entityModel) {
    return createTextArea(property, entityModel, -1, -1);
  }

  public static JTextArea createTextArea(final Property property, final EntityModel entityModel,
                                         final int rows, final int columns) {
    if (property.getPropertyType() != Type.STRING)
      throw new RuntimeException("Cannot create a text area for a non-string property");

    final JTextArea ret = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    ret.setLineWrap(true);
    ret.setWrapStyleWord(true);

    new TextPropertyLink(entityModel, property, ret, null, true, LinkType.READ_WRITE);

    return ret;
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel) {
    return createTextField(property, entityModel, LinkType.READ_WRITE, null, true);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, null);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final State enabledState) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, null, enabledState);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final AbstractDateMaskFormat dateFormat,
                                           final State enabledState) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, dateFormat,
            enabledState, false);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final AbstractDateMaskFormat dateFormat,
                                           final State enabledState, final boolean valueContainsLiteralCharacters) {
    final boolean transferFocusOnEnter = FrameworkSettings.get().transferTextFieldFocusOnEnter;
    final JTextField ret;
    switch (property.getPropertyType()) {
      case STRING:
        new TextPropertyLink(entityModel, property, ret = formatMaskString == null
                ? new TextFieldPlus(transferFocusOnEnter) :
                UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters, false, transferFocusOnEnter),
                null, immediateUpdate, linkType);
        break;
      case INT:
        new IntTextPropertyLink(entityModel, property,
                (IntField) (ret = new IntField(transferFocusOnEnter, 0)),
                null, immediateUpdate, linkType, null);
        break;
      case DOUBLE:
        new DoubleTextPropertyLink(entityModel, property,
                (DoubleField) (ret = new DoubleField(transferFocusOnEnter, 0)),
                null, immediateUpdate, linkType, null);
        break;
      case SHORT_DATE:
      case LONG_DATE:
        new DateTextPropertyLink(entityModel, property,
                (JFormattedTextField) (ret = UiUtil.createFormattedField(formatMaskString, true)),
                null, linkType, null, dateFormat, formatMaskString);
        break;
      default:
        throw new IllegalArgumentException("Not a text based property: " + property);
    }
    UiUtil.linkToEnabledState(enabledState, ret);

    return ret;
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel) {
    return createPropertyComboBox(propertyID, entityModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(propertyID, entityModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(propertyID, entityModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue) {
    return createPropertyComboBox(EntityRepository.get().getProperty(entityModel.getEntityID(), propertyID),
            entityModel, refreshEvent, state, nullValue);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel) {
    return createPropertyComboBox(property, entityModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(property, entityModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(property, entityModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue) {
    return createPropertyComboBox(property, entityModel, refreshEvent, state, nullValue, false);
  }


  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue, final boolean editable) {
    final SteppedComboBox ret = createComboBox(property, entityModel,
            entityModel.getPropertyComboBoxModel(property, refreshEvent, nullValue), state, editable);
    if (!editable)
      MaximumMatch.enable(ret);

    return ret;
  }

  public static TableColumnModel getTableColumnModel(final List<Property> columnProperties,
                                                     final TableCellRenderer renderer) {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final TableColumn[] columns = getTableColumns(columnProperties);
    for (final TableColumn column : columns) {
      column.setResizable(true);
      if (renderer != null)
        column.setCellRenderer(renderer);
      columnModel.addColumn(column);
    }

    return columnModel;
  }

  public static TableColumn[] getTableColumns(final List<Property> columnProperties) {
    final ArrayList<TableColumn> ret = new ArrayList<TableColumn>(columnProperties.size());
    int i = 0;
    for (final Property property : columnProperties) {
      final TableColumn col = new TableColumn(i++);
      col.setIdentifier(property);
      col.setHeaderValue(property.getCaption());
      final int prw = property.getPreferredColumnWidth();
      if (prw >= 0)
        col.setMinWidth(prw);
      ret.add(col);
    }

    return ret.toArray(new TableColumn[ret.size()]);
  }
}
