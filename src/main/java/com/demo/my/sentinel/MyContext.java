package com.demo.my.sentinel;

public class MyContext {
    private String contextName;
    private String sourceName;

    public MyContext(String contextName, String sourceName) {
        this.contextName = contextName;
        this.sourceName = sourceName;
    }

    public String getContextName() {
        return contextName;
    }

    public String getSourceName() {
        return sourceName;
    }
}
