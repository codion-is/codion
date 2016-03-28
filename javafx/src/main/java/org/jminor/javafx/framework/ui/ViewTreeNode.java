/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import java.util.List;

public interface ViewTreeNode {

  ViewTreeNode getParentView();

  ViewTreeNode getPreviousSiblingView();

  ViewTreeNode getNextSiblingView();

  List<? extends ViewTreeNode> getChildViews();
}
