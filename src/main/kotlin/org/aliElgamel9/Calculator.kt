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
            var functionName: String = "", var i: Int = 0, var number: String = "",
            val numbers: MutableList<Double> = MutableList(0){0.0},
            val operands: MutableList<Char> = MutableList(0){' '}
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
                    convertStringToDouble(number)
                        .apply{ result = OperationResult(this);numbers.add(this) }
                // return error if converting process failed
                if (result.result.isNaN()) return result
                // check if there is a remain function not calculated yet
                if (functionName.isNotEmpty())
                    namedFunction(value, i - 1, functionName, numbers)
                        .apply { result = this;  numbers.add(this.result)}
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
                    c.isLetter() -> return facingNamedFunction(this, c, value) //first letter of a function
                    openBows.contains(c) -> return facingOpenBow(this, value, c)
                    closeBows.contains(c) -> return if (openBow != null || perfectBow.find { it[1] == c }?.get(0) == openBow)
                     OperationResult(errorMessage = "break") else OperationResult(Double.NaN, i, "error bow")
                    else -> return remainOperators(this, value, c)
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

        private fun facingNamedFunction(calData: CalculationData, c: Char, value:String): OperationResult {
            return calData.run {
                // if a function came after a digit like 4log(10) then convert it as 4*log(10)
                with(implicitMultiple(calData)){if(result.isNaN()) return this}
                val checkSign = if(number.isNotEmpty() && number[0] == '-') -1 else 1
                if(c == 'e'){
                    numbers.add(Math.E * checkSign)
                    number = ""
                }else if(c == 'P' && i+1<value.length && value[i+1] == 'I'){
                    numbers.add(Math.PI * checkSign)
                    i++
                    number = ""
                } else
                    functionName += c
                OperationResult(1.0)
            }
        }

        private fun facingOpenBow(calData: CalculationData, value: String, c: Char): OperationResult {
            return calData.run {
                // detect implicit multiple (5)(5) | 6(5) | e(5)
                with(implicitMultiple(calData)) { if(this.result.isNaN()) return this }
                // open bow indicates the expression might be either (4+3) or cos(4+3)
                val result = if (functionName.isEmpty()) calculate(value, i + 1, c)
                else namedFunction(value, i, functionName, numbers)
                if (result.result.isNaN()) return result
                // if number has negative sign then inverse sign of the result
                if(number.isNotEmpty() && number[0] == '-')
                    applyNamedFunctionResult(OperationResult(-result.result,
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
                        numbers.add(this)
                    }. also{ number = "" }
                }
                // if expression alike 5! or 5% then insert Double.NaN that will not be used in calculations
                // but indicates that no error might occurs
                if (halfOperators.contains(c) || (multiOperators.contains(c) &&
                            (i + 1 == value.length || !value[i + 1].isDigit())))
                    numbers.add(Double.NaN)
                operands.add(c)
                OperationResult(1.0)
            }
        }
        // when a result came from bows or named function then insert it
        private fun applyNamedFunctionResult(operationResult: OperationResult, calData: CalculationData) {
            calData.apply {
                operationResult.let {
                    numbers.add(it.result)
                    i = it.terminateAt
                    number = ""
                    functionName = ""
                }
            }
        }

        private fun implicitMultiple(calData: CalculationData): OperationResult {
            return calData.run {
                // 1- 3(4)
                if (number.isNotEmpty() && ((number[0] != '-' && number[0] != '+') ||
                    number.length > 1)){
                    with(convertStringToDouble(number)) {
                        number = ""
                        numbers.add(this)
                        if (this.isNaN()) return OperationResult()
                    }
                    operands.add('*')
                } // 2- (4)(5) || e(5) which could be detected by the numbers and operands size
                else if(numbers.size - 1 == operands.size)
                    operands.add('*')
                OperationResult(1.0)
            }
        }
        // control flow of calculations of named functions
        private fun namedFunction(value: String, startAt: Int, functionName: String,
                                  numbers: MutableList<Double>
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
            value: String, startAt: Int = 0, expectOpenBow: Char, functionName: String):
                OperationResult {
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
                                        numbers: MutableList<Double>
        ): OperationResult {
            val iBefore = startAt-1-functionName.length
            // before the function should be number or close bow
            if (numbers.isEmpty() || iBefore< 0 || !(value[iBefore].isDigit() ||
                        closeBows.contains(value[iBefore]))) return OperationResult()
            // the number before it
            val n = numbers.removeLast()
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

        private fun calculateSingleOperators(numbers: MutableList<Double>,
                                             operands: MutableList<Char>): OperationResult {
            // if operators equals operands then return error
            if (numbers.size != operands.size + 1) return OperationResult()
            // if list has only one number then return this number
            if (numbers.size == 1) return OperationResult(numbers.removeLast())

            // to ignore last elements without deleting this elements
            var newSize = operands.size
            // It's a queue with no queue class
            for (singleOperands in operatorsPriority) {
                // start add new numbers from here
                var headAdd = 0
                // start to take numbers from here and make operation on them
                var headRemove = -1

                var a: Double? = null
                // exit loop when operands are empty
                while (++headRemove != newSize) {

                    val operand = operands[headRemove]
                    if (a == null) a = numbers[headRemove]
                    if (singleOperands.contains(operand)) {
                        val b = numbers[headRemove + 1]
                        a = calculateSingleOperators(a, b, operand)
                    } else {
                        numbers[headAdd] = a
                        operands[headAdd++] = operand
                        a = null
                    }
                }
                if (a != null) numbers[headAdd++] = a
                else numbers[headAdd] = numbers[newSize]
                newSize = headAdd
            }
            val result = numbers[0]
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