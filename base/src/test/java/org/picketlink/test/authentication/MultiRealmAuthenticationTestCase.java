/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.test.authentication;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.Identity;
import org.picketlink.annotations.PicketLink;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.User;
import org.picketlink.test.util.ArchiveUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Perform some tests against an IDM configuration with multiple realms configured.
 * </p>
 * <p>
 * We test a scenario where the same user exists in different realms with different credentials. Without using multiple realms,
 * adding the same user to a realm (with the same loginName) is not possible.
 * </p>
 * 
 * @author Pedro Igor
 * 
 */
@RunWith(Arquillian.class)
public class MultiRealmAuthenticationTestCase {

    private static final String USER_NAME = "john";
    private static final String STAGING_REALM_NAME = "Staging";
    private static final String TESTING_REALM_NAME = "Testing";

    @Inject
    private RealmSelector realmSelector;

    @Inject
    private Instance<IdentityManager> identityManagerInstance;
    
    @Inject
    private PartitionManager partitionManager;

    @Inject
    private DefaultLoginCredentials credentials;

    @Inject
    private Identity identity;

    @Deployment
    public static WebArchive deploy() {
        List<Class> classes = new ArrayList<Class>();

        classes.add(MultiRealmAuthenticationTestCase.class);
        classes.add(AbstractAuthenticationTestCase.class);

        return ArchiveUtils.create(classes.toArray(new Class[classes.size()]));
    }

    @Before
    public void onSetup() {
        createPartitionIfNecessary(new Realm(STAGING_REALM_NAME));
        this.identity.logout();
    }

    @Test
    public void testIdentityManagerForStagingRealm() throws Exception {
        this.realmSelector.setRealmName(STAGING_REALM_NAME);

        User user = new User(USER_NAME);

        IdentityManager identityManager = this.identityManagerInstance.get();

        identityManager.add(user);

        assertEquals(STAGING_REALM_NAME, user.getPartition().getName());

        assertLogin(user, identityManager);
    }

    private void assertLogin(User user, IdentityManager identityManager) {
        Realm userRealm = (Realm) user.getPartition();
        Password password = new Password(buildUserPassword(userRealm));

        identityManager.updateCredential(user, password);

        this.credentials.setUserId(user.getLoginName());
        this.credentials.setPassword(String.valueOf(password.getValue()));

        this.identity.login();

        assertTrue(this.identity.isLoggedIn());
    }

    /**
     * <p>User's password is a concatenation of loginName + Partition.id.</p>
     * 
     * @param realm
     * @return
     */
    private String buildUserPassword(Realm realm) {
        return USER_NAME + realm.getName();
    }

    @RequestScoped
    public static class RealmSelector implements Serializable {

        @Inject
        private PartitionManager partitionManager;

        private String realmName;

        @Produces
        @PicketLink
        public Partition select() {
            if (this.realmName == null) {
                this.realmName = Realm.DEFAULT_REALM;
            }

            return this.partitionManager.getPartition(Realm.class, this.realmName);
        }

        void setRealmName(String realmName) {
            this.realmName = realmName;
        }
    }

    @ApplicationScoped
    public static class Resources {

        @Produces
        public IdentityConfiguration buildIDMConfiguration() {
            IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

            builder.named("default").stores().file().supportAllFeatures();

            return builder.build();
        }

    }

    private void createPartitionIfNecessary(Partition partition) {
        if (this.partitionManager.getPartition(partition.getClass(), partition.getName()) == null) {
            this.partitionManager.add(partition);
        }
    }

}
