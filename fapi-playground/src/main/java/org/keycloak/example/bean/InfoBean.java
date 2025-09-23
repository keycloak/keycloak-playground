package org.keycloak.example.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfoBean {

    // Key is title, value is text-area
    private final List<Out> outputs = new ArrayList<>();

    public InfoBean(String... infos) {
        for (int i = 0 ; i < infos.length ; i = i+2) {
            String key = infos[i];
            String value = infos[i + 1];
            outputs.add(new Out(key, value));
        }
    }

    public InfoBean addOutput(String title, String content) {
        this.outputs.add(new Out(title, content));
        return this;
    }

    public List<Out> getOutputs() {
        return outputs;
    }

    public static class Out {

        private final String title;
        private final String content;

        public Out(String title, String textArea) {
            this.title = title;
            this.content = textArea;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}
