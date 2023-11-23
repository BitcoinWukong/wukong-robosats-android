package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.generateSecureToken
import com.bitcoinwukong.robosats_android.utils.tokenSha256Hash
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgpKeyGeneratorTest {
    @Test
    fun testGenerateKeyPair() {
        val robotToken = generateSecureToken()
        val robotTokenHash = tokenSha256Hash(robotToken)

        val (publicKey, encryptedPrivateKey) = PgpKeyGenerator.generateKeyPair(robotTokenHash, robotToken)

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
