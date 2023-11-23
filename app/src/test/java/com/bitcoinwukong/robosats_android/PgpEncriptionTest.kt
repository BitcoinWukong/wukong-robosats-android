package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.generateSecureToken
import com.bitcoinwukong.robosats_android.utils.tokenSha256Hash
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgpKeyGeneratorTest {
    val robotToken = "C2etfi7nPeUD7rCcwAOy4XoLvEAxbTRGSK6H"
    val encPrivKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
            "\n" +
            "xYYEZVO9bxYJKwYBBAHaRw8BAQdAVyePBQK63FB2r5ZpIqO998WaqZjmro+L\n" +
            "FNH+sw2raQD+CQMIHkZZZnDa6d/gHioGTKf6JevirkCBWwz8tFLGFs5DFwjD\n" +
            "tI4ew9CJd09AUxfMq2WvTilhMNrdw2nmqtmAoaIyIo43azVT1VQoxSDnWxFv\n" +
            "Tc1MUm9ib1NhdHMgSUQgZWQ3ZDNiMmIyZTU4OWFiMjY3MjA2MDVlNzQxNGFi\n" +
            "NGZiY2EyMWNiNGIzMWU1YjRlNjJhNmZhNTFjMjRhOWVhYsKMBBAWCgA+BYJl\n" +
            "U71vBAsJBwgJkC40W0tJjZckAxUICgQWAAIBAhkBApsDAh4BFiEEMiFWI4Wn\n" +
            "Nai8+7C8LjRbS0mNlyQAAJN4AQD4B1eYDynoj4UWpdoVVGHklFrlmg7jGEZv\n" +
            "5BjTSSBmKwEAl57xd1boUcGPYndvgz0Biq592m+6P6UuZpUCyE31TgzHiwRl\n" +
            "U71vEgorBgEEAZdVAQUBAQdA5GRu9J6yD2gl4JEqKBM11Dm2SCZUZd93P/6/\n" +
            "AEiXcTcDAQgH/gkDCGSRul0JyboW4JZSQVlHNVlx2mrfE1gRTh2R5hJWU9Kg\n" +
            "aw2gET8OwWDYU4F8wKTo/s7BGn+HN4jrZeLw1k/etKUKLzuPC06KUXhj3rMF\n" +
            "Ti3CeAQYFggAKgWCZVO9bwmQLjRbS0mNlyQCmwwWIQQyIVYjhac1qLz7sLwu\n" +
            "NFtLSY2XJAAAWdQA/0SCTP1jW9vKWKrauF5njmqiwq20LNrmvJl6RgWUpfBN\n" +
            "AQC1kpWxyAmEzcvS+ildGuaV28XF6c3o3I6SZlcM7ls/Dw==\n" +
            "=YAfZ\n" +
            "-----END PGP PRIVATE KEY BLOCK-----"
    val pubKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "\n" +
            "mDMEZVO9bxYJKwYBBAHaRw8BAQdAVyePBQK63FB2r5ZpIqO998WaqZjmro+LFNH+\n" +
            "sw2raQC0TFJvYm9TYXRzIElEIGVkN2QzYjJiMmU1ODlhYjI2NzIwNjA1ZTc0MTRh\n" +
            "YjRmYmNhMjFjYjRiMzFlNWI0ZTYyYTZmYTUxYzI0YTllYWKIjAQQFgoAPgWCZVO9\n" +
            "bwQLCQcICZAuNFtLSY2XJAMVCAoEFgACAQIZAQKbAwIeARYhBDIhViOFpzWovPuw\n" +
            "vC40W0tJjZckAACTeAEA+AdXmA8p6I+FFqXaFVRh5JRa5ZoO4xhGb+QY00kgZisB\n" +
            "AJee8XdW6FHBj2J3b4M9AYqufdpvuj+lLmaVAshN9U4MuDgEZVO9bxIKKwYBBAGX\n" +
            "VQEFAQEHQORkbvSesg9oJeCRKigTNdQ5tkgmVGXfdz/+vwBIl3E3AwEIB4h4BBgW\n" +
            "CAAqBYJlU71vCZAuNFtLSY2XJAKbDBYhBDIhViOFpzWovPuwvC40W0tJjZckAABZ\n" +
            "1AD/RIJM/WNb28pYqtq4XmeOaqLCrbQs2ua8mXpGBZSl8E0BALWSlbHICYTNy9L6\n" +
            "KV0a5pXbxcXpzejcjpJmVwzuWz8P\n" +
            "=32+r\n" +
            "-----END PGP PUBLIC KEY BLOCK-----"

    @Test
    fun testDecryptPrivateKey() {
        assertTrue(PgpKeyGenerator.doKeysMatch(pubKey, encPrivKey, robotToken))
    }

    @Test
    fun testGenerateKeyPair() {
        val robotToken = generateSecureToken()
        val robotTokenHash = tokenSha256Hash(robotToken)

        val (publicKey, encryptedPrivateKey) = PgpKeyGenerator.generateKeyPair(
            robotTokenHash,
            robotToken
        )

        assertNotNull("Public key should not be null", publicKey)
        assertNotNull("Encrypted private key should not be null", encryptedPrivateKey)
    }

    @Test
    fun testGenerateKeyPairPublicKey() {
        val identity = "TestUser"
        val passphrase = "testPassphrase"

        val (publicKey, _) = PgpKeyGenerator.generateKeyPair(identity, passphrase)

        assertTrue(
            "Public key should start with public key header",
            publicKey.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")
        )
        assertTrue(
            "Public key should end with public key footer",
            publicKey.endsWith("-----END PGP PUBLIC KEY BLOCK-----\n")
        )
    }

    @Test
    fun testGenerateKeyPairEncryptedPrivateKeyFormat() {
        val identity = "TestUser"
        val passphrase = "testPassphrase"

        val (_, encryptedPrivateKey) = PgpKeyGenerator.generateKeyPair(identity, passphrase)

        assertTrue(
            "Encrypted private key should start with private key header",
            encryptedPrivateKey.startsWith("-----BEGIN PGP PRIVATE KEY BLOCK-----")
        )
        assertTrue(
            "Encrypted private key should end with private key footer",
            encryptedPrivateKey.endsWith("-----END PGP PRIVATE KEY BLOCK-----\n")
        )
    }
}
