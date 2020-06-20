/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.manual;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;
import static is.codion.framework.demos.petstore.domain.Petstore.Product;

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
    Entity insects = store.entity(Category.TYPE);
    insects.put(Category.NAME, "Insects");
    insects.put(Category.DESCRIPTION, "Creepy crawlies");

    connection.insert(insects);

    //populate a new product for the insect category
    Entity smallBeetles = store.entity(Product.TYPE);
    smallBeetles.put(Product.CATEGORY_FK, insects);
    smallBeetles.put(Product.NAME, "Small Beetles");
    smallBeetles.put(Product.DESCRIPTION, "Beetles on the smaller side");

    connection.insert(smallBeetles);

    //see what products are available for the Cats category
    Entity categoryCats = connection.selectSingle(Category.NAME, "Cats");

    List<Entity> catProducts = connection.select(Product.CATEGORY_FK, categoryCats);

    catProducts.forEach(System.out::println);
    // end::entitiesInAction[]
  }
}
