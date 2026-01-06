package at.bestsolution.ion.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public class IonJson {
    /**
     * Builder for configuring and creating a MsgpackJson instance.
     */
    public static class Builder {
        private Map<String, JsonString> stringCache;

        private Builder() {
        }

        /**
         * Sets a cache of strings to be used during decoding. This is helpful for
         * example when deoding JSON data representing an Enum where the same strings
         * are used
         * 
         * @param stringCache the strings to cache
         * @return the builder instance
         */
        public Builder cachedStrings(Set<String> stringCache) {
            this.stringCache = stringCache.stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(s -> s,
                            s -> Json.createValue(s)));
            return this;
        }

        /**
         * Builds the MsgpackJson instance with the configured settings.
         * 
         * @return the mspack instance
         */
        public IonJson build() {
            return new IonJson(stringCache);
        }
    }

    private static final JsonNumber[] CACHE = new JsonNumber[256];
    private Map<String, JsonString> stringCache = null;

    /**
     * Creates a new builder for configuring a MsgpackJson instance.
     * 
     * @return the builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    IonJson(Map<String, JsonString> stringCache) {
        this.stringCache = stringCache;
    }

    public List<JsonValue> decodeList(IonReader reader) throws IOException {
        List<JsonValue> result = new java.util.ArrayList<>();
        JsonValue value;
        while ((value = decode(reader)) != null) {
            result.add(value);
        }
        return result;
    }

    public JsonValue decode(IonReader reader) throws IOException {
        IonType type = reader.next();
        if (type == null) {
            return null;
        }
        return decodeRec(reader, type);
    }

    private JsonValue decodeRec(IonReader reader, IonType type) throws IOException {
        return switch (type) {
            case LIST -> {
                reader.stepIn();
                var arrayBuilder = Json.createArrayBuilder();
                IonType childType;
                while ((childType = reader.next()) != null) {
                    arrayBuilder.add(decodeRec(reader, childType));
                }
                reader.stepOut();
                yield arrayBuilder.build();
            }
            case STRUCT -> {
                reader.stepIn();
                var objectBuilder = Json.createObjectBuilder();
                IonType childType;
                while ((childType = reader.next()) != null) {
                    objectBuilder.add(reader.getFieldName(), decodeRec(reader, childType));
                }
                reader.stepOut();
                yield objectBuilder.build();
            }
            case STRING -> {
                String str = reader.stringValue();
                if (stringCache != null) {
                    JsonString cached = stringCache.get(str);
                    if (cached != null) {
                        yield cached;
                    }
                }
                yield Json.createValue(str);
            }
            case INT -> {
                var size = reader.getIntegerSize();
                yield switch (size) {
                    case LONG -> number(reader.longValue());
                    case INT -> number(reader.intValue());
                    case BIG_INTEGER -> Json.createValue(reader.bigIntegerValue());
                };
            }
            case FLOAT -> jakarta.json.Json.createValue(reader.doubleValue());
            case BOOL -> reader.booleanValue() ? JsonValue.TRUE : JsonValue.FALSE;
            case NULL -> JsonValue.NULL;
            case BLOB -> throw new IOException("BLOB not supported");
            case CLOB -> throw new IOException("CLOB not supported");
            case SYMBOL -> throw new IOException("SYMBOL not supported");
            case DATAGRAM -> throw new IOException("DATAGRAM not supported");
            case DECIMAL -> throw new IOException("DECIMAL not supported");
            case TIMESTAMP -> throw new IOException("TIMESTAMP not supported");
            case SEXP -> throw new IOException("SEXP not supported");
        };
    }

    public void encodeList(IonWriter writer, List<? extends JsonValue> values) throws IOException {
        for (JsonValue value : values) {
            encode(writer, value);
        }
    }

    public void encode(IonWriter writer, JsonValue value) throws IOException {
        switch (value.getValueType()) {
            case ARRAY:
                writer.stepIn(IonType.LIST);
                for (JsonValue v : value.asJsonArray()) {
                    encode(writer, v);
                }
                writer.stepOut();
                break;
            case OBJECT:
                writer.stepIn(IonType.STRUCT);
                value.asJsonObject().forEach((k, v) -> {
                    try {
                        writer.setFieldName(k);
                        encode(writer, v);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                writer.stepOut();
                break;
            case STRING:
                writer.writeString(((JsonString) value).getString());
                break;
            case NUMBER:
                var num = (JsonNumber) value;
                if (num.isIntegral()) {
                    try {
                        writer.writeInt(num.longValueExact());
                    } catch (ArithmeticException e) {
                        writer.writeInt(num.bigIntegerValue());
                    }
                } else {
                    writer.writeFloat(num.doubleValue());
                }
                break;
            case TRUE:
                writer.writeBool(true);
                break;
            case FALSE:
                writer.writeBool(false);
                break;
            case NULL:
                writer.writeNull();
                break;
        }
    }

    private static JsonNumber number(long l) {
        if (l >= -128 && l <= 127) {
            int idx = (int) l + 128;
            JsonNumber cached = CACHE[idx];
            if (cached == null) {
                cached = Json.createValue((int) l);
                CACHE[idx] = cached;
            }
            return cached;
        }
        return Json.createValue(l);
    }

    private static JsonNumber number(int l) {
        if (l >= -128 && l <= 127) {
            int idx = (int) l + 128;
            JsonNumber cached = CACHE[idx];
            if (cached == null) {
                cached = Json.createValue(l);
                CACHE[idx] = cached;
            }
            return cached;
        }
        return Json.createValue(l);
    }

}
