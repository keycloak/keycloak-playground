package org.keycloak.example.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import freemarker.cache.URLTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerUtil {

    public FreeMarkerUtil() {
    }

    public Response processTemplate(Map<String, Object> attributes, String templateName) {
        try {
            String result = processTemplateInternal(attributes, templateName);
//            javax.ws.rs.core.MediaType mediaType = contentType == null ? MediaType.TEXT_HTML_UTF_8_TYPE : contentType;
//            Response.ResponseBuilder builder = Response.status(status == null ? Response.Status.OK : status).type(mediaType).language(locale).entity(result);
//            for (Map.Entry<String, String> entry : httpResponseHeaders.entrySet()) {
//                builder.header(entry.getKey(), entry.getValue());
//            }
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8_TYPE).language(Locale.ENGLISH).entity(result);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process template", e);
        }
    }

    private String processTemplateInternal(Object data, String templateName) {
        try {
            Template template = getTemplate(templateName);
            Writer out = new StringWriter();
            template.process(data, out);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process template " + templateName, e);
        }
    }

    private Template getTemplate(String templateName) throws IOException {
        Configuration cfg = new Configuration();

        // Assume *.ftl files are html.  This lets freemarker know how to
        // sanitize and prevent XSS attacks.
        if (templateName.toLowerCase().endsWith(".ftl")) {
            cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        }

        cfg.setTemplateLoader(new ThemeTemplateLoader());
        return cfg.getTemplate(templateName, "UTF-8");
    }


    static class ThemeTemplateLoader extends URLTemplateLoader {


        @Override
        protected URL getURL(String name) {
            return getClass().getClassLoader().getResource("/templates/" + name);
        }

    }
}
