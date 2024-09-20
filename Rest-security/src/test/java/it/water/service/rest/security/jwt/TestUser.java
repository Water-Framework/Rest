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
package it.water.service.rest.security.jwt;

import it.water.core.api.entity.Authenticable;
import it.water.core.api.model.User;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@AllArgsConstructor
public class TestUser implements Authenticable, User {
    private String screenName;
    private Set<TestRole> roles;

    @Override
    public String getScreenNameFieldName() {
        return "screenName";
    }

    @Override
    public String getScreenName() {
        return screenName;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public Collection<String> getRoles() {
        Collection<String> roleNames = new ArrayList<>();
        roles.stream().forEach(role -> roleNames.add(role.getName()));
        return roleNames;
    }

    @Override
    public String getPassword() {
        return "....";
    }

    @Override
    public String getPasswordConfirm() {
        return "....";
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
        return null;
    }

    @Override
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    public Integer getEntityVersion() {
        return 0;
    }

    @Override
    public void setEntityVersion(Integer integer) {
        //do nothing
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getLastname() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public String getUsername() {
        return screenName;
    }
}
