package com.harvey.vault.storage;

import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VaultSerializerTest {

    private final VaultSerializer serializer = new VaultSerializer();

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static Vault vaultWith(Entry... entries) {
        Vault v = new Vault();
        for (Entry e : entries) {
            v.add(e);
        }
        return v;
    }

    // ---- round trips ----

    @Test
    void roundTripPreservesEntries() throws Exception {
        Vault original = vaultWith(
                new Entry("github", "harvey", "p@ss1"),
                new Entry("zoom", "harvey2", "p@ss2"));
        Vault copy = serializer.fromBytes(serializer.toBytes(original));
        assertEquals(original.all(), copy.all());
    }

    @Test
    void emptyVaultRoundTrips() throws Exception {
        Vault copy = serializer.fromBytes(serializer.toBytes(new Vault()));
        assertEquals(0, copy.size());
    }

    @Test
    void unicodeSurvivesRoundTrip() throws Exception {
        Vault original = vaultWith(new Entry("café", "zoë", "pässwörd✓😀"));
        Vault copy = serializer.fromBytes(serializer.toBytes(original));
        assertEquals(original.all(), copy.all());
    }

    @Test
    void specialCharactersAreEscapedCorrectly() throws Exception {
        Vault original = vaultWith(new Entry("site", "user", "q\"uote\\slash{brace}"));
        Vault copy = serializer.fromBytes(serializer.toBytes(original));
        assertEquals(original.all(), copy.all());
    }

    @Test
    void outputIncludesFormatVersion() {
        String json = new String(serializer.toBytes(new Vault()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"version\":1"));
    }

    // ---- untrusted input is rejected ----

    @Test
    void malformedJsonIsRejected() {
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes("{not json")));
    }

    @Test
    void emptyInputIsRejected() {
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(new byte[0]));
    }

    @Test
    void unsupportedVersionIsRejected() {
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes("{\"version\":99,\"entries\":[]}")));
    }

    @Test
    void missingEntriesIsRejected() {
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes("{\"version\":1}")));
    }

    @Test
    void blankSiteIsRejected() {
        String json = "{\"version\":1,\"entries\":"
                + "[{\"site\":\" \",\"username\":\"u\",\"password\":\"p\"}]}";
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes(json)));
    }

    @Test
    void missingPasswordIsRejected() {
        String json = "{\"version\":1,\"entries\":"
                + "[{\"site\":\"a\",\"username\":\"u\"}]}";
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes(json)));
    }

    @Test
    void duplicateSitesAreRejected() {
        String json = "{\"version\":1,\"entries\":["
                + "{\"site\":\"a\",\"username\":\"u\",\"password\":\"p\"},"
                + "{\"site\":\"A\",\"username\":\"u2\",\"password\":\"p2\"}]}";
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes(json)));
    }

    @Test
    void unknownFieldsAreRejected() {
        assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bytes("{\"version\":1,\"entries\":[],\"extra\":true}")));
    }

    @Test
    void errorDoesNotLeakVaultContents() {
        // truncated JSON that contains a password
        byte[] bad = bytes("{\"version\":1,\"entries\":[{\"site\":\"bank\","
                + "\"username\":\"u\",\"password\":\"hunter2\"");
        VaultFormatException ex = assertThrows(VaultFormatException.class,
                () -> serializer.fromBytes(bad));
        assertFalse(ex.getMessage().contains("hunter2"));
        assertNull(ex.getCause());
    }
}