/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 snkas
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.ethz.systems.floodns.ext.basicsim;

import java.util.Properties;

public class NoDuplicatesProperties extends Properties {

    @Override
    public synchronized Object put(Object key, Object value) {
        if (get(key) != null) {
            throw new IllegalArgumentException("Duplicate key in properties: " + key);
        }
        return super.put(key, value);
    }

    public void validate(String[] permittedProperties) {
        for (String key : permittedProperties) {
            if (get(key) == null) {
                throw new IllegalArgumentException("Missing required property " + key + ".");
            }
        }
        for (Object key : this.keySet()) {
            String keyString = (String) key;
            boolean found = false;
            for (String property : permittedProperties) {
                if (key.equals(property)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Non-permitted property: " + keyString);
            }
        }
    }

    private String getPropertyOrFail(String property) {
        String res = getProperty(property);
        if (res == null) {
            throw new IllegalArgumentException("Property " + property + " does not exist");
        }
        return res;
    }

    public double getPositiveDoubleOrFail(String property) {
        String res = getPropertyOrFail(property);
        double value = Double.parseDouble(res);
        if (value < 0) {
            throw new IllegalArgumentException("Double value must be positive: " + value);
        }
        return value;
    }

    public long getPositiveLongOrFail(String property) {
        String res = getPropertyOrFail(property);
        long value = Long.parseLong(res);
        if (value < 0) {
            throw new IllegalArgumentException("Long value must be positive: " + value);
        }
        return value;
    }

    public String getStringOrFail(String property) {
        String res = getPropertyOrFail(property);
        if (res.startsWith("\"") && res.endsWith("\"")) {
            return res.substring(1, res.length() - 1);
        }
        return res;
    }

}
