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

package org.picketlink.test.authorization;

import org.apache.deltaspike.security.api.authorization.AccessDeniedException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.picketlink.test.authorization.permission.AnotherEntity;
import org.picketlink.test.authorization.permission.SomeEntity;

import javax.inject.Inject;

import static org.junit.Assert.fail;
import static org.picketlink.idm.model.basic.BasicModel.addToGroup;
import static org.picketlink.idm.model.basic.BasicModel.grantGroupRole;
import static org.picketlink.idm.model.basic.BasicModel.grantRole;

/**
 * <p>
 * Perform some authentication tests using the {@link org.picketlink.authentication.internal.IdmAuthenticator}, which is the default {@link java.net.Authenticator}.
 * </p>
 *
 * @author Pedro Igor
 *
 */
@RunWith(Arquillian.class)
public class AnnotationBasedAuthorizationTestCase extends AbstractAuthorizationTestCase {

    @Inject
    protected AnnotationProtectedBean protectedBean;

    @Inject
    protected ClassLevelAnnotationProtectedBean classLevelAnnotationProtectedBean;

    @Deployment
    public static WebArchive deploy() {
        return create(AnnotationBasedAuthorizationTestCase.class, AnnotationProtectedBean.class, ClassLevelAnnotationProtectedBean.class);
    }

    @Before
    public void onSetup() throws Exception {
        User john = BasicModel.getUser(this.identityManager, USER_NAME);

        if (john == null) {
            john = new User(USER_NAME);

            this.identityManager.add(john);

            this.identityManager.updateCredential(john, new Password(USER_PASSWORD));

            Role tester = new Role("Tester");

            this.identityManager.add(tester);

            Group qaGroup = new Group("QA");

            this.identityManager.add(qaGroup);

            grantRole(relationshipManager, john, tester);
            addToGroup(relationshipManager, john, qaGroup);
            grantGroupRole(relationshipManager, john, tester, qaGroup);

            Role inheritedRole = new Role("Inherited Role");

            this.identityManager.add(inheritedRole);

            Group inheritedRoleGroup = new Group("Inherited Role Group");

            this.identityManager.add(inheritedRoleGroup);

            grantRole(relationshipManager, inheritedRoleGroup, inheritedRole);
            addToGroup(relationshipManager, john, inheritedRoleGroup);

            this.permissionManager.grantPermission(john, "profile", "read");
            this.permissionManager.grantPermission(john, SomeEntity.class, "create");

            SomeEntity someEntity = new SomeEntity();

            someEntity.setId(1l);

            this.permissionManager.grantPermission(john, someEntity, "write");

            this.permissionManager.grantPermission(john, AnotherEntity.class, "load");

            AnotherEntity anotherEntity = new AnotherEntity();

            anotherEntity.setId(1l);

            this.permissionManager.grantPermission(john, anotherEntity, "delete");

            this.userTransaction.commit();
        }
    }

    @Test
    public void testSuccessfulInvocationFromAuthenticatedUser() throws Exception {
        performAuthentication();
        this.protectedBean.protectedFromUnauthenticatedUsers();
    }

    @Test
    public void testSuccessfulInvocationWithPermission() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithResourcePermission();
        this.protectedBean.protectedWithResourceClassPermission();
        this.protectedBean.protectedWithResourceClassIdentifierPermission();
    }

    @Test
    public void testSuccessfulInvocationWithPermissionAnotherEntity() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithResourceClassPermissionAnotherEntity();
        this.protectedBean.protectedWithResourceClassIdentifierPermissionAnotherEntity();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredRole() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredRole();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredGroup() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredGroup();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredRoleAndGroup() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredMemberAndRole();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredPartitionName() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredPartitionName();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredPartitionType() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredPartitionType();
    }

    @Test
    public void testSuccessfulInvocationWithRequiredPartitionNameAndType() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithRequiredPartitionTypeAndName();
    }

    @Test
    public void testSuccessfulClassLevelAuthorization() throws Exception {
        performAuthentication();
        this.classLevelAnnotationProtectedBean.protectedWithRequiredRole();
    }

    @Test
    public void testSuccessfulInheritedRoleFromGroup() throws Exception {
        performAuthentication();
        this.protectedBean.protectedWithInheritedRole();
    }

    @Test
    public void failInvocationFromUnAuthenticatedUser() throws Exception {
        try {
            this.protectedBean.protectedFromUnauthenticatedUsers();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromUnAuthenticatedUserPermission() throws Exception {
        try {
            this.protectedBean.protectedWithResourceWithoutPermission();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromInvalidUserPermission() throws Exception {
        performAuthentication();
        try {
            this.protectedBean.protectedWithResourceWithoutPermission();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromInvalidUserResourceClassPermission() throws Exception {
        performAuthentication();
        try {
            this.protectedBean.protectedWithoutResourceClassPermission();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromInvalidUserResourceClassIdentifierPermission() throws Exception {
        performAuthentication();
        try {
            this.protectedBean.protectedWithoutResourceClassIdentifierPermission();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromInvalidUserResourceClassPermissionAnotherEntity() throws Exception {
        performAuthentication();
        try {
            this.protectedBean.protectedWithoutResourceClassPermissionAnotherEntity();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationFromInvalidUserResourceClassIdentifierPermissionAnotherEntity() throws Exception {
        performAuthentication();
        try {
            this.protectedBean.protectedWithoutResourceClassIdentifierPermissionAnotherEntity();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationWithInvalidRole() throws Exception {
        try {
            performAuthentication();
            this.protectedBean.protectedWithRequiredInvalidRole();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationWithInvalidGroup() throws Exception {
        try {
            performAuthentication();
            this.protectedBean.protectedWithRequiredInvalidGroup();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationWithRequiredGroupAndInvalidRole() throws Exception {
        try {
            performAuthentication();
            this.protectedBean.protectedWithRequiredMemberAndInvalidRole();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationWithInvalidPartitionName() throws Exception {
        try {
            performAuthentication();
            this.protectedBean.protectedWithInvalidPartitionName();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }

    @Test
    public void failInvocationWithInvalidPartitionType() throws Exception {
        try {
            performAuthentication();
            this.protectedBean.protectedWithInvalidPartitionType();
            fail();
        } catch (Exception e) {
            if (!AccessDeniedException.class.isInstance(e) && !AccessDeniedException.class.isInstance(e.getCause())) {
                fail();
            }
        }
    }
}