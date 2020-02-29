/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.manual;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public final class EntitiesInAction {

  public static void main(final String[] args) throws DatabaseException {
    // tag::entitiesInAction[]
    EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider()
            .setDomainClassName(Petstore.class.getName())
            .setClientTypeId("Manual")
            .setUser(User.parseUser("scott:tiger"));

    Domain store = connectionProvider.getDomain();

    EntityConnection connection = connectionProvider.getConnection();

    //populate a new category
    Entity insects = store.entity(T_CATEGORY);
    insects.put(CATEGORY_NAME, "Insects");
    insects.put(CATEGORY_DESCRIPTION, "Creepy crawlies");

    connection.insert(singletonList(insects));

    //populate a new product for the insect category
    Entity smallBeetles = store.entity(T_PRODUCT);
    smallBeetles.put(PRODUCT_CATEGORY_FK, insects);
    smallBeetles.put(PRODUCT_NAME, "Small Beetles");
    smallBeetles.put(PRODUCT_DESCRIPTION, "Beetles on the smaller side");

    connection.insert(singletonList(smallBeetles));

    //see what products are available for the Cats category
    Entity categoryCats = connection.selectSingle(T_CATEGORY, CATEGORY_NAME, "Cats");

    List<Entity> catProducts = connection.select(T_PRODUCT, PRODUCT_CATEGORY_FK, categoryCats);

    catProducts.forEach(System.out::println);
    // end::entitiesInAction[]
  }
}
