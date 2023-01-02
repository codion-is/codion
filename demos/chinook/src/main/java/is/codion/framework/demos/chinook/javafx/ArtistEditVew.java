/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.javafx;

import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class ArtistEditVew extends EntityEditView {

  public ArtistEditVew(FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusAttribute(Artist.NAME);
    createTextField(Artist.NAME);

    BorderPane pane = new BorderPane();
    pane.setCenter(createInputPanel(Artist.NAME));

    return pane;
  }
}
