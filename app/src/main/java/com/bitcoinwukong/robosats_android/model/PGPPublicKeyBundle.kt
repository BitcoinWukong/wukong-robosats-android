package com.bitcoinwukong.robosats_android.model

import org.bouncycastle.openpgp.PGPPublicKey

data class PGPPublicKeyBundle(
    val signingKey: PGPPublicKey,
    val encryptionKey: PGPPublicKey
)
