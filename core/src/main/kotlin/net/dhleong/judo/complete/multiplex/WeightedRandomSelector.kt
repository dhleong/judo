package net.dhleong.judo.complete.multiplex

import net.dhleong.judo.complete.MultiplexSelector
import kotlin.math.abs

/**
 * @author dhleong
 */
class WeightedRandomSelector(
    private val weights: DoubleArray,
    private val random: () -> Double = Math::random
) : MultiplexSelector {

    companion object {
        fun distributeByWordIndex(vararg weights: DoubleArray): (Int) -> WeightedRandomSelector {
            // verify weights
            weights.forEach { weightArray ->
                if (abs(weightArray.sum() - 1f) > 0.001f) {
                    throw IllegalArgumentException(
                        "Weights in $weightArray do not sum to 1f")
                }
            }

            // we're all good
            return { wordIndex ->
                WeightedRandomSelector(
                    if (wordIndex >= weights.size) weights.last()
                    else weights[wordIndex]
                )
            }
        }
    }

    private val weightsSorting = weights.indices.sortedBy { weights[it] }

    override fun select(candidates: List<String>): Int {
        var dieRoll = random()

        do {
            var emptyWeight = 0.0
            var lastWeight = 0.0
            for (i in weights.indices) {
                val weightIndex = weightsSorting[i]
                val weight = weights[weightIndex]
                val totalWeight = lastWeight + weight

                if (dieRoll <= totalWeight) {
                    if (candidates[weightIndex].isEmpty()) {
                        emptyWeight += weight
                    } else {
                        return weightIndex
                    }
                }

                lastWeight = totalWeight
            }

            // if we got here, either our weights were wrong (unlikely)
            // or it was a higher roll and one of the more heavily-weighted
            // sources was empty (which means emptyWeight > 0). So, we
            // adjust our dieRoll by the weight of those empty sources, so
            // lower-weighted but non-empty sources can fill the gap
            dieRoll -= emptyWeight

            // if emptyWeight was 0 and we got here, the weights are wrong
        } while (emptyWeight > 0)

        // shouldn't happen if you use [distributeByWordIndex]
        throw IllegalStateException(
            "$dieRoll not in range of any weight! weights=${weights.joinToString(",")}")
    }
}
