package com.harvey.vault.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;

import java.io.IOException;
import java.util.List;

public class VaultSerializer {

    static final int FORMAT_VERSION = 1;
    private static final String BAD_FORMAT =
            "Vault data is corrupt or in an unsupported format";

    // The shape of the JSON document: {"version":1,"entries":[...]}
    record VaultData(int version, List<Entry> entries) {}

    private final ObjectMapper mapper = new ObjectMapper();

    public byte[] toBytes(Vault vault) {
        try {
            return mapper.writeValueAsBytes(new VaultData(FORMAT_VERSION, vault.all()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize vault");
        }
    }

    public Vault fromBytes(byte[] json) throws VaultFormatException {
        try {
            VaultData data = mapper.readValue(json, VaultData.class);
            if (data == null
                    || data.version() != FORMAT_VERSION
                    || data.entries() == null) {
                throw new VaultFormatException(BAD_FORMAT);
            }
            Vault vault = new Vault();
            for (Entry entry : data.entries()) {
                if (entry == null) {
                    throw new VaultFormatException(BAD_FORMAT);
                }
                vault.add(entry); // rejects duplicate sites
            }
            return vault;
        } catch (IOException | RuntimeException e) {
            // Deliberately no cause and no original message: parser errors can
            // quote part of the input, and the input is the user's passwords.
            throw new VaultFormatException(BAD_FORMAT);
        }
    }
}