/*
 * Thai PUA Shaping
 *
 * Translated from: https://github.com/behdad/harfbuzz/blob/master/src/hb-ot-shape-complex-thai.cc
 * License: MIT
 *
 * Example usage:
 *   doThaiShaping(preProcessTextThai("พ่อผู้ใหญ่ช่างใจป้ำจริงๆ ไก่จิกเด็กตายบนปากโอ่ง"))
 */

private enum class ThaiConsonentType {
    NC,
    AC,
    RC,
    DC,
    NOT_CONSONANT
}

private fun getConsonentType(codepoint: Int): ThaiConsonentType {
    if (codepoint == 0x0E1B || codepoint == 0x0E1D || codepoint == 0x0E1F /* || codepoint == 0x0E2C*/)
        return ThaiConsonentType.AC
    if (codepoint == 0x0E0D || codepoint == 0x0E10)
        return ThaiConsonentType.RC
    if (codepoint == 0x0E0E || codepoint == 0x0E0F)
        return ThaiConsonentType.DC
    if (codepoint in 0x0E01..0x0E2E)
        return ThaiConsonentType.NC
    return ThaiConsonentType.NOT_CONSONANT
}

private enum class ThaiMarkType {
    AV,
    BV,
    T,
    NOT_MARK
}

private fun getMarkType(codepoint: Int): ThaiMarkType {
    if (codepoint == 0x0E31 || codepoint in 0x0E34..0x0E37
            || codepoint == 0x0E47 || codepoint in 0x0E4D..0x0E4E)
        return ThaiMarkType.AV
    if (codepoint in 0x0E38..0x0E3A)
        return ThaiMarkType.BV
    if (codepoint in 0x0E48..0x0E4C)
        return ThaiMarkType.T
    return ThaiMarkType.NOT_MARK
}

private enum class ThaiActionType {
    NOP,
    SD, /* Shift combining-mark down */
    SL, /* Shift combining-mark left */
    SDL, /* Shift combining-mark down-left */
    RD   /* Remove descender from base */
}

private fun thaiPuaShape(codepoint: Int, action: ThaiActionType): Int {
    val mapping = mapOf(
            Pair(ThaiActionType.SD,
                    mapOf(
                            Pair(0x0E48, 0xF70A), // MAI EK
                            Pair(0x0E49, 0xF70B), /* MAI THO */
                            Pair(0x0E4A, 0xF70C), /* MAI TRI */
                            Pair(0x0E4B, 0xF70D), /* MAI CHATTAWA */
                            Pair(0x0E4C, 0xF70E), /* THANTHAKHAT */
                            Pair(0x0E38, 0xF718), /* SARA U */
                            Pair(0x0E39, 0xF719), /* SARA UU */
                            Pair(0x0E3A, 0xF71A)  /* PHINTHU */
                    )),
            Pair(ThaiActionType.SDL,
                    mapOf(
                            Pair(0x0E48, 0xF705), // MAI EK
                            Pair(0x0E49, 0xF706), /* MAI THO */
                            Pair(0x0E4A, 0xF707), /* MAI TRI */
                            Pair(0x0E4B, 0xF708), /* MAI CHATTAWA */
                            Pair(0x0E4C, 0xF709)  /* THANTHAKHAT */
                    )),
            Pair(ThaiActionType.SL,
                    mapOf(
                            Pair(0x0E48, 0xF713), /* MAI EK */
                            Pair(0x0E49, 0xF714), /* MAI THO */
                            Pair(0x0E4A, 0xF715), /* MAI TRI */
                            Pair(0x0E4B, 0xF716), /* MAI CHATTAWA */
                            Pair(0x0E4C, 0xF717), /* THANTHAKHAT */
                            Pair(0x0E31, 0xF710), /* MAI HAN-AKAT */
                            Pair(0x0E34, 0xF701), /* SARA I */
                            Pair(0x0E35, 0xF702), /* SARA II */
                            Pair(0x0E36, 0xF703), /* SARA UE */
                            Pair(0x0E37, 0xF704), /* SARA UEE */
                            Pair(0x0E47, 0xF712), /* MAITAIKHU */
                            Pair(0x0E4D, 0xF711)  /* NIKHAHIT */
                    )),
            Pair(ThaiActionType.RD,
                    mapOf(
                            Pair(0x0E0D, 0xF70F), /* YO YING */
                            Pair(0x0E10, 0xF700)  /* THO THAN */
                    ))
    )

    return when(action) {
        ThaiActionType.SD,
        ThaiActionType.SL,
        ThaiActionType.SDL,
        ThaiActionType.RD -> {
            if (mapping[action]!!.contains(codepoint))
                mapping[action]!![codepoint]!!
            else
                codepoint
        }
        else -> codepoint
    }
}

private enum class ThaiAboveStateType {
    /* Cluster above looks like: */
    T0, /*  ⣤                      */
    T1, /*     ⣼                   */
    T2, /*        ⣾                */
    T3  /*           ⣿             */
}
private val ThaiAboveStartState = mapOf(
        Pair(ThaiConsonentType.NC, ThaiAboveStateType.T0),
        Pair(ThaiConsonentType.AC, ThaiAboveStateType.T1),
        Pair(ThaiConsonentType.RC, ThaiAboveStateType.T0),
        Pair(ThaiConsonentType.DC, ThaiAboveStateType.T0),
        Pair(ThaiConsonentType.NOT_CONSONANT, ThaiAboveStateType.T3)
)

private data class ThaiAboveStateMachineEdge(val action: ThaiActionType, val state: ThaiAboveStateType)
private val ThaiAboveStateMachine = mapOf(
        Pair(ThaiAboveStateType.T0,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T3)),
                        Pair(ThaiMarkType.BV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T0)),
                        Pair(ThaiMarkType.T, ThaiAboveStateMachineEdge(ThaiActionType.SD, ThaiAboveStateType.T3))
                )),
        Pair(ThaiAboveStateType.T1,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiAboveStateMachineEdge(ThaiActionType.SL, ThaiAboveStateType.T2)),
                        Pair(ThaiMarkType.BV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T1)),
                        Pair(ThaiMarkType.T, ThaiAboveStateMachineEdge(ThaiActionType.SDL, ThaiAboveStateType.T2))
                )),
        Pair(ThaiAboveStateType.T2,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T3)),
                        Pair(ThaiMarkType.BV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T2)),
                        Pair(ThaiMarkType.T, ThaiAboveStateMachineEdge(ThaiActionType.SL, ThaiAboveStateType.T3))
                )),
        Pair(ThaiAboveStateType.T3,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T3)),
                        Pair(ThaiMarkType.BV, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T3)),
                        Pair(ThaiMarkType.T, ThaiAboveStateMachineEdge(ThaiActionType.NOP, ThaiAboveStateType.T3))
                ))
)

private enum class ThaiBelowStateType {
    B0, /* No descender */
    B1, /* Removable descender */
    B2  /* Strict descender */
}
private val ThaiBelowStartState = mapOf(
        Pair(ThaiConsonentType.NC, ThaiBelowStateType.B0),
        Pair(ThaiConsonentType.AC, ThaiBelowStateType.B0),
        Pair(ThaiConsonentType.RC, ThaiBelowStateType.B1),
        Pair(ThaiConsonentType.DC, ThaiBelowStateType.B2),
        Pair(ThaiConsonentType.NOT_CONSONANT, ThaiBelowStateType.B2)
)

private data class ThaiBelowStateMachineEdge(val action: ThaiActionType, val state: ThaiBelowStateType)
private val ThaiBelowStateMachine = mapOf(
        Pair(ThaiBelowStateType.B0,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B0)),
                        Pair(ThaiMarkType.BV, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B2)),
                        Pair(ThaiMarkType.T, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B0))
                )),
        Pair(ThaiBelowStateType.B1,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B1)),
                        Pair(ThaiMarkType.BV, ThaiBelowStateMachineEdge(ThaiActionType.RD, ThaiBelowStateType.B2)),
                        Pair(ThaiMarkType.T, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B1))
                )),
        Pair(ThaiBelowStateType.B2,
                mapOf(
                        Pair(ThaiMarkType.AV, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B2)),
                        Pair(ThaiMarkType.BV, ThaiBelowStateMachineEdge(ThaiActionType.SD, ThaiBelowStateType.B2)),
                        Pair(ThaiMarkType.T, ThaiBelowStateMachineEdge(ThaiActionType.NOP, ThaiBelowStateType.B2))
                ))
)

fun doThaiShaping(input: String): String {
    val out = StringBuilder(input)
    var aboveState = ThaiAboveStartState[ThaiConsonentType.NOT_CONSONANT]!!
    var belowState = ThaiBelowStartState[ThaiConsonentType.NOT_CONSONANT]!!
    var base = 0

    for ((i: Int, c: Char) in input.withIndex()) {
        val cp = c.toInt()
        val markType = getMarkType(cp)
        if (markType == ThaiMarkType.NOT_MARK) {
            val ct = getConsonentType(cp)
            aboveState = ThaiAboveStartState[ct]!!
            belowState = ThaiBelowStartState[ct]!!
            base = i
            continue
        }

        val aboveEdge = ThaiAboveStateMachine[aboveState]!![markType]!!
        val belowEdge = ThaiBelowStateMachine[belowState]!![markType]!!
        aboveState = aboveEdge.component2()
        belowState = belowEdge.component2()

        /* At least one of the above/below actions is NOP. */
        val action = if (aboveEdge.action != ThaiActionType.NOP) aboveEdge.action else belowEdge.action
        if (action == ThaiActionType.RD) {
            out[base] = thaiPuaShape(input[base].toInt(), action).toChar()
        }
        else {
            out[i] = thaiPuaShape(cp, action).toChar()
        }
    }

    return out.toString()
}

private fun isSaraAm(c: Char) = c.toInt() == 0x0E33
private fun isToneMark(c: Char): Boolean {
    val cp = c.toInt()
    return cp in 0x0E34..0x0E37 || cp in 0x0E47..0x0E4E || cp == 0x0E31
}

fun preProcessTextThai(input: String): String {
    val buffer = StringBuilder()
    for (c: Char in input) {
        if (!isSaraAm(c)) {
            buffer.append(c)
            continue
        }

        val last = buffer.last()
        if (isToneMark(last)) {
            /* Decompose and reorder. */
            buffer[buffer.length - 1] = 0x0E4D.toChar()
            buffer.append(last)
            buffer.append(0x0E32.toChar())
        }
        else {
            buffer.append(0x0E4D.toChar())
            buffer.append(0x0E32.toChar())
        }
    }

    return buffer.toString()
}
