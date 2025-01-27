/**
 * Copyright (c) 2012-2015 Edgar Espina
 * <p>
 * This file is part of Handlebars.java.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package events.boudicca.publisherhtml.handlebars;

import com.github.jknack.handlebars.io.URLTemplateLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.net.URL;

import static java.util.Objects.requireNonNull;

/**
 * COPIED FROM https://github.com/jknack/handlebars.java/tree/master/handlebars-springmvc/src/main/java/com/github/jknack/handlebars/springmvc
 * and modified to jakarta namespace
 *
 * A template loader for a Spring application.
 * <ul>
 * <li>Must support fully qualified URLs, e.g. "file:C:/page.html".
 * <li>Must support classpath pseudo-URLs, e.g. "classpath:page.html".
 * <li>Should support relative file paths, e.g. "WEB-INF/page.html".
 * </ul>
 *
 * @author edgar.espina
 * @since 0.4.1
 * @see ResourceLoader#getResource(String)
 */
public class SpringTemplateLoader extends URLTemplateLoader {

    /**
     * The Spring {@link ResourceLoader}.
     */
    private ResourceLoader loader;

    /**
     * Creates a new {@link SpringTemplateLoader}.
     *
     * @param loader The resource loader. Required.
     */
    public SpringTemplateLoader(final ResourceLoader loader) {
        this.loader = requireNonNull(loader, "A resource loader is required.");
    }

    /**
     * Creates a new {@link SpringTemplateLoader}.
     *
     * @param applicationContext The application's context. Required.
     */
    public SpringTemplateLoader(final ApplicationContext applicationContext) {
        this((ResourceLoader) applicationContext);
    }

    @Override
    protected URL getResource(final String location) throws IOException {
        Resource resource = loader.getResource(location);
        if (!resource.exists()) {
            return null;
        }
        return resource.getURL();
    }

    @Override
    public String resolve(final String location) {
        String protocol = null;
        if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            protocol = ResourceUtils.CLASSPATH_URL_PREFIX;
        } else if (location.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
            protocol = ResourceUtils.FILE_URL_PREFIX;
        }
        if (protocol == null) {
            return super.resolve(location);
        }
        return protocol + super.resolve(location.substring(protocol.length()));
    }

}
