/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.tests.e2e.dbprovider;

import com.google.common.collect.Lists;
import java.sql.Connection;
import java.sql.Statement;
import org.apache.sentry.service.thrift.ServiceConstants.SentryPrincipalType;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestOwnerPrivilegesWithGrantOption extends TestOwnerPrivileges {
  @BeforeClass
  public static void setup() throws Exception {
    ownerPrivilegeGrantEnabled = true;

    TestOwnerPrivileges.setup();
  }

  /**
   * Verify that the owner with grant option can call alter table set owner on this table
   *
   * @throws Exception
   */
  @Test
  public void testAuthorizeAlterTableSetOwnerByOwner() throws Exception {
    String ownerRole = "owner_role";
    dbNames = new String[]{DB1};
    roles = new String[]{"admin_role", "create_db1", ownerRole};

    // create required roles, and assign them to USERGROUP1
    setupUserRoles(roles, statement);

    // create test DB
    statement.execute("DROP DATABASE IF EXISTS " + DB1 + " CASCADE");
    statement.execute("CREATE DATABASE " + DB1);

    // setup privileges for USER1
    statement.execute("GRANT CREATE ON DATABASE " + DB1 + " TO ROLE create_db1");
    statement.execute("USE " + DB1);

    // USER1_1 create table
    Connection connectionUSER1_1 = hiveServer2.createConnection(USER1_1, USER1_1);
    Statement statementUSER1_1 = connectionUSER1_1.createStatement();
    statementUSER1_1.execute("CREATE TABLE " + DB1 + "." + tableName1
        + " (under_col int comment 'the under column')");

    Connection connectionUSER2_1 = hiveServer2.createConnection(USER2_1, USER2_1);
    Statement statementUSER2_1 = connectionUSER2_1.createStatement();

    try {
      // user1_1 is owner of the table having all with grant on this table and can issue
      // command: alter table set owner for user
      statementUSER1_1
          .execute("ALTER TABLE " + DB1 + "." + tableName1 + " SET OWNER USER " + USER2_1);

      // verify privileges is transferred to USER2_1
      verifyTableOwnerPrivilegeExistForPrincipal(statement, SentryPrincipalType.USER,
          Lists.newArrayList(USER2_1),
          DB1, tableName1, 1);

      // alter table set owner for role
      statementUSER2_1
          .execute("ALTER TABLE " + DB1 + "." + tableName1 + " SET OWNER ROLE " + ownerRole);

      // verify privileges is transferred to ownerRole
      verifyTableOwnerPrivilegeExistForPrincipal(statement, SentryPrincipalType.ROLE,
          Lists.newArrayList(ownerRole),
          DB1, tableName1, 1);

    } finally {
      statement.close();
      connection.close();

      statementUSER1_1.close();
      connectionUSER1_1.close();

      statementUSER2_1.close();
      connectionUSER2_1.close();
    }
  }
}
