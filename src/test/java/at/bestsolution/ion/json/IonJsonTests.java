package at.bestsolution.ion.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;

public class IonJsonTests {
    private static JsonObject loadSampleJson() {
        try (var is = IonJsonTests.class.getResourceAsStream("sample.json")) {
            return Json.createReader(is).readObject();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static JsonObject loadNumberJson() {
        try (var is = IonJsonTests.class.getResourceAsStream("number.json")) {
            return Json.createReader(is).readObject();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static JsonObject loadStringJson() {
        try (var is = IonJsonTests.class.getResourceAsStream("string.json")) {
            return Json.createReader(is).readObject();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String toJsonString(JsonValue obj) {
        try (var writer = new java.io.StringWriter()) {
            Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, "true")).createWriter(writer)
                    .write(obj);
            return writer.toString();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void string() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var value = Json.createValue("test string");
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        ionJson.encode(writer, value);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals("test string", ((JsonString) decodedJson).getString());
    }

    @Test
    public void bool() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        ionJson.encodeList(writer, List.of(JsonValue.TRUE, JsonValue.FALSE));
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decodeList(reader);
        reader.close();
        assertSame(decodedJson.get(0), JsonValue.TRUE);
        assertSame(decodedJson.get(1), JsonValue.FALSE);
    }

    @Test
    public void nil() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        ionJson.encode(writer, JsonValue.NULL);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertSame(JsonValue.NULL, decodedJson);
    }

    @Test
    public void integer() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        ionJson.encode(writer, Json.createValue(100));
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals(100, ((JsonNumber) decodedJson).intValue());
    }

    @Test
    public void floatingPoint() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        ionJson.encode(writer, Json.createValue(100.01));
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals(100.01, ((JsonNumber) decodedJson).doubleValue());
    }

    @Test
    public void array() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        var value = Json.createArrayBuilder()
                .addNull()
                .add(true)
                .add(1.01)
                .add(Long.MAX_VALUE)
                .add(Integer.MAX_VALUE)
                .add(BigInteger.valueOf(Long.MAX_VALUE).add(new BigInteger("1")))
                .add("Foo")
                .build();
        ionJson.encode(writer, value);
        writer.close();

        var encodedJson = bos.toByteArray();
        System.err.println(new String(encodedJson));
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader).asJsonArray();
        reader.close();
        assertSame(JsonValue.NULL, decodedJson.get(0));
        assertSame(JsonValue.TRUE, decodedJson.get(1));
        assertEquals(1.01, decodedJson.getJsonNumber(2).doubleValue());
        assertEquals(Long.MAX_VALUE, decodedJson.getJsonNumber(3).longValueExact());
        assertEquals(Integer.MAX_VALUE, decodedJson.getJsonNumber(4).intValueExact());
        assertThrows(ArithmeticException.class, () -> decodedJson.getJsonNumber(5).longValueExact());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(new BigInteger("1")),
                decodedJson.getJsonNumber(5).bigIntegerValue());
        assertEquals("Foo", decodedJson.getJsonString(6).getString());
    }

    @Test
    public void struct() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonBinaryWriterBuilder.standard().build(bos);
        var value = Json.createObjectBuilder()
                .add("key1", "value1")
                .add("key2", 100)
                .add("key3", true)
                .build();
        ionJson.encode(writer, value);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader).asJsonObject();
        reader.close();
        assertEquals("value1", decodedJson.getString("key1"));
        assertEquals(100, decodedJson.getJsonNumber("key2").intValue());
        assertEquals(JsonValue.TRUE, decodedJson.get("key3"));
    }

    @Test
    public void testSimple() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var jsonObject = loadSampleJson();
        var bos = new ByteArrayOutputStream();
        var writer = IonTextWriterBuilder.standard().build(bos);
        ionJson.encode(writer, jsonObject);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        System.err.println(new String(encodedJson));
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals(toJsonString(jsonObject), toJsonString(decodedJson));
    }

    @Test
    public void testList() throws IOException {
        var sample = loadSampleJson();
        var numbers = loadNumberJson();

        var ionJson = IonJson.builder()
                .build();
        var bos = new ByteArrayOutputStream();
        var writer = IonTextWriterBuilder.standard().build(bos);
        ionJson.encodeList(writer, List.of(sample, numbers));
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decodeList(reader);
        reader.close();
        assertEquals(2, decodedJson.size());
    }

    @Test
    public void testCacheNumberCache() throws IOException {
        var ionJson = IonJson.builder()
                .build();
        var jsonObject = loadNumberJson();
        var bos = new ByteArrayOutputStream();
        var writer = IonTextWriterBuilder.standard().build(bos);
        ionJson.encode(writer, jsonObject);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals(toJsonString(jsonObject), toJsonString(decodedJson));
        var cached = decodedJson.asJsonObject().getJsonArray("cachedNumbers");
        assertSame(cached.get(0), cached.get(2));
        assertSame(cached.get(1), cached.get(3));
        var notCached = decodedJson.asJsonObject().getJsonArray("notCachedNumbers");
        assertNotSame(notCached.get(0), notCached.get(1));
        assertNotSame(notCached.get(2), notCached.get(3));
    }

    @Test
    public void testCacheStringCache() throws IOException {
        var ionJson = IonJson.builder()
                .cachedStrings(Set.of("hello", "world"))
                .build();
        var jsonObject = loadStringJson();
        var bos = new ByteArrayOutputStream();
        var writer = IonTextWriterBuilder.standard().build(bos);
        ionJson.encode(writer, jsonObject);
        writer.flush();
        writer.close();
        var encodedJson = bos.toByteArray();
        var reader = IonReaderBuilder.standard().build(encodedJson);
        var decodedJson = ionJson.decode(reader);
        reader.close();
        assertEquals(toJsonString(jsonObject), toJsonString(decodedJson));
        var cached = decodedJson.asJsonObject().getJsonArray("cachedStrings");
        assertSame(cached.get(0), cached.get(1));
        assertSame(cached.get(2), cached.get(3));
        var notCached = decodedJson.asJsonObject().getJsonArray("notCachedStrings");
        assertNotSame(notCached.get(0), notCached.get(1));
        assertNotSame(notCached.get(2), notCached.get(3));
    }
}
