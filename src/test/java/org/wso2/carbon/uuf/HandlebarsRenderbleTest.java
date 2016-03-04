package org.wso2.carbon.uuf;

import com.github.jknack.handlebars.HandlebarsError;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.uuf.core.HandlebarsRenderble;
import org.wso2.carbon.uuf.core.Renderble;

import java.util.Collections;
import java.util.Map;

public class HandlebarsRenderbleTest {

    @Test
    public void testZones() {

        StringTemplateSource source = new StringTemplateSource("my-file.hbs", "{{defineZone \"my-zone\"}}");
        HandlebarsRenderble hb = new HandlebarsRenderble(source);
        String s = hb.render(ImmutableMap.of("name", "Leela"), ImmutableMap.of("my-zone", new MockHelloRenderble()));
        Assert.assertEquals(s, "Welcome to the <world> of tomorrow, Leela");
    }

    @Test
    public void testLayoutName() {
        HandlebarsRenderble renderble = new HandlebarsRenderble(new StringTemplateSource(
                "my-file.hbs",
                "{{layout \"my-layout\"}}"));

        String layoutName = renderble.getLayoutName();
        Assert.assertEquals(layoutName, "my-layout", "a layout is defined in the template");

    }

    @Test
    public void testFillingZones() {
        HandlebarsRenderble renderble = new HandlebarsRenderble(new StringTemplateSource(
                "my-file.hbs",
                "\n{{#fillZone \"my-zone\"}} {{a}}{{/fillZone}}"));
        Map<String, Renderble> fillingZones = renderble.getFillingZones();
        Renderble fillingZone = fillingZones.get("my-zone");
        Assert.assertNotNull(fillingZone, "zone's inner content must be available under name 'my-zone'");
        try {
            fillingZone.render(Collections.EMPTY_MAP);
            Assert.fail("can't render with an empty map since 'a' var is expected.");
        } catch (HandlebarsException ex) {
            HandlebarsError error = ex.getError();
            Assert.assertEquals(error.line, 2, "error is in the 2nd line");
            Assert.assertEquals(error.column, 26, "error is in the 26th column");
        }

        String rendered = fillingZone.render(ImmutableMap.of("a", "apple"));
        Assert.assertEquals(rendered.trim(), "apple");
    }
}
