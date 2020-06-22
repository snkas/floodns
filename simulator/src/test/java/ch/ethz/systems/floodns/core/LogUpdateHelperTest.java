/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 snkas
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.ethz.systems.floodns.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Simulator.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class LogUpdateHelperTest {

    @Mock
    private Simulator simulator;

    private void setTime(long t) {
        when(simulator.getCurrentTime()).thenReturn(t);
    }

    @Test
    public void testOne() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(0);
        helper.update(11.0);
        setTime(10);
        helper.finish();
        assertEquals(0, (long) lsf.starts.get(0));
        assertEquals(10, (long) lsf.ends.get(0));
        assertEquals(11.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(1, lsf.size());
    }

    @Test
    public void testOneMerge() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(80);
        helper.update(10.0);
        helper.update(6.0);
        helper.update(7.0);
        helper.update(22.0);
        setTime(99);
        helper.update(10.0);
        helper.update(11.0);
        helper.update(22.0);
        setTime(200);
        helper.finish();
        assertEquals(80, (long) lsf.starts.get(0));
        assertEquals(200, (long) lsf.ends.get(0));
        assertEquals(22.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(1, lsf.size());
    }

    @Test
    public void testDoubleNoMerge() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(1);
        helper.update(10.0);
        setTime(80);
        helper.update(8.0);
        setTime(81);
        helper.finish();
        assertEquals(1, (long) lsf.starts.get(0));
        assertEquals(80, (long) lsf.ends.get(0));
        assertEquals(10.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(80, (long) lsf.starts.get(1));
        assertEquals(81, (long) lsf.ends.get(1));
        assertEquals(8.0, lsf.values.get(1), simulator.getFlowPrecision());
        assertEquals(2, lsf.size());
    }

    @Test
    public void testTripleMerge() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(1);
        helper.update(10.0);
        setTime(100);
        helper.update(8.0);
        setTime(111);
        helper.update(8.0);
        setTime(400);
        helper.finish();
        assertEquals(1, (long) lsf.starts.get(0));
        assertEquals(100, (long) lsf.ends.get(0));
        assertEquals(10.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(100, (long) lsf.starts.get(1));
        assertEquals(400, (long) lsf.ends.get(1));
        assertEquals(8.0, lsf.values.get(1), simulator.getFlowPrecision());
        assertEquals(2, lsf.size());
    }

    @Test
    public void testTripleDiff() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(1);
        helper.update(10.0);
        helper.update(6584.0);
        setTime(100);
        helper.update(-6584.0);
        helper.update(21.0);
        setTime(999);
        helper.update(-11.0);
        setTime(1001);
        helper.finish();
        assertEquals(1, (long) lsf.starts.get(0));
        assertEquals(100, (long) lsf.ends.get(0));
        assertEquals(6584.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(100, (long) lsf.starts.get(1));
        assertEquals(999, (long) lsf.ends.get(1));
        assertEquals(21.0, lsf.values.get(1), simulator.getFlowPrecision());
        assertEquals(999, (long) lsf.starts.get(2));
        assertEquals(1001, (long) lsf.ends.get(2));
        assertEquals(-11.0, lsf.values.get(2), simulator.getFlowPrecision());
        assertEquals(3, lsf.size());
    }

    @Test
    public void testTripleLastEmpty() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(1);
        helper.update(10.0);
        helper.update(6584.0);
        setTime(100);
        helper.update(-6584.0);
        helper.update(21.0);
        setTime(999);
        helper.update(-11.0);
        helper.finish();
        assertEquals(1, (long) lsf.starts.get(0));
        assertEquals(100, (long) lsf.ends.get(0));
        assertEquals(6584.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(100, (long) lsf.starts.get(1));
        assertEquals(999, (long) lsf.ends.get(1));
        assertEquals(21.0, lsf.values.get(1), simulator.getFlowPrecision());
        assertEquals(2, lsf.size());
    }

    @Test
    public void testTripleDiffFirstMerge() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));
        setTime(1);
        helper.update(10.0);
        helper.update(-6584.0);
        setTime(100);
        helper.update(-6584.0);
        setTime(999);
        helper.update(-11.0);
        setTime(1001);
        helper.finish();
        assertEquals(1, (long) lsf.starts.get(0));
        assertEquals(999, (long) lsf.ends.get(0));
        assertEquals(-6584.0, lsf.values.get(0), simulator.getFlowPrecision());
        assertEquals(999, (long) lsf.starts.get(1));
        assertEquals(1001, (long) lsf.ends.get(1));
        assertEquals(-11.0, lsf.values.get(1), simulator.getFlowPrecision());
        assertEquals(2, lsf.size());
    }

    @Test
    public void testMulti() {
        LSF lsf = new LSF();
        LogUpdateHelper<Double> helper = new LogUpdateHelper<>(simulator, lsf, new LogUpdateHelper.LogFlowCompareFunction(1e-10));

        // Create 100 intervals
        for (int i = 0; i < 100; i++) {
            setTime(i * 10);
            helper.update(i * 3.0);
        }
        setTime(1000);
        helper.finish();

        // Check that all 100 intervals are correct
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 10, (long) lsf.starts.get(i));
            assertEquals((i + 1) * 10, (long) lsf.ends.get(i));
            assertEquals(i * 3.0, lsf.values.get(i), simulator.getFlowPrecision());
        }
        assertEquals(100, lsf.size());

    }

    private class LSF implements LogUpdateHelper.LogSaveFunction<Double> {

        private List<Long> starts;
        private List<Long> ends;
        private List<Double> values;

        private LSF() {
            this.starts = new ArrayList<>();
            this.ends = new ArrayList<>();
            this.values = new ArrayList<>();

        }

        @Override
        public void save(long start, long end, Double val) {
            starts.add(start);
            ends.add(end);
            values.add(val);
        }

        public int size() {
            return starts.size();
        }

    }

}
