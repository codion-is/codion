/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.User;
import org.jminor.framework.server.provider.RemoteEntityConnectionProvider;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;
import java.util.UUID;

/**
 * User: Björn Darri
 * Date: 3.7.2010
 * Time: 22:49:33
 */
public class EntityApplet extends JApplet {

  private final EntityPanelProvider entityPanelProvider;
  private EntityPanel instance;

  public EntityApplet(final EntityPanelProvider entityPanelProvider) {
    this.entityPanelProvider = entityPanelProvider;
  }

  @Override
  public final void init() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          final EntityPanel panel = entityPanelProvider.createPanel(new RemoteEntityConnectionProvider(new User("scott", "tiger"),
                  UUID.randomUUID(), entityPanelProvider.toString()));
          instance = panel;
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
  public final void destroy() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        instance.getModel().getConnectionProvider().disconnect();
      }
    });
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}
