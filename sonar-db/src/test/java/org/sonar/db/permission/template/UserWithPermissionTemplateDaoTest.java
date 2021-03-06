/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.permission.template;

import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.PermissionQuery.builder;

public class UserWithPermissionTemplateDaoTest {

  private static final Long TEMPLATE_ID = 50L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession dbSession = dbTester.getSession();

  PermissionTemplateDao dao = dbTester.getDbClient().permissionTemplateDao();

  @Test
  public void select_logins() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().build(), TEMPLATE_ID)).containsOnly("user1", "user2", "user3");
    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().withAtLeastOnePermission().setPermission("user").build(),
      TEMPLATE_ID)).containsOnly("user1", "user2");
  }

  @Test
  public void return_no_logins_on_unknown_template_key() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().setPermission("user").withAtLeastOnePermission().build(), 999L)).isEmpty();
  }

  @Test
  public void select_only_logins_with_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUserLoginsByQueryAndTemplate(
      dbSession,
      builder().setPermission("user").withAtLeastOnePermission().build(),
      TEMPLATE_ID)).containsOnly("user1", "user2");
  }

  @Test
  public void select_only_enable_users() {
    dbTester.prepareDbUnit(getClass(), "select_only_enable_users.xml");

    List<String> result = dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().setPermission("user").build(), TEMPLATE_ID);
    assertThat(result).hasSize(2);

    // Disabled user should not be returned
    assertThat(result.stream().filter(input -> input.equals("disabledUser")).findFirst()).isEmpty();
  }

  @Test
  public void search_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<String> result = dao.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().withAtLeastOnePermission().setPermission("user").setSearchQuery("SEr1").build(),
      TEMPLATE_ID);
    assertThat(result).containsOnly("user1");

    result = dao.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().withAtLeastOnePermission().setPermission("user").setSearchQuery("user").build(),
      TEMPLATE_ID);
    assertThat(result).hasSize(2);
  }

  @Test
  public void should_be_sorted_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions_should_be_sorted_by_user_name.xml");

    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().build(), TEMPLATE_ID)).containsOnly("user1", "user2", "user3");
  }

  @Test
  public void should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().setPageIndex(1).setPageSize(2).build(), TEMPLATE_ID)).containsOnly("user1", "user2");
    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().setPageIndex(2).setPageSize(2).build(), TEMPLATE_ID)).containsOnly("user3");
    assertThat(dao.selectUserLoginsByQueryAndTemplate(dbSession, builder().setPageIndex(3).setPageSize(1).build(), TEMPLATE_ID)).containsOnly("user3");
  }

  @Test
  public void count_users() throws Exception {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.countUserLoginsByQueryAndTemplate(dbSession, builder().build(), TEMPLATE_ID)).isEqualTo(3);
    assertThat(dao.countUserLoginsByQueryAndTemplate(dbSession, builder().withAtLeastOnePermission().setPermission("user").build(), TEMPLATE_ID)).isEqualTo(2);
  }

  @Test
  public void select_user_permission_templates_by_template_and_logins() throws Exception {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 50L, singletonList("user1")))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsOnly(
        tuple("user1", UserRole.USER),
        tuple("user1", UserRole.ADMIN),
        tuple("user1", UserRole.CODEVIEWER)
      );

    assertThat(dao.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 50L, asList("user1", "user2", "user3")))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsOnly(
        tuple("user1", UserRole.USER),
        tuple("user1", UserRole.ADMIN),
        tuple("user1", UserRole.CODEVIEWER),
        tuple("user2", UserRole.USER)
      );

    assertThat(dao.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 50L, singletonList("unknown"))).isEmpty();
    assertThat(dao.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 50L, Collections.emptyList())).isEmpty();
    assertThat(dao.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 123L, singletonList("user1"))).isEmpty();
  }
}
