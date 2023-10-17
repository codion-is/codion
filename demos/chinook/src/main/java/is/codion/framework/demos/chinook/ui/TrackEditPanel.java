/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
    Control newMediaTypeControl = createInsertControl(mediaTypeBox, mediaTypeEditPanelSupplier);
    Control editMediaTypeControl = createEditControl(mediaTypeBox, mediaTypeEditPanelSupplier);
    EntityComboBox genreBox = createForeignKeyComboBox(Track.GENRE_FK)
            .preferredWidth(140)
            .build();
    Supplier<EntityEditPanel> genreEditPanelSupplier = () ->
            new GenreEditPanel(new SwingEntityEditModel(Genre.TYPE, editModel().connectionProvider()));
    Control newGenreControl = createInsertControl(genreBox, genreEditPanelSupplier);
    Control editGenreControl = createEditControl(genreBox, genreEditPanelSupplier);
    createTextInputPanel(Track.COMPOSER);
    createIntegerField(Track.MILLISECONDS)
            .columns(5);

    ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(editModel().value(Track.MILLISECONDS));

    createIntegerField(Track.BYTES)
            .columns(6);
    createTextField(Track.UNITPRICE)
            .columns(4);

    JPanel mediaTypePanel = createEastButtonPanel(mediaTypeBox, newMediaTypeControl, editMediaTypeControl);
    JPanel genrePanel = createEastButtonPanel(genreBox, newGenreControl, editGenreControl);
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