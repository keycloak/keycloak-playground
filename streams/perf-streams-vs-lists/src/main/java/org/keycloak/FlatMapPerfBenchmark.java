/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.keycloak;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Mode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class FlatMapPerfBenchmark {

    private void testStream(String desc, Function<Stream<Integer>, Stream<Integer>> modifyStream) {
        AtomicInteger ai = new AtomicInteger();

        Integer[] a = new Integer[100000];
        for (int i = 0; i < a.length; i ++) {
            a[i] = i;
        }

        final Stream<Integer> s = modifyStream.apply(Stream.<Integer>of(a).onClose(() -> System.out.print(".")));
        s.forEach(ai::set);
    }

    @Benchmark
    public void testStreamSequential() {
        testStream("1s", s -> s);
    }

    @Benchmark
    public void testStreamParallel() {
        testStream("1", Stream::parallel);
    }

    @Benchmark
    public void testStreamSequentialFlatMap() {
        testStream("2", s -> Stream.of(s).flatMap(Function.identity()));
    }

    @Benchmark
    public void FlatMapParallelOuterFirst() {
        testStream("3", s -> Stream.of(s).parallel().flatMap(Function.identity()));
    }

    @Benchmark
    public void testStreamFlatMapParallelOuterLast() {
        testStream("4", s -> Stream.of(s).flatMap(Function.identity()).parallel());
    }

    @Benchmark
    public void testStreamFlatMapParallelInner() {
        testStream("5", s -> Stream.of(s).flatMap(s2 -> s2.parallel()));
    }

}
