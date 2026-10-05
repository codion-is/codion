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
package is.codion.manual.javadoc;

import is.codion.common.utilities.TypeReference;
import is.codion.common.utilities.proxy.ProxyBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link ProxyBuilder} javadoc snippets, each the region of the same name.
 */
final class ProxyBuilderSnippets {

  void usage() {
    List<String> list = new ArrayList<>(); // @start region=usage

    ProxyBuilder<List<String>> builder = ProxyBuilder.of(new TypeReference<List<String>>() {})
            .delegate(list)
            .method("add", Object.class, parameters -> {
              Object item = parameters.arguments().get(0);
              System.out.println("Adding: " + item);

              return parameters.delegate().add((String) item);
            })
            .method("size", parameters -> {
              System.out.println("Size");

              return parameters.delegate().size();
            });

    List<String> proxy1 = builder.build();

    // Builder can be reused and modified
    builder.method("remove", Object.class, parameters -> {
      Object item = parameters.arguments().get(0);
      System.out.println("Removing: " + item);

      return parameters.delegate().remove(item);
    });

    List<String> proxy2 = builder.build(); // Has all three methods
    // proxy1 still has only add() and size() methods proxied // @end
  }
}
