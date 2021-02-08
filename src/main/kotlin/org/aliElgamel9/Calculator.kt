package org.aliElgamel9

import kotlin.math.*

class Calculator {
    companion object {
        private val highPriorityOperators = listOf('!', '%', '^')
        private val multiplicationOperators = listOf('*', '/')
        // that need only one number in right hand side
        private val halfOperators = listOf('!')
        // that can act as a normal operator or half operator 100% = 1, 4%2 = 0
        private val multiOperators = listOf('%')
        private val summationOperands = listOf('+', '-')

        private val operatorsPriority = listOf(highPriorityOperators, multiplicationOperators, summationOperands)

        // bows that user can use to grouping operands (expressions)
        private val perfectBow = listOf("()", "[]", "{}", "<>")
        private const val openBows = "(<[{"
        private const val closeBows = ")>]}"

        // simple calculate method for user
        fun calculate(value: String): OperationResult {
            return calculate(value, 0)
        }

        // to edit data of calculation progress from other functions
        private data class CalculationData(
            var functionName: String = "", var i: Int = 0,
            val numbers: TerribleStack<Double> = TerribleStack(),
            val operands: TerribleStack<Char> = TerribleStack(), var number: String = ""
        )

        /*
            value is the expression that the user want the result of it
            startAt indicates where to start process in expression value
            openBow indicates if the expression is start by a bow and which one
         */
        private fun calculate(value: String, startAt: Int = 0, openBow: Char? = null): OperationResult {
            val calData = CalculationData(i = startAt)
            // return either a final result of value or error
            return calData.run {
                // iterating over the whole value beginning from startAt
                while (i < value.length) {
                    val c = value[i]
                    // get the result of current char depending on last char also
                    val result = calculationFlowResult(calData, value, c, openBow)
                    // check if the error message handle orders
                    if(result.errorMessage?.equals("break") == true) break
                    // check if there is an error occurs
                    if(result.result.isNaN()) return result
                    i++
                }
                var result = OperationResult(1.0)
                // check if there is a remain number that not converted yet
                if (number.isNotEmpty())
                    convertStringToDouble(number).apply { result = OperationResult(this);numbers.push(this) }
                // return error if converting process failed
                if (result.result.isNaN()) return result
                // check if there is a remain function not calculated yet
                if (functionName.isNotEmpty())
                    namedFunction(value, i - 1, functionName, numbers).apply { result = this;  numbers.push(this.result)}
                // return error if calculate named function process failed
                if (result.result.isNaN()) return result
                // calculate all operators that doesn't include letters or bows then return the result
                calculateSingleOperators(numbers, operands).run {
                    this.apply { terminateAt = i }
                }
            }
        }

        // control the flow of the calculations
        private fun calculationFlowResult(calData: CalculationData, value: String, c: Char, openBow: Char?): OperationResult {
            return calData.run{
                when(true){
                    functionName.isNotEmpty() -> return progressFunctionName(this, c, value)
                    c.isDigit() || c == '.' -> number += c //digit
                    (c == '+' || c == '-') && numbers.size == operands.size &&
                            (number.isEmpty() || !number[0].isDigit()) -> number = inverseSign(number, c) //sign
                    c.isLetter() -> return facingNamedFunction(calData, c) //first letter of a function
                    openBows.contains(c) -> return facingOpenBow(calData, value, c)
                    closeBows.contains(c) -> return if (openBow == null || perfectBow.find { it[1] == c }?.get(0) != openBow)
                        OperationResult(Double.NaN, i, "error bow") else OperationResult(errorMessage = "break")
                    else -> return remainOperators(calData, value, c)
                }
                OperationResult(1.0)
            }
        }

        private fun progressFunctionName(calData: CalculationData, c: Char, value: String): OperationResult {
            return calData.run {
                if (c.isLetter()) functionName += c
                else {
                    val result = namedFunction(value, i, functionName, numbers)
                    if (result.result.isNaN()) return result
                    if(number.isNotEmpty() && number[0] == '-')
                        applyNamedFunctionResult(
                            OperationResult(-result.result,
                                result.terminateAt, result.errorMessage), calData)
                    else applyNamedFunctionResult(result, calData)
                }
                OperationResult(1.0)
            }
        }

        fun inverseSign(number: String, sign: Char): String {
            if (number.isEmpty()) return sign.toString()
            if (sign == '-') return if (number == "+") "-" else "+"
            return number
        }

        private fun facingNamedFunction(calData: CalculationData, c: Char): OperationResult {
            return calData.run {
                functionName += c
                // if a function came after a digit like 4log10 then convert it as 4*log10
                if (number.isNotEmpty() && number[0].isDigit()) return implicitMultiple(calData)
                OperationResult(1.0)
            }
        }

        private fun facingOpenBow(calData: CalculationData, value: String, c: Char): OperationResult {
            return calData.run {
                // if a parentheses came after a digit like 5(4+5) then convert it as 5*(4+5)
                if (number.isNotEmpty() && number[0].isDigit())
                    with(implicitMultiple(calData)) { if(this.result.isNaN()) return this }
                // open bow indicates the expression might be either (4+3) or cos(4+3)
                val result = if (functionName.isEmpty()) calculate(value, i + 1, c)
                else namedFunction(value, i, functionName, numbers)
                if (result.result.isNaN()) return result
                // if number has negative sign then inverse sign of the result
                if(number.isNotEmpty() && number[0] == '-')
                    applyNamedFunctionResult(
                        OperationResult(-result.result,
                            result.terminateAt, result.errorMessage), calData)
                else applyNamedFunctionResult(result, calData)
                OperationResult(1.0)
            }
        }

        private fun remainOperators(calData: CalculationData, value: String, c: Char): OperationResult {
            return calData.run {
                // if numbers equals operators then must be a number not converted yet
                if (numbers.size == operands.size) {
                    with(convertStringToDouble(number)){
                        if (this.isNaN()) return OperationResult()
                        numbers.push(this)
                    }. also{ number = "" }
                }
                // if expression alike 5! or 5% then insert Double.NaN that will not be used in calculations
                // but indicates that no error might occurs
                if (halfOperators.contains(c) || (multiOperators.contains(c) &&
                            (i + 1 == value.length || !value[i + 1].isDigit())))
                    numbers.push(Double.NaN)
                operands.push(c)
                OperationResult(1.0)
            }
        }
        // when a result came from bows or named function then insert it
        private fun applyNamedFunctionResult(operationResult: OperationResult, calData: CalculationData) {
            calData.apply {
                operationResult.let {
                    numbers.push(it.result)
                    i = it.terminateAt
                    number = ""
                    functionName = ""
                }
            }
        }

        private fun implicitMultiple(calData: CalculationData): OperationResult {
            return calData.run {
                val result = convertStringToDouble(number).also { number = "" }
                if (result.isNaN()) return OperationResult()
                operands.push('*')
                numbers.push(result)
                OperationResult(1.0)
            }
        }
        // control flow of calculations of named functions
        private fun namedFunction(value: String, startAt: Int, functionName: String,
                                  numbers: TerribleStack<Double>
        ): OperationResult {
            return when (functionName) {
                "cos" -> namedFunctionWithBow(value, startAt, '(', "cos")
                "sin" -> namedFunctionWithBow(value, startAt, '(', "sin")
                "tan" -> namedFunctionWithBow(value, startAt, '(', "tan")
                "sqr" -> namedFunctionWithBow(value, startAt, '(', "sqr")
                "ln" -> namedFunctionWithBow(value, startAt, '(', "ln")
                "abs" -> namedFunctionWithBow(value, startAt, '(', "abs")
                "log" -> calculateLog(value, startAt)
                "e" -> OperationResult(Math.E, startAt - 1)
                "C" -> multiNamedFunctions(value, startAt, "C", numbers)
                "P" -> multiNamedFunctions(value, startAt, "P", numbers)
                else -> OperationResult(errorMessage = "undefined function")
            }
        }

        fun namedFunctionWithBow(
            value: String, startAt: Int = 0, expectOpenBow: Char,
            functionName: String
        ): OperationResult {
            // not fount the expected bow then there is an error would happen
            if (value[startAt] != expectOpenBow)
                return OperationResult(Double.NaN, startAt, "'(' parentheses not found")
            return with(calculate(value, startAt + 1, expectOpenBow)){
                if (result.isNaN()) return this
                when (functionName) {
                    "cos" -> OperationResult(cos(Math.toRadians(result)), terminateAt)
                    "sin" -> OperationResult(sin(Math.toRadians(result)), terminateAt)
                    "tan" -> OperationResult(tan(Math.toRadians(result)), terminateAt)
                    "sqr" -> OperationResult(sqrt(result), terminateAt)
                    "ln" -> OperationResult(ln(result), terminateAt)
                    "abs" -> OperationResult(abs(result), terminateAt)
                    else -> OperationResult(errorMessage = "undefined function")
                }
            }
        }
        // functions that's need 2 numbers one before and one after. It has format n[function name]r as nCr
        private fun multiNamedFunctions(value: String, startAt: Int, functionName: String,
                                        numbers: TerribleStack<Double>
        ): OperationResult {
            val iBeforeNamedFunction = startAt-1-functionName.length
            // before the function should be number or close bow
            if (numbers.empty() || iBeforeNamedFunction< 0 || !(value[iBeforeNamedFunction].isDigit() ||
                        closeBows.contains(value[iBeforeNamedFunction]))) return OperationResult()
            // the number before it
            val n = numbers.getLast()
            val r: Double
            var endAt: Int = value.length
            // the next number is surrounded by bows
            if (openBows.contains(value[startAt]))
                calculate(value, startAt, value[startAt]).apply { endAt = terminateAt;r = result; }
            // the next number is pure as 543
            else {
                for (i in startAt until value.length)
                    if (!value[i].isDigit()){
                        endAt = i
                        // if after the last digit of second number is letter then there is an error as nCrb
                        if(value[i].isLetter()) return OperationResult()
                    }
                r = convertStringToDouble(value.substring(startAt, endAt))
            }
            if (r.isNaN()) return OperationResult()
            return when (functionName) {
                "C" -> if (n > r) OperationResult(combinations(n, r), endAt) else OperationResult()
                "P" -> if (n > r) OperationResult(permutations(n, r), endAt) else OperationResult()
                else -> OperationResult(errorMessage = "wrong Operation")
            }
        }

        fun permutations(n: Double, r: Double): Double = factorial(n) / factorial(n - r)

        fun combinations(n: Double, r: Double): Double = permutations(n, r) / factorial(r)

        fun factorial(n: Double): Double {
            val a = n.toInt()
            if (a < n) return Double.NaN
            var result = 1.0
            for (i in 2..a)
                result *= i
            return result
        }
        /*
        the user could write the log in three formats
        1) log(x) as log(10)
        2) logbase(x) as log2(10)
        3) log[base](x) as log[2](10)
         */
        fun calculateLog(value: String, startIndex: Int): OperationResult {
            val parenthesesIndex = value.indexOf('(', startIndex)
            if (parenthesesIndex == -1)
                return OperationResult(Double.NaN, startIndex, "error log format")
            // initializing log information
            var resultBase = OperationResult(10.0)
            var resultX = OperationResult()
            if (parenthesesIndex == startIndex)  //first case log(x)
                resultX = calculate(value, startIndex + 1, '(')
            else if (value[startIndex] == '[') { //second case log[base](x)
                resultBase = calculate(value, startIndex + 1, '[')
                if (!resultBase.result.isNaN()) {
                    resultX = if (resultBase.terminateAt + 1 != parenthesesIndex)
                        OperationResult(Double.NaN, resultBase.terminateAt + 1, "wrong format log")
                    else calculate(value, parenthesesIndex + 1, '(')
                }
            } else { //third case logbase(x)
                val num = convertStringToDouble(value.substring(startIndex, parenthesesIndex))
                resultBase = OperationResult(num, startIndex, "wrong number")
                if (!num.isNaN())
                    resultX = calculate(value, parenthesesIndex + 1, '(')
            }
            if (resultBase.result.isNaN()) return resultBase
            else if (resultX.result.isNaN()) return resultX
            return log(resultX.result, resultBase.result).run {
                if (this.isNaN()) OperationResult(
                    Double.NaN, startIndex, "Math Error in calculating log",
                )
                OperationResult(this, resultX.terminateAt)
            }
        }

        private fun calculateSingleOperators(numbers: TerribleStack<Double>, operands: TerribleStack<Char>):
                OperationResult {
            // if operators equals operands then return error
            if (numbers.size != operands.size + 1) return OperationResult()
            // if stack has only one number then return this number
            if (numbers.size == 1) return OperationResult(numbers.pop())
            // declaring 2 stacks to insert reduced expressions on it
            var newNumbers: TerribleStack<Double>
            var newOperands: TerribleStack<Char>
            // hold 2 stacks that has information of calculations
            var currentNumbers = numbers
            var currentOperands = operands
            for (singleOperands in operatorsPriority) {
                newNumbers = TerribleStack()
                newOperands = TerribleStack()
                var a: Double? = null
                // exit loop when operands are empty
                while (!currentOperands.empty()) {
                    // take one operand with 2 numbers, the first number might be the result from the previous operand
                    val operand = currentOperands.pop()
                    if (a == null) a = currentNumbers.pop()
                    if (singleOperands.contains(operand)) {
                        val b = currentNumbers.pop()
                        a = calculateSingleOperators(a, b, operand)
                    } else {
                        newNumbers.push(a).also { a = null }
                        newOperands.push(operand)
                    }
                }
                if (a != null) newNumbers.push(a!!)
                if (!currentNumbers.empty()) newNumbers.push(currentNumbers.pop())
                // assign 2 stacks that has information of calculations to new stack that has reduced expression
                currentNumbers = newNumbers
                currentOperands = newOperands
            }
            val result = currentNumbers.pop()
            if (result.isNaN()) return OperationResult(errorMessage = "wrong operations")
            return OperationResult(result)
        }

        private fun calculateSingleOperators(x1: Double, x2: Double, operand: Char): Double {
            return when (operand) {
                '*' -> x1 * x2
                '/' -> x1 / x2
                '^' -> x1.pow(x2)
                '+' -> if (x1.isNaN()) x2 else x1 + x2
                '-' -> if (x1.isNaN()) -x2 else x1 - x2
                '!' -> if (!x2.isNaN() || floor(x1) != x1) Double.NaN else factorial(x1.toInt())
                '%' -> if (x2.isNaN()) x1 / 100.0 else x1 % x2
                else -> Double.NaN
            }
        }

        fun factorial(x1: Int): Double {
            var result = 1.0
            for (i in 2..x1) result *= i
            return result
        }

        // return NAN if value can't convert to double
        fun convertStringToDouble(value: String): Double {
            return try {
                value.toDouble()
            } catch (ex: NumberFormatException) {
                Double.NaN
            }
        }
    }
}
data class OperationResult(val result:Double=Double.NaN, var terminateAt:Int=-1, val errorMessage:String?=null)

class TerribleStack<E> {
    private val list = mutableListOf<E>()
    var lastPopIndex = 0

    fun push(e: E) {
        list.add(e)
    }

    fun empty(): Boolean = lastPopIndex == list.size

    fun pop(): E = list[lastPopIndex++]

    fun getLast(): E {
        val temp = list[list.size - 1]
        list.removeLast()
        return temp
    }

    val size: Int
        get() = list.size - lastPopIndex
}