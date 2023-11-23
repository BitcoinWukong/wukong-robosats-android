package com.bitcoinwukong.robosats_android.utils

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

object PgpKeyGenerator {
    init {
        // Register the Bouncy Castle provider
        Security.addProvider(BouncyCastleProvider())
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
                PGPEncryptedData.CAST5,
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


}
