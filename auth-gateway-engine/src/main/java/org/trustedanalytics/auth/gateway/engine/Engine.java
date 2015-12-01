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
package org.trustedanalytics.auth.gateway.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.auth.gateway.spi.Authorizable;
import org.trustedanalytics.auth.gateway.spi.AuthorizableGatewayException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class Engine {

    private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

    private List<Authorizable> supportedAuthorizables;
    private long timeoutInSeconds;


    public Engine(List<Authorizable> supportedAuthorizables, long timeoutInSeconds) {
        this.supportedAuthorizables = supportedAuthorizables;
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public void addUser(String userId, String userName) throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.addUser(userId, userName),
                authorizable.getName(), "adding user"));
        }

        runTasks(tasks, "Error adding user");
    }

    public void addOrganization(String orgId, String orgName) throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.addOrganization(orgId, orgName),
                authorizable.getName(), "adding organization"));
        }

        runTasks(tasks, "Error adding organization");
    }

    public void addUserToOrg(String userId, String orgId) throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.addUserToOrg(userId, orgId),
                authorizable.getName(), "adding user to organization"));
        }

        runTasks(tasks, "Error adding user to organization");
    }

    public void removeUser(String userId, String userName) throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.removeUser(userId, userName),
                authorizable.getName(), "removing user"));
        }

        runTasks(tasks, "Error removing user");
    }

    public void removeOrganization(String orgId, String orgName)
        throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.removeOrganization(orgId, orgName),
                authorizable.getName(), "removing organization"));
        }

        runTasks(tasks, "Error removing organization");
    }

    public void removeUserFromOrg(String userId, String orgId) throws AuthorizableGatewayException {
        List<CompletableFuture<Void>> tasks = new LinkedList<>();
        for (Authorizable authorizable : supportedAuthorizables) {
            tasks.add(createFutureForMethod(() -> authorizable.removeUserFromOrg(userId, orgId),
                authorizable.getName(), "removing user from organization"));
        }

        runTasks(tasks, "Error removing user from organization");
    }

    private CompletableFuture<Void> createFutureForMethod(ThrowableAction consumer,
        String authorizableName, String authorizableOperation) {

        return CompletableFuture.completedFuture(null).thenAcceptAsync((x) -> {
            try {
                consumer.apply();
                LOGGER.info(authorizableName + " finished " + authorizableOperation);
            } catch (AuthorizableGatewayException e) {
                throw new RuntimeException(authorizableName + " failed: " + e.getMessage(), e);
            }
        });
    }

    private void runTasks(List<CompletableFuture<Void>> tasks, String errorMessagePrefix)
        throws AuthorizableGatewayException {

        CompletableFuture<Void> allDone =
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
        try {
            allDone.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            LOGGER.error(errorMessagePrefix, e);
            throw new AuthorizableGatewayException(errorMessagePrefix + " " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(errorMessagePrefix, e);
            throw new AuthorizableGatewayException(errorMessagePrefix + " " + e.toString());
        } catch (TimeoutException e) {
            LOGGER.error(errorMessagePrefix, e);
            throw new AuthorizableGatewayException(errorMessagePrefix + " " + e.toString());
        }
    }

}
