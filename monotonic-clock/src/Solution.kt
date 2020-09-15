/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 */
class Solution : MonotonicClock {
    private var l1 by RegularInt(0)
    private var l2 by RegularInt(0)
    private var m1 by RegularInt(0)
    private var m2 by RegularInt(0)
    private var r by RegularInt(0)

    override fun write(time: Time) {
        // write right-to-left
        l2 = time.d1
        m2 = time.d2
        r = time.d3
        m1 = time.d2
        l1 = time.d1
    }

    override fun read(): Time {
        // read left-to-right
        val u1 = l1
        val v1 = m1
        val w = r
        val u2 = l2
        val v2 = m2
        return if (u1 == u2) {
            if (v1 == v2) {
                Time(u2, v2, w)
            } else {
                Time(u2, v2, 0)
            }
        } else {
            Time(u2,0,0)
        }
    }
}