// This file will only build on 1.5.0, so it can't go in src/ yet.
//
// Build with:
//   javac -classpath /usr/local/jdk1.5.0/lib/sa-jdi.jar:/usr/local/jdk1.5.0/lib/tools.jar HeapDump.java 
//
// Run with:
//   java -classpath /usr/local/jdk1.5.0/lib/sa-jdi.jar:/usr/local/jdk1.5.0/lib/tools.jar:. HeapDump

import java.util.*;

// Found in "sa-jdi.jar"...
import sun.jvm.hotspot.bugspot.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

// Found in "tools.jar"...
import sun.jvmstat.monitor.*;

public class HeapDump implements HeapVisitor {
    private BugSpotAgent agent;
    private HashMap<Klass, KlassInfo> map;
    private int objectCount;

    private int zeroLengthPrimitiveArrayCount;

    private long startTime;
    private long endTime;

    private HeapDump(int pid) {
        agent = new BugSpotAgent();
        agent.attach(pid);

        VM vm = VM.getVM();
        System.err.println("Running on: " + vm.getOS() + "/" + vm.getCPU());
        System.err.println("VM release: " + vm.getVMRelease());
        System.err.println("VM internal info: " + vm.getVMInternalInfo());

        ObjectHeap heap = vm.getObjectHeap();
        System.err.println("ObjectHeap = " + heap);

        heap.iterate(this);

        long duration = endTime - startTime;
        System.err.println("Time taken: " + duration + "ms");
        System.err.println("Speed: " + objectCount/duration + " objects/ms");
        System.err.println("Objects on heap: " + objectCount);
        System.err.println("Unique classes: " + map.size());

        agent.detach();
    }

    public void prologue(long x) {
        map = new HashMap<Klass, KlassInfo>();
        objectCount = 0;
        startTime = System.currentTimeMillis();
    }

    public void doObj(Oop oop) {
        ++objectCount;
        Klass klass = oop.getKlass();
        KlassInfo info = map.get(klass);
        if (info == null) {
            info = new KlassInfo(klass);
            map.put(klass, info);
        }
        ++info.instanceCount;
        info.totalByteCount += oop.getObjectSize();
        if (oop instanceof TypeArray) {
            long arrayLength = ((TypeArray) oop).getLength();
            if (arrayLength == 0) {
                System.out.println(klass.signature() + " array of length " + arrayLength + " (" + oop.getObjectSize() + " bytes)");
                ++zeroLengthPrimitiveArrayCount;
            }
        }
    }

    public void epilogue() {
        endTime = System.currentTimeMillis();
        dumpPerKlassInfo();
    }

    private void dumpPerKlassInfo() {
        long overallByteCount = 0;
        for (KlassInfo info : map.values()) {
            overallByteCount += info.totalByteCount;
            System.out.println(info);
        }
        System.out.println("Overall bytes on heap: " + overallByteCount);
        System.out.println("Zero-length primitive arrays: " + zeroLengthPrimitiveArrayCount);
    }

    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.err.println("usage: HeapDump <pid>");
            MonitoredHost localhost = MonitoredHost.getMonitoredHost("localhost");
            Set ids = new TreeSet(localhost.activeVms());
            for (Object id : ids) {
                MonitoredVm vm = localhost.getMonitoredVm(new VmIdentifier("//" + id));
                String name = MonitoredVmUtil.mainClass(vm, false);
                System.err.println(id + "\t" + name);
            }
            System.exit(0);
        }
        for (String pid : args) {
            new HeapDump(Integer.parseInt(pid));
        }
    }

    private static class KlassInfo {
        public Klass klass;

        public int instanceCount = 0;
        public int totalByteCount = 0;

        public KlassInfo(Klass klass) {
            this.klass = klass;
        }

        public String toString() {
            return instanceCount + "\t" + totalByteCount + "B\t" + name();
        }

        private String name() {
            Symbol symbol = klass.getName();
            return (symbol != null) ? symbol.asString() : "?";
        }
    }
}
