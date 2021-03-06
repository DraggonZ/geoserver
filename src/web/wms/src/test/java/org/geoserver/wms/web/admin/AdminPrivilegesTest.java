/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.admin;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.data.StyleEditPage;
import org.geoserver.wms.web.data.StyleNewPage;
import org.geoserver.wms.web.data.StylePage;
import org.junit.Test;

public class AdminPrivilegesTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Arrays.asList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");
        
        Catalog cat = getCatalog();

        //add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testStyleAllPageAsAdmin() throws Exception {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        tester.debugComponentTrees();
        Catalog cat = getCatalog();
    
        DataView view = 
            (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(cat.getStyles().size(), view.getItemCount());
    }

    @Test
    public void testStyleAllPage() throws Exception {
        loginAsCite();
    
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        
        Catalog cat = getCatalog();
    
        DataView view = 
            (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
    
        int expected = cat.getStyles().size() - cat.getStylesByWorkspace("sf").size();
    
        AdminRequest.start(new Object());
        assertEquals(expected, view.getItemCount());
    
        for (Iterator<Item> it = view.getItems(); it.hasNext();) {
            String name = it.next().get("itemProperties:0:component:link:label")
                .getDefaultModelObjectAsString();
            assertFalse("sf_style".equals(name));
        }
    }

    @Test
    public void testStyleNewPageAsAdmin() throws Exception {
        login();
    
        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("form:workspace", null);
        
        DropDownChoice choice = 
            (DropDownChoice) tester.getComponentFromLastRenderedPage("form:workspace");
        assertTrue(choice.isNullValid());
        assertFalse(choice.isRequired());
    }
    
    @Test
    public void testStyleNewPage() throws Exception {
        loginAsCite();
    
        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);
    
        Catalog cat = getCatalog();
        tester.assertModelValue("form:workspace", cat.getWorkspaceByName("cite"));
        
        DropDownChoice choice = 
            (DropDownChoice) tester.getComponentFromLastRenderedPage("form:workspace");
        assertFalse(choice.isNullValid());
        assertTrue(choice.isRequired());
    }

    @Test
    public void testStyleEditPageGlobal() throws Exception {
        loginAsCite();
    
        tester.startPage(StyleEditPage.class, new PageParameters(StyleEditPage.NAME+"=point"));
        tester.assertRenderedPage(StyleEditPage.class);

        //assert all form components disabled except for cancel
        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:workspace").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:copy").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("cancel").isEnabled());
    }
}
