import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MathTest {

    @Test
    fun mathTest() = runTest {
        assertEquals(3.141592653589793, eval("MATH.PI").double, 0.001)
        assertEquals(2.718281828459045, eval("MATH.E").double, 0.001)

        assertEquals(3.141592653589793, eval("PI").double, 0.001)

        // SQRT
        assertEquals(10.0, eval("SQRT(100)").double, 0.001)

        // SIN
        assertEquals(0.0, eval("SIN(0)").double, 0.001)
        assertEquals(1.0, eval("SIN(PI / 2)").double, 0.001)

        // COS
        assertEquals(1.0, eval("COS(0)").double, 0.001)
        assertEquals(-1.0, eval("COS(PI)").double, 0.001)

        // TAN
        assertEquals(1.0, eval("TAN(PI / 4)").double, 0.001)

        // POW
        assertEquals(8.0, eval("POW(2; 3)").double, 0.001)

        // EXP
        assertEquals(2.718281828459045, eval("EXP(1)").double, 0.001)
        assertEquals(7.389056098930649, eval("EXP(2)").double, 0.001)

        // LOG (自然対数)
        assertEquals(1.0, eval("LOG(MATH.E)").double, 0.001)
    }

}

