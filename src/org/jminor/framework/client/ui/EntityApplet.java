/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.User;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

/**
 * User: Björn Darri
 * Date: 3.7.2010
 * Time: 22:49:33
 */
public class EntityApplet extends JApplet {

  private final EntityPanelProvider entityPanelProvider;

  public EntityApplet(final EntityPanelProvider entityPanelProvider) {
    this.entityPanelProvider = entityPanelProvider;
  }

  @Override
  public void init() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          final EntityPanel panel = entityPanelProvider.createInstance(new EntityDbRemoteProvider(new User("scott", "tiger"),
                  entityPanelProvider.getCaption(), entityPanelProvider.toString()));
          entityPanelProvider.setInstance(panel);
          panel.initializeUI();
          getContentPane().add(panel);
        }
      });
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroy() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        entityPanelProvider.getInstance().getModel().getDbProvider().disconnect();
      }
    });
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }
}
