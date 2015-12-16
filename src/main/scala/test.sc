import scala.util.Random

/*
val string: String = "hello, fekkk"
println(string.split(",")(0))
println(string.split(",")(1))
var str:String = "hello who sjsa"
"%s, %s".format(str, "no")
val randomID = Random.nextInt(100000-1) + 1
val apiLocation = "http://127.0.0.1:8080/project4"
var path = "%s%s%d".format(apiLocation,"/users/getProfile/", randomID)
"%s%s%d%s".format(apiLocation,"/users/", randomID, "/getProfile")
"%s%s%d%s".format(apiLocation,"/users/", randomID, "/posts/post")*/
val random = new scala.util.Random(new java.security.SecureRandom())
val x= random.alphanumeric
val sb = new StringBuilder
x take 10 foreach println
def randomAlphaNumericString(): String = {
  val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
  val sb = new StringBuilder
  val random = new scala.util.Random(new java.security.SecureRandom())
  for (i <- 1 to 10) {
    val randomNum = random.nextInt(chars.length)
    sb.append(chars(randomNum))
  }
  sb.toString
}
println(randomAlphaNumericString())

var ncjd = "hdsjds, hsjhjs, djsdjks, djsdkjs"

println(ncjd.split(",")(0) )
println(ncjd.split(",")(1))
ncjd.replace(ncjd.split(",")(0) + ",", "")
println(ncjd.toUpperCase())