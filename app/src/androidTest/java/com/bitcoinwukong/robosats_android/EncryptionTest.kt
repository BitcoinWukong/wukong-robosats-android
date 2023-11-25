package com.bitcoinwukong.robosats_android

import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import org.junit.Assert
import org.junit.Test

class EncryptionTest {
    companion object {
        const val token = "BFK7MX9J3bxiOQzz4tWylTM9BqL6HRVIFIMp"
        const val encryptedPrivateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
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
        const val keyId = 6130617834869337044
        const val encryptedMessage_1 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdA+z5qZG4UQ4XZW80hjHdGwbNSrL7zO45csZpw5JuO\\4jUwEpBa4zr4w8FLaQ+srHqC9Bmyac3ZUN/EI4b3CgWbmIHumNNzwgn1nAth\\AIsJLEdTwV4DcDBaMBvbOlISAQdA+rmSu7t9XEhW0677kn+IsPaY2jADrDKl\\FFQ5HJeVpjIwCC+ry0SDlhSQphIHyFKyo2oV4YSVVsP5N2MkWBaYptYLRuxR\\BI6QjHHIR4Wc4C970rwBC0bPUi+Eyjvpc622b8ndg2G6STQwwfUlZjuASZ0z\\UdTKD9qA2eT7cvFVXXGe6hsEUbP+ZUtGgY9xUAiXXjavL92M30yg//SJKq2p\\MvCrtEnAZxYRsZjZzS4lTNSTMQf1IANuQXL4Zx5rc072OBgLuKbvC2AfzY8f\\qH57cNb2l7OGmjbSzfe6w1freIVKh2g/kVs5tXzYMm0mm5TkBAWa5x5qFgSP\\aIJgvbQWBR7odUMDAsv7Px7p8H7IXw==\\=lFWF\\-----END PGP MESSAGE-----\\"
        const val encryptedMessage_2 =
            "-----BEGIN PGP MESSAGE-----\\\\wV4DVRRUcH0Pq9QSAQdAZO7KAUrxMaUwULL+6EXVhOoYrQZON+Zyhq4Aw52m\\GiIwnrTQJT8rhO3x8TDHnFpvEA9Cas3Fm8p7v41Lj+Wu7ezndABXvJNuYF/m\\oz/o0eEXwV4DcDBaMBvbOlISAQdAEz9Ot7zLfWzVvCcJEsnV70PsHMkPIy9h\\fMiTvgNn1wUwd/t2IFFtKSPLtFKVnRyvBIrj/vR+0xDE1cgj0KDJ9/AJ3i9o\\0U0jbM2VNMqQ+qJM0sAJAaR5/hEEl6JrhRQzm/AYwdNJV00AOAJG57wLial+\\t1k4m34pFvkVdDycKSRWv92ob07EvzuAwbEHnXMqnBcTSTX3hWe1juj6Kwuo\\SMArMQhOlU2ao0DgN9HJd4hYI6DpkURaRZyxp+c7H+uBUHN7XYGiUuuKmfFs\\4M0A/qYC5PxAuDnq4/xe8mipl+Id9a5pYTO4BJLIYDOK1CcQ0hPS9mnL+/u7\\5cyopyr5tc988c8BPpmg3TfOs+2dLDHamHjWM3+77J4kNYZQ\\=CQvx\\-----END PGP MESSAGE-----\\"
    }

    @Test
    fun testDecryptPrivateKey() {
        val pgpPrivateKey = PgpKeyGenerator.decryptPrivateKey(
            encryptedPrivateKey,
            token
        )
        Assert.assertEquals(keyId, pgpPrivateKey.keyID)
    }

    @Test
    fun testDecryptMessage() {
        Assert.assertEquals(
            "Hello",
            PgpKeyGenerator.decryptMessage(encryptedMessage_1, encryptedPrivateKey, token)
        )
        Assert.assertEquals(
            "How are you doing?",
            PgpKeyGenerator.decryptMessage(encryptedMessage_2, encryptedPrivateKey, token)
        )
    }
}
