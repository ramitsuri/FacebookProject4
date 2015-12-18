package com.ramitsuri.project4

import java.security._
import java.security.spec.X509EncodedKeySpec
import java.util
import javax.crypto.{ Cipher}
import javax.crypto.spec.SecretKeySpec
import akka.actor.{Props, ActorSystem, Actor}
import akka.util.Timeout
import com.ramitsuri.project4.Encryption.{AES, DS}
import org.apache.commons.codec.binary.Base64


object Encryption {

  object RSA {

    def getKeyPair() : KeyPair = {
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(2048)
      val keyPair = keyPairGenerator.generateKeyPair()

      keyPair
    }

    def encrypt(dataToEncrypt: String, publicKey: PublicKey) : String = {

      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, publicKey)
      val encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes());
      val encryptedtext = new String(Base64.encodeBase64(encryptedBytes));
      encryptedtext
    }

    def decrypt(dataToDecrypt: String, privateKey: PrivateKey): String = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, privateKey)
      val encryptedtextBytes = Base64.decodeBase64(dataToDecrypt.getBytes());
      val decryptedBytes = cipher.doFinal(encryptedtextBytes);
      val decryptedString = new String(decryptedBytes);
      decryptedString
    }
  }

  object AES{

    private val SALT: String = "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k"

    def encrypt(key: String, value: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
      Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
    }

    def decrypt(key: String, encryptedValue: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
      cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key))
      new String(cipher.doFinal(Base64.decodeBase64(encryptedValue)))
    }

    def keyToSpec(key: String): SecretKeySpec = {
      var keyBytes: Array[Byte] = (SALT + key).getBytes("UTF-8")
      val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
      keyBytes = sha.digest(keyBytes)
      keyBytes = util.Arrays.copyOf(keyBytes, 16)
      new SecretKeySpec(keyBytes, "AES")
    }

  }

  object DS{
    def sign(data: String, privateKey: PrivateKey) = {
      val bytes = data.getBytes
       val sig  = Signature.getInstance("MD5WithRSA")
      sig.initSign(privateKey)
      sig.update(bytes)
      val signatureBytes = sig.sign()
      signatureBytes
      /*println("Singature:" + Base64.encodeBase64(signatureBytes))


      println(sig.verify(signatureBytes))*/
      }

    def verify(data: String, signatureBytes: Array[Byte], publicKey: PublicKey) = {
      val sig  = Signature.getInstance("MD5WithRSA")
      sig.initVerify(publicKey)
      sig.update(data.getBytes)
      sig.verify(signatureBytes)
    }
  }


}
object maindshu extends App{
  /*val snjd = "hellkfdsdblsjdjsckjdkjckdvkfvkfkvnkjdfkvfdkfdnjsvnjfvkjfd kjvd jkv jdf kjvdfnjnvldfnlksdbkjdcjdow"
  val kp:KeyPair = Encryption.RSA.getKeyPair()

  val btarr: Array[Byte] = kp.getPublic.getEncoded
  val pubk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(btarr))
  println(Encryption.RSA.decrypt(Encryption.RSA.encrypt(snjd, pubk), kp.getPrivate))*/
 /* println(kp.getPublic())
  val sa = kp.getPublic()

  println(sa.hashCode())

  println(Encryption.RSA.encrypt(snjd, kp.getPublic()))
println(Encryption.RSA.decrypt(Encryption.RSA.encrypt(snjd, kp.getPublic()), kp.getPrivate))*/
  /*val kp:KeyPair = Encryption.RSA.getKeyPair()
  val data = "hellowowsnj"
  println(DS.sign(data, kp.getPrivate))

  println(DS.verify("hellwowsnj", DS.sign(data, kp.getPrivate), kp.getPublic))*/
  val random = new java.security.SecureRandom()
  val key = new Array[Byte](16)
  random.nextBytes(key)
  println(key)
  println(Base64.encodeBase64String(key))
  println(Base64.decodeBase64(Base64.encodeBase64String(key)))
  println(AES.encrypt(Base64.encodeBase64String(key), "hello" ))
  println(AES.decrypt(Base64.encodeBase64String(key),AES.encrypt(Base64.encodeBase64String(key), "hello" )))
}