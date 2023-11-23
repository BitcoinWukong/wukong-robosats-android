package com.bitcoinwukong.robosats_android.utils

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

object PgpKeyGenerator {
    init {
        // Register the Bouncy Castle provider
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateKeyPair(
        identity: String,
        passphrase: String,
        algorithm: Int = PGPPublicKey.RSA_GENERAL,
        keySize: Int = 2048
    ): Pair<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize)

        val keyPair = keyPairGenerator.generateKeyPair()
        val pgpKeyPair = JcaPGPKeyPair(algorithm, keyPair, Date())
        val secretKeyRing = generateSecretKey(pgpKeyPair, identity, passphrase.toCharArray())

        val publicKey = exportPublicKey(secretKeyRing.publicKey)
        val encryptedPrivateKey = exportEncryptedPrivateKey(secretKeyRing)

        return Pair(publicKey, encryptedPrivateKey)
    }

    fun decryptPrivateKey(encryptedPrivateKey: String, passphrase: String): PGPPrivateKey? {
        val secretKeyRing = readSecretKeyRing(encryptedPrivateKey)
        val secretKey = secretKeyRing.secretKey
        return secretKey?.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray())
        )
    }

    private fun readSecretKeyRing(armoredPrivateKey: String): PGPSecretKeyRing {
        val inputStream: InputStream =
            ByteArrayInputStream(armoredPrivateKey.toByteArray(Charsets.UTF_8))
        val pgpObjectFactory = PGPUtil.getDecoderStream(inputStream)
        val pgpSecretKeyRingCollection =
            PGPSecretKeyRingCollection(pgpObjectFactory, JcaKeyFingerprintCalculator())
        return pgpSecretKeyRingCollection.iterator().next()
    }

    fun doKeysMatch(publicKeyArmored: String, encryptedPrivateKey: String, passphrase: String): Boolean {
        val decryptedPrivateKey = decryptPrivateKey(encryptedPrivateKey, passphrase)
        val publicKey = readPublicKey(publicKeyArmored)

        return decryptedPrivateKey?.keyID == publicKey?.keyID
    }

    private fun readPublicKey(armoredPublicKey: String): PGPPublicKey? {
        val inputStream: InputStream = ByteArrayInputStream(armoredPublicKey.toByteArray(Charsets.UTF_8))
        val pgpObjectFactory = PGPUtil.getDecoderStream(inputStream)
        val pgpPubKeyRingCollection = PGPPublicKeyRingCollection(pgpObjectFactory, JcaKeyFingerprintCalculator())
        val pgpPubKeyRing = pgpPubKeyRingCollection.iterator().next()

        return pgpPubKeyRing.publicKey
    }

    private fun generateSecretKey(
        keyPair: PGPKeyPair,
        identity: String,
        passphrase: CharArray
    ): PGPSecretKeyRing {
        val digestCalculator = JcaPGPDigestCalculatorProviderBuilder().build().get(PGPUtil.SHA1)

        val keyRingGen = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            keyPair,
            identity,
            digestCalculator,
            null,
            null,
            JcaPGPContentSignerBuilder(keyPair.publicKey.algorithm, PGPUtil.SHA256),
            JcePBESecretKeyEncryptorBuilder(
                PGPEncryptedData.AES_128,
                digestCalculator
            ).setProvider("BC").build(passphrase)
        )

        return keyRingGen.generateSecretKeyRing()
    }

    private fun exportPublicKey(publicKey: PGPPublicKey): String {
        val out = ByteArrayOutputStream()
        val armoredOut = ArmoredOutputStream(out)
        publicKey.encode(armoredOut)
        armoredOut.close()
        return out.toString("UTF-8")
    }

    private fun exportEncryptedPrivateKey(secretKeyRing: PGPSecretKeyRing): String {
        val out = ByteArrayOutputStream()
        val armoredOut = ArmoredOutputStream(out)
        secretKeyRing.encode(armoredOut)
        armoredOut.close()
        return out.toString("UTF-8")
    }
}
