/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.trustedanalytics.auth.gateway.hdfs;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.auth.gateway.hdfs.kerberos.KerberosProperties;
import org.trustedanalytics.auth.gateway.hdfs.utils.PathCreator;
import org.trustedanalytics.auth.gateway.spi.AuthorizableGatewayException;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HdfsGatewayTest {

  private static final String ORG = "test_org";

  private static final String USER = "test_user";

  private static final Path ORG_PATH = new Path("/org/test_org");

  private static final Path ORG_USERS_PATH = new Path("/org/test_org/user");

  private static final Path TMP_PATH = new Path("/org/test_org/tmp");

  private static final Path BROKER_PATH = new Path("/org/test_org/broker");

  private static final Path USER_PATH = new Path("/org/test_org/user/test_user");

  @Mock
  private HdfsClient hdfsClient;

  @Mock
  private PathCreator pathCreator;

  @Mock
  private KerberosProperties kerberosProperties;

  @InjectMocks
  private HdfsGateway hdfsGateway;

  private FsPermission userPermission;

  private FsPermission groupPermission;

  @Before
  public void init() throws IOException {
    userPermission = new FsPermission(FsAction.ALL, FsAction.NONE, FsAction.NONE);
    groupPermission = new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.NONE);
    when(pathCreator.createOrgPath("test_org")).thenReturn(ORG_PATH);
    when(pathCreator.createOrgBrokerPath("test_org")).thenReturn(BROKER_PATH);
    when(pathCreator.createOrgTmpPath("test_org")).thenReturn(TMP_PATH);
    when(pathCreator.createOrgUsersPath("test_org")).thenReturn(ORG_USERS_PATH);
    when(pathCreator.createUserPath("test_org", "test_user")).thenReturn(USER_PATH);
    when(kerberosProperties.getTechnicalPrincipal()).thenReturn("test_cf");
  }

  @Test
  public void addOrganization_createDirectoryCalled_creationSuccess()
      throws AuthorizableGatewayException, IOException {
    hdfsGateway.addOrganization(ORG, ORG);
    verify(hdfsClient).createDirectory(ORG_PATH, "test_org_admin", "test_org", userPermission);
    verify(hdfsClient).createDirectory(BROKER_PATH, "test_org_admin", "test_org", userPermission);
    verify(hdfsClient)
        .createDirectory(ORG_USERS_PATH, "test_org_admin", "test_org", userPermission);
    verify(hdfsClient).createDirectory(TMP_PATH, "test_org_admin", "test_org", groupPermission);
    verify(hdfsClient).setACLForDirectory(ORG_PATH, "test_cf");
    verify(hdfsClient).setACLForDirectory(BROKER_PATH, "test_cf");
  }

  @Test(expected = AuthorizableGatewayException.class)
  public void addOrganization_hdfsClientThrowIOException_throwAuthorizableGatewayException()
      throws AuthorizableGatewayException, IOException {
    doThrow(new IOException()).when(hdfsClient).createDirectory(ORG_PATH, "test_org_admin",
        "test_org", userPermission);
    hdfsGateway.addOrganization(ORG, ORG);
  }

  @Test
  public void removeOrganization_deleteDirectoryCalled_deleteDirectoryMethodCalled()
      throws AuthorizableGatewayException, IOException {
    hdfsGateway.removeOrganization(ORG, ORG);
    verify(hdfsClient).deleteDirectory(pathCreator.createOrgPath("test_org"));
  }

  @Test(expected = AuthorizableGatewayException.class)
  public void removeOrganization_hdfsClientThrowIOException_throwAuthorizableGatewayException()
      throws AuthorizableGatewayException, IOException {
    doThrow(new IOException()).when(hdfsClient).deleteDirectory(ORG_PATH);
    hdfsGateway.removeOrganization(ORG, ORG);
  }

  @Test
  public void addUserToOrg_createDirectoryCalled_creationSuccess()
      throws AuthorizableGatewayException, IOException {
    hdfsGateway.addUserToOrg(USER, ORG);
    verify(hdfsClient).createDirectory(USER_PATH, "test_user", "test_org", userPermission);
  }

  @Test(expected = AuthorizableGatewayException.class)
  public void addUserToOrg_hdfsClientThrowIOException_throwAuthorizableGatewayException()
      throws AuthorizableGatewayException, IOException {
    doThrow(new IOException()).when(hdfsClient).createDirectory(USER_PATH, "test_user", "test_org",
        userPermission);
    hdfsGateway.addUserToOrg(USER, ORG);
  }

  @Test
  public void removeUserFromOrg_deleteDirectoryCalled_deleteDirectoryMethodCalled()
      throws AuthorizableGatewayException, IOException {
    hdfsGateway.removeUserFromOrg(USER, ORG);
    verify(hdfsClient).deleteDirectory(USER_PATH);
  }

  @Test(expected = AuthorizableGatewayException.class)
  public void removeUserFromOrg_hdfsClientThrowIOException_throwAuthorizableGatewayException()
      throws AuthorizableGatewayException, IOException {
    doThrow(new IOException()).when(hdfsClient).deleteDirectory(USER_PATH);
    hdfsGateway.removeUserFromOrg(USER, ORG);
  }
}