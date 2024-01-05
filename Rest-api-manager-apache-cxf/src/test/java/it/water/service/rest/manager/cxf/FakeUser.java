/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.service.rest.manager.cxf;

import it.water.core.api.entity.Authenticable;
import it.water.core.api.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class FakeUser implements User, Authenticable {
    @Override
    public String getName() {
        return "Fake";
    }

    @Override
    public String getLastname() {
        return "User";
    }

    @Override
    public String getEmail() {
        return "Fake@fake.com";
    }

    @Override
    public String getUsername() {
        return "fakeUsr";
    }

    @Override
    public boolean hasRole(String s) {
        return false;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String getScreenNameFieldName() {
        return "screenName";
    }

    @Override
    public String getScreenName() {
        return "fake";
    }

    @Override
    public Collection<String> getRoles() {
        return Collections.emptySet();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getPasswordConfirm() {
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Date getEntityCreateDate() {
        return new Date();
    }

    @Override
    public Date getEntityModifyDate() {
        return new Date();
    }

    @Override
    public int getEntityVersion() {
        return 0;
    }

    @Override
    public String getSystemApiClassName() {
        return null;
    }
}
