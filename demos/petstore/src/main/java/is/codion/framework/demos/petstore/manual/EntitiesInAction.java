/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.manual;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnection;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.demos.petstore.domain.Petstore;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.Entity;

import java.util.List;

import static dev.codion.framework.demos.petstore.domain.Petstore.*;

public final class EntitiesInAction {

  public static void main(final String[] args) throws DatabaseException {
    // tag::entitiesInAction[]
    EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider()
            .setDomainClassName(Petstore.class.getName())
            .setClientTypeId("Manual")
            .setUser(Users.parseUser("scott:tiger"));

    Entities store = connectionProvider.getEntities();

    EntityConnection connection = connectionProvider.getConnection();

    //populate a new category
    Entity insects = store.entity(T_CATEGORY);
    insects.put(CATEGORY_NAME, "Insects");
    insects.put(CATEGORY_DESCRIPTION, "Creepy crawlies");

    connection.insert(insects);

    //populate a new product for the insect category
    Entity smallBeetles = store.entity(T_PRODUCT);
    smallBeetles.put(PRODUCT_CATEGORY_FK, insects);
    smallBeetles.put(PRODUCT_NAME, "Small Beetles");
    smallBeetles.put(PRODUCT_DESCRIPTION, "Beetles on the smaller side");

    connection.insert(smallBeetles);

    //see what products are available for the Cats category
    Entity categoryCats = connection.selectSingle(T_CATEGORY, CATEGORY_NAME, "Cats");

    List<Entity> catProducts = connection.select(T_PRODUCT, PRODUCT_CATEGORY_FK, categoryCats);

    catProducts.forEach(System.out::println);
    // end::entitiesInAction[]
  }
}
