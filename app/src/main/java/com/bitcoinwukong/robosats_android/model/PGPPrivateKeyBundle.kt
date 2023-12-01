package com.bitcoinwukong.robosats_android.model

import org.bouncycastle.openpgp.PGPPrivateKey

data class PGPPrivateKeyBundle(
    val signingKey: PGPPrivateKey,
    val encryptionKey: PGPPrivateKey
)
