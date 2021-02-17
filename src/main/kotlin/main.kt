import org.aliElgamel9.*
import org.junit.jupiter.api.Assertions

fun main(){
    val result = Calculator.calculate("-6eee")
    Assertions.assertEquals("-120.513", "%.3f".format(result.result))
    // write your function
    println(Calculator.calculate("cos(log(10)*ln(e))").result.toString().
    replaceLast('E',"*10^"))
}
    // It is general method but the useful from it to convert from
    // the scientific notation to the normal notation E5 to 10^5
fun String.replaceLast(oldChar: Char, newValue: String): String{
    for(i in length-1 downTo 0)
        if(this[i] == oldChar) return substring(0, i) + newValue + substring(i+1)
    return this
}
