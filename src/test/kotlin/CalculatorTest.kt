import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.aliElgamel9.*

internal class CalculatorTest{
    @Test
    fun testLog(){
        // first case
        var result = Calculator.calculate("log(30)")
        assertEquals("1.477", "%.3f".format(result.result))
        // same first case
        result = Calculator.calculate("log((30*3)[6*5/3+2])")
        assertEquals("3.033", "%.3f".format(result.result))

        // second case
        result = Calculator.calculate("log4(30)")
        assertEquals("2.453", "%.3f".format(result.result))

        // third case
        result = Calculator.calculate("log[40*7/3](30)")
        assertEquals("0.750", "%.3f".format(result.result))
    }
    @Test
    fun testCalculate(){
        var result = Calculator.calculate("abs(-4)")
        assertEquals("4.000", "%.3f".format(result.result))

        result = Calculator.calculate("cos(60)4")
        assertEquals("NaN", "%.3f".format(result.result))

        result = Calculator.calculate("6cos(60)")
        assertEquals("3.000", "%.3f".format(result.result))
    }
}