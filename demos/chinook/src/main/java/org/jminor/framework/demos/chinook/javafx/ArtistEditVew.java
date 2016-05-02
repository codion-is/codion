/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.javafx;

import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class ArtistEditVew extends EntityEditView {

  public ArtistEditVew(final FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusProperty(Chinook.ARTIST_NAME);
    createTextField(Chinook.ARTIST_NAME);

    final BorderPane pane = new BorderPane();
    pane.setCenter(createPropertyPanel(Chinook.ARTIST_NAME));

    return pane;
  }
}
