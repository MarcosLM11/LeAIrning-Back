package com.marcos.usersservice.config;

public class ApiVersionParser implements org.springframework.web.accept.ApiVersionParser {

    @Override
    public Comparable parseVersion(String version) {
        //Allow v1, v2, v3... instead of 1.0, 2.0, 3.0...
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        //Auto append .0 for major versions
        if (!version.contains(".")) {
            version = version + ".0";
        }

        return version;
    }
}
