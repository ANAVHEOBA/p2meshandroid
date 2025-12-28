package com.example.p2meshandroid.domain.usecase

import com.example.p2meshandroid.data.repository.WalletInfo
import com.example.p2meshandroid.data.repository.WalletRepository
import com.example.p2meshandroid.data.repository.PaymentInfo
import uniffi.p2pmesh_bridge.signedIouFromBytes

/**
 * Use case for loading wallet from storage
 * Returns null if no wallet exists in storage
 */
class LoadWalletUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<WalletInfo?> {
        return walletRepository.tryLoadFromStorage()
    }

    fun hasStoredWallet(): Boolean {
        return walletRepository.hasStoredWallet()
    }
}

/**
 * Use case for creating a new wallet
 */
class CreateWalletUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<WalletInfo> {
        return walletRepository.createNewWallet()
    }
}

/**
 * Use case for restoring wallet from backup
 */
class RestoreWalletUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(secretKey: ByteArray): Result<WalletInfo> {
        return walletRepository.restoreFromSecretKey(secretKey)
    }
}

/**
 * Use case for getting current wallet info
 */
class GetWalletInfoUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<WalletInfo> {
        return walletRepository.getWalletInfo()
    }
}

/**
 * Use case for sending a payment
 */
class SendPaymentUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(recipientDid: String, amount: ULong): Result<PaymentInfo> {
        // Create the payment
        val paymentResult = walletRepository.createPayment(recipientDid, amount)

        // If successful, mark as sent
        paymentResult.onSuccess { payment ->
            walletRepository.markPaymentSent(payment.iou)
        }

        return paymentResult
    }
}

/**
 * Use case for receiving a payment
 */
class ReceivePaymentUseCase(
    private val walletRepository: WalletRepository
) {
    /**
     * Process payment when sender's public key can be derived from DID
     */
    suspend operator fun invoke(paymentBytes: ByteArray): Result<Unit> {
        return runCatching {
            val iou = signedIouFromBytes(paymentBytes)
            walletRepository.processPayment(iou).getOrThrow()
        }
    }

    /**
     * Process payment with explicit sender public key
     */
    suspend fun withSenderKey(paymentBytes: ByteArray, senderPubKey: ByteArray): Result<Unit> {
        return runCatching {
            val iou = signedIouFromBytes(paymentBytes)
            walletRepository.processPaymentWithKey(iou, senderPubKey).getOrThrow()
        }
    }
}

/**
 * Use case for getting pending payments
 */
class GetPendingPaymentsUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<List<PaymentInfo>> {
        return walletRepository.getPendingPayments()
    }
}

/**
 * Use case for backing up wallet
 */
class BackupWalletUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<WalletBackup> {
        return runCatching {
            val secretKey = walletRepository.getSecretKey().getOrThrow()
            val state = walletRepository.exportState().getOrThrow()
            WalletBackup(secretKey = secretKey, state = state)
        }
    }
}

data class WalletBackup(
    val secretKey: ByteArray,
    val state: ByteArray
) {
    val secretKeyHex: String
        get() = secretKey.joinToString("") { "%02x".format(it) }
}

/**
 * Use case for funding wallet from faucet (demo)
 */
class FundFromFaucetUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(amount: ULong): Result<WalletInfo> {
        return walletRepository.fundFromFaucet(amount)
    }
}
