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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.javadoc;

import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.image.ImageView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.framework.model.SwingEntityTableModel;

/**
 * The {@link ImageView} javadoc snippets, each the region of the same name.
 */
final class ImageViewSnippets {

  void renderer(SwingEntityTableModel tableModel) {
    FilterTable<Entity, Attribute<?>> table = FilterTable.builder() // @start region=renderer
            .model(tableModel)
            .cellRenderer(Country.FLAG, byte[].class, renderer -> renderer
                    .component(ImageView.builder().buildValue()))
            .build(); // @end
  }
}
