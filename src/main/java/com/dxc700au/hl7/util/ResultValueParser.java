
package com.dxc700au.hl7.util;

public class ResultValueParser {

    private ResultValueParser() {
    }

    public static String parse(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }


        return value

                .replace("\0", "")

                .replace("\u001A", "")

                .trim();
    }
}

