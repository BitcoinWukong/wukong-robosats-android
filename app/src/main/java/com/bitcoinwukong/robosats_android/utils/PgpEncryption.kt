package com.bitcoinwukong.robosats_android.utils


import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGInputStream
import org.bouncycastle.bcpg.BCPGOutputStream
import org.bouncycastle.bcpg.ECSecretBCPGKey
import org.bouncycastle.bcpg.PublicSubkeyPacket
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPSignatureList
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
import java.net.URLEncoder
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

    fun decryptPrivateKey(encryptedPrivateKey: String, passphrase: String): PGPPrivateKey {
        val secretKeyRingCollection = PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(ByteArrayInputStream(encryptedPrivateKey.toByteArray())),
            JcaKeyFingerprintCalculator()
        )

        if (secretKeyRingCollection.keyRings.hasNext()) {
            val keyRing = secretKeyRingCollection.keyRings.next()

            // Check if there's a subkey (index 1) and use it for decryption
            val secretKeys = keyRing.secretKeys.asSequence().toList()
            if (secretKeys.size > 1) { // Check if there's a subkey
                val subkey = secretKeys[1] // Use the subkey (index 1)
                val decryptor = JcePBESecretKeyDecryptorBuilder().setProvider("BC")
                    .build(passphrase.toCharArray())
                return subkey.extractPrivateKey(decryptor)
            }
        }

        throw IllegalArgumentException("Unable to decrypt private key $encryptedPrivateKey")
    }

    fun decodeEncryptedMessage(encryptedMessage: String): PGPEncryptedDataList {
        val encryptedMessage = encryptedMessage.replace("\\", "\n")
        val inputStream =
            PGPUtil.getDecoderStream(ByteArrayInputStream(encryptedMessage.toByteArray()))
        val pgpObjectFactory = PGPObjectFactory(inputStream, JcaKeyFingerprintCalculator())
        return pgpObjectFactory.nextObject() as PGPEncryptedDataList
    }

    fun getEncryptedData(
        encryptedMessage: String,
        pgpPrivateKey: PGPPrivateKey
    ): PGPPublicKeyEncryptedData {
        for (pgpData in decodeEncryptedMessage(encryptedMessage)) {
            if (pgpData is PGPPublicKeyEncryptedData && pgpData.keyID == pgpPrivateKey.keyID) {
                return pgpData
            }
        }

        throw IllegalArgumentException("No encrypted data found for the provided private key")
    }

    fun extractLiteralData(
        pgpData: PGPPublicKeyEncryptedData,
        pgpPrivateKey: PGPPrivateKey
    ): PGPLiteralData {
        val dataDecryptorFactory =
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey)
        val clearData = pgpData.getDataStream(dataDecryptorFactory)

        val plainFactory = PGPObjectFactory(clearData, JcaKeyFingerprintCalculator())
        var messageContent: String? = null

        val pgpObjectsList = plainFactory.asSequence().toList()
        for (pgpObject in pgpObjectsList) {
            if (pgpObject is PGPLiteralData) {
                return pgpObject
            }
        }

        throw IllegalArgumentException("Unable to decrypt the message content with the provided private key")
    }

    fun generatePGPLiteralData(message: String): PGPLiteralData? {
        val byteStream = ByteArrayOutputStream()

        // Use PGPLiteralDataGenerator to generate literal data into a stream
        val literalDataGenerator = PGPLiteralDataGenerator()
        val output = literalDataGenerator.open(
            byteStream,
            PGPLiteralData.TEXT,
            "",
            message.toByteArray().size.toLong(),
            Date()
        )

        output.use {
            it.write(message.toByteArray())
        }

        // Parse the generated data back into a PGPLiteralData object
        val literalDataBytes = ByteArrayInputStream(byteStream.toByteArray())
        val pgpFact = PGPObjectFactory(literalDataBytes, JcaKeyFingerprintCalculator())

        return pgpFact.nextObject() as? PGPLiteralData
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

        val pgpObjectsList = plainFactory.asSequence().toList()
        for (pgpObject in pgpObjectsList) {
            if (pgpObject is PGPLiteralData) {
                val literalDataInputStream = pgpObject.inputStream
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

    fun convertPGPPublicKeyEncryptedDataToByteArray(pgpPublicKeyEncryptedData: PGPPublicKeyEncryptedData, pgpPrivateKey: PGPPrivateKey): ByteArray {
        val dataDecryptorFactory =
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey)
        val clearData = pgpPublicKeyEncryptedData.getDataStream(dataDecryptorFactory)

        ByteArrayOutputStream().use { baos ->
            clearData.copyTo(baos)
            return baos.toByteArray()
        }
    }

    fun encryptAndSignMessage(message: String, publicKey: PGPPublicKey, privateKey: PGPPrivateKey): ByteArray {
        val out = ByteArrayOutputStream()

        val signatureGenerator = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(privateKey.publicKeyPacket.algorithm, PGPUtil.SHA256).setProvider("BC")
        ).apply {
            init(PGPSignature.BINARY_DOCUMENT, privateKey)
        }

        val encryptedDataGenerator = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                .setWithIntegrityPacket(true)
                .setSecureRandom(SecureRandom())
                .setProvider("BC")
        ).apply {
            addMethod(JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"))
        }

        ArmoredOutputStream(out).use { armoredOutputStream ->
            encryptedDataGenerator.open(armoredOutputStream, ByteArray(1 shl 16)).use { encryptedOut ->
                // Write One Pass Signature
                val onePassSignature = signatureGenerator.generateOnePassVersion(false)
                onePassSignature.encode(encryptedOut)

                // Generate Literal Data
                val literalDataGenerator = PGPLiteralDataGenerator()
                val literalOut = literalDataGenerator.open(
                    encryptedOut, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE, message.toByteArray().size.toLong(), Date()
                )

                try {
                    literalOut.write(message.toByteArray())
                    signatureGenerator.update(message.toByteArray())
                } finally {
                    literalOut.close()
                }

                // Write Signature List
                val signature = signatureGenerator.generate()
                signature.encode(encryptedOut)
            }
        }

        return out.toByteArray()
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

    fun decryptMessage(
        encryptedMessage: String,
        encryptedPrivateKey: String,
        passphrase: String
    ): String {
        val pgpPrivateKey = decryptPrivateKey(encryptedPrivateKey, passphrase)
        return decryptMessage(encryptedMessage, pgpPrivateKey)
    }

    fun readPublicKey(armoredPublicKey: String): PGPPublicKey? {
        val inputStream: InputStream =
            ByteArrayInputStream(armoredPublicKey.toByteArray(Charsets.UTF_8))
        val pgpObjectFactory = PGPUtil.getDecoderStream(inputStream)
        val pgpPubKeyRingCollection =
            PGPPublicKeyRingCollection(pgpObjectFactory, JcaKeyFingerprintCalculator())
        val pgpPubKeyRing = pgpPubKeyRingCollection.iterator().next()
        val keys = pgpPubKeyRing.publicKeys.asSequence().toList()

        return keys[1]
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
        armoredOut.setHeader("Version", null)
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

    fun serializePGPPrivateKey(privateKey: PGPPrivateKey): String {
        val byteStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteStream)
        dataOut.writeLong(privateKey.keyID)
        val bcpgOut = BCPGOutputStream(dataOut)
        bcpgOut.writePacket(privateKey.publicKeyPacket)

        val privateDataPacket = privateKey.privateKeyDataPacket as ECSecretBCPGKey
        bcpgOut.writeObject(privateDataPacket)
        return Base64.getEncoder().encodeToString(byteStream.toByteArray())
    }

    fun deserializePGPPrivateKey(serializedKey: String): PGPPrivateKey {
        val data = Base64.getDecoder().decode(serializedKey)
        val byteStream = ByteArrayInputStream(data)
        val dataIn = DataInputStream(byteStream)
        val keyID = dataIn.readLong()
        val bcpgIn = BCPGInputStream(dataIn)
        val publicKeyPacket = bcpgIn.readPacket() as PublicSubkeyPacket
        val privateDataPacket = ECSecretBCPGKey(bcpgIn)

        return PGPPrivateKey(keyID, publicKeyPacket, privateDataPacket)
    }

}
