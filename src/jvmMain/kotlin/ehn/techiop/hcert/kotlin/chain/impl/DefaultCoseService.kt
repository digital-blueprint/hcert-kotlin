package ehn.techiop.hcert.kotlin.chain.impl

import COSE.*
import com.upokecenter.cbor.CBORObject
import ehn.techiop.hcert.kotlin.chain.CoseService
import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.VerificationResult

actual open class DefaultCoseService actual constructor(private val cryptoService: CryptoService) : CoseService {

    override fun encode(input: ByteArray): ByteArray {
        return Sign1Message().also {
            it.SetContent(input)
            cryptoService.getCborHeaders().forEach { header ->
                it.addAttribute(
                    CBORObject.FromObject(header.first.value),
                    CBORObject.FromObject(header.second),
                    Attribute.PROTECTED
                )
            }
            it.sign(cryptoService.getCborSigningKey().toCoseRepresentation() as OneKey)
        }.EncodeToBytes()
    }

    override fun decode(input: ByteArray, verificationResult: VerificationResult): ByteArray {
        verificationResult.coseVerified = false
        return try {
            (Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message).also {
                try {
                    val kid = it.findAttribute(HeaderKeys.KID)?.GetByteString() ?: throw IllegalArgumentException("kid")
                    val verificationKey = cryptoService.getCborVerificationKey(kid, verificationResult)
                    verificationResult.coseVerified = it.validate(verificationKey.toCoseRepresentation() as OneKey)
                } catch (e: Throwable) {
                    it.GetContent()
                }
            }.GetContent()
        } catch (e: Throwable) {
            input
        }
    }

}