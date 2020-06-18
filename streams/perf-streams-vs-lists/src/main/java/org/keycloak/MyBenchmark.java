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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Mode;
import org.keycloak.representations.idm.RoleRepresentation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MyBenchmark {

    @Param({"10", "100", "1000", "20000"})
    public int size;

    private static AtomicInteger COUNTER = new AtomicInteger();

    public OutputStream devNull = new OutputStream() {
        @Override public void write(byte[] b, int off, int len) throws IOException { }
        @Override public void write(byte[] b) throws IOException { }
        @Override public void write(int b) throws IOException { }
    };

    private static RoleRepresentation getNewRole() {
        return new RoleRepresentation("role " + COUNTER.incrementAndGet(), "description", true);
    }

    private void writeJson(Object o) {
        try {
            JsonSerialization.prettyMapper.writeValue(devNull, o);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Benchmark
    public void streamStaticReference() {
        Stream<RoleRepresentation> s = Stream.generate(MyBenchmark::getNewRole).limit(size);
        writeJson(s);
    }

    @Benchmark
    public void streamLambda() {
        Stream<RoleRepresentation> s = Stream.generate(() -> new RoleRepresentation("role " + COUNTER.incrementAndGet(), "description", true)).limit(size);
        writeJson(s);
    }

    @Benchmark
    public void list() {
        List<RoleRepresentation> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            list.add(getNewRole());
        }
        writeJson(list);
    }

    @Benchmark
    public void parallelStreamStaticReference() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(5);
        List<Future<Object>> f = es.invokeAll(Arrays.asList(
          Executors.callable(this::streamStaticReference),
          Executors.callable(this::streamStaticReference),
          Executors.callable(this::streamStaticReference),
          Executors.callable(this::streamStaticReference),
          Executors.callable(this::streamStaticReference)
        ));
        es.shutdown();
        for (Future<Object> future : f) {
            future.get();
        }
    }

    @Benchmark
    public void parallelStreamLambda() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(5);
        List<Future<Object>> f = es.invokeAll(Arrays.asList(
          Executors.callable(this::streamLambda),
          Executors.callable(this::streamLambda),
          Executors.callable(this::streamLambda),
          Executors.callable(this::streamLambda),
          Executors.callable(this::streamLambda)
        ));
        es.shutdown();
        for (Future<Object> future : f) {
            future.get();
        }
    }

    @Benchmark
    public void parallelList() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(5);
        List<Future<Object>> f = es.invokeAll(Arrays.asList(
          Executors.callable(this::list),
          Executors.callable(this::list),
          Executors.callable(this::list),
          Executors.callable(this::list),
          Executors.callable(this::list)
        ));
        es.shutdown();
        for (Future<Object> future : f) {
            future.get();
        }
    }
}
