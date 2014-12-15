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
package org.picketlink.test.integration.federation.saml;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.picketlink.test.integration.federation.saml.QuickstartArchiveUtil.resolveFromFederation;

/**
 * @author Pedro Igor
 */
@RunWith (Arquillian.class)
@RunAsClient
public class SAMLSPAccountChooserTestCase extends AbstractFederationTestCase {

    @ArquillianResource
    @OperateOnDeployment("idp-domainA")
    private URL idpDomainAUrl;

    @ArquillianResource
    @OperateOnDeployment("idp-domainB")
    private URL idpDomainBUrl;

    @Deployment(name = "idp-domainA")
    public static WebArchive deployIdentityProviderA() {
        return resolveFromFederation("picketlink-federation-saml-idp-with-signature");
    }

    @Deployment(name = "idp-domainB")
    public static WebArchive deployIdentityProviderB() {
        return resolveFromFederation("picketlink-federation-saml-idp-basic");
    }

    @Deployment(name = "service-provider")
    public static WebArchive deployServiceProvider() {
        return resolveFromFederation("picketlink-federation-saml-sp-idp-chooser");
    }

    @Test
    @OperateOnDeployment("service-provider")
    public void testGlobalLogout(@ArquillianResource URL url) throws Exception {
        WebRequest request = new GetMethodWebRequest(formatUrl(url));
        WebConversation conversation = new WebConversation();
        WebResponse response = conversation.getResponse(request);

        WebLink link = response.getLinkWithID("domainA");

        assertNotNull(link);

        response = link.click();

        assertTrue(response.getURL().getPath().startsWith("/idp-sig"));
        assertEquals(1, response.getForms().length);

        request = new GetMethodWebRequest(formatUrl(url));
        response = conversation.getResponse(request);

        link = response.getLinkWithID("domainB");

        assertNotNull(link);

        response = link.click();

        assertTrue(response.getURL().getPath().endsWith("/idp/"));
        assertEquals(1, response.getForms().length);
    }
}