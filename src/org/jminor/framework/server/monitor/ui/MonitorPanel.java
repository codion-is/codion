/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.DbConnection;
import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;
import org.jminor.framework.server.monitor.ClientTypeMonitor;
import org.jminor.framework.server.monitor.ConnectionPoolInstanceMonitor;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.framework.server.monitor.HostMonitor;
import org.jminor.framework.server.monitor.MonitorModel;
import org.jminor.framework.server.monitor.ServerMonitor;
import org.jminor.framework.server.monitor.UserInstanceMonitor;
import org.jminor.framework.server.monitor.UserMonitor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:11:06
 */
public class MonitorPanel extends JPanel {

  private final Event evtAlwaysOnTopChanged = new Event("MonitorPanel.evtAlwaysOnTopChanged");

  private final MonitorModel model;

  private JTree hostTree;
  private JFrame monitorFrame;

  private JPanel detailBasePanel;

//  private final ImageIcon hostIcon = Images.loadImage("host.gif");
//  private final ImageIcon serverIcon = Images.loadImage("server.gif");
//  private final ImageIcon userIcon = Images.loadImage("user.gif");
//  private final ImageIcon connectionIcon = Images.loadImage("connection.gif");

  public MonitorPanel() throws RemoteException {
    this(new MonitorModel(System.getProperty(FrameworkConstants.SERVER_HOST_NAME_PROPERTY)));
  }

  public MonitorPanel(final MonitorModel model) {
    this.model = model;
    initUI();
  }

  public MonitorModel getModel() {
    return model;
  }

  /**
   * @return Value for property 'alwaysOnTop'.
   */
  public boolean getAlwaysOnTop() {
    return monitorFrame != null && monitorFrame.isAlwaysOnTop();
  }

  /**
   * @param val Value to set for property 'alwaysOnTop'.
   */
  public void setAlwaysOnTop(final boolean val) {
    monitorFrame.setAlwaysOnTop(val);
    evtAlwaysOnTopChanged.fire();
  }

  public void disconnectSelected() throws RemoteException {
    throw new RuntimeException("MonitorPanel.disconnectSelected() has not been implemented");
  }

  public void exit() {
    System.exit(0);
  }

  public void setLookAndFeel() throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
    UiUtil.setLookAndFeel(monitorFrame);
  }

  public void setLoggingLevel() throws RemoteException {
    //move to server
  }

  public void refresh() throws RemoteException {
    model.refresh();
    UiUtil.expandAll(hostTree, new TreePath(hostTree.getModel().getRoot()), true);
  }

  private void initUI() {
    this.hostTree = initializeHostTree();
    setLayout(new BorderLayout());
    final JScrollPane treeScroller = new JScrollPane(hostTree);
    treeScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    treeScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    final JSplitPane splitBase = new JSplitPane();
    splitBase.setDividerSize(18);
    splitBase.setOneTouchExpandable(true);
    splitBase.setLeftComponent(treeScroller);
    detailBasePanel = new JPanel(new BorderLayout());
    splitBase.setRightComponent(detailBasePanel);
    add(splitBase, BorderLayout.CENTER);
  }

  private JTree initializeHostTree() {
    final JTree ret = new JTree(model.getTreeModel());
    ret.setRootVisible(false);
    ret.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {//for linux :|
          final Component selectedPanel = detailBasePanel.getComponents().length > 0 ?
                  detailBasePanel.getComponent(0) : null;
          if (selectedPanel instanceof IPopupProvider)
            ((IPopupProvider) selectedPanel).getPopupMenu().show(ret, e.getX(), e.getY());
        }
      }
    });
    ret.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) ret.getLastSelectedPathComponent();
        try {
          if (node == null)
            return;

          detailBasePanel.removeAll();
          JPanel detailPanel = null;
          if (node instanceof HostMonitor)
            detailPanel = new HostMonitorPanel((HostMonitor) node);
          else if (node instanceof ServerMonitor)
            detailPanel = new ServerMonitorPanel((ServerMonitor) node);
          else if (node instanceof ConnectionPoolMonitor)
            detailPanel = new ConnectionPoolMonitorPanel((ConnectionPoolMonitor) node);
          else if (node instanceof ConnectionPoolInstanceMonitor)
            detailPanel = new ConnectionPoolInstanceMonitorPanel((ConnectionPoolInstanceMonitor) node);
          else if (node instanceof UserMonitor)
            detailPanel = new UserMonitorPanel((UserMonitor) node);
          else if (node instanceof UserInstanceMonitor)
            detailPanel = new UserInstanceMonitorPanel((UserInstanceMonitor) node);
          else if (node instanceof ClientTypeMonitor)
            detailPanel = new ClientTypeMonitorPanel((ClientTypeMonitor) node);
          else if (node instanceof ClientInstanceMonitor)
            detailPanel = new ClientInstanceMonitorPanel((ClientInstanceMonitor) node);

          if (detailPanel != null)
            detailBasePanel.add(detailPanel, BorderLayout.CENTER);

          revalidate();
          repaint();
        }
        catch (RemoteException e1) {
          e1.printStackTrace();
          throw new RuntimeException(e1);
        }
      }
    });

    return ret;
  }

  private ControlSet initMainMenuControlSets() {
    final ControlSet ret = new ControlSet();
    final ControlSet file = new ControlSet("File", 'F');
    file.add(initExitControl());
    ret.add(file);
    final ControlSet view = new ControlSet("View", 'V');
    view.add(initRefreshControl());
    view.addSeparator();
    view.add(initAlwaysOnTopControl());
    view.addSeparator();
    view.add(initLookAndFeelControl());
    ret.add(view);

    return ret;
  }

  private Control initRefreshControl() {
    final Control ret = ControlFactory.methodControl(model, "refresh", "Refresh");
    ret.setMnemonic('R');

    return ret;
  }

  private Control initAlwaysOnTopControl() {
    final Control ret =
            ControlFactory.toggleControl(this, "alwaysOnTop", "Always on Top", evtAlwaysOnTopChanged);
    ret.setMnemonic('A');

    return ret;
  }

  private Control initLookAndFeelControl() {
    final Control ret =
            ControlFactory.methodControl(this, "setLookAndFeel", "Set look & feel");
    ret.setMnemonic('L');

    return ret;
  }

  private Control initExitControl() {
    return ControlFactory.methodControl(this, "exit", "Exit", null, null, 'X');
  }

  public static void main(final String[] args) {
    try {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      final User user = UiUtil.getUser(null, new User("scott", "tiger"));
      authenticate(user);
      new MonitorPanel().showFrame();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void authenticate(final User user) throws AuthenticationException, ClassNotFoundException {
    DbConnection db = null;
    try {
      db = new DbConnection(user);
      System.out.println(user + " is authenticated");
    }
    finally {
      if (db != null)
        db.disconnect();
    }
  }

  private void showFrame() {
    monitorFrame = UiUtil.createFrame(Images.loadImage("jminor_logo_red24.png").getImage());
    monitorFrame.setJMenuBar(ControlProvider.createMenuBar(initMainMenuControlSets()));
    monitorFrame.setTitle("JMinor - EntityDb Server Monitor");
    monitorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    monitorFrame.getContentPane().add(this);
    UiUtil.resizeWindow(monitorFrame, 0.75);
    UiUtil.centerWindow(monitorFrame);
    monitorFrame.setVisible(true);
  }
}
