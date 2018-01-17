import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import opencl.CLContext;
import opencl.CLDevice;
import opencl.CLEnum;
import opencl.CLPlatform;
import opencl.CLProgram;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import static org.lwjgl.opencl.CL10.*;

class VecAddProgram extends CLProgram {
    private long clKernelVADD;

    VecAddProgram(CLContext context) throws Exception {
        super(context);

        clKernelVADD = clCreateKernel(this.program, "vadd", (IntBuffer)null);
    }

    @Override
    protected String getSource() {
        try {
            return Main.readKernelSource("add.cl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int[] vadd(int a[], int b[]) {
        assert(a.length == b.length);

        IntBuffer aBuffer = BufferUtils.createIntBuffer(a.length).put(a);
        IntBuffer bBuffer = BufferUtils.createIntBuffer(b.length).put(b);
        IntBuffer cBuffer = BufferUtils.createIntBuffer(a.length);
        aBuffer.rewind();
        bBuffer.rewind();

        long aClBuffer = clCreateBuffer(this.context.getContextID(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, aBuffer, null);
        long bClBuffer = clCreateBuffer(this.context.getContextID(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, bBuffer, null);
        long cClBuffer = clCreateBuffer(this.context.getContextID(), CL_MEM_WRITE_ONLY, 4*cBuffer.capacity(), null);
        clFinish(queue);

        clSetKernelArg1p(this.clKernelVADD, 0, aClBuffer);
        clSetKernelArg1p(this.clKernelVADD, 1, bClBuffer);
        clSetKernelArg1p(this.clKernelVADD, 2, cClBuffer);

        PointerBuffer globalSizeBuffer = BufferUtils.createPointerBuffer(1);
        globalSizeBuffer.put(a.length).flip();

        clEnqueueNDRangeKernel(queue, clKernelVADD, 1, null, globalSizeBuffer, null, null, null);
        clEnqueueReadBuffer(queue, cClBuffer, true, 0, cBuffer, null, null);
        clFinish(queue);

        int c[] = new int[cBuffer.capacity()];
        cBuffer.get(c);

        return c;
    }
}

public class Main {
    public static String readKernelSource(String filename) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource("kernel/" + filename).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void demoRunVADD(CLDevice device) {
        Random rand = new Random();
        int a[] = IntStream.range(0, 10240).map(__ -> rand.nextInt(500)).toArray();
        int b[] = IntStream.range(0, 10240).map(__ -> rand.nextInt(500)).toArray();
        int c_cpu[] = new int[10240];
        for (int i = 0; i < a.length; i++) {
            c_cpu[i] = a[i] + b[i];
        }

        try {
            CLContext context = new CLContext(device);
            VecAddProgram program = new VecAddProgram(context);
            int c_gpu[] = program.vadd(a, b);

            if (Arrays.equals(c_cpu, c_gpu)) {
                System.out.println("GPU vector addition succeeded");
            } else {
                System.out.println("GPU vector addition failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        // Device to run opencl program on
        Optional<CLDevice> gpuDevice = Optional.empty();

        CLPlatform[] platforms = CLEnum.getPlatforms();
        for (CLPlatform platform : platforms) {
            System.out.print(platform);

            System.out.println("  Devices:");
            CLDevice[] devices = CLEnum.getDevices(platform);
            for (CLDevice device : devices) {
                String deviceString = Stream.of(device.toString().split("\n"))
                    .map(line -> String.format("    %s", line))
                    .reduce("", (s, n) -> s + n + "\n");
                System.out.print(deviceString);

                if (device.isGPU()) {
                    gpuDevice = Optional.of(device);
                }
            }
            System.out.println();
            System.out.flush();
        }

        if (gpuDevice.isPresent()) {
            demoRunVADD(gpuDevice.get());
        } else {
            System.err.println("No OpenCL compatible GPU found.");
        }
    }
}
