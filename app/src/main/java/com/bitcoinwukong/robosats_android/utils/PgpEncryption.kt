package com.bitcoinwukong.robosats_android.utils

import android.util.Log
import com.bitcoinwukong.robosats_android.model.PGPPrivateKeyBundle
import com.bitcoinwukong.robosats_android.model.PGPPublicKeyBundle
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGInputStream
import org.bouncycastle.bcpg.BCPGObject
import org.bouncycastle.bcpg.BCPGOutputStream
import org.bouncycastle.bcpg.ECSecretBCPGKey
import org.bouncycastle.bcpg.EdSecretBCPGKey
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import java.util.Date

object PgpKeyGenerator {
    init {
        // Remove the built-in Android Bouncy Castle security provider and replace it with
        // the standard one that has more algorithms
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }

    fun decryptPrivateKeys(
        encryptedPrivateKey: String,
        passphrase: String
    ): PGPPrivateKeyBundle {
        val secretKeyRingCollection = PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(ByteArrayInputStream(encryptedPrivateKey.toByteArray())),
            JcaKeyFingerprintCalculator()
        )

        if (secretKeyRingCollection.keyRings.hasNext()) {
            val keyRing = secretKeyRingCollection.keyRings.next()
            val decryptor =
                JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray())
            val keyList = keyRing.secretKeys.asSequence().toList()

            val primaryKey = keyList[0].extractPrivateKey(decryptor)
            val subKey = keyList[1].extractPrivateKey(decryptor)

            return PGPPrivateKeyBundle(primaryKey, subKey)
        }

        throw IllegalArgumentException("Unable to decrypt private key $encryptedPrivateKey")
    }

    fun getEncryptedData(
        encryptedMessage: String,
        pgpPrivateKey: PGPPrivateKey
    ): PGPPublicKeyEncryptedData {
        val inputStream =
            PGPUtil.getDecoderStream(
                ByteArrayInputStream(
                    encryptedMessage.replace("\\", "\n").toByteArray()
                )
            )
        val pgpObjectFactory = PGPObjectFactory(inputStream, JcaKeyFingerprintCalculator())
        var pgpObject: Any?
        while (pgpObjectFactory.nextObject().also { pgpObject = it } != null) {
            if (pgpObject is PGPEncryptedDataList) {
                for (pgpData in pgpObject as PGPEncryptedDataList) {
                    if (pgpData is PGPPublicKeyEncryptedData && pgpData.keyID == pgpPrivateKey.keyID) {
                        return pgpData
                    }
                }
            }
        }

        throw IllegalArgumentException("No encrypted data found for the provided private key")
    }

    fun extractPgpObjectsList(
        pgpData: PGPPublicKeyEncryptedData,
        pgpPrivateKey: PGPPrivateKey
    ): List<Any> {
        val dataDecryptorFactory =
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey)
        val clearData = pgpData.getDataStream(dataDecryptorFactory)

        val plainFactory = PGPObjectFactory(clearData, JcaKeyFingerprintCalculator())
        return plainFactory.asSequence().toList()
    }


    fun readPGPLiteralData(literalData: PGPLiteralData): ByteArray {
        ByteArrayOutputStream().use { baos ->
            literalData.inputStream.use { input ->
                input.copyTo(baos)
            }
            return baos.toByteArray()
        }
    }

    fun createPGPEncryptedDataByteArray(
        message: String,
        signatureKey: PGPPrivateKey,
        encryptionKey: PGPPublicKey
    ): ByteArray {
        val buffer = ByteArrayOutputStream()

        // Set up the encrypted data generator
        val encGen = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                .setSecureRandom(SecureRandom())
                .setWithIntegrityPacket(true)
                .setProvider("BC")
        )
        encGen.addMethod(JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey).setProvider("BC"))

        // Set up the signature generator
        val sigGen = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(
                signatureKey.publicKeyPacket.algorithm,
                PGPUtil.SHA512
            ).setProvider("BC")
        ).apply {
            init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signatureKey)
        }

        val bOut = ByteArrayOutputStream()

        // Create one-pass signature list
        val onePassSignature = sigGen.generateOnePassVersion(false)
        onePassSignature.encode(bOut)

        // Create literal data
        val literalDataGen = PGPLiteralDataGenerator()
        val pOut = literalDataGen.open(
            bOut, PGPLiteralData.UTF8, "", Date(), ByteArray(256)
        ).apply {
            write(message.toByteArray())
        }
        pOut.close()

        // Create signature list
        sigGen.update(message.toByteArray())
        val signature = sigGen.generate()
        signature.encode(bOut)

        val encryptedOut = encGen.open(buffer, bOut.size().toLong())

        // Writing to encrypted output stream
        encryptedOut.write(bOut.toByteArray())
        encryptedOut.close()

        return buffer.toByteArray()
    }

    fun getPGPPublicKeyEncryptedDataFromByteArray(byteArray: ByteArray): PGPPublicKeyEncryptedData {
        val pgpData = PGPObjectFactory(
            byteArray,
            JcaKeyFingerprintCalculator()
        ).nextObject() as PGPEncryptedDataList
        return pgpData.get(0) as PGPPublicKeyEncryptedData
    }

    fun encryptMessage(
        message: String,
        signatureKey: PGPPrivateKey,
        senderPublicKey: PGPPublicKey,
        receiverPublicKey: PGPPublicKey
    ): String {
        val bOut = ByteArrayOutputStream()

        // Common components (one-pass signature, literal data, and signature list)
        val signatureData = createSignatureData(message, signatureKey)
        bOut.write(signatureData)

        // Encrypt for both receiverPublicKey and senderPublicKey
        val encryptedData = encryptDataForMultipleKeys(
            bOut.toByteArray(),
            listOf(receiverPublicKey, senderPublicKey)
        )

        // Create armored string
        return createArmoredEncryptedMessage(encryptedData)
    }

    private fun encryptDataForMultipleKeys(
        data: ByteArray,
        publicKeys: List<PGPPublicKey>
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        val encGen = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                .setSecureRandom(SecureRandom())
                .setWithIntegrityPacket(true)
                .setProvider("BC")
        )

        // Add all public keys to the encryption generator
        publicKeys.forEach { publicKey ->
            encGen.addMethod(JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"))
        }

        encGen.open(buffer, data.size.toLong()).use { encryptedOut ->
            encryptedOut.write(data)
        }

        return buffer.toByteArray()
    }

    fun createArmoredEncryptedMessage(encryptedDataBytes: ByteArray): String {
        // Convert the byte array into an armored string
        return ByteArrayOutputStream().use { armoredStream ->
            ArmoredOutputStream(armoredStream).use { armorOut ->
                armorOut.setHeader("Version", null)
                armorOut.write(encryptedDataBytes)
            }
            armoredStream.toString("UTF-8")
        }.replace("\n", "\\")
    }

    private fun createSignatureData(message: String, signatureKey: PGPPrivateKey): ByteArray {
        val sigGen = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(
                signatureKey.publicKeyPacket.algorithm,
                PGPUtil.SHA512
            ).setProvider("BC")
        ).apply {
            init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signatureKey)
        }

        val bOut = ByteArrayOutputStream()

        // One-pass signature list
        val onePassSignature = sigGen.generateOnePassVersion(false)
        onePassSignature.encode(bOut)

        // Literal data
        val literalDataGen = PGPLiteralDataGenerator()
        literalDataGen.open(bOut, PGPLiteralData.UTF8, "", Date(), ByteArray(256)).use { pOut ->
            pOut.write(message.toByteArray())
        }

        // Signature list
        sigGen.update(message.toByteArray())
        val signature = sigGen.generate()
        signature.encode(bOut)

        return bOut.toByteArray()
    }

    fun decryptMessageContent(
        pgpData: PGPPublicKeyEncryptedData,
        pgpPrivateKey: PGPPrivateKey
    ): String {
        val dataDecryptorFactory =
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey)
        val clearData = pgpData.getDataStream(dataDecryptorFactory)

        val plainFactory = PGPObjectFactory(clearData, JcaKeyFingerprintCalculator())
        var messageContent: String? = null

        var pgpObject: Any?
        while (plainFactory.nextObject().also { pgpObject = it } != null) {
            if (pgpObject is PGPLiteralData) {
                val literalDataInputStream = (pgpObject as PGPLiteralData).inputStream
                messageContent = literalDataInputStream.bufferedReader().readText()
                break
            }
        }

        if (messageContent != null) {
            return messageContent
        }

        throw IllegalArgumentException("Unable to decrypt the message content with the provided private key")
    }

    fun decryptMessage(encryptedMessage: String, pgpPrivateKey: PGPPrivateKey): String {
        val encryptedData = getEncryptedData(encryptedMessage, pgpPrivateKey)
        return decryptMessageContent(encryptedData, pgpPrivateKey)
    }

    fun generateKeyPair(
        identity: String,
        passphrase: String,
        algorithm: Int = PGPPublicKey.RSA_GENERAL,
        keySize: Int = 2048
    ): Pair<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize, SecureRandom())

        // Generate the primary key pair (used for signing)
        val primaryPair = keyPairGenerator.generateKeyPair()
        val pgpPrimaryPair = JcaPGPKeyPair(algorithm, primaryPair, Date())

        // Generate the subkey pair (used for encryption)
        val subKeyPair = keyPairGenerator.generateKeyPair()
        val pgpSubKeyPair = JcaPGPKeyPair(algorithm, subKeyPair, Date())

        // Key ring generator with primary key
        val digestCalculator = JcaPGPDigestCalculatorProviderBuilder().build().get(PGPUtil.SHA1)

        val keyRingGen = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            pgpPrimaryPair,
            identity,
            digestCalculator,
            null,
            null,
            JcaPGPContentSignerBuilder(pgpPrimaryPair.publicKey.algorithm, PGPUtil.SHA1),
            JcePBESecretKeyEncryptorBuilder(
                PGPEncryptedData.CAST5,
                digestCalculator
            ).setProvider("BC").build(passphrase.toCharArray())
        )

        // Add the subkey to the key ring
        keyRingGen.addSubKey(pgpSubKeyPair)

        // Generate the key ring
        val secretKeyRing = keyRingGen.generateSecretKeyRing()
        val publicKeyRing = PGPPublicKeyRing(secretKeyRing.publicKeys.asSequence().toList())
        val publicKey = exportPublicKey(publicKeyRing)
        val encryptedPrivateKey = exportEncryptedPrivateKey(secretKeyRing)

        return Pair(publicKey, encryptedPrivateKey)
    }


    fun decryptMessage(
        encryptedMessage: String,
        encryptedPrivateKey: String,
        passphrase: String
    ): String {
        val pgpPrivateKeys = decryptPrivateKeys(encryptedPrivateKey, passphrase)
        return decryptMessage(encryptedMessage, pgpPrivateKeys.encryptionKey)
    }

    fun readPublicKey(armoredPublicKey: String): PGPPublicKeyBundle {
        val inputStream: InputStream =
            ByteArrayInputStream(armoredPublicKey.toByteArray(Charsets.UTF_8))
        val pgpObjectFactory = PGPUtil.getDecoderStream(inputStream)
        val pgpPubKeyRingCollection =
            PGPPublicKeyRingCollection(pgpObjectFactory, JcaKeyFingerprintCalculator())
        val pgpPubKeyRing = pgpPubKeyRingCollection.iterator().next()
        val keys = pgpPubKeyRing.publicKeys.asSequence().toList()

        return PGPPublicKeyBundle(keys[0], keys[1])
    }

    private fun exportPublicKey(publicKeyRing: PGPPublicKeyRing): String {
        val outputStream = ByteArrayOutputStream()
        val armoredStream = ArmoredOutputStream(outputStream)
        publicKeyRing.encode(armoredStream)
        armoredStream.close()
        return String(outputStream.toByteArray(), Charsets.UTF_8)
    }

    private fun exportEncryptedPrivateKey(secretKeyRing: PGPSecretKeyRing): String {
        val out = ByteArrayOutputStream()
        val armoredOut = ArmoredOutputStream(out)
        secretKeyRing.encode(armoredOut)
        armoredOut.close()
        return out.toString("UTF-8")
    }

    fun serializePGPPrivateKey(privateKey: PGPPrivateKey): String {
        val byteStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteStream)
        dataOut.writeLong(privateKey.keyID)
        val bcpgOut = BCPGOutputStream(dataOut)
        bcpgOut.writePacket(privateKey.publicKeyPacket)

        val privateDataPacket = privateKey.privateKeyDataPacket as BCPGObject
        bcpgOut.writeObject(privateDataPacket)
        return Base64.getEncoder().encodeToString(byteStream.toByteArray())
    }

    fun deserializePGPPrivateKey(serializedKey: String, isSigningKey: Boolean): PGPPrivateKey {
        val data = Base64.getDecoder().decode(serializedKey)
        val byteStream = ByteArrayInputStream(data)
        val dataIn = DataInputStream(byteStream)
        val keyID = dataIn.readLong()
        val bcpgIn = BCPGInputStream(dataIn)
        val publicKeyPacket = bcpgIn.readPacket() as PublicKeyPacket
        val privateDataPacket = if (isSigningKey) {
            EdSecretBCPGKey(bcpgIn)
        } else {
            ECSecretBCPGKey(bcpgIn)
        }
        return PGPPrivateKey(keyID, publicKeyPacket, privateDataPacket)
    }

}
