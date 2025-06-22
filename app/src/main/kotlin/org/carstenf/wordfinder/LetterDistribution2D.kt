package org.carstenf.wordfinder

//fun pickRandomLetter2DDistribution(letterCounts: IntArray, languageCode: LANGUAGE) : Char {
//    return pickRandomLetter2DDistributionF(letterCounts, languageCode) { it }
//}

fun pickRandomLetter2DDistributionF(letterCounts: IntArray,
                                   languageCode: LANGUAGE,
                                   operation: (Int) -> Int): Char {
    var letterCountMatrix = letterCountsEnglish

    if (languageCode == LANGUAGE.DE) {
        letterCountMatrix = letterCountsGerman
    }

    var totalCount = 0
    for (k in 0..25) {
        totalCount += operation(letterCountMatrix[k][letterCounts[k]])
    }

    var r = Math.random() * totalCount
    var i = 0

    while ( operation(letterCountMatrix[i][letterCounts[i]]) < r) {
        r -= operation(letterCountMatrix[i][letterCounts[i]])
        i += 1
    }

    if (letterCounts[i] < 10) // Make sure we never
        letterCounts[i]++
    return ('A'.code + i).toChar()
}

// Letter a: [41745, 8867, 724, 57, 2, 0, 0, 0, 0, 0]
// Letter b: [12141, 1027, 60, 2, 0, 0, 0, 0, 0, 0]
// Letter c: [24456, 3466, 201, 2, 0, 0, 0, 0, 0, 0]
// Letter d: [22996, 2878, 195, 6, 0, 0, 0, 0, 0, 0]
// Letter e: [56804, 21358, 4889, 695, 73, 3, 0, 0, 0, 0]
// Letter f: [8623, 1064, 21, 1, 0, 0, 0, 0, 0, 0]
// Letter g: [18740, 1939, 192, 10, 0, 0, 0, 0, 0, 0]
// Letter h: [14385, 1078, 25, 0, 0, 0, 0, 0, 0, 0]
// Letter i: [46249, 13244, 2545, 465, 69, 9, 1, 0, 0, 0]
// Letter j: [1230, 16, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter k: [5917, 286, 5, 2, 0, 0, 0, 0, 0, 0]
// Letter l: [30257, 5518, 508, 32, 0, 0, 0, 0, 0, 0]
// Letter m: [16925, 1805, 95, 2, 0, 0, 0, 0, 0, 0]
// Letter n: [39952, 9275, 1213, 82, 5, 0, 0, 0, 0, 0]
// Letter o: [33806, 7674, 816, 61, 3, 0, 0, 0, 0, 0]
// Letter p: [17608, 2205, 105, 2, 0, 0, 0, 0, 0, 0]
// Letter q: [1307, 0, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter r: [41592, 7859, 596, 13, 0, 0, 0, 0, 0, 0]
// Letter s: [48317, 15403, 4131, 1166, 238, 27, 2, 0, 0, 0]
// Letter t: [38222, 8605, 759, 31, 0, 0, 0, 0, 0, 0]
// Letter u: [21525, 1923, 100, 8, 0, 0, 0, 0, 0, 0]
// Letter v: [6944, 213, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter w: [5916, 237, 8, 0, 0, 0, 0, 0, 0, 0]
// Letter x: [1885, 6, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter y: [10102, 266, 1, 0, 0, 0, 0, 0, 0, 0]
// Letter z: [2696, 210, 6, 4, 0, 0, 0, 0, 0, 0]
val letterCountsEnglish = arrayOf(
    intArrayOf(7348, 1560, 127, 10, 0, 0, 0, 0, 0, 0),
    intArrayOf(2137, 180, 10, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4305, 610, 35, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4048, 506, 34, 1, 0, 0, 0, 0, 0, 0),
    intArrayOf(10000, 3759, 860, 122, 12, 0, 0, 0, 0, 0),
    intArrayOf(1518, 187, 3, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(3299, 341, 33, 1, 0, 0, 0, 0, 0, 0),
    intArrayOf(2532, 189, 4, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(8141, 2331, 448, 81, 12, 1, 0, 0, 0, 0),
    intArrayOf(216, 2, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1041, 50, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(5326, 971, 89, 5, 0, 0, 0, 0, 0, 0),
    intArrayOf(2979, 317, 16, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(7033, 1632, 213, 14, 0, 0, 0, 0, 0, 0),
    intArrayOf(5951, 1350, 143, 10, 0, 0, 0, 0, 0, 0),
    intArrayOf(3099, 388, 18, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(230, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(7322, 1383, 104, 2, 0, 0, 0, 0, 0, 0),
    intArrayOf(8505, 2711, 727, 205, 41, 4, 0, 0, 0, 0),
    intArrayOf(6728, 1514, 133, 5, 0, 0, 0, 0, 0, 0),
    intArrayOf(3789, 338, 17, 1, 0, 0, 0, 0, 0, 0),
    intArrayOf(1222, 37, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1041, 41, 1, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(331, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1778, 46, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(474, 36, 1, 0, 0, 0, 0, 0, 0, 0)
)

// Letter a: [356683, 85223, 7652, 519, 17, 0, 0, 0, 0, 0]
// Letter b: [160176, 16709, 898, 11, 0, 0, 0, 0, 0, 0]
// Letter c: [193497, 19278, 754, 6, 0, 0, 0, 0, 0, 0]
// Letter d: [197379, 19462, 611, 26, 0, 0, 0, 0, 0, 0]
// Letter e: [618850, 475775, 245253, 68822, 8606, 419, 14, 0, 0, 0]
// Letter f: [129908, 16156, 1228, 35, 0, 0, 0, 0, 0, 0]
// Letter g: [202311, 26263, 1897, 55, 0, 0, 0, 0, 0, 0]
// Letter h: [258856, 41442, 2911, 123, 1, 0, 0, 0, 0, 0]
// Letter i: [333877, 85665, 12203, 983, 69, 0, 0, 0, 0, 0]
// Letter j: [7808, 41, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter k: [139865, 11136, 379, 1, 0, 0, 0, 0, 0, 0]
// Letter l: [263767, 48382, 5136, 275, 11, 0, 0, 0, 0, 0]
// Letter m: [173994, 31400, 3060, 198, 10, 0, 0, 0, 0, 0]
// Letter n: [418303, 149322, 32267, 4842, 475, 18, 1, 0, 0, 0]
// Letter o: [181046, 24644, 2602, 172, 13, 0, 0, 0, 0, 0]
// Letter p: [103009, 15122, 1308, 59, 9, 2, 0, 0, 0, 0]
// Letter q: [4335, 14, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter r: [428704, 143364, 22738, 1793, 85, 2, 0, 0, 0, 0]
// Letter s: [399683, 137703, 32224, 5415, 528, 27, 1, 0, 0, 0]
// Letter t: [398276, 132104, 24672, 2760, 172, 3, 0, 0, 0, 0]
// Letter u: [256825, 44138, 3246, 72, 0, 0, 0, 0, 0, 0]
// Letter v: [58989, 1072, 10, 0, 0, 0, 0, 0, 0, 0]
// Letter w: [68049, 2102, 20, 0, 0, 0, 0, 0, 0, 0]
// Letter x: [7851, 26, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter y: [11544, 353, 0, 0, 0, 0, 0, 0, 0, 0]
// Letter z: [97785, 5622, 125, 0, 0, 0, 0, 0, 0, 0]
val letterCountsGerman = arrayOf(
    intArrayOf(5763, 1377, 123, 8, 0, 0, 0, 0, 0, 0),
    intArrayOf(2588, 270, 14, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(3126, 311, 12, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(3189, 314, 9, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(10000, 7688, 3963, 1112, 139, 6, 0, 0, 0, 0),
    intArrayOf(2099, 261, 19, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(3269, 424, 30, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4182, 669, 47, 1, 0, 0, 0, 0, 0, 0),
    intArrayOf(5395, 1384, 197, 15, 1, 0, 0, 0, 0, 0),
    intArrayOf(126, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(2260, 179, 6, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4262, 781, 82, 4, 0, 0, 0, 0, 0, 0),
    intArrayOf(2811, 507, 49, 3, 0, 0, 0, 0, 0, 0),
    intArrayOf(6759, 2412, 521, 78, 7, 0, 0, 0, 0, 0),
    intArrayOf(2925, 398, 42, 2, 0, 0, 0, 0, 0, 0),
    intArrayOf(1664, 244, 21, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(70, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(6927, 2316, 367, 28, 1, 0, 0, 0, 0, 0),
    intArrayOf(6458, 2225, 520, 87, 8, 0, 0, 0, 0, 0),
    intArrayOf(6435, 2134, 398, 44, 2, 0, 0, 0, 0, 0),
    intArrayOf(4150, 713, 52, 1, 0, 0, 0, 0, 0, 0),
    intArrayOf(953, 17, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1099, 33, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(126, 1, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(186, 5, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1580, 90, 2, 0, 0, 0, 0, 0, 0, 0)
)

fun rescaleArray(array: Array<IntArray>, maxTarget: Int = 10000): Array<IntArray> {
    // Step 1: Find the maximum value across the entire 2D array
    val currentMax = array.flatMap { it.asIterable() }.maxOrNull() ?: 0

    // Step 2: If the current maximum is already <= maxTarget, return the array as is
    if (currentMax <= maxTarget) return array

    // Step 3: Calculate the scaling factor
    val scaleFactor = maxTarget.toDouble() / currentMax

    // Step 4: Rescale the entire array based on the global maximum
    return array.map { row ->
        row.map { (it * scaleFactor).toInt() }.toIntArray()
    }.toTypedArray()
}

fun main() {
    // Rescale the array
    val rescaledArray = rescaleArray(letterCountsGerman)

    // Print the rescaled array
    rescaledArray.forEach { row ->
        println( "intArrayOf(${row.joinToString(", ")}), ") // NON-NLS
    }
}
