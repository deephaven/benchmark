/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.Random;
import io.deephaven.benchmark.util.Ids;

/**
 * Instances of this class provide distributions of data based on given source and destination ranges and a source
 * value. The result is a value from the destination range. However, not all input values are necessary for the result.
 * For example, a function returning a random value need only ensure that the value is found within the given
 * destination range. On the other hand, a scaling function would used source range and destination range to translate a
 * source value to a value within the destination range.
 * <p/>
 * Note: In a practical sense, and from the perspective of usage in the <code>columnDefs</code> class, the distribution
 * functions do not return data values but indexes to data values. Put another way, the function return data positions.
 */
abstract class DFunction {

    /**
     * A factory method do get the distribution function and assign it an id
     * 
     * @param distribution a distribution {@code runlength | ascending | random}
     * @param id an id for the function to differentiate it from others of the same type
     * @return a function matching the give distribution
     */
    static DFunction get(String distribution, String id) {
        var df = switch (distribution.toLowerCase()) {
            case "runlength" -> new RunLengthDFunction();
            case "linearconv" -> new LinearConvDFunction();
            case "ascending" -> new AscendingDFunction();
            case "random" -> new RandomDFunction();
            // case "random-even-neg" -> new RandomDFunction();
            // case "random-odd-neg" -> new RandomDFunction();
            // case "random-shift" -> new RandomDFunction();
            default -> throw new RuntimeException("Undefined distribution function name: " + distribution);
        };
        df.name = distribution;
        df.id = id;
        df.init(id);
        return df;
    }

    private String name = null;
    private String id = null;

    /**
     * Apply the function to the given source/destination ranges and src value
     * 
     * @param srcMin the minimum source range
     * @param srcMax the maximum source range
     * @param srcVal a source value within the source range
     * @param dstMin the minimum destination range
     * @param dstMax the maximum destination range
     * @return a value from the destination range
     */
    abstract long apply(long srcMin, long srcMax, long srcVal, long dstMin, long dstMax);

    /**
     * Initialized the DFunction instance with it's ID
     * 
     * @param id an id for the function to differentiate it from others of the same type
     */
    protected void init(String id) {
        this.id = id;
    }

    /**
     * Get this distribution function's name
     * 
     * @return this function's name
     */
    final String getName() {
        return name;
    }

    /**
     * Get this distribution function's id
     * 
     * @return this function's id
     */
    final String getId() {
        return id;
    }

    /**
     * Translate a value from a source range into a value for the destination range. For example, if the source range is
     * 1-100, and the destination range was 1-50, the result of iterating {@code srcVal} from 1-100 would be
     * 1,1,2,2,3,3... and so on.
     */
    static class LinearConvDFunction extends DFunction {
        @Override
        long apply(long srcMin, long srcMax, long srcVal, long dstMin, long dstMax) {
            check(srcMin, srcMax, dstMin, dstMax);
            if (srcMin == srcMax)
                return (dstMax - dstMin) / 2;
            double srcOffset = srcVal - srcMin;
            double srcRange = srcMax - srcMin;
            double dstRange = dstMax - dstMin;
            return (long) (srcOffset / srcRange * dstRange + dstMin);
        }
    }

    /**
     * Produce repeating values according to the size of the given range. For example, if the destination range is 1-10,
     * the result of iterating {@code srcVal} from 1-100 would be 1,1,2,2,3,3... and so on.
     */
    static class RunLengthDFunction extends DFunction {
        final AscendingDFunction func = new AscendingDFunction();

        @Override
        long apply(long srcMin, long srcMax, long srcVal, long dstMin, long dstMax) {
            long dstSize = dstMax - dstMin;
            long v = func.apply(srcMin, srcMax, srcVal, dstMin, dstSize * dstSize);
            return v / dstSize;
        }
    }

    /**
     * Produce values that are in sequential order without going outside the destination range. For example, if the
     * destination range is 1-4, the result of iterating
     * {@code srvVal) from 1-100 would be 1,2,3,4,1,2,3,4... and so on.
     */
    static class AscendingDFunction extends DFunction {
        @Override
        long apply(long srcMin, long srcMax, long srcVal, long dstMin, long dstMax) {
            check(srcMin, srcMax, dstMin, dstMax);
            return (srcVal % (dstMax - dstMin)) + dstMin;
        }
    }

    /**
     * Produce values that are pseudo-randomly selected from the destination range. The source range and value are
     * ignored.
     */
    static class RandomDFunction extends DFunction {
        private Random random = null;

        @Override
        long apply(long srcMin, long srcMax, long srcVal, long dstMin, long dstMax) {
            check(srcMin, srcMax, dstMin, dstMax);
            return random.nextLong(dstMin, dstMax);
        }

        @Override
        protected void init(String id) {
            random = new Random(Ids.hash64(id));
        }
    }

    /**
     * Ensure that source and destination minimums are not greater than their corresponding maximums
     * 
     * @param srcMin the minimum source range
     * @param srcMax the maximum source range
     * @param dstMin the minimum destination range
     * @param dstMax the maximum destination range
     */
    static void check(long srcMin, long srcMax, long dstMin, long dstMax) {
        if (srcMin > srcMax)
            throw new RuntimeException("srcMin is greater than srcMax: " + srcMin + " > " + srcMax);
        if (dstMin > dstMax)
            throw new RuntimeException("dstMin is greater than dstMax: " + dstMin + " > " + dstMax);
    }

}
