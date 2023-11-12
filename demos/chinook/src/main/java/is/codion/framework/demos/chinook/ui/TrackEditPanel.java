/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JPanel;
import java.util.function.Supplier;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(12);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Track.ALBUM_FK);

    createForeignKeySearchField(Track.ALBUM_FK);
    createTextField(Track.NAME);
    EntityComboBox mediaTypeBox = createForeignKeyComboBox(Track.MEDIATYPE_FK)
            .preferredWidth(120)
            .build();
    Supplier<EntityEditPanel> mediaTypeEditPanelSupplier = () ->
            new MediaTypeEditPanel(new SwingEntityEditModel(MediaType.TYPE, editModel().connectionProvider()));
    Control addMediaTypeControl = createAddControl(mediaTypeBox, mediaTypeEditPanelSupplier);
    Control editMediaTypeControl = createEditControl(mediaTypeBox, mediaTypeEditPanelSupplier);
    EntityComboBox genreBox = createForeignKeyComboBox(Track.GENRE_FK)
            .preferredWidth(140)
            .build();
    Supplier<EntityEditPanel> genreEditPanelSupplier = () ->
            new GenreEditPanel(new SwingEntityEditModel(Genre.TYPE, editModel().connectionProvider()));
    Control addGenreControl = createAddControl(genreBox, genreEditPanelSupplier);
    Control editGenreControl = createEditControl(genreBox, genreEditPanelSupplier);
    createTextFieldPanel(Track.COMPOSER);
    createIntegerField(Track.MILLISECONDS)
            .columns(5);

    ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(editModel().value(Track.MILLISECONDS));

    createIntegerField(Track.BYTES)
            .columns(6);
    createTextField(Track.UNITPRICE)
            .columns(4);

    JPanel mediaTypePanel = createEastButtonPanel(mediaTypeBox, addMediaTypeControl, editMediaTypeControl);
    JPanel genrePanel = createEastButtonPanel(genreBox, addGenreControl, editGenreControl);
    JPanel genreMediaTypePanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Track.GENRE_FK, genrePanel))
            .add(createInputPanel(Track.MEDIATYPE_FK, mediaTypePanel))
            .build();

    JPanel durationPanel = flexibleGridLayoutPanel(1, 3)
            .add(createInputPanel(Track.BYTES))
            .add(createInputPanel(Track.MILLISECONDS))
            .add(minutesSecondsValue.component())
            .build();

    JPanel unitPricePanel = borderLayoutPanel()
            .eastComponent(createInputPanel(Track.UNITPRICE))
            .build();

    setLayout(flexibleGridLayout(4, 2));
    addInputPanel(Track.ALBUM_FK);
    addInputPanel(Track.NAME);
    add(genreMediaTypePanel);
    addInputPanel(Track.COMPOSER);
    add(durationPanel);
    add(unitPricePanel);
  }
}