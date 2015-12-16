import scala.util.Random

val string: String = "hello, fekkk"

println(string.split(",")(0))
println(string.split(",")(1))

var str:String = "hello who sjsa"
"%s, %s".format(str, "no")
val randomID = Random.nextInt(100000-1) + 1
val apiLocation = "http://127.0.0.1:8080/project4"
var path = "%s%s%d".format(apiLocation,"/users/getProfile/", randomID)
"%s%s%d%s".format(apiLocation,"/users/", randomID, "/getProfile")
"%s%s%d%s".format(apiLocation,"/users/", randomID, "/posts/post")