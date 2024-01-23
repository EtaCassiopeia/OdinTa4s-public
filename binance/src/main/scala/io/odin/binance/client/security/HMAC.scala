package io.odin.binance.client.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HMAC {

  def sha256(sharedSecret: String, preHashString: String): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secret)
    val hashString = mac.doFinal(preHashString.getBytes)
    new String(hashString.map("%02x".format(_)).mkString)
  }
}
