/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.demos.chinook.domain.Chinook;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.Enumeration;

public class EntitiesTest {

  @Test
  public void getDependencyTreeModel() {
    new Chinook();
    final TreeModel model = Entities.getDependencyTreeModel();
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    final Enumeration tree = root.depthFirstEnumeration();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_MEDIATYPE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_CUSTOMER, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_EMPLOYEE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_GENRE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLIST, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_INVOICELINE, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_PLAYLISTTRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_TRACK, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ALBUM, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertEquals(Chinook.T_ARTIST, node.getUserObject());
    node = (DefaultMutableTreeNode) tree.nextElement();
    assertNull(node.getUserObject());
  }
}
