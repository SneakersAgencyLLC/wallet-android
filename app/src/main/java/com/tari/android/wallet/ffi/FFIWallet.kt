/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ffi

import com.orhanobut.logger.Logger
import com.tari.android.wallet.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */
internal typealias FFIWalletPtr = Long

internal class FFIWallet(
    commsConfig: FFICommsConfig,
    logPath: String
) : FFIBase() {

    companion object {
        private var atomicInstance = AtomicReference<FFIWallet>()
        var instance: FFIWallet?
            get() = atomicInstance.get()
            set(value) = atomicInstance.set(value)
    }

    // region JNI

    private external fun jniCreate(
        commsConfig: FFICommsConfig,
        logPath: String,
        callbackReceivedTx: String,
        callbackReceivedTxSig: String,
        callbackReceivedTxReply: String,
        callbackReceivedTxReplySig: String,
        callbackReceivedFinalizedTx: String,
        callbackReceivedFinalizedTxSig: String,
        callbackTxBroadcast: String,
        callbackTxBroadcastSig: String,
        callbackTxMined: String,
        callbackTxMinedSig: String,
        callbackDirectSendResult: String,
        callbackDirectSendResultSig: String,
        callbackStoreAndForwardSendResult: String,
        callbackStoreAndForwardSendResultSig: String,
        callbackTxCancellation: String,
        callbackTxCancellationSig: String,
        callbackBaseNodeSync: String,
        callbackBaseNodeSyncSig: String,
        libError: FFIError
    )

    private external fun jniLogMessage(
        message: String
    )

    private external fun jniGetPublicKey(
        libError: FFIError
    ): FFIPublicKeyPtr

    private external fun jniGetAvailableBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingIncomingBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingOutgoingBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetContacts(libError: FFIError): FFIContactsPtr

    private external fun jniAddUpdateContact(
        contactPtr: FFIContact,
        libError: FFIError
    ): Boolean

    private external fun jniRemoveContact(
        contactPtr: FFIContact,
        libError: FFIError
    ): Boolean

    private external fun jniGetCompletedTxs(
        libError: FFIError
    ): FFICompletedTxsPtr

    private external fun jniGetCancelledTxs(
        libError: FFIError
    ): FFICompletedTxsPtr

    private external fun jniGetCompletedTxById(
        id: String,
        libError: FFIError
    ): FFICompletedTxPtr

    private external fun jniGetCancelledTxById(
        id: String,
        libError: FFIError
    ): FFICompletedTxPtr

    private external fun jniGetPendingOutboundTxs(
        libError: FFIError
    ): FFIPendingOutboundTxsPtr

    private external fun jniGetPendingOutboundTxById(
        id: String,
        libError: FFIError
    ): FFIPendingOutboundTxPtr

    private external fun jniGetPendingInboundTxs(
        libError: FFIError
    ): FFIPendingInboundTxsPtr

    private external fun jniGetPendingInboundTxById(
        id: String,
        libError: FFIError
    ): FFIPendingInboundTxPtr

    private external fun jniCancelPendingTx(
        id: String,
        libError: FFIError
    ): Boolean

    private external fun jniIsCompletedTxOutbound(
        completedTx: FFICompletedTx,
        libError: FFIError
    ): Boolean

    private external fun jniSendTx(
        publicKeyPtr: FFIPublicKey,
        amount: String,
        fee: String,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniCoinSplit(
        amount: String,
        splitCount: String,
        fee: String,
        message: String,
        lockHeight: String,
        libError: FFIError
    ): ByteArray

    private external fun jniSignMessage(
        message: String,
        libError: FFIError
    ): String

    private external fun jniVerifyMessageSignature(
        publicKeyPtr: FFIPublicKey,
        message: String,
        signature: String,
        libError: FFIError
    ): Boolean

    private external fun jniImportUTXO(
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        amount: String,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniAddBaseNodePeer(
        publicKey: FFIPublicKey,
        address: String,
        libError: FFIError
    ): Boolean

    private external fun jniSyncWithBaseNode(
        libError: FFIError
    ): ByteArray

    private external fun jniGetTorIdentity(
        libError: FFIError
    ): FFIByteVectorPtr

    private external fun jniDestroy()

    // endregion

    var ptr = nullptr
    var listenerAdapter: FFIWalletListenerAdapter? = null

    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    init {
        if (ptr == nullptr) { // so it can only be assigned once for the singleton
            val error = FFIError()
            Logger.i("Pre jniCreate.")
            jniCreate(
                commsConfig, logPath,
                this::onTxReceived.name, "(J)V",
                this::onTxReplyReceived.name, "(J)V",
                this::onTxFinalized.name, "(J)V",
                this::onTxBroadcast.name, "(J)V",
                this::onTxMined.name, "(J)V",
                this::onDirectSendResult.name, "([BZ)V",
                this::onStoreAndForwardSendResult.name, "([BZ)V",
                this::onTxCancelled.name, "(J)V",
                this::onBaseNodeSyncComplete.name, "([BZ)V",
                error
            )
            Logger.i("Post jniCreate with code: %d.", error.code)
            throwIf(error)
        }
    }

    fun getPointer(): FFIWalletPtr {
        return ptr
    }

    fun getAvailableBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetAvailableBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPendingIncomingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingIncomingBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPendingOutgoingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingOutgoingBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPublicKey(): FFIPublicKey {
        val error = FFIError()
        val result = FFIPublicKey(jniGetPublicKey(error))
        throwIf(error)
        return result
    }

    fun getContacts(): FFIContacts {
        val error = FFIError()
        val result = FFIContacts(jniGetContacts(error))
        throwIf(error)
        return result
    }

    fun addUpdateContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniAddUpdateContact(contact, error)
        throwIf(error)
        return result
    }

    fun removeContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniRemoveContact(contact, error)
        throwIf(error)
        return result
    }

    fun isCompletedTxOutbound(completedTx: FFICompletedTx): Boolean {
        val error = FFIError()
        val result = jniIsCompletedTxOutbound(completedTx, error)
        throwIf(error)
        return result
    }

    fun getCompletedTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCompletedTxs(error))
        throwIf(error)
        return result
    }

    fun getCancelledTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCancelledTxs(error))
        throwIf(error)
        return result
    }

    fun getCompletedTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCompletedTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getCancelledTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCancelledTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxs(): FFIPendingOutboundTxs {
        val error = FFIError()
        val result = FFIPendingOutboundTxs(jniGetPendingOutboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxById(id: BigInteger): FFIPendingOutboundTx {
        val error = FFIError()
        val result =
            FFIPendingOutboundTx(jniGetPendingOutboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxs(): FFIPendingInboundTxs {
        val error = FFIError()
        val result = FFIPendingInboundTxs(jniGetPendingInboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxById(id: BigInteger): FFIPendingInboundTx {
        val error = FFIError()
        val result =
            FFIPendingInboundTx(jniGetPendingInboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun cancelPendingTx(id: BigInteger): Boolean {
        val error = FFIError()
        val result = jniCancelPendingTx(id.toString(), error)
        throwIf(error)
        return result
    }

    fun onTxBroadcast(completedTxPtr: FFICompletedTxPtr) {
        Logger.i("Tx completed. Pointer: %s", completedTxPtr.toString())
        val walletKey = getPublicKey()
        val walletHex = walletKey.toString()
        walletKey.destroy()
        val tx = FFICompletedTx(completedTxPtr)
        val (direction, user) = defineParticipantAndDirection(tx, walletHex)
        val status = mapStatus(tx)
        val completed = CompletedTx(
            tx.getId(),
            direction,
            user,
            MicroTari(tx.getAmount()),
            MicroTari(tx.getFee()),
            tx.getTimestamp(),
            tx.getMessage(),
            status
        )
        tx.destroy()
        if (status != TxStatus.MINED) {
            Logger.e("Constructed CompletedTx has status that's not MINED: $completed")
        }
        GlobalScope.launch { listenerAdapter?.onTxBroadcast(completed) }
    }

    fun onTxMined(completedTxPtr: FFICompletedTxPtr) {
        Logger.i("Tx mined. Pointer: %s", completedTxPtr.toString())
        val walletKey = getPublicKey()
        val walletHex = walletKey.toString()
        walletKey.destroy()
        val tx = FFICompletedTx(completedTxPtr)
        val (direction, user) = defineParticipantAndDirection(tx, walletHex)
        val completed = CompletedTx(
            tx.getId(),
            direction,
            user,
            MicroTari(tx.getAmount()),
            MicroTari(tx.getFee()),
            tx.getTimestamp(),
            tx.getMessage(),
            mapStatus(tx)
        )
        tx.destroy()
        if (completed.status != TxStatus.MINED) {
            Logger.e("Constructed CompletedTx has status that's not MINED: $completed")
        }
        GlobalScope.launch { listenerAdapter?.onTxMined(completed) }
    }

    fun onTxReceived(pendingInboundTxPtr: FFIPendingInboundTxPtr) {
        Logger.i("Tx received. Pointer: %s", pendingInboundTxPtr.toString())
        val tx = FFIPendingInboundTx(pendingInboundTxPtr)
        val id = tx.getId()
        val source = tx.getSourcePublicKey()
        val sourceHex = source.toString()
        val sourceEmoji = source.getEmojiNodeId()
        source.destroy()
        val amount = tx.getAmount()
        val timestamp = tx.getTimestamp()
        val message = tx.getMessage()
        val status = when (tx.getStatus()) {
            FFITxStatus.BROADCAST -> TxStatus.BROADCAST
            FFITxStatus.COMPLETED -> TxStatus.COMPLETED
            FFITxStatus.IMPORTED -> TxStatus.IMPORTED
            FFITxStatus.MINED -> TxStatus.MINED
            FFITxStatus.PENDING -> TxStatus.PENDING
            FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
            else -> TxStatus.UNKNOWN
        }
        tx.destroy()

        val pk = PublicKey(sourceHex, sourceEmoji)
        val user = User(pk)
        val pendingTx = PendingInboundTx(id, user, MicroTari(amount), timestamp, message, status)
        GlobalScope.launch { listenerAdapter?.onTxReceived(pendingTx) }
    }

    fun onTxReplyReceived(completedTxPtr: FFICompletedTxPtr) {
        Logger.i("Tx reply received. Pointer: %s", completedTxPtr.toString())
        val walletKey = getPublicKey()
        val walletHex = walletKey.toString()
        walletKey.destroy()
        val tx = FFICompletedTx(completedTxPtr)
        val (direction, user) = defineParticipantAndDirection(tx, walletHex)
        val completedTx = CompletedTx(
            tx.getId(),
            direction,
            user,
            MicroTari(tx.getAmount()),
            MicroTari(tx.getFee()),
            tx.getTimestamp(),
            tx.getMessage(),
            mapStatus(tx)
        )
        tx.destroy()

        if (completedTx.status != TxStatus.MINED) {
            Logger.e("Constructed CompletedTx has status that's not MINED: $completedTx")
        }
        GlobalScope.launch { listenerAdapter?.onTxReplyReceived(completedTx) }
    }

    fun onTxFinalized(completedTx: FFICompletedTxPtr) {
        Logger.i("Tx finalized. Pointer: %s", completedTx.toString())
        val walletKey = getPublicKey()
        val walletHex = walletKey.toString()
        walletKey.destroy()
        val tx = FFICompletedTx(completedTx)
        val id = tx.getId()
        val amount = tx.getAmount()
        val fee = tx.getFee()
        val timestamp = tx.getTimestamp()
        val message = tx.getMessage()
        val status = mapStatus(tx)
        val (direction, user) = defineParticipantAndDirection(tx, walletHex)
        tx.destroy()
        val completed = CompletedTx(
            id,
            direction,
            user,
            MicroTari(amount),
            MicroTari(fee),
            timestamp,
            message,
            status
        )
        if (status != TxStatus.MINED) {
            Logger.e("Constructed CompletedTx has status that's not MINED: $completed")
        }
        GlobalScope.launch { listenerAdapter?.onTxFinalized(completed) }
    }

    fun onDirectSendResult(bytes: ByteArray, success: Boolean) {
        Logger.i("Direct send result received. Success: $success")
        val txId = BigInteger(1, bytes)
        GlobalScope.launch { listenerAdapter?.onDirectSendResult(txId, success) }
    }

    fun onStoreAndForwardSendResult(bytes: ByteArray, success: Boolean) {
        Logger.i("Store and forward send result received. Success: $success")
        val txId = BigInteger(1, bytes)
        GlobalScope.launch { listenerAdapter?.onStoreAndForwardSendResult(txId, success) }
    }

    fun onTxCancelled(completedTx: FFICompletedTxPtr) {
        Logger.i("Tx cancelled. Pointer: %s", completedTx.toString())
        val walletKey = getPublicKey()
        val walletHex = walletKey.toString()
        walletKey.destroy()
        val tx = FFICompletedTx(completedTx)
        val (direction, user) = defineParticipantAndDirection(tx, walletHex)
        val cancelled = CancelledTx(
            tx.getId(),
            direction,
            user,
            MicroTari(tx.getAmount()),
            MicroTari(tx.getFee()),
            tx.getTimestamp(),
            tx.getMessage(),
            mapStatus(tx)
        )
        tx.destroy()
        GlobalScope.launch { listenerAdapter?.onTxCancelled(cancelled) }
    }

    private fun defineParticipantAndDirection(
        tx: FFICompletedTx,
        walletHex: String
    ): Pair<Tx.Direction, User> {
        val source = tx.getSourcePublicKey()
        val sourceHex = source.toString()
        val sourceEmoji = source.getEmojiNodeId()
        source.destroy()
        val destination = tx.getDestinationPublicKey()
        val destinationHex = destination.toString()
        val destinationEmoji = destination.getEmojiNodeId()
        destination.destroy()
        val direction = if (destinationHex == walletHex) Tx.Direction.INBOUND
        else Tx.Direction.OUTBOUND
        val user = User(
            if (walletHex == sourceHex) PublicKey(destinationHex, destinationEmoji)
            else PublicKey(sourceHex, sourceEmoji)
        )
        return Pair(direction, user)
    }

    private fun mapStatus(tx: FFICompletedTx): TxStatus = when (tx.getStatus()) {
        FFITxStatus.BROADCAST -> TxStatus.BROADCAST
        FFITxStatus.COMPLETED -> TxStatus.COMPLETED
        FFITxStatus.IMPORTED -> TxStatus.IMPORTED
        FFITxStatus.MINED -> TxStatus.MINED
        FFITxStatus.PENDING -> TxStatus.PENDING
        FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
        else -> TxStatus.UNKNOWN
    }

    fun onBaseNodeSyncComplete(bytes: ByteArray, success: Boolean) {
        Logger.i("Base node sync complete. Success: $success")
        val requestId = BigInteger(1, bytes)
        GlobalScope.launch { listenerAdapter?.onBaseNodeSyncComplete(requestId, success) }
    }

    fun sendTx(
        destination: FFIPublicKey,
        amount: BigInteger,
        fee: BigInteger,
        message: String
    ): BigInteger {
        val minimumLibFee = 100L
        if (fee < BigInteger.valueOf(minimumLibFee)) {
            throw FFIException(message = "Fee is less than the minimum of $minimumLibFee taris.")
        }
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getPublicKey()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val error = FFIError()
        val bytes = jniSendTx(
            destination,
            amount.toString(),
            fee.toString(),
            message,
            error
        )
        Logger.d("Send status code (0 means ok): %d", error.code)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun coinSplit(
        amount: BigInteger,
        count: BigInteger,
        height: BigInteger,
        fee: BigInteger,
        message: String
    ): BigInteger {
        val minimumLibFee = 100L
        if (fee < BigInteger.valueOf(minimumLibFee)) {
            throw FFIException(message = "Fee is less than the minimum of $minimumLibFee taris.")
        }
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }

        val error = FFIError()
        val bytes = jniCoinSplit(
            amount.toString(),
            count.toString(),
            fee.toString(),
            message,
            height.toString(),
            error
        )
        Logger.d("Coin spit code (0 means ok): %d", error.code)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun signMessage(message: String): String {
        val error = FFIError()
        val result = jniSignMessage(message, error)
        throwIf(error)
        return result
    }

    fun verifyMessageSignature(
        contactPublicKey: FFIPublicKey,
        message: String,
        signature: String
    ): Boolean {
        val error = FFIError()
        val result =
            jniVerifyMessageSignature(contactPublicKey, message, signature, error)
        throwIf(error)
        return result
    }

    fun importUTXO(
        amount: BigInteger,
        message: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey
    ): BigInteger {
        val error = FFIError()
        val bytes = jniImportUTXO(
            spendingKey,
            sourcePublicKey,
            amount.toString(),
            message,
            error
        )
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun syncWithBaseNode(): BigInteger {
        val error = FFIError()
        val bytes = jniSyncWithBaseNode(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getTorIdentity(): ByteArray {
        val error = FFIError()
        val resultPtr = jniGetTorIdentity(error)
        throwIf(error)
        return FFIByteVector(resultPtr).getBytes()
    }

    fun addBaseNodePeer(
        baseNodePublicKey: FFIPublicKey,
        baseNodeAddress: String
    ): Boolean {
        val error = FFIError()
        val result = jniAddBaseNodePeer(baseNodePublicKey, baseNodeAddress, error)
        throwIf(error)
        return result
    }

    override fun destroy() {
        jniDestroy()
    }

    // region JNI

    private external fun jniGenerateTestData(
        datastorePath: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestBroadcastTx(
        txPtr: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestFinalizeReceivedTx(
        txPtr: FFIPendingInboundTx,
        libError: FFIError
    ): Boolean

    private external fun jniTestCompleteSentTx(
        txPtr: FFIPendingOutboundTx,
        libError: FFIError
    ): Boolean

    private external fun jniTestMineTx(
        txId: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestReceiveTx(libError: FFIError): Boolean

    // endregion

    fun generateTestData(datastorePath: String): Boolean {
        val error = FFIError()
        val result = jniGenerateTestData(datastorePath, error)
        throwIf(error)
        return result
    }

    fun testBroadcastTx(tx: BigInteger): Boolean {
        val error = FFIError()
        val result = jniTestBroadcastTx(tx.toString(), error)
        throwIf(error)
        return result
    }

    fun testCompleteSentTx(tx: FFIPendingOutboundTx): Boolean {
        val error = FFIError()
        val result = jniTestCompleteSentTx(tx, error)
        throwIf(error)
        return result
    }

    fun testMineTx(tx: BigInteger): Boolean {
        val error = FFIError()
        val result = jniTestMineTx(tx.toString(), error)
        throwIf(error)
        return result
    }

    fun testFinalizeReceivedTx(tx: FFIPendingInboundTx): Boolean {
        val error = FFIError()
        val result = jniTestFinalizeReceivedTx(tx, error)
        throwIf(error)
        return result
    }

    fun testReceiveTx(): Boolean {
        val error = FFIError()
        val result = jniTestReceiveTx(error)
        throwIf(error)
        return result
    }
}
