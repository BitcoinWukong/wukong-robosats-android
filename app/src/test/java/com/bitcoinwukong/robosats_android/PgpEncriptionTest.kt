package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.createPGPEncryptedData
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.decryptPrivateKeys
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.extractPgpObjectsList
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.getEncryptedData
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.readPGPLiteralData
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator.readPublicKey
import com.bitcoinwukong.robosats_android.utils.generateSecureToken
import com.bitcoinwukong.robosats_android.utils.tokenSha256Hash
import org.bouncycastle.bcpg.ECDHPublicBCPGKey
import org.bouncycastle.bcpg.ECSecretBCPGKey
import org.bouncycastle.bcpg.PublicSubkeyPacket
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPSignatureList
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgpKeyGeneratorTest {
    @Test
    fun testSerializeDeserializePrivateKey() {
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
        val pgpPrivateKeys = decryptPrivateKeys(encPrivKey, robotToken)
        val decryptionKey = pgpPrivateKeys.second
        val encodedKeyStr = PgpKeyGenerator.serializePGPPrivateKey(decryptionKey)
        val decodedPrivateKey = PgpKeyGenerator.deserializePGPPrivateKey(encodedKeyStr)
        assertEquals(decryptionKey.keyID, decodedPrivateKey.keyID)
        assertEquals(
            ((decryptionKey.publicKeyPacket as PublicSubkeyPacket).key as ECDHPublicBCPGKey).encodedPoint,
            ((decodedPrivateKey.publicKeyPacket as PublicSubkeyPacket).key as ECDHPublicBCPGKey).encodedPoint
        )
        assertEquals(
            (decryptionKey.privateKeyDataPacket as ECSecretBCPGKey).x,
            (decodedPrivateKey.privateKeyDataPacket as ECSecretBCPGKey).x
        )
    }

    @Test
    fun testDecryptPrivateKey() {
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
        val keyId = 7088936486162781302
        val pgpPrivateKey = decryptPrivateKeys(encPrivKey, robotToken).second
        assertEquals(keyId, pgpPrivateKey.keyID)
    }

    @Test
    fun testReadPublicKey() {
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
        val keyId = 7088936486162781302
        val pgpPrivateKey = readPublicKey(pubKey)
        assertEquals(keyId, pgpPrivateKey!!.keyID)
    }

    @Test
    fun testDecryptMessage() {
        val token = "BFK7MX9J3bxiOQzz4tWylTM9BqL6HRVIFIMp"
        val encryptedPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "\n" +
                "    xYYEZVvV/xYJKwYBBAHaRw8BAQdAly0Ja3zv2dp4yGJSVHKSg4EdaMLPFRbs\n" +
                "    e3YHulYfRtz+CQMI5AxhtOiljJ3gpdDAKPvXeDwlbfI117Bt8mghzcShibTE\n" +
                "    gD5tsX1lSEb+6FatZv1bMqHOgWCk8AN9yEg2KDvhBpMJkSjMlwU6dBOnIPpK\n" +
                "    481MUm9ib1NhdHMgSUQgNTljZDY0NjE3Y2ZkYzg5OWM1MWMzZDMyMWFiMzk4\n" +
                "    MTY1MGFmMjQxOGI1M2MwZjdhZWViNzUyNGJmYmIxMjczYsKMBBAWCgA+BYJl\n" +
                "    W9X/BAsJBwgJkIcXOaDFqg8eAxUICgQWAAIBAhkBApsDAh4BFiEE5Cdin5qQ\n" +
                "    nUTawdejhxc5oMWqDx4AAN39AP954xh7wtYn+3ks75CpSFPo7CRmiAuMmIib\n" +
                "    RXsh0+u3JgEAnqm0RwZnJxKq3DOBagawyeWROqmDTSx37GrnE/CLKwXHiwRl\n" +
                "    W9X/EgorBgEEAZdVAQUBAQdA+7nK3JmX6lA/viMnWT/N7jzF+W8T5GRGOsAU\n" +
                "    D/EjhAEDAQgH/gkDCLLQ+gcXEoCo4Ngj8HHzMRt5ZA486nnV7pxX+t0FkACt\n" +
                "    PX8kccVTQWDG5IVrv9gWXmRM3JV2dGfGvMIIcyEdGz4Tn7D3Xbkcr1jz8Ljx\n" +
                "    lIXCeAQYFggAKgWCZVvV/wmQhxc5oMWqDx4CmwwWIQTkJ2KfmpCdRNrB16OH\n" +
                "    FzmgxaoPHgAAo+kA/Agh94fIbvVVZxIZSWzAnmn1SZQB+RaV+bfQzVlw0QmA\n" +
                "    AP9cekz8onDDYojNUCecYkVdJR3EGPZgJ6mMdNv7bvcfBQ==\n" +
                "    =bMIk\n" +
                "    -----END PGP PRIVATE KEY BLOCK-----"
        val encryptedMessage1 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdA+z5qZG4UQ4XZW80hjHdGwbNSrL7zO45csZpw5JuO\\4jUwEpBa4zr4w8FLaQ+srHqC9Bmyac3ZUN/EI4b3CgWbmIHumNNzwgn1nAth\\AIsJLEdTwV4DcDBaMBvbOlISAQdA+rmSu7t9XEhW0677kn+IsPaY2jADrDKl\\FFQ5HJeVpjIwCC+ry0SDlhSQphIHyFKyo2oV4YSVVsP5N2MkWBaYptYLRuxR\\BI6QjHHIR4Wc4C970rwBC0bPUi+Eyjvpc622b8ndg2G6STQwwfUlZjuASZ0z\\UdTKD9qA2eT7cvFVXXGe6hsEUbP+ZUtGgY9xUAiXXjavL92M30yg//SJKq2p\\MvCrtEnAZxYRsZjZzS4lTNSTMQf1IANuQXL4Zx5rc072OBgLuKbvC2AfzY8f\\qH57cNb2l7OGmjbSzfe6w1freIVKh2g/kVs5tXzYMm0mm5TkBAWa5x5qFgSP\\aIJgvbQWBR7odUMDAsv7Px7p8H7IXw==\\=lFWF\\-----END PGP MESSAGE-----\\"
        val encryptedMessage2 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdAZO7KAUrxMaUwULL+6EXVhOoYrQZON+Zyhq4Aw52m\\GiIwnrTQJT8rhO3x8TDHnFpvEA9Cas3Fm8p7v41Lj+Wu7ezndABXvJNuYF/m\\oz/o0eEXwV4DcDBaMBvbOlISAQdAEz9Ot7zLfWzVvCcJEsnV70PsHMkPIy9h\\fMiTvgNn1wUwd/t2IFFtKSPLtFKVnRyvBIrj/vR+0xDE1cgj0KDJ9/AJ3i9o\\0U0jbM2VNMqQ+qJM0sAJAaR5/hEEl6JrhRQzm/AYwdNJV00AOAJG57wLial+\\t1k4m34pFvkVdDycKSRWv92ob07EvzuAwbEHnXMqnBcTSTX3hWe1juj6Kwuo\\SMArMQhOlU2ao0DgN9HJd4hYI6DpkURaRZyxp+c7H+uBUHN7XYGiUuuKmfFs\\4M0A/qYC5PxAuDnq4/xe8mipl+Id9a5pYTO4BJLIYDOK1CcQ0hPS9mnL+/u7\\5cyopyr5tc988c8BPpmg3TfOs+2dLDHamHjWM3+77J4kNYZQ\\=CQvx\\-----END PGP MESSAGE-----\\"
        assertEquals(
            "Hello",
            PgpKeyGenerator.decryptMessage(encryptedMessage1, encryptedPrivateKey, token)
        )
        assertEquals(
            "How are you doing?",
            PgpKeyGenerator.decryptMessage(encryptedMessage2, encryptedPrivateKey, token)
        )
    }

    @Test
    fun testCreatePGPEncryptedData() {
        val publicKeyArmor = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
                "\n" +
                "        mDMEZVvV/xYJKwYBBAHaRw8BAQdAly0Ja3zv2dp4yGJSVHKSg4EdaMLPFRbse3YH\n" +
                "        ulYfRty0TFJvYm9TYXRzIElEIDU5Y2Q2NDYxN2NmZGM4OTljNTFjM2QzMjFhYjM5\n" +
                "        ODE2NTBhZjI0MThiNTNjMGY3YWVlYjc1MjRiZmJiMTI3M2KIjAQQFgoAPgWCZVvV\n" +
                "        /wQLCQcICZCHFzmgxaoPHgMVCAoEFgACAQIZAQKbAwIeARYhBOQnYp+akJ1E2sHX\n" +
                "        o4cXOaDFqg8eAADd/QD/eeMYe8LWJ/t5LO+QqUhT6OwkZogLjJiIm0V7IdPrtyYB\n" +
                "        AJ6ptEcGZycSqtwzgWoGsMnlkTqpg00sd+xq5xPwiysFuDgEZVvV/xIKKwYBBAGX\n" +
                "        VQEFAQEHQPu5ytyZl+pQP74jJ1k/ze48xflvE+RkRjrAFA/xI4QBAwEIB4h4BBgW\n" +
                "        CAAqBYJlW9X/CZCHFzmgxaoPHgKbDBYhBOQnYp+akJ1E2sHXo4cXOaDFqg8eAACj\n" +
                "        6QD8CCH3h8hu9VVnEhlJbMCeafVJlAH5FpX5t9DNWXDRCYAA/1x6TPyicMNiiM1Q\n" +
                "        J5xiRV0lHcQY9mAnqYx02/tu9x8F\n" +
                "        =8/1E\n" +
                "        -----END PGP PUBLIC KEY BLOCK-----"
        val encryptedPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "\n" +
                "    xYYEZVvV/xYJKwYBBAHaRw8BAQdAly0Ja3zv2dp4yGJSVHKSg4EdaMLPFRbs\n" +
                "    e3YHulYfRtz+CQMI5AxhtOiljJ3gpdDAKPvXeDwlbfI117Bt8mghzcShibTE\n" +
                "    gD5tsX1lSEb+6FatZv1bMqHOgWCk8AN9yEg2KDvhBpMJkSjMlwU6dBOnIPpK\n" +
                "    481MUm9ib1NhdHMgSUQgNTljZDY0NjE3Y2ZkYzg5OWM1MWMzZDMyMWFiMzk4\n" +
                "    MTY1MGFmMjQxOGI1M2MwZjdhZWViNzUyNGJmYmIxMjczYsKMBBAWCgA+BYJl\n" +
                "    W9X/BAsJBwgJkIcXOaDFqg8eAxUICgQWAAIBAhkBApsDAh4BFiEE5Cdin5qQ\n" +
                "    nUTawdejhxc5oMWqDx4AAN39AP954xh7wtYn+3ks75CpSFPo7CRmiAuMmIib\n" +
                "    RXsh0+u3JgEAnqm0RwZnJxKq3DOBagawyeWROqmDTSx37GrnE/CLKwXHiwRl\n" +
                "    W9X/EgorBgEEAZdVAQUBAQdA+7nK3JmX6lA/viMnWT/N7jzF+W8T5GRGOsAU\n" +
                "    D/EjhAEDAQgH/gkDCLLQ+gcXEoCo4Ngj8HHzMRt5ZA486nnV7pxX+t0FkACt\n" +
                "    PX8kccVTQWDG5IVrv9gWXmRM3JV2dGfGvMIIcyEdGz4Tn7D3Xbkcr1jz8Ljx\n" +
                "    lIXCeAQYFggAKgWCZVvV/wmQhxc5oMWqDx4CmwwWIQTkJ2KfmpCdRNrB16OH\n" +
                "    FzmgxaoPHgAAo+kA/Agh94fIbvVVZxIZSWzAnmn1SZQB+RaV+bfQzVlw0QmA\n" +
                "    AP9cekz8onDDYojNUCecYkVdJR3EGPZgJ6mMdNv7bvcfBQ==\n" +
                "    =bMIk\n" +
                "    -----END PGP PRIVATE KEY BLOCK-----"
        val token = "BFK7MX9J3bxiOQzz4tWylTM9BqL6HRVIFIMp"
        val encryptedMessage1 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdA+z5qZG4UQ4XZW80hjHdGwbNSrL7zO45csZpw5JuO\\4jUwEpBa4zr4w8FLaQ+srHqC9Bmyac3ZUN/EI4b3CgWbmIHumNNzwgn1nAth\\AIsJLEdTwV4DcDBaMBvbOlISAQdA+rmSu7t9XEhW0677kn+IsPaY2jADrDKl\\FFQ5HJeVpjIwCC+ry0SDlhSQphIHyFKyo2oV4YSVVsP5N2MkWBaYptYLRuxR\\BI6QjHHIR4Wc4C970rwBC0bPUi+Eyjvpc622b8ndg2G6STQwwfUlZjuASZ0z\\UdTKD9qA2eT7cvFVXXGe6hsEUbP+ZUtGgY9xUAiXXjavL92M30yg//SJKq2p\\MvCrtEnAZxYRsZjZzS4lTNSTMQf1IANuQXL4Zx5rc072OBgLuKbvC2AfzY8f\\qH57cNb2l7OGmjbSzfe6w1freIVKh2g/kVs5tXzYMm0mm5TkBAWa5x5qFgSP\\aIJgvbQWBR7odUMDAsv7Px7p8H7IXw==\\=lFWF\\-----END PGP MESSAGE-----\\"
        val message1 = "Hello"
        val encryptedMessage2 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdAZO7KAUrxMaUwULL+6EXVhOoYrQZON+Zyhq4Aw52m\\GiIwnrTQJT8rhO3x8TDHnFpvEA9Cas3Fm8p7v41Lj+Wu7ezndABXvJNuYF/m\\oz/o0eEXwV4DcDBaMBvbOlISAQdAEz9Ot7zLfWzVvCcJEsnV70PsHMkPIy9h\\fMiTvgNn1wUwd/t2IFFtKSPLtFKVnRyvBIrj/vR+0xDE1cgj0KDJ9/AJ3i9o\\0U0jbM2VNMqQ+qJM0sAJAaR5/hEEl6JrhRQzm/AYwdNJV00AOAJG57wLial+\\t1k4m34pFvkVdDycKSRWv92ob07EvzuAwbEHnXMqnBcTSTX3hWe1juj6Kwuo\\SMArMQhOlU2ao0DgN9HJd4hYI6DpkURaRZyxp+c7H+uBUHN7XYGiUuuKmfFs\\4M0A/qYC5PxAuDnq4/xe8mipl+Id9a5pYTO4BJLIYDOK1CcQ0hPS9mnL+/u7\\5cyopyr5tc988c8BPpmg3TfOs+2dLDHamHjWM3+77J4kNYZQ\\=CQvx\\-----END PGP MESSAGE-----\\"
        val message2 = "How are you doing?"

        val (signatureKey, decryptionKey) = decryptPrivateKeys(encryptedPrivateKey, token)
        val encryptedData = getEncryptedData(encryptedMessage1, decryptionKey)
        val pgpObjectsList = extractPgpObjectsList(encryptedData, decryptionKey)

        val publicKey = readPublicKey(publicKeyArmor)!!
        val generatedEncryptedData = createPGPEncryptedData("Hello", signatureKey, publicKey)
        val generatedPgpObjectsList = extractPgpObjectsList(generatedEncryptedData, decryptionKey)
        assertEquals(3, pgpObjectsList.size)
        assertEquals(pgpObjectsList.size, generatedPgpObjectsList.size)

        // Verify generatedOnePassSignature
        val onePassSignature = pgpObjectsList[0] as PGPOnePassSignatureList
        val generatedOnePassSignature = generatedPgpObjectsList[0] as PGPOnePassSignatureList
        assertEquals(onePassSignature.size(), generatedOnePassSignature.size())
        assertArrayEquals(onePassSignature[0].encoded, generatedOnePassSignature[0].encoded)

        // Verify generatedLiteralData
        val literalData = pgpObjectsList[1] as PGPLiteralData
        val generatedLiteralData = generatedPgpObjectsList[1] as PGPLiteralData
        val data1 = readPGPLiteralData(literalData)
        val data2 = readPGPLiteralData(generatedLiteralData)
        assertArrayEquals(data1, data2)

        // Verify generatedSignature
        val signature = pgpObjectsList[2] as PGPSignatureList
        val generatedSignature = generatedPgpObjectsList[2] as PGPSignatureList
        assertNotNull(generatedSignature)
    }

//    fun testCreatingPGPPublicKeyEncryptedData() {
//        val publicKeyArmor = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
//                "\n" +
//                "        mDMEZVvV/xYJKwYBBAHaRw8BAQdAly0Ja3zv2dp4yGJSVHKSg4EdaMLPFRbse3YH\n" +
//                "        ulYfRty0TFJvYm9TYXRzIElEIDU5Y2Q2NDYxN2NmZGM4OTljNTFjM2QzMjFhYjM5\n" +
//                "        ODE2NTBhZjI0MThiNTNjMGY3YWVlYjc1MjRiZmJiMTI3M2KIjAQQFgoAPgWCZVvV\n" +
//                "        /wQLCQcICZCHFzmgxaoPHgMVCAoEFgACAQIZAQKbAwIeARYhBOQnYp+akJ1E2sHX\n" +
//                "        o4cXOaDFqg8eAADd/QD/eeMYe8LWJ/t5LO+QqUhT6OwkZogLjJiIm0V7IdPrtyYB\n" +
//                "        AJ6ptEcGZycSqtwzgWoGsMnlkTqpg00sd+xq5xPwiysFuDgEZVvV/xIKKwYBBAGX\n" +
//                "        VQEFAQEHQPu5ytyZl+pQP74jJ1k/ze48xflvE+RkRjrAFA/xI4QBAwEIB4h4BBgW\n" +
//                "        CAAqBYJlW9X/CZCHFzmgxaoPHgKbDBYhBOQnYp+akJ1E2sHXo4cXOaDFqg8eAACj\n" +
//                "        6QD8CCH3h8hu9VVnEhlJbMCeafVJlAH5FpX5t9DNWXDRCYAA/1x6TPyicMNiiM1Q\n" +
//                "        J5xiRV0lHcQY9mAnqYx02/tu9x8F\n" +
//                "        =8/1E\n" +
//                "        -----END PGP PUBLIC KEY BLOCK-----"
//        val encryptedPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
//                "\n" +
//                "    xYYEZVvV/xYJKwYBBAHaRw8BAQdAly0Ja3zv2dp4yGJSVHKSg4EdaMLPFRbs\n" +
//                "    e3YHulYfRtz+CQMI5AxhtOiljJ3gpdDAKPvXeDwlbfI117Bt8mghzcShibTE\n" +
//                "    gD5tsX1lSEb+6FatZv1bMqHOgWCk8AN9yEg2KDvhBpMJkSjMlwU6dBOnIPpK\n" +
//                "    481MUm9ib1NhdHMgSUQgNTljZDY0NjE3Y2ZkYzg5OWM1MWMzZDMyMWFiMzk4\n" +
//                "    MTY1MGFmMjQxOGI1M2MwZjdhZWViNzUyNGJmYmIxMjczYsKMBBAWCgA+BYJl\n" +
//                "    W9X/BAsJBwgJkIcXOaDFqg8eAxUICgQWAAIBAhkBApsDAh4BFiEE5Cdin5qQ\n" +
//                "    nUTawdejhxc5oMWqDx4AAN39AP954xh7wtYn+3ks75CpSFPo7CRmiAuMmIib\n" +
//                "    RXsh0+u3JgEAnqm0RwZnJxKq3DOBagawyeWROqmDTSx37GrnE/CLKwXHiwRl\n" +
//                "    W9X/EgorBgEEAZdVAQUBAQdA+7nK3JmX6lA/viMnWT/N7jzF+W8T5GRGOsAU\n" +
//                "    D/EjhAEDAQgH/gkDCLLQ+gcXEoCo4Ngj8HHzMRt5ZA486nnV7pxX+t0FkACt\n" +
//                "    PX8kccVTQWDG5IVrv9gWXmRM3JV2dGfGvMIIcyEdGz4Tn7D3Xbkcr1jz8Ljx\n" +
//                "    lIXCeAQYFggAKgWCZVvV/wmQhxc5oMWqDx4CmwwWIQTkJ2KfmpCdRNrB16OH\n" +
//                "    FzmgxaoPHgAAo+kA/Agh94fIbvVVZxIZSWzAnmn1SZQB+RaV+bfQzVlw0QmA\n" +
//                "    AP9cekz8onDDYojNUCecYkVdJR3EGPZgJ6mMdNv7bvcfBQ==\n" +
//                "    =bMIk\n" +
//                "    -----END PGP PRIVATE KEY BLOCK-----"
//        val token = "BFK7MX9J3bxiOQzz4tWylTM9BqL6HRVIFIMp"
//        val encryptedMessage1 =
//            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdA+z5qZG4UQ4XZW80hjHdGwbNSrL7zO45csZpw5JuO\\4jUwEpBa4zr4w8FLaQ+srHqC9Bmyac3ZUN/EI4b3CgWbmIHumNNzwgn1nAth\\AIsJLEdTwV4DcDBaMBvbOlISAQdA+rmSu7t9XEhW0677kn+IsPaY2jADrDKl\\FFQ5HJeVpjIwCC+ry0SDlhSQphIHyFKyo2oV4YSVVsP5N2MkWBaYptYLRuxR\\BI6QjHHIR4Wc4C970rwBC0bPUi+Eyjvpc622b8ndg2G6STQwwfUlZjuASZ0z\\UdTKD9qA2eT7cvFVXXGe6hsEUbP+ZUtGgY9xUAiXXjavL92M30yg//SJKq2p\\MvCrtEnAZxYRsZjZzS4lTNSTMQf1IANuQXL4Zx5rc072OBgLuKbvC2AfzY8f\\qH57cNb2l7OGmjbSzfe6w1freIVKh2g/kVs5tXzYMm0mm5TkBAWa5x5qFgSP\\aIJgvbQWBR7odUMDAsv7Px7p8H7IXw==\\=lFWF\\-----END PGP MESSAGE-----\\"
//        val message1 = "Hello"
//        val encryptedMessage2 =
//            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdAZO7KAUrxMaUwULL+6EXVhOoYrQZON+Zyhq4Aw52m\\GiIwnrTQJT8rhO3x8TDHnFpvEA9Cas3Fm8p7v41Lj+Wu7ezndABXvJNuYF/m\\oz/o0eEXwV4DcDBaMBvbOlISAQdAEz9Ot7zLfWzVvCcJEsnV70PsHMkPIy9h\\fMiTvgNn1wUwd/t2IFFtKSPLtFKVnRyvBIrj/vR+0xDE1cgj0KDJ9/AJ3i9o\\0U0jbM2VNMqQ+qJM0sAJAaR5/hEEl6JrhRQzm/AYwdNJV00AOAJG57wLial+\\t1k4m34pFvkVdDycKSRWv92ob07EvzuAwbEHnXMqnBcTSTX3hWe1juj6Kwuo\\SMArMQhOlU2ao0DgN9HJd4hYI6DpkURaRZyxp+c7H+uBUHN7XYGiUuuKmfFs\\4M0A/qYC5PxAuDnq4/xe8mipl+Id9a5pYTO4BJLIYDOK1CcQ0hPS9mnL+/u7\\5cyopyr5tc988c8BPpmg3TfOs+2dLDHamHjWM3+77J4kNYZQ\\=CQvx\\-----END PGP MESSAGE-----\\"
//        val message2 = "How are you doing?"
//
//        PGPPublicKeyEncryptedData
//        val (signatureKey, decryptionKey) = decryptPrivateKeys(encryptedPrivateKey, token)
////        val encryptedData = getEncryptedData(encryptedMessage1, decryptionKey)
//
//        val publicKey = readPublicKey(publicKeyArmor)!!
//        val encryptedData = createPGPEncryptedData("Hello", signatureKey, publicKey)
//        val pgpObjectsList = extractPgpObjectsList(encryptedData, decryptionKey)
//
//    }

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
